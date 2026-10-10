package net.silvertide.silverlib.feature.breakage.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.silvertide.silverlib.feature.breakage.BreakageComponents;
import net.silvertide.silverlib.feature.breakage.BrokenItems;
import net.silvertide.silverlib.feature.breakage.ItemBreakingEvent;
import net.silvertide.silverlib.feature.breakage.ItemKeptBrokenEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Unique
    private static final String SILVER_LIB$HURT_AND_BREAK = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V";
    @Unique
    private static final float SILVER_LIB$BREAK_SOUND_VOLUME = 0.8F;
    @Unique
    private static final float SILVER_LIB$BREAK_SOUND_MIN_PITCH = 0.8F;
    @Unique
    private static final float SILVER_LIB$BREAK_SOUND_PITCH_RANGE = 0.4F;

    @Inject(method = SILVER_LIB$HURT_AND_BREAK, at = @At("HEAD"), cancellable = true)
    private void silver_lib$brokenItemsTakeNoDamage(int damage, ServerLevel level, @Nullable LivingEntity entity, Consumer<Item> onBreak, CallbackInfo ci) {
        if (BrokenItems.isBroken((ItemStack) (Object) this)) ci.cancel();
    }

    @Inject(method = SILVER_LIB$HURT_AND_BREAK, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"), cancellable = true)
    private void silver_lib$keepBrokenInsteadOfDestroyingIfRequested(int damage, ServerLevel level, @Nullable LivingEntity entity, Consumer<Item> onBreak, CallbackInfo ci) {
        ItemStack stack = (ItemStack) (Object) this;
        if (!NeoForge.EVENT_BUS.post(new ItemBreakingEvent(stack, level, entity)).isKeptBroken()) return;
        stack.set(BreakageComponents.KEPT_BROKEN.get(), Unit.INSTANCE);
        stack.setDamageValue(stack.getMaxDamage());
        if (entity != null) silver_lib$playBreakSound(level, entity);
        NeoForge.EVENT_BUS.post(new ItemKeptBrokenEvent(stack, level, entity));
        ci.cancel();
    }

    @Unique
    private static void silver_lib$playBreakSound(ServerLevel level, LivingEntity entity) {
        float pitch = SILVER_LIB$BREAK_SOUND_MIN_PITCH + level.getRandom().nextFloat() * SILVER_LIB$BREAK_SOUND_PITCH_RANGE;
        level.playSound(null, entity.blockPosition(), SoundEvents.ITEM_BREAK, entity.getSoundSource(), SILVER_LIB$BREAK_SOUND_VOLUME, pitch);
    }
}
