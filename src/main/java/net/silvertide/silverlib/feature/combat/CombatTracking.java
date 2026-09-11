package net.silvertide.silverlib.feature.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.Util;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class CombatTracking {

    private CombatTracking() {}

    public static CombatLog logOf(LivingEntity entity) {
        return entity.getExistingData(CombatAttachments.COMBAT_LOG.get()).orElseGet(CombatLog::new);
    }

    public static SupportLog supportOf(Player player) {
        return player.getExistingData(CombatAttachments.COMBAT_SUPPORT.get()).orElseGet(SupportLog::new);
    }

    public static Set<UUID> participants(LivingEntity victim, long windowTicks) {
        if (!(victim.level() instanceof ServerLevel serverLevel)) return Set.of();

        long earliestTick = serverLevel.getGameTime() - windowTicks;
        Set<UUID> damagerIds = logOf(victim).contributorsSince(earliestTick);
        if (damagerIds.isEmpty()) return Set.of();

        Set<UUID> participantIds = new HashSet<>(damagerIds);
        PlayerList playerList = serverLevel.getServer().getPlayerList();
        for (UUID damagerId : damagerIds) {
            ServerPlayer damager = playerList.getPlayer(damagerId);
            if (damager != null) participantIds.addAll(supportOf(damager).supportersSince(earliestTick));
        }
        return participantIds;
    }

    public static void recordDamageOutsideVanillaHurt(LivingEntity victim, Entity attacker, float dealt, float mitigated) {
        recordDamage(victim, attacker, dealt, mitigated);
    }

    static void recordDamage(LivingEntity victim, Entity attacker, float dealt, float mitigated) {
        if (!(victim.level() instanceof ServerLevel serverLevel)) return;
        if (!Float.isFinite(dealt) || dealt <= 0.0f) return;

        float defensiveReduction = Float.isFinite(mitigated) ? Math.max(0.0f, mitigated) : 0.0f;
        ServerPlayer contributor = attributedPlayer(attacker);
        CombatLog log = victim.getData(CombatAttachments.COMBAT_LOG.get());
        long gameTick = serverLevel.getGameTime();

        if (contributor != null && contributor != victim) {
            log.recordDirectDamage(contributor.getUUID(), dealt, defensiveReduction, gameTick);
        } else if (attacker == null) {
            log.recordSourcelessDamage(dealt, gameTick);
        } else {
            log.recordNonPlayerDamage(dealt, gameTick);
        }
    }

    public static void recordEffect(LivingEntity target, Entity applier, ResourceLocation effectId) {
        if (!(target.level() instanceof ServerLevel serverLevel) || effectId == null) return;

        ServerPlayer contributor = attributedPlayer(applier);
        if (contributor == null || contributor == target) return;

        long gameTick = serverLevel.getGameTime();
        if (target instanceof Player supported) {
            supported.getData(CombatAttachments.COMBAT_SUPPORT.get())
                    .recordEffect(contributor.getUUID(), effectId, gameTick);
        } else {
            target.getData(CombatAttachments.COMBAT_LOG.get())
                    .recordEffect(contributor.getUUID(), effectId, gameTick);
        }
    }

    public static void recordHealing(Entity healer, Player healed, float amount) {
        if (!(healed.level() instanceof ServerLevel serverLevel)) return;
        if (!Float.isFinite(amount) || amount <= 0.0f) return;

        ServerPlayer supporter = attributedPlayer(healer);
        if (supporter == null || supporter == healed) return;

        healed.getData(CombatAttachments.COMBAT_SUPPORT.get())
                .recordHealing(supporter.getUUID(), amount, serverLevel.getGameTime());
    }

    public static void setSummonOwner(Entity summon, Entity owner) {
        if (summon == null || owner == null || summon.level().isClientSide()) return;

        summon.setData(CombatAttachments.SUMMON_OWNER.get(), owner.getUUID());
    }

    public static ServerPlayer attributedPlayer(Entity entity) {
        if (entity == null || entity instanceof FakePlayer) return null;
        if (entity instanceof ServerPlayer player) return player;

        return ownerOf(entity) instanceof ServerPlayer owner && !(owner instanceof FakePlayer) ? owner : null;
    }

    private static Entity ownerOf(Entity entity) {
        Entity declaredOwner = declaredOwnerOf(entity);
        return declaredOwner instanceof ServerPlayer ? declaredOwner : summonerOf(entity);
    }

    private static Entity declaredOwnerOf(Entity entity) {
        if (entity instanceof OwnableEntity ownable) return ownable.getOwner();
        if (entity instanceof TraceableEntity traceable) return traceable.getOwner();
        return null;
    }

    private static ServerPlayer summonerOf(Entity summon) {
        if (!(summon.level() instanceof ServerLevel serverLevel)) return null;

        UUID summonerId = summon.getExistingData(CombatAttachments.SUMMON_OWNER.get()).orElse(Util.NIL_UUID);
        return Util.NIL_UUID.equals(summonerId) ? null : serverLevel.getServer().getPlayerList().getPlayer(summonerId);
    }
}
