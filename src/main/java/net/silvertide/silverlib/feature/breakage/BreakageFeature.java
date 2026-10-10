package net.silvertide.silverlib.feature.breakage;

import net.neoforged.bus.api.IEventBus;

public final class BreakageFeature {

    private BreakageFeature() {}

    public static void init(IEventBus modEventBus) {
        BreakageComponents.register(modEventBus);
    }
}
