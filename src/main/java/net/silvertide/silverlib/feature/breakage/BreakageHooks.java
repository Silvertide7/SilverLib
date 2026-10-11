package net.silvertide.silverlib.feature.breakage;

import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public final class BreakageHooks {
    private static final float BREAK_SOUND_VOLUME = 0.8F;
    private static final float BREAK_SOUND_MIN_PITCH = 0.8F;
    private static final float BREAK_SOUND_PITCH_RANGE = 0.4F;
    private static final DispenseItemBehavior EJECT_BEHAVIOR = new DefaultDispenseItemBehavior();

    private BreakageHooks() {}

    public static boolean takesNoDamage(ItemStack stack) {
        return BrokenItems.isBroken(stack);
    }

    public static boolean keepBrokenInsteadOfDestroyingIfRequested(ItemStack stack, ServerLevel level, @Nullable LivingEntity entity) {
        if (!NeoForge.EVENT_BUS.post(new ItemBreakingEvent(stack, level, entity)).isKeptBroken() || stack.isEmpty()) return false;
        if (entity != null) releaseFromEntity(stack, entity);
        stack.set(BreakageComponents.KEPT_BROKEN.get(), Unit.INSTANCE);
        stack.setDamageValue(stack.getMaxDamage());
        if (entity != null) playBreakSound(level, entity);
        NeoForge.EVENT_BUS.post(new ItemKeptBrokenEvent(stack, level, entity));
        return true;
    }

    public static void clearMarkerIfRepaired(ItemStack stack) {
        if (stack.has(BreakageComponents.KEPT_BROKEN.get()) && stack.getDamageValue() < stack.getMaxDamage()) {
            stack.remove(BreakageComponents.KEPT_BROKEN.get());
        }
    }

    public static DispenseItemBehavior ejectBrokenItemsInsteadOf(ItemStack stack, DispenseItemBehavior behavior) {
        return BrokenItems.isBroken(stack) ? EJECT_BEHAVIOR : behavior;
    }

    private static void releaseFromEntity(ItemStack stack, LivingEntity entity) {
        if (entity.getUseItem() == stack) entity.stopUsingItem();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (entity.getItemBySlot(slot) == stack) EnchantmentHelper.stopLocationBasedEffects(stack, entity, slot);
        }
    }

    private static void playBreakSound(ServerLevel level, LivingEntity entity) {
        float pitch = BREAK_SOUND_MIN_PITCH + level.getRandom().nextFloat() * BREAK_SOUND_PITCH_RANGE;
        level.playSound(null, entity.blockPosition(), SoundEvents.ITEM_BREAK, entity.getSoundSource(), BREAK_SOUND_VOLUME, pitch);
    }
}
