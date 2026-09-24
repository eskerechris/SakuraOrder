package me.chris.sakuraOrder.util;

import me.chris.sakuraOrder.api.model.IOrder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides shared calculations for order item delivery and deposit handling.
 */
public final class DeliveryCalculator {

    private DeliveryCalculator() {}

    public record ValiditySplit(
            @NotNull List<ItemStack> validStacks,
            @NotNull List<ItemStack> invalidStacks
    ) {
        public int validAmount() {
            return validStacks.stream().mapToInt(ItemStack::getAmount).sum();
        }

        public int invalidAmount() {
            return invalidStacks.stream().mapToInt(ItemStack::getAmount).sum();
        }
    }

    @NotNull
    public static ValiditySplit splitByValidity(
            @NotNull IOrder order,
            @NotNull List<ItemStack> providedStacks
    ) {
        List<ItemStack> validStacks = new ArrayList<>();
        List<ItemStack> invalidStacks = new ArrayList<>();

        for (ItemStack stack : providedStacks) {
            if (order.getItemStack().isSimilar(stack)) {
                validStacks.add(stack);
            } else {
                invalidStacks.add(stack);
            }
        }

        return new ValiditySplit(validStacks, invalidStacks);
    }

    /**
     * Returns the amount of matching items the order can currently accept.
     */
    public static int deliverableAmount(@NotNull IOrder order, int validAmount) {
        long residual = Math.max(0L, order.getAmount() - order.getDelivered());
        return (int) Math.min(validAmount, residual);
    }

    public record AmountSplit(
            @NotNull List<ItemStack> taken,
            @NotNull List<ItemStack> remainder
    ) {}

    @NotNull
    public static AmountSplit takeUpTo(@NotNull List<ItemStack> stacks, int amount) {
        List<ItemStack> taken = new ArrayList<>();
        List<ItemStack> remainder = new ArrayList<>();
        int remaining = amount;

        for (ItemStack stack : stacks) {
            if (remaining <= 0) {
                remainder.add(stack);
                continue;
            }

            int stackAmount = stack.getAmount();
            if (stackAmount <= remaining) {
                taken.add(stack);
                remaining -= stackAmount;
            } else {
                ItemStack takenPart = stack.clone();
                takenPart.setAmount(remaining);
                taken.add(takenPart);

                ItemStack remainderPart = stack.clone();
                remainderPart.setAmount(stackAmount - remaining);
                remainder.add(remainderPart);

                remaining = 0;
            }
        }

        return new AmountSplit(taken, remainder);
    }

    public record DepositResolution(
            @NotNull List<ItemStack> toDeliver,
            @NotNull List<ItemStack> toReturnNow
    ) {}

    /**
     * Resolves a raw deposit into items that can proceed to confirmation and items
     * that must be returned immediately.
     *
     * <p>When the requested item is not a shulker box, matching loose items are
     * consumed first, followed by matching contents extracted from provided shulkers.
     * Each processed shulker is rebuilt with its remaining contents.</p>
     *
     * <p>When the requested item is itself a shulker box, provided shulkers are
     * treated as normal items and are never opened.</p>
     */
    @NotNull
    public static DepositResolution resolveDeposit(
            @NotNull IOrder order,
            @NotNull List<ItemStack> providedStacks
    ) {
        if (ShulkerUtil.isShulkerBox(order.getItemStack().getType())) {
            ValiditySplit split = splitByValidity(order, providedStacks);
            int deliverable = deliverableAmount(order, split.validAmount());
            AmountSplit amountSplit = takeUpTo(split.validStacks(), deliverable);

            List<ItemStack> toReturnNow = new ArrayList<>(split.invalidStacks());
            toReturnNow.addAll(amountSplit.remainder());

            return new DepositResolution(amountSplit.taken(), toReturnNow);
        }

        List<ItemStack> looseValid = new ArrayList<>();
        List<ItemStack> shulkerCandidates = new ArrayList<>();
        List<ItemStack> toReturnNow = new ArrayList<>();

        for (ItemStack stack : providedStacks) {
            if (order.getItemStack().isSimilar(stack)) {
                looseValid.add(stack);
            } else if (ShulkerUtil.isShulkerBox(stack.getType())) {
                shulkerCandidates.add(stack);
            } else {
                toReturnNow.add(stack);
            }
        }

        long residual = Math.max(0L, order.getAmount() - order.getDelivered());

        int looseAmount = looseValid.stream().mapToInt(ItemStack::getAmount).sum();
        int deliverableFromLoose = (int) Math.min(looseAmount, residual);
        AmountSplit looseSplit = takeUpTo(looseValid, deliverableFromLoose);
        toReturnNow.addAll(looseSplit.remainder());

        List<ItemStack> toDeliver = new ArrayList<>(looseSplit.taken());
        long remainingResidual = residual - deliverableFromLoose;

        for (ItemStack shulker : shulkerCandidates) {
            if (remainingResidual <= 0) {
                toReturnNow.add(shulker);
                continue;
            }

            int cap = (int) Math.min(remainingResidual, Integer.MAX_VALUE);
            ShulkerUtil.Extraction extraction =
                    ShulkerUtil.extract(shulker, order.getItemStack(), cap);

            if (extraction.extracted().isEmpty()) {
                toReturnNow.add(shulker);
                continue;
            }

            toDeliver.addAll(extraction.extracted());
            toReturnNow.add(extraction.rebuiltShulker());
            remainingResidual -= extraction.extracted()
                    .stream()
                    .mapToInt(ItemStack::getAmount)
                    .sum();
        }

        return new DepositResolution(toDeliver, toReturnNow);
    }
}