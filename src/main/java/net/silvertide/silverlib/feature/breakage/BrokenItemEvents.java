package net.silvertide.silverlib.feature.breakage;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.enchanting.GetEnchantmentLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
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

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onEnchantmentLevel(GetEnchantmentLevelEvent event) {
        if (BrokenItems.isBroken(event.getStack())) event.getEnchantments().removeIf(enchantment -> !repairsWithExperience(enchantment));
    }

    @SubscribeEvent
    static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (!isCreativePlayer(event.getEntity()) && BrokenItems.isBroken(event.getItem())) event.setCanceled(true);
    }

    @SubscribeEvent
    static void onEntityTick(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof LivingEntity living && living.isUsingItem() && !isCreativePlayer(living) && BrokenItems.isBroken(living.getUseItem())) {
            living.stopUsingItem();
        }
    }

    private static boolean isCreativePlayer(LivingEntity entity) {
        return entity instanceof Player player && player.hasInfiniteMaterials();
    }

    private static boolean repairsWithExperience(Holder<Enchantment> enchantment) {
        return enchantment.value().effects().has(EnchantmentEffectComponents.REPAIR_WITH_XP);
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
