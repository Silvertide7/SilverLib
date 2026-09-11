package net.silvertide.silverlib.feature.combat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class CombatLog {

    public static final long INDIRECT_ATTRIBUTION_TICKS = 100L;

    public static final Codec<CombatLog> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("total_damage_taken").forGetter(CombatLog::totalDamageTaken),
            Codec.LONG.optionalFieldOf("last_damage_tick", 0L).forGetter(CombatLog::lastDamageTick),
            UUIDUtil.STRING_CODEC.optionalFieldOf("last_direct_contributor")
                    .forGetter(log -> Optional.ofNullable(log.lastDirectContributor)),
            Codec.LONG.optionalFieldOf("last_direct_tick", 0L).forGetter(log -> log.lastDirectTick),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Contribution.CODEC)
                    .fieldOf("contributions").forGetter(log -> log.contributions)
    ).apply(instance, CombatLog::new));

    private final Map<UUID, Contribution> contributions;
    private float totalDamageTaken;
    private long lastDamageTick;
    private UUID lastDirectContributor;
    private long lastDirectTick;

    public CombatLog() {
        this(0.0f, 0L, Optional.empty(), 0L, Map.of());
    }

    private CombatLog(float totalDamageTaken,
                      long lastDamageTick,
                      Optional<UUID> lastDirectContributor,
                      long lastDirectTick,
                      Map<UUID, Contribution> contributions) {
        this.totalDamageTaken = totalDamageTaken;
        this.lastDamageTick = lastDamageTick;
        this.lastDirectContributor = lastDirectContributor.orElse(null);
        this.lastDirectTick = lastDirectTick;
        this.contributions = new HashMap<>(contributions);
    }

    public float totalDamageTaken() {
        return totalDamageTaken;
    }

    public long lastDamageTick() {
        return lastDamageTick;
    }

    public float playerDamage() {
        float dealt = 0.0f;
        for (Contribution contribution : contributions.values()) {
            dealt += contribution.damageDealt();
        }
        return dealt;
    }

    public float playerIndirectDamage() {
        float indirect = 0.0f;
        for (Contribution contribution : contributions.values()) {
            indirect += contribution.indirectDamage();
        }
        return indirect;
    }

    public float nonPlayerDamage() {
        return Math.max(0.0f, totalDamageTaken - playerDamage());
    }

    public float nonPlayerShare() {
        return totalDamageTaken <= 0.0f ? 0.0f : nonPlayerDamage() / totalDamageTaken;
    }

    public float unattributedShare() {
        if (totalDamageTaken <= 0.0f) return 0.0f;

        return Math.max(0.0f, totalDamageTaken - playerDamage() - playerIndirectDamage()) / totalDamageTaken;
    }

    public float damageDealtBy(UUID playerId) {
        Contribution contribution = contributions.get(playerId);
        return contribution == null ? 0.0f : contribution.damageDealt();
    }

    public float indirectDamageBy(UUID playerId) {
        Contribution contribution = contributions.get(playerId);
        return contribution == null ? 0.0f : contribution.indirectDamage();
    }

    public float damageMitigatedBy(UUID playerId) {
        Contribution contribution = contributions.get(playerId);
        return contribution == null ? 0.0f : contribution.damageMitigated();
    }

    public Optional<Contribution> contribution(UUID playerId) {
        return Optional.ofNullable(contributions.get(playerId));
    }

    public Set<UUID> contributors() {
        return Set.copyOf(contributions.keySet());
    }

    public Set<UUID> contributorsSince(long earliestTick) {
        Set<UUID> recent = new HashSet<>();
        contributions.forEach((playerId, contribution) -> {
            if (contribution.lastContributionTick() >= earliestTick) recent.add(playerId);
        });
        return recent;
    }

    public boolean isEmpty() {
        return totalDamageTaken <= 0.0f && contributions.isEmpty();
    }

    void clear() {
        contributions.clear();
        totalDamageTaken = 0.0f;
        lastDamageTick = 0L;
        lastDirectContributor = null;
        lastDirectTick = 0L;
    }

    void recordDirectDamage(UUID playerId, float dealt, float mitigated, long gameTick) {
        totalDamageTaken += dealt;
        lastDamageTick = gameTick;
        lastDirectContributor = playerId;
        lastDirectTick = gameTick;

        merge(playerId, new Contribution(dealt, 0.0f, mitigated, Set.of(), gameTick));
    }

    void recordSourcelessDamage(float dealt, long gameTick) {
        recordNonPlayerDamage(dealt, gameTick);

        if (lastDirectContributor == null || gameTick - lastDirectTick > INDIRECT_ATTRIBUTION_TICKS) return;

        merge(lastDirectContributor, new Contribution(0.0f, dealt, 0.0f, Set.of(), gameTick));
    }

    void recordNonPlayerDamage(float dealt, long gameTick) {
        totalDamageTaken += dealt;
        lastDamageTick = gameTick;
    }

    void recordEffect(UUID playerId, ResourceLocation effectId, long gameTick) {
        merge(playerId, new Contribution(0.0f, 0.0f, 0.0f, Set.of(effectId), gameTick));
    }

    private void merge(UUID playerId, Contribution addition) {
        contributions.merge(playerId, addition, Contribution::combinedWith);
    }

    public record Contribution(float damageDealt,
                               float indirectDamage,
                               float damageMitigated,
                               Set<ResourceLocation> effectsApplied,
                               long lastContributionTick) {

        static final Codec<Contribution> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.fieldOf("dealt").forGetter(Contribution::damageDealt),
                Codec.FLOAT.optionalFieldOf("indirect", 0.0f).forGetter(Contribution::indirectDamage),
                Codec.FLOAT.fieldOf("mitigated").forGetter(Contribution::damageMitigated),
                ResourceLocation.CODEC.listOf().xmap(Set::copyOf, List::copyOf)
                        .optionalFieldOf("effects", Set.of()).forGetter(Contribution::effectsApplied),
                Codec.LONG.fieldOf("tick").forGetter(Contribution::lastContributionTick)
        ).apply(instance, Contribution::new));

        Contribution combinedWith(Contribution later) {
            return new Contribution(
                    damageDealt + later.damageDealt,
                    indirectDamage + later.indirectDamage,
                    damageMitigated + later.damageMitigated,
                    union(effectsApplied, later.effectsApplied),
                    Math.max(lastContributionTick, later.lastContributionTick));
        }

        private static Set<ResourceLocation> union(Set<ResourceLocation> existing, Set<ResourceLocation> added) {
            if (added.isEmpty()) return existing;
            if (existing.isEmpty()) return added;

            Set<ResourceLocation> merged = new HashSet<>(existing);
            merged.addAll(added);
            return Set.copyOf(merged);
        }
    }
}
