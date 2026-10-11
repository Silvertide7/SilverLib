package net.silvertide.silverlib.feature.soulbound;

import net.neoforged.bus.api.IEventBus;

public final class SoulboundFeature {

    private SoulboundFeature() {}

    public static void init(IEventBus modEventBus) {
        SoulboundAttachments.register(modEventBus);
    }
}
