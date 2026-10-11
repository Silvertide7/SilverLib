package net.silvertide.silverlib.feature.soulbound;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;

public final class SoulboundItems {

    private SoulboundItems() {}

    public static boolean isSoulbound(Player player, ItemStack stack) {
        return !stack.isEmpty() && NeoForge.EVENT_BUS.post(new SoulboundCheckEvent(player, stack)).isKept();
    }
}
