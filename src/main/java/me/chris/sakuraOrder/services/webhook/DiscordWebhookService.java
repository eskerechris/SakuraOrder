package me.chris.sakuraOrder.services.webhook;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.services.webhook.WebhookService;
import me.chris.sakuraOrder.api.model.WebhookEvent;
import me.chris.sakuraOrder.model.WebhookPayload;
import me.chris.sakuraOrder.services.scheduler.SchedulerService;
import me.chris.sakuraOrder.util.NumberParser;
import me.chris.sakuraOrder.model.WebhookPayload.Field;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Sends marketplace events to Discord webhooks without blocking the caller.
 * <p>
 * Each distinct webhook URL gets its own queue, in-flight flag and pause window, so a rate
 * limit or a slow response on one webhook never delays messages bound for a different one.
 * Every drain cycle attempts at most one send per URL.
 */
public final class DiscordWebhookService implements WebhookService {

    private static final Gson GSON = new Gson();
    private static final int QUEUE_CAPACITY = 500;
    private static final int MAX_ATTEMPTS = 3;
    /**
     * 10 ticks = 500 ms: one message per URL per run stays under Discord's
     * approximately 5 requests per 2 seconds limit, per webhook.
     */
    private static final long DRAIN_PERIOD_TICKS = 10L;

    /** Represents a queued webhook message and its current delivery attempt. */
    private record Message(@NotNull URI url, @NotNull String body, int attempt) {}

    /**
     * Represents the Discord content generated for a webhook event before
     * serialization into a {@link WebhookPayload}.
     */
    private record Described(
            @NotNull String key,
            @NotNull String title,
            @Nullable String avatarUrl,
            @NotNull List<Field> fields
    ) {}

    /** Per-webhook-URL delivery state; its own queue, in-flight flag and pause window. */
    private static final class UrlState {
        final BlockingDeque<Message> queue = new LinkedBlockingDeque<>(QUEUE_CAPACITY);
        final AtomicBoolean inFlight = new AtomicBoolean();
        volatile long pausedUntil;
        volatile long lastOverflowLog;
    }

    private final OrderPlugin plugin;
    private final Logger logger;
    private final File file;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final Map<URI, UrlState> states = new ConcurrentHashMap<>();
    private final WrappedTask task;

    private volatile WebhookConfig config = WebhookConfig.disabled();

    public DiscordWebhookService(@NotNull OrderPlugin plugin, @NotNull SchedulerService scheduler) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.file = new File(plugin.getDataFolder(), "webhook.yml");
        reload();
        this.task = scheduler.runTimerAsync(this::drain, DRAIN_PERIOD_TICKS, DRAIN_PERIOD_TICKS);
    }

    @Override
    public void reload() {
        if (!file.exists()) {
            plugin.saveResource("webhook.yml", false);
        }
        this.config = WebhookConfig.load(YamlConfiguration.loadConfiguration(file), logger);
    }

    @Override
    public void send(@NotNull WebhookEvent event) {
        WebhookConfig current = config;
        if (!current.enabled()) {
            return;
        }
        try {
            Described described = describe(event);
            WebhookConfig.EventSettings settings = current.event(described.key());
            if (settings == null || settings.url() == null) {
                return;
            }
            String body = GSON.toJson(WebhookPayload.of(
                    described.title(),
                    settings.color(),
                    described.avatarUrl(),
                    described.fields()));

            UrlState state = states.computeIfAbsent(settings.url(), u -> new UrlState());
            if (!state.queue.offer(new Message(settings.url(), body, 0))) {
                long now = System.currentTimeMillis();
                if (now - state.lastOverflowLog > 30_000L) {
                    state.lastOverflowLog = now;
                    logger.warning("(Webhook) Queue full for one webhook, dropping events until Discord catches up.");
                }
            }
        } catch (RuntimeException ex) {
            logger.warning("(Webhook) Failed to enqueue event: " + ex.getClass().getSimpleName());
        }
    }

    @Override
    public void close() {
        task.cancel();
    }

    /**
     * Attempts one send per known webhook URL.
     * <p>
     * Each URL is independent, a pause or an in-flight request on one never affects another.
     */
    private void drain() {
        for (Map.Entry<URI, UrlState> entry : states.entrySet()) {
            drainOne(entry.getKey(), entry.getValue());
        }
    }

    private void drainOne(@NotNull URI url, @NotNull UrlState state) {
        if (System.currentTimeMillis() < state.pausedUntil || !state.inFlight.compareAndSet(false, true)) {
            return;
        }
        Message message = state.queue.poll();
        if (message == null) {
            state.inFlight.set(false);
            return;
        }
        try {
            http.sendAsync(request(message), HttpResponse.BodyHandlers.ofString())
                    .whenComplete((response, error) -> {
                        try {
                            handle(state, message, response, error);
                        } catch (RuntimeException ex) {
                            logger.warning("(Webhook) Unexpected error: " + ex.getClass().getSimpleName());
                        } finally {
                            state.inFlight.set(false);
                        }
                    });
        } catch (RuntimeException ex) {
            state.inFlight.set(false);
            logger.warning("(Webhook) Unexpected error: " + ex.getClass().getSimpleName());
        }
    }

    private void handle(
            @NotNull UrlState state,
            @NotNull Message message,
            HttpResponse<String> response,
            Throwable error
    ) {
        if (error != null) {
            retry(state, message, 1_000L * (message.attempt() + 1));
            return;
        }
        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return;
        }
        if (status == 429) {
            retry(state, message, retryAfterMillis(response));
        } else if (status >= 500) {
            retry(state, message, 1_000L * (message.attempt() + 1));
        } else {
            // 4xx other than 429: bad/revoked webhook, retrying is pointless
            logger.warning("(Webhook) Discord rejected a message (HTTP " + status + ").");
        }
    }

    private void retry(@NotNull UrlState state, @NotNull Message message, long delayMillis) {
        if (message.attempt() + 1 >= MAX_ATTEMPTS) {
            logger.warning("(Webhook) Giving up on a message after " + MAX_ATTEMPTS + " attempts.");
            return;
        }
        state.pausedUntil = System.currentTimeMillis() + delayMillis;
        // Back at the head of this URL's own queue so event order is preserved.
        if (!state.queue.offerFirst(new Message(message.url(), message.body(), message.attempt() + 1))) {
            logger.warning("(Webhook) Failed to requeue a webhook message.");
        }
    }

    private static @NotNull HttpRequest request(@NotNull Message message) {
        return HttpRequest.newBuilder(message.url())
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(message.body(), StandardCharsets.UTF_8))
                .build();
    }

    private static long retryAfterMillis(@NotNull HttpResponse<String> response) {
        try {
            JsonObject json = GSON.fromJson(response.body(), JsonObject.class);
            if (json != null && json.has("retry_after")) {
                return Math.clamp((long) (json.get("retry_after").getAsDouble() * 1000), 500L, 30_000L);
            }
        } catch (RuntimeException ignored) {
            // fall through to the header / default
        }
        return response.headers().firstValue("Retry-After")
                .map(v -> {
                    try {
                        return Math.clamp((long) (Double.parseDouble(v) * 1000), 500L, 30_000L);
                    } catch (NumberFormatException ex) {
                        return 2_000L;
                    }
                })
                .orElse(2_000L);
    }

    private static @NotNull Described describe(@NotNull WebhookEvent event) {
        return switch (event) {
            case WebhookEvent.OrderCreated e -> new Described(
                    "order-create",
                    e.buyerName(),
                    avatarUrl(e.buyerId().toString()),
                    List.of(
                            new Field("Event", "Created"),
                            new Field("Item", md(e.itemName())),
                            new Field("Amount", NumberParser.formatNumber(e.amount())),
                            new Field("Price per item", money(e.pricePerItem())),
                            new Field("Order", id(e.orderId()))));
            case WebhookEvent.OrderDelivered e -> new Described(
                    "order-delivery",
                    e.buyerName(),
                    avatarUrl(e.buyerId().toString()),
                    List.of(
                            new Field("Event", "Delivered"),
                            new Field("Seller", md(e.sellerName())),
                            new Field("Item", md(e.itemName())),
                            new Field("Delivered", NumberParser.formatNumber(e.deliveredAmount())),
                            new Field("Earned", money(e.earned())),
                            new Field("Order", id(e.orderId()))));
            case WebhookEvent.OrderCollected e -> new Described(
                    "order-collect",
                    e.collectorName(),
                    avatarUrl(e.collectorId().toString()),
                    List.of(
                            new Field("Event", "Collected"),
                            new Field("Item", md(e.itemName())),
                            new Field("Collected", NumberParser.formatNumber(e.collectedAmount())),
                            new Field("Order", id(e.orderId()))));
            case WebhookEvent.OrderCancelled e -> new Described(
                    "order-cancel",
                    e.buyerName(),
                    avatarUrl(e.buyerId().toString()),
                    List.of(
                            new Field("Event", "Cancelled by " + md(e.cancelledByName())),
                            new Field("Item", md(e.itemName())),
                            new Field("Delivered", NumberParser.formatNumber(e.delivered()) + "/" + NumberParser.formatNumber(e.amount())),
                            new Field("Refunded", money(e.refunded())),
                            new Field("Order", id(e.orderId()))));
        };
    }

    /** Returns the URL of the player's 100px avatar. */
    private static @NotNull String avatarUrl(@NotNull String player) {
        return "https://mc-heads.net/avatar/" + player + "/100";
    }

    /** Formats an order identifier as inline Discord code. */
    private static @NotNull String id(@NotNull UUID orderId) {
        return "`" + orderId.toString().substring(0, 8) + "`";
    }

    /** Formats a monetary value for display in a Discord embed. */
    private static @NotNull String money(@NotNull BigDecimal value) {
        return NumberParser.formatNumber(value);
    }

    /**
     * Escapes Discord markdown characters so user-provided names such as
     * {@code My_Name_} are not interpreted as formatting.
     */
    private static @NotNull String md(@NotNull String raw) {
        StringBuilder sb = new StringBuilder(raw.length() + 8);
        for (char c : raw.toCharArray()) {
            if ("\\*_~`|>".indexOf(c) >= 0) {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.isEmpty() ? "-" : sb.toString();
    }
}