package net.silvertide.silverlib.feature.soulbound;

import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.silvertide.silverlib.SilverLib;

@GameTestHolder(SilverLib.MOD_ID)
@PrefixGameTestTemplate(false)
public class SoulboundGameTests {
    private static final String SOULBOUND_NAME = "soulbound-test";

    static {
        NeoForge.EVENT_BUS.addListener((SoulboundCheckEvent event) -> {
            Component name = event.getStack().get(DataComponents.CUSTOM_NAME);
            if (name != null && SOULBOUND_NAME.equals(name.getString())) event.keep();
        });
    }

    private static ItemStack soulboundStack(Item item) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(SOULBOUND_NAME));
        return stack;
    }

    @GameTest(template = "empty")
    @SuppressWarnings("removal")
    public static void soulboundItemsSurviveDeathAndReturn(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getInventory().setItem(0, soulboundStack(Items.DIAMOND_PICKAXE));
        player.getInventory().setItem(5, soulboundStack(Items.DIAMOND_SWORD));
        player.getInventory().setItem(6, new ItemStack(Items.IRON_SWORD));
        player.getInventory().setItem(39, soulboundStack(Items.DIAMOND_HELMET));

        player.die(player.damageSources().generic());

        helper.assertTrue(player.getInventory().getItem(5).isEmpty(), "soulbound sword should leave the dead inventory");
        helper.assertTrue(player.getInventory().getItem(6).isEmpty(), "plain sword should drop");
        helper.assertTrue(player.getData(SoulboundAttachments.SOULBOUND_STASH).size() == 3, "stash should hold 3, got " + player.getData(SoulboundAttachments.SOULBOUND_STASH).size());

        ServerPlayer respawned = helper.makeMockServerPlayerInLevel();
        respawned.copyAttachmentsFrom(player, true);
        respawned.getInventory().setItem(5, new ItemStack(Items.DIRT));
        NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(respawned, false));

        helper.assertTrue(respawned.getInventory().getItem(39).is(Items.DIAMOND_HELMET), "helmet should return to its armor slot");
        helper.assertTrue(respawned.getInventory().getItem(0).is(Items.DIAMOND_PICKAXE), "pickaxe should keep its slot even though the sword was displaced");
        helper.assertTrue(respawned.getInventory().getItem(5).is(Items.DIRT), "occupied slot should be left alone");
        helper.assertTrue(respawned.getInventory().items.stream().anyMatch(stack -> stack.is(Items.DIAMOND_SWORD)), "sword should overflow into the inventory");
        helper.assertTrue(respawned.getData(SoulboundAttachments.SOULBOUND_STASH).isEmpty(), "stash should be cleared");
        helper.succeed();
    }
}
