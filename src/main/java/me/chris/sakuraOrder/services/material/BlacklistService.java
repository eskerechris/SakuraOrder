package me.chris.sakuraOrder.services.material;

import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.services.material.MaterialBlacklistService;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * Manages blacklisted materials and material variant rules.
 */
public class BlacklistService implements MaterialBlacklistService {

    private final OrderPlugin plugin;
    private final File file;

    private volatile Set<Material> blacklisted = Collections.emptySet();
    private volatile List<VariantRule> variantRules = Collections.emptyList();

    public BlacklistService(@NotNull OrderPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "blacklist.yml");
        reload();
    }

    /**
     * Reloads the blacklist configuration from disk.
     *
     * <p>The parsed configuration is published only after the reload has
     * completed, allowing readers to continue using the previous state while
     * the new configuration is being built.</p>
     */
    @Override
    public void reload() {
        if (!file.exists()) {
            plugin.saveResource("blacklist.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<String> names = config.getStringList("blacklisted-materials");

        Set<Material> exact = new HashSet<>();
        List<Pattern> materialPatterns = new ArrayList<>();
        List<VariantRule> variants = new ArrayList<>();

        for (String raw : names) {
            String name = raw.trim();
            if (name.isEmpty()) continue;

            String[] parts = name.split(":", 3);
            String materialPart = parts[0].trim();

            if (parts.length == 1) {
                if (materialPart.contains("*")) {
                    materialPatterns.add(wildcardToPattern(materialPart));
                } else {
                    Material material = Material.getMaterial(materialPart.toUpperCase(Locale.ROOT));
                    if (material == null) {
                        plugin.getLogger().log(Level.WARNING,
                                "blacklist.yml: unknown material '" + materialPart + "', ignored");
                        continue;
                    }
                    exact.add(material);
                }
                continue;
            }

            String variantPart = parts[1].trim();
            Integer level = null;
            if (parts.length == 3 && !parts[2].isBlank()) {
                try {
                    level = Integer.parseInt(parts[2].trim());
                } catch (NumberFormatException e) {
                    plugin.getLogger().log(Level.WARNING,
                            "blacklist.yml: invalid level in '" + name + "', ignored");
                    continue;
                }
            }

            variants.add(new VariantRule(
                    wildcardToPattern(materialPart),
                    wildcardToPattern(variantPart),
                    level
            ));
        }

        Set<Material> expanded = new HashSet<>(exact);
        for (Material material : Material.values()) {
            if (material.isLegacy()) continue;
            for (Pattern pattern : materialPatterns) {
                if (pattern.matcher(material.name()).matches()) {
                    expanded.add(material);
                    break;
                }
            }
        }

        this.blacklisted = Collections.unmodifiableSet(expanded);
        this.variantRules = List.copyOf(variants);
    }

    @Override
    public boolean isBlacklisted(@NotNull Material material) {
        return blacklisted.contains(material);
    }

    @Override
    public boolean isVariantBlacklisted(
            @NotNull Material material,
            @NotNull String variantId,
            @Nullable Integer level
    ) {
        if (isBlacklisted(material)) return true;

        String upperVariant = variantId.toUpperCase(Locale.ROOT);
        for (VariantRule rule : variantRules) {
            if (!rule.materialPattern().matcher(material.name()).matches()) continue;
            if (!rule.variantPattern().matcher(upperVariant).matches()) continue;
            if (rule.level() != null && level != null && !rule.level().equals(level)) continue;

            return true;
        }
        return false;
    }

    @Override
    @NotNull
    public Set<Material> getBlacklisted() {
        return blacklisted;
    }

    private static Pattern wildcardToPattern(@NotNull String wildcard) {
        String[] parts = wildcard.toUpperCase(Locale.ROOT).split("\\*", -1);
        StringBuilder regex = new StringBuilder("^");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) regex.append(".*");
            if (!parts[i].isEmpty()) {
                regex.append(Pattern.quote(parts[i]));
            }
        }
        regex.append("$");
        return Pattern.compile(regex.toString());
    }

    private record VariantRule(
            @NotNull Pattern materialPattern,
            @NotNull Pattern variantPattern,
            @Nullable Integer level
    ) {}
}