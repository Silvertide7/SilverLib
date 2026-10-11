package net.silvertide.silverlib.feature.breakage.mixin;

import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.silvertide.silverlib.feature.breakage.BreakageHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DispenserBlock.class)
public abstract class DispenserBlockMixin {

    @Inject(method = "getDispenseMethod", at = @At("RETURN"), cancellable = true)
    private void silver_lib$ejectBrokenItemsInsteadOfUsingThem(Level level, ItemStack stack, CallbackInfoReturnable<DispenseItemBehavior> cir) {
        cir.setReturnValue(BreakageHooks.ejectBrokenItemsInsteadOf(stack, cir.getReturnValue()));
    }
}
