package net.silvertide.silverlib.feature.combat;

import net.neoforged.bus.api.IEventBus;

public final class CombatFeature {

    private CombatFeature() {}

    public static void init(IEventBus modEventBus) {
        CombatAttachments.register(modEventBus);
    }
}
