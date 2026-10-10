package net.silvertide.silverlib.feature.breakage;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Unit;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.silvertide.silverlib.SilverLib;

public final class BreakageComponents {

    private static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, SilverLib.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> KEPT_BROKEN =
            DATA_COMPONENTS.registerComponentType("kept_broken",
                    builder -> builder.persistent(Unit.CODEC).networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));

    private BreakageComponents() {}

    static void register(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
    }
}
