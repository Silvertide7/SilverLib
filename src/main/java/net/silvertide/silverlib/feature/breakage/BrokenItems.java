package net.silvertide.silverlib.feature.breakage;

import net.minecraft.world.item.ItemStack;

public final class BrokenItems {

    private BrokenItems() {}

    public static boolean isBroken(ItemStack stack) {
        return stack.has(BreakageComponents.KEPT_BROKEN.get())
                && stack.isDamageableItem()
                && stack.getDamageValue() >= stack.getMaxDamage();
    }
}
