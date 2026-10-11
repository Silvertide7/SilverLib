package net.silvertide.silverlib.feature.soulbound;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.silvertide.silverlib.SilverLib;

import java.util.List;
import java.util.function.Supplier;

final class SoulboundAttachments {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, SilverLib.MOD_ID);

    static final Supplier<AttachmentType<List<StashedItem>>> SOULBOUND_STASH =
            ATTACHMENT_TYPES.register("soulbound_stash",
                    () -> AttachmentType.<List<StashedItem>>builder(() -> List.of())
                            .serialize(StashedItem.LIST_CODEC_SKIPPING_UNREADABLE_ITEMS, stash -> !stash.isEmpty())
                            .copyOnDeath()
                            .build());

    private SoulboundAttachments() {}

    static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
