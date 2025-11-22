package dev.doctor4t.trainmurdermystery.event;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.item.ItemStack;

import static net.fabricmc.fabric.api.event.EventFactory.createArrayBacked;

public interface ShouldDropOnDeath {

    Event<ShouldDropOnDeath> EVENT = createArrayBacked(ShouldDropOnDeath.class, listeners -> stack -> {
        for (ShouldDropOnDeath listener : listeners) {
            if (listener.shouldDrop(stack)) {
                return true;
            }
        }
        return false;
    });

    boolean shouldDrop(ItemStack stack);
}
