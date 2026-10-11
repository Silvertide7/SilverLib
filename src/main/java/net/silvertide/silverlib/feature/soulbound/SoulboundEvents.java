package net.silvertide.silverlib.feature.soulbound;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.silvertide.silverlib.SilverLib;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = SilverLib.MOD_ID)
final class SoulboundEvents {

    private SoulboundEvents() {}

    @SubscribeEvent
    static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        List<StashedItem> stash = player.getData(SoulboundAttachments.SOULBOUND_STASH);
        if (stash.isEmpty()) return;
        player.removeData(SoulboundAttachments.SOULBOUND_STASH);
        Inventory inventory = player.getInventory();
        List<ItemStack> displacedStacks = new ArrayList<>();
        for (StashedItem stashedItem : stash) {
            if (isEmptySlot(inventory, stashedItem.inventorySlot())) {
                inventory.setItem(stashedItem.inventorySlot(), stashedItem.stack());
            } else {
                displacedStacks.add(stashedItem.stack());
            }
        }
        displacedStacks.forEach(stack -> addToInventoryOrDropAtFeet(player, stack));
    }

    private static boolean isEmptySlot(Inventory inventory, int inventorySlot) {
        return inventorySlot >= 0 && inventorySlot < inventory.getContainerSize() && inventory.getItem(inventorySlot).isEmpty();
    }

    private static void addToInventoryOrDropAtFeet(ServerPlayer player, ItemStack stack) {
        Inventory inventory = player.getInventory();
        if (inventory.getFreeSlot() >= 0) inventory.add(stack);
        if (!stack.isEmpty()) player.drop(stack, false, false);
    }
}
