package net.silvertide.silverlib.feature.breakage;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.Nullable;

public class ItemBreakingEvent extends Event {
    private final ItemStack stack;
    private final ServerLevel level;
    @Nullable
    private final LivingEntity entity;
    private boolean keptBroken;

    public ItemBreakingEvent(ItemStack stack, ServerLevel level, @Nullable LivingEntity entity) {
        this.stack = stack;
        this.level = level;
        this.entity = entity;
    }

    public ItemStack getStack() {
        return stack;
    }

    public ServerLevel getLevel() {
        return level;
    }

    @Nullable
    public LivingEntity getEntity() {
        return entity;
    }

    public void keepBroken() {
        keptBroken = true;
    }

    public boolean isKeptBroken() {
        return keptBroken;
    }
}
