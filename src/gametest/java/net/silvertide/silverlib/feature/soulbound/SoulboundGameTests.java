package net.silvertide.silverlib.feature.soulbound;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.silvertide.silverlib.SilverLib;

import java.util.List;

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
    public static void stashCodecKeepsPartialEntriesAndSkipsUnknownItems(GameTestHelper helper) {
        RegistryOps<Tag> ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        ListTag stash = (ListTag) StashedItem.LIST_CODEC_SKIPPING_UNREADABLE_ITEMS
                .encodeStart(ops, List.of(new StashedItem(2, new ItemStack(Items.DIAMOND_SWORD)), new StashedItem(3, new ItemStack(Items.DIAMOND_PICKAXE))))
                .getOrThrow();
        CompoundTag swordStack = stash.getCompound(0).getCompound("stack");
        CompoundTag components = new CompoundTag();
        components.put("silver_lib:component_from_a_removed_mod", new CompoundTag());
        swordStack.put("components", components);
        stash.getCompound(1).getCompound("stack").putString("id", "removed_mod:missing_item");

        List<StashedItem> decoded = StashedItem.LIST_CODEC_SKIPPING_UNREADABLE_ITEMS.parse(ops, stash).getOrThrow();
        helper.assertTrue(decoded.size() == 1, "only the readable entry should survive, got " + decoded.size());
        helper.assertTrue(decoded.getFirst().inventorySlot() == 2 && decoded.getFirst().stack().is(Items.DIAMOND_SWORD), "sword should load without the unknown component");
        helper.succeed();
    }

    @GameTest(template = "empty")
    @SuppressWarnings("removal")
    public static void creativeOverflowDropsInsteadOfVanishing(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) player.getInventory().items.set(slot, new ItemStack(Items.DIRT));
        player.setData(SoulboundAttachments.SOULBOUND_STASH, List.of(new StashedItem(0, soulboundStack(Items.DIAMOND_SWORD))));

        NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(player, false));

        List<ItemEntity> dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(4.0), item -> item.getItem().is(Items.DIAMOND_SWORD));
        helper.assertTrue(dropped.size() == 1, "displaced sword should drop at the creative player's feet, found " + dropped.size());
        dropped.forEach(ItemEntity::discard);
        helper.succeed();
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
