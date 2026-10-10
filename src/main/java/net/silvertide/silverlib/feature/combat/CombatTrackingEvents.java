package net.silvertide.silverlib.feature.combat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.silvertide.silverlib.SilverLib;

@EventBusSubscriber(modid = SilverLib.MOD_ID)
public final class CombatTrackingEvents {

    private CombatTrackingEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void recordIncomingDamage(LivingDamageEvent.Pre event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel)) return;

        float absorbableHealth = victim.getHealth() + victim.getAbsorptionAmount();
        float dealt = Math.min(event.getNewDamage(), absorbableHealth);
        if (!Float.isFinite(dealt) || dealt <= 0.0f) return;

        endStaleEngagementIfWhole(victim);
        CombatTracking.recordDamage(
                victim, event.getSource().getEntity(), dealt, defensiveReduction(event.getContainer()));
    }

    private static void endStaleEngagementIfWhole(LivingEntity victim) {
        if (victim.getHealth() < victim.getMaxHealth() || victim.getAbsorptionAmount() > 0.0f) return;

        victim.getExistingData(CombatAttachments.COMBAT_LOG.get()).ifPresent(CombatLog::clear);
    }

    private static float defensiveReduction(DamageContainer container) {
        return container.getReduction(DamageContainer.Reduction.ARMOR)
                + container.getReduction(DamageContainer.Reduction.ENCHANTMENTS)
                + container.getReduction(DamageContainer.Reduction.MOB_EFFECTS)
                + container.getReduction(DamageContainer.Reduction.INNATE_RESISTANCE);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void endEngagementWhenHealedToFull(LivingHealEvent event) {
        LivingEntity healed = event.getEntity();
        if (!(healed.level() instanceof ServerLevel) || healed.getHealth() <= 0.0f) return;
        if (Float.isNaN(event.getAmount())) return;
        if (healed.getHealth() + event.getAmount() < healed.getMaxHealth()) return;

        healed.getExistingData(CombatAttachments.COMBAT_LOG.get()).ifPresent(CombatLog::clear);
    }

    @SubscribeEvent
    public static void recordAppliedEffect(MobEffectEvent.Added event) {
        LivingEntity target = event.getEntity();
        if (!(target.level() instanceof ServerLevel)) return;

        MobEffectInstance applied = event.getEffectInstance();
        if (!countsFor(target, applied)) return;
        if (!improvesOnExistingEffect(event.getOldEffectInstance(), applied)) return;

        ResourceLocation effectId = BuiltInRegistries.MOB_EFFECT.getKey(applied.getEffect().value());
        CombatTracking.recordEffect(target, event.getEffectSource(), effectId);
    }

    private static boolean countsFor(LivingEntity target, MobEffectInstance applied) {
        boolean beneficial = applied.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL;
        return target instanceof Player ? beneficial : !beneficial;
    }

    private static boolean improvesOnExistingEffect(MobEffectInstance existing, MobEffectInstance applied) {
        if (existing == null) return true;
        if (applied.getAmplifier() != existing.getAmplifier()) {
            return applied.getAmplifier() > existing.getAmplifier();
        }
        if (existing.isInfiniteDuration()) return false;

        return applied.isInfiniteDuration() || applied.getDuration() > existing.getDuration();
    }
}
