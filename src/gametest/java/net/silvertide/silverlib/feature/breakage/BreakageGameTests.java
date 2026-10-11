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
import net.neoforged.neoforge.event.entity.player.PlayerDestroyItemEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.silvertide.silverlib.SilverLib;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@GameTestHolder(SilverLib.MOD_ID)
@PrefixGameTestTemplate(false)
public class BreakageGameTests {
    private static final String KEEP_BROKEN_NAME = "keep-broken-test";
    private static final int DAMAGE_FAR_BEYOND_REMAINING = 10_000;
    private static final int FULL_BOW_CHARGE_TICKS = 20;

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
    public static void keptBowReleaseDoesNotPostDestroyEvent(GameTestHelper helper) throws ReflectiveOperationException {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "bow-test"));
        ItemStack bow = new ItemStack(Items.BOW);
        bow.set(DataComponents.CUSTOM_NAME, Component.literal(KEEP_BROKEN_NAME));
        bow.setDamageValue(bow.getMaxDamage() - 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, bow);
        player.getInventory().add(new ItemStack(Items.ARROW, 16));
        player.startUsingItem(InteractionHand.MAIN_HAND);
        Field useItemRemaining = LivingEntity.class.getDeclaredField("useItemRemaining");
        useItemRemaining.setAccessible(true);
        useItemRemaining.setInt(player, bow.getUseDuration(player) - FULL_BOW_CHARGE_TICKS);

        List<ItemStack> destroyed = new ArrayList<>();
        Consumer<PlayerDestroyItemEvent> recordDestroyed = event -> destroyed.add(event.getOriginal());
        NeoForge.EVENT_BUS.addListener(recordDestroyed);
        try {
            player.releaseUsingItem();
        } finally {
            NeoForge.EVENT_BUS.unregister(recordDestroyed);
        }
        helper.assertTrue(player.getMainHandItem() == bow && BrokenItems.isBroken(bow), "kept bow should stay in hand, broken");
        helper.assertTrue(destroyed.isEmpty(), "releasing a kept bow must not post PlayerDestroyItemEvent");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void zeroDamageDoesNotReBreakAndDirectRepairClearsMarker(GameTestHelper helper) {
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.set(DataComponents.CUSTOM_NAME, Component.literal(KEEP_BROKEN_NAME));
        sword.setDamageValue(sword.getMaxDamage() - 1);
        sword.hurtAndBreak(DAMAGE_FAR_BEYOND_REMAINING, helper.getLevel(), (LivingEntity) null, item -> {});

        List<ItemStack> breaking = new ArrayList<>();
        Consumer<ItemBreakingEvent> recordBreaking = event -> breaking.add(event.getStack());
        NeoForge.EVENT_BUS.addListener(recordBreaking);
        try {
            sword.hurtAndBreak(0, helper.getLevel(), (LivingEntity) null, item -> {});
        } finally {
            NeoForge.EVENT_BUS.unregister(recordBreaking);
        }
        helper.assertTrue(breaking.isEmpty(), "zero damage on a broken item must not post ItemBreakingEvent again");

        sword.set(DataComponents.DAMAGE, 0);
        helper.assertTrue(sword.has(BreakageComponents.KEPT_BROKEN.get()), "a direct damage write leaves the marker until the next hurtAndBreak");
        sword.hurtAndBreak(1, helper.getLevel(), (LivingEntity) null, item -> {});
        helper.assertTrue(!sword.has(BreakageComponents.KEPT_BROKEN.get()), "hurtAndBreak should clear a stale marker");
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
