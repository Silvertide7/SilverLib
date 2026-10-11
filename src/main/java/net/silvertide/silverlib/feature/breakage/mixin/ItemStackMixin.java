package net.silvertide.silverlib.feature.breakage.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.silvertide.silverlib.feature.breakage.BreakageHooks;
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

    @Inject(method = SILVER_LIB$HURT_AND_BREAK, at = @At("HEAD"), cancellable = true)
    private void silver_lib$brokenItemsTakeNoDamage(int damage, ServerLevel level, @Nullable LivingEntity entity, Consumer<Item> onBreak, CallbackInfo ci) {
        if (BreakageHooks.takesNoDamage((ItemStack) (Object) this, damage)) ci.cancel();
    }

    @Inject(method = SILVER_LIB$HURT_AND_BREAK, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"), cancellable = true)
    private void silver_lib$keepBrokenInsteadOfDestroyingIfRequested(int damage, ServerLevel level, @Nullable LivingEntity entity, Consumer<Item> onBreak, CallbackInfo ci) {
        if (BreakageHooks.keepBrokenInsteadOfDestroyingIfRequested((ItemStack) (Object) this, level, entity)) ci.cancel();
    }

    @Inject(method = "setDamageValue", at = @At("TAIL"))
    private void silver_lib$clearBrokenMarkerOnRepair(int damage, CallbackInfo ci) {
        BreakageHooks.clearMarkerIfRepaired((ItemStack) (Object) this);
    }
}
