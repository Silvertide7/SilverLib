package net.silvertide.silverlib.feature.soulbound;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;

@ApiStatus.Internal
public final class SoulboundHooks {

    private SoulboundHooks() {}

    public static void stashSoulboundItemsBeforeDeathDrops(Player player) {
        if (player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
        Inventory inventory = player.getInventory();
        List<StashedItem> stash = new ArrayList<>(player.getData(SoulboundAttachments.SOULBOUND_STASH));
        for (int inventorySlot = 0; inventorySlot < inventory.getContainerSize(); inventorySlot++) {
            ItemStack stack = inventory.getItem(inventorySlot);
            if (SoulboundItems.isSoulbound(player, stack)) {
                stash.add(new StashedItem(inventorySlot, stack));
                inventory.setItem(inventorySlot, ItemStack.EMPTY);
            }
        }
        if (!stash.isEmpty()) player.setData(SoulboundAttachments.SOULBOUND_STASH, List.copyOf(stash));
    }
}
