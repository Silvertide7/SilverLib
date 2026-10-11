package net.silvertide.silverlib.feature.breakage;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.silvertide.silverlib.SilverLib;

import java.lang.reflect.Method;
import java.util.UUID;

@GameTestHolder(SilverLib.MOD_ID)
@PrefixGameTestTemplate(false)
public class BreakageGameTests {
    private static final String KEEP_BROKEN_NAME = "keep-broken-test";
    private static final int DAMAGE_FAR_BEYOND_REMAINING = 10_000;

    static {
        NeoForge.EVENT_BUS.addListener((ItemBreakingEvent event) -> {
            Component name = event.getStack().get(DataComponents.CUSTOM_NAME);
            if (name != null && KEEP_BROKEN_NAME.equals(name.getString())) event.keepBroken();
        });
    }

    @GameTest(template = "empty")
    public static void keptItemIsBrokenWithoutEnchantmentsUntilRepaired(GameTestHelper helper) {
        Holder<Enchantment> sharpness = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.set(DataComponents.CUSTOM_NAME, Component.literal(KEEP_BROKEN_NAME));
        sword.enchant(sharpness, 3);
        sword.setDamageValue(sword.getMaxDamage() - 1);

        sword.hurtAndBreak(DAMAGE_FAR_BEYOND_REMAINING, helper.getLevel(), (LivingEntity) null, item -> {});
        helper.assertTrue(!sword.isEmpty() && BrokenItems.isBroken(sword), "kept sword should be broken, not destroyed");
        helper.assertTrue(sword.getEnchantmentLevel(sharpness) == 0, "broken sword should have no effective enchantments");
        helper.assertTrue(sword.getAttributeModifiers().modifiers().isEmpty(), "broken sword should have no attribute modifiers");

        sword.hurtAndBreak(DAMAGE_FAR_BEYOND_REMAINING, helper.getLevel(), (LivingEntity) null, item -> {});
        helper.assertTrue(!sword.isEmpty(), "broken sword should take no further damage");

        sword.setDamageValue(0);
        helper.assertTrue(!BrokenItems.isBroken(sword) && !sword.has(BreakageComponents.KEPT_BROKEN.get()), "repair should clear the broken marker");
        helper.assertTrue(sword.getEnchantmentLevel(sharpness) == 3, "repaired sword should get its enchantments back");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void keptShieldStaysInHandAndStopsBlockingNextTick(GameTestHelper helper) throws ReflectiveOperationException {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "shield-test"));
        ItemStack shield = new ItemStack(Items.SHIELD);
        shield.set(DataComponents.CUSTOM_NAME, Component.literal(KEEP_BROKEN_NAME));
        shield.setDamageValue(shield.getMaxDamage() - 1);
        player.setItemInHand(InteractionHand.OFF_HAND, shield);
        player.startUsingItem(InteractionHand.OFF_HAND);

        Method hurtCurrentlyUsedShield = Player.class.getDeclaredMethod("hurtCurrentlyUsedShield", float.class);
        hurtCurrentlyUsedShield.setAccessible(true);
        hurtCurrentlyUsedShield.invoke(player, 50.0F);

        helper.assertTrue(player.getOffhandItem() == shield && BrokenItems.isBroken(shield), "kept shield should stay in the off hand, broken");
        NeoForge.EVENT_BUS.post(new EntityTickEvent.Pre(player));
        helper.assertTrue(!player.isUsingItem(), "player should stop blocking with a broken shield on the next tick");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void negativeDamageRepairsBrokenItem(GameTestHelper helper) {
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.set(DataComponents.CUSTOM_NAME, Component.literal(KEEP_BROKEN_NAME));
        sword.setDamageValue(sword.getMaxDamage() - 1);
        sword.hurtAndBreak(DAMAGE_FAR_BEYOND_REMAINING, helper.getLevel(), (LivingEntity) null, item -> {});
        helper.assertTrue(BrokenItems.isBroken(sword), "sword should be kept broken first");
        sword.hurtAndBreak(-10, helper.getLevel(), (LivingEntity) null, item -> {});
        helper.assertTrue(!BrokenItems.isBroken(sword), "negative damage should repair a broken item");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void unrequestedItemIsDestroyedLikeVanilla(GameTestHelper helper) {
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.setDamageValue(sword.getMaxDamage() - 1);
        sword.hurtAndBreak(DAMAGE_FAR_BEYOND_REMAINING, helper.getLevel(), (LivingEntity) null, item -> {});
        helper.assertTrue(sword.isEmpty(), "an item nobody keeps should be destroyed");
        helper.succeed();
    }
}
