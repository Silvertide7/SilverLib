package net.silvertide.silverlib;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.silvertide.silverlib.feature.combat.CombatFeature;

@Mod(SilverLib.MOD_ID)
public class SilverLib {
    public static final String MOD_ID = "silver_lib";

    public SilverLib(IEventBus modEventBus) {
        CombatFeature.init(modEventBus);
    }
}
