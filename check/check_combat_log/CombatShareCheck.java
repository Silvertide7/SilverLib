import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CombatShareCheck {

    private static final UUID ALICE = UUID.nameUUIDFromBytes("alice".getBytes());
    private static final UUID BOB = UUID.nameUUIDFromBytes("bob".getBytes());

    private static final long INDIRECT_ATTRIBUTION_TICKS = 100L;

    private static int checksRun;

    public static void main(String[] args) {
        emptyLogDividesByZeroSafely();
        allPlayerDamageReportsZeroNonPlayerShare();
        allEnvironmentalDamageReportsFullNonPlayerShare();
        mixedDamageReportsTheEnvironmentalFraction();
        overkillIsClampedToWhatTheTargetHadLeft();
        absorptionCountsAsDealtNotMitigated();
        mitigationIsTheDifferenceBeforeAndAfterArmour();
        repeatContributionAccumulatesBothFiguresAndRefreshesTheTick();
        contributorsSinceIsInclusiveAtTheCutoff();
        participantsUnionNeverDuplicates();
        indirectDamageIsBankedSeparatelyFromDirect();
        indirectAttributionExpiresAfterTheLastDirectHit();
        damageFromAnotherMobIsNeverAttributedToAPlayer();
        healingToFullEndsTheEngagement();
        aPartialHealDoesNotEndTheEngagement();
        aHealedThenCreeperKilledPetReadsAsACreeperKill();

        System.out.println(checksRun + " checks passed");
    }

    private static void emptyLogDividesByZeroSafely() {
        check(new Log().nonPlayerShare() == 0.0f, "empty log divides by zero safely");
    }

    private static void allPlayerDamageReportsZeroNonPlayerShare() {
        Log log = new Log();
        log.recordDamage(ALICE, 10.0f, 0.0f, 100L);
        check(log.nonPlayerShare() == 0.0f, "all-player damage reports zero non-player share");
    }

    private static void allEnvironmentalDamageReportsFullNonPlayerShare() {
        Log log = new Log();
        log.recordDamage(null, 10.0f, 0.0f, 100L);
        check(log.nonPlayerShare() == 1.0f, "all-environmental damage reports full non-player share");
    }

    private static void mixedDamageReportsTheEnvironmentalFraction() {
        Log log = new Log();
        log.recordDamage(ALICE, 7.0f, 0.0f, 100L);
        log.recordDamage(null, 3.0f, 0.0f, 100L);
        check(Math.abs(log.nonPlayerShare() - 0.3f) < 1.0e-6f, "mixed damage reports the environmental fraction");
    }

    private static void overkillIsClampedToWhatTheTargetHadLeft() {
        check(dealtBy(100.0f, 5.0f, 0.0f) == 5.0f, "overkill is clamped to remaining health");
    }

    private static void absorptionCountsAsDealtNotMitigated() {
        check(dealtBy(10.0f, 0.0f, 10.0f) == 10.0f, "absorption counts as dealt");
        check(mitigatedBy(10.0f, 10.0f) == 0.0f, "absorption is not mitigation");
    }

    private static void mitigationIsTheDifferenceBeforeAndAfterArmour() {
        check(mitigatedBy(10.0f, 6.0f) == 4.0f, "mitigation is original minus post-armour");
        check(dealtBy(6.0f, 20.0f, 0.0f) == 6.0f, "a non-lethal hit deals its full post-armour value");
    }

    private static void repeatContributionAccumulatesBothFiguresAndRefreshesTheTick() {
        Log log = new Log();
        log.recordDamage(ALICE, 3.0f, 1.0f, 100L);
        log.recordDamage(ALICE, 4.0f, 2.0f, 140L);
        check(log.damageDealtBy(ALICE) == 7.0f, "repeat contribution accumulates damage");
        check(log.damageMitigatedBy(ALICE) == 3.0f, "repeat contribution accumulates mitigation");
        check(log.lastTick(ALICE) == 140L, "repeat contribution refreshes the tick");
    }

    private static void contributorsSinceIsInclusiveAtTheCutoff() {
        Log log = new Log();
        log.recordDamage(ALICE, 1.0f, 0.0f, 100L);
        check(log.contributorsSince(100L).contains(ALICE), "contribution at the cutoff tick counts");
        check(!log.contributorsSince(101L).contains(ALICE), "contribution before the cutoff is dropped");
    }

    private static void participantsUnionNeverDuplicates() {
        Set<UUID> damagers = new HashSet<>(Set.of(ALICE, BOB));
        Set<UUID> participants = new HashSet<>(damagers);
        participants.addAll(Set.of(ALICE));
        check(participants.size() == 2, "a damager who also supported is counted once");
    }

    private static void indirectDamageIsBankedSeparatelyFromDirect() {
        Log log = new Log();
        log.recordDirect(ALICE, 14.0f, 0.0f, 100L);
        log.recordUnattributed(6.0f, 120L);
        check(log.damageDealtBy(ALICE) == 14.0f, "burn damage is not folded into direct damage");
        check(log.indirectDamageBy(ALICE) == 6.0f, "burn damage is banked as indirect");
        check(Math.abs(log.nonPlayerShare() - 0.3f) < 1.0e-6f, "strict share still counts burn as non-player");
        check(log.unattributedShare() == 0.0f, "lenient share credits the igniter");
    }

    private static void indirectAttributionExpiresAfterTheLastDirectHit() {
        Log log = new Log();
        log.recordDirect(ALICE, 5.0f, 0.0f, 100L);
        log.recordUnattributed(50.0f, 100L + INDIRECT_ATTRIBUTION_TICKS + 1L);
        check(log.indirectDamageBy(ALICE) == 0.0f, "lava long after the last hit is not attributed");
        check(log.unattributedShare() > 0.9f, "tag-and-abandon still reads as unattributed");
    }

    private static void damageFromAnotherMobIsNeverAttributedToAPlayer() {
        Log log = new Log();
        log.recordDirect(ALICE, 5.0f, 0.0f, 100L);
        log.recordNonPlayer(45.0f, 110L);
        check(log.indirectDamageBy(ALICE) == 0.0f, "zombie damage inside the window is not the player's");
        check(Math.abs(log.unattributedShare() - 0.9f) < 1.0e-6f, "a 10% contribution reads as 10%, not 100%");
    }

    private static void healingToFullEndsTheEngagement() {
        Log log = new Log();
        log.recordUnattributed(200.0f, 100L);
        log.healBy(200.0f, 20.0f, 20.0f);
        log.recordDirect(ALICE, 100.0f, 0.0f, 200L);
        check(log.totalDamageTaken() == 100.0f, "a heal to full clears earlier damage");
        check(log.nonPlayerShare() == 0.0f, "a healed-up golem killed later reads as a player kill");
    }

    private static void aPartialHealDoesNotEndTheEngagement() {
        Log log = new Log();
        log.recordDirect(ALICE, 15.0f, 0.0f, 100L);
        log.healBy(10.0f, 5.0f, 20.0f);
        check(log.damageDealtBy(ALICE) == 15.0f, "a heal short of full keeps the engagement");
    }

    private static void aHealedThenCreeperKilledPetReadsAsACreeperKill() {
        Log log = new Log();
        log.recordDirect(ALICE, 2000.0f, 0.0f, 100L);
        log.healBy(2000.0f, 0.0f, 20.0f);
        log.recordUnattributedByMob(100.0f, 200L);
        check(log.nonPlayerShare() == 1.0f, "the mirror case no longer credits the owner");
    }

    private static float dealtBy(float postArmourDamage, float health, float absorption) {
        return Math.min(postArmourDamage, health + absorption);
    }

    private static float mitigatedBy(float originalDamage, float postArmourDamage) {
        return Math.max(0.0f, originalDamage - postArmourDamage);
    }

    private static void check(boolean passed, String description) {
        if (!passed) throw new AssertionError(description);
        checksRun++;
    }

    private static final class Log {
        private final Map<UUID, float[]> contributions = new HashMap<>();
        private final Map<UUID, Long> ticks = new HashMap<>();
        private float totalDamageTaken;

        private UUID lastDirectContributor;
        private long lastDirectTick;
        private long lastDamageTick;

        void recordDamage(UUID playerId, float dealt, float mitigated, long tick) {
            if (playerId == null) recordUnattributed(dealt, tick);
            else recordDirect(playerId, dealt, mitigated, tick);
        }

        void recordDirect(UUID playerId, float dealt, float mitigated, long tick) {
            totalDamageTaken += dealt;
            lastDamageTick = tick;
            lastDirectContributor = playerId;
            lastDirectTick = tick;

            float[] totals = contributions.computeIfAbsent(playerId, id -> new float[3]);
            totals[0] += dealt;
            totals[1] += mitigated;
            ticks.merge(playerId, tick, Math::max);
        }

        void recordNonPlayer(float dealt, long tick) {
            totalDamageTaken += dealt;
            lastDamageTick = tick;
        }

        void recordUnattributed(float dealt, long tick) {
            recordNonPlayer(dealt, tick);

            if (lastDirectContributor == null || tick - lastDirectTick > INDIRECT_ATTRIBUTION_TICKS) return;

            contributions.computeIfAbsent(lastDirectContributor, id -> new float[3])[2] += dealt;
            ticks.merge(lastDirectContributor, tick, Math::max);
        }

        void healBy(float amount, float currentHealth, float maxHealth) {
            if (currentHealth + amount >= maxHealth) clear();
        }

        void recordUnattributedByMob(float dealt, long tick) {
            recordNonPlayer(dealt, tick);
        }

        void clear() {
            contributions.clear();
            ticks.clear();
            totalDamageTaken = 0.0f;
            lastDamageTick = 0L;
            lastDirectContributor = null;
            lastDirectTick = 0L;
        }

        float totalDamageTaken() {
            return totalDamageTaken;
        }

        float indirectDamageBy(UUID playerId) {
            return contributions.getOrDefault(playerId, new float[3])[2];
        }

        float playerIndirectDamage() {
            float indirect = 0.0f;
            for (float[] totals : contributions.values()) indirect += totals[2];
            return indirect;
        }

        float unattributedShare() {
            if (totalDamageTaken <= 0.0f) return 0.0f;
            return Math.max(0.0f, totalDamageTaken - playerDamage() - playerIndirectDamage()) / totalDamageTaken;
        }

        float damageDealtBy(UUID playerId) {
            return contributions.getOrDefault(playerId, new float[3])[0];
        }

        float damageMitigatedBy(UUID playerId) {
            return contributions.getOrDefault(playerId, new float[3])[1];
        }

        long lastTick(UUID playerId) {
            return ticks.getOrDefault(playerId, 0L);
        }

        float playerDamage() {
            float dealt = 0.0f;
            for (float[] totals : contributions.values()) dealt += totals[0];
            return dealt;
        }

        float nonPlayerShare() {
            return totalDamageTaken <= 0.0f ? 0.0f : Math.max(0.0f, totalDamageTaken - playerDamage()) / totalDamageTaken;
        }

        Set<UUID> contributorsSince(long earliestTick) {
            Set<UUID> recent = new HashSet<>();
            ticks.forEach((playerId, tick) -> {
                if (tick >= earliestTick) recent.add(playerId);
            });
            return recent;
        }
    }
}
