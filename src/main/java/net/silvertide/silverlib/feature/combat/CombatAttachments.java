package net.silvertide.silverlib.feature.combat;

import net.minecraft.Util;
import net.minecraft.core.UUIDUtil;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.silvertide.silverlib.SilverLib;

import java.util.UUID;
import java.util.function.Supplier;

public final class CombatAttachments {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, SilverLib.MOD_ID);

    public static final Supplier<AttachmentType<CombatLog>> COMBAT_LOG =
            ATTACHMENT_TYPES.register("combat_log",
                    () -> AttachmentType.builder(CombatLog::new)
                            .serialize(CombatLog.CODEC, log -> !log.isEmpty())
                            .build());

    public static final Supplier<AttachmentType<SupportLog>> COMBAT_SUPPORT =
            ATTACHMENT_TYPES.register("combat_support",
                    () -> AttachmentType.builder(SupportLog::new)
                            .serialize(SupportLog.CODEC, log -> !log.isEmpty())
                            .copyOnDeath()
                            .build());

    public static final Supplier<AttachmentType<UUID>> SUMMON_OWNER =
            ATTACHMENT_TYPES.register("summon_owner",
                    () -> AttachmentType.builder(() -> Util.NIL_UUID)
                            .serialize(UUIDUtil.STRING_CODEC, ownerId -> !Util.NIL_UUID.equals(ownerId))
                            .build());

    private CombatAttachments() {}

    static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
