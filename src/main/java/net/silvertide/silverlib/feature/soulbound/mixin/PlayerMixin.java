package net.silvertide.silverlib.feature.soulbound.mixin;

import net.minecraft.world.entity.player.Player;
import net.silvertide.silverlib.feature.soulbound.SoulboundHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(method = "dropEquipment", at = @At("HEAD"))
    private void silver_lib$stashSoulboundItemsBeforeDeathDrops(CallbackInfo ci) {
        SoulboundHooks.stashSoulboundItemsBeforeDeathDrops((Player) (Object) this);
    }
}
