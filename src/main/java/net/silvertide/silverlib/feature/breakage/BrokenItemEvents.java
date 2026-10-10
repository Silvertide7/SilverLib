package net.silvertide.silverlib.feature.breakage;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.silvertide.silverlib.SilverLib;

@EventBusSubscriber(modid = SilverLib.MOD_ID)
final class BrokenItemEvents {

    private BrokenItemEvents() {}

    private static boolean blocksUse(Player player, ItemStack stack) {
        return !player.hasInfiniteMaterials() && BrokenItems.isBroken(stack);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onItemAttributes(ItemAttributeModifierEvent event) {
        if (BrokenItems.isBroken(event.getItemStack())) event.clearModifiers();
    }

    @SubscribeEvent
    static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (BrokenItems.isBroken(event.getEntity().getMainHandItem())) event.setNewSpeed(0.0F);
    }

    @SubscribeEvent
    static void onAttack(AttackEntityEvent event) {
        if (blocksUse(event.getEntity(), event.getEntity().getMainHandItem())) event.setCanceled(true);
    }

    @SubscribeEvent
    static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (blocksUse(event.getEntity(), event.getItemStack())) event.setCanceled(true);
    }

    @SubscribeEvent
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (blocksUse(event.getEntity(), event.getItemStack())) event.setUseItem(TriState.FALSE);
    }

    @SubscribeEvent
    static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (blocksUse(event.getEntity(), event.getItemStack())) event.setCanceled(true);
    }

    @SubscribeEvent
    static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (blocksUse(event.getEntity(), event.getItemStack())) event.setCanceled(true);
    }

    @SubscribeEvent
    static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (blocksUse(event.getEntity(), event.getItemStack())) event.setCanceled(true);
    }
}
