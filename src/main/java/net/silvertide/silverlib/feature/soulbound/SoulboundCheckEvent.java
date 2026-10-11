package net.silvertide.silverlib.feature.soulbound;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

public class SoulboundCheckEvent extends Event {
    private final Player player;
    private final ItemStack stack;
    private boolean kept;

    public SoulboundCheckEvent(Player player, ItemStack stack) {
        this.player = player;
        this.stack = stack;
    }

    public Player getPlayer() {
        return player;
    }

    public ItemStack getStack() {
        return stack;
    }

    public void keep() {
        kept = true;
    }

    public boolean isKept() {
        return kept;
    }
}
