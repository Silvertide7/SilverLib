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

public final class SupportLog {

    public static final Codec<SupportLog> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Support.CODEC)
                    .fieldOf("supporters").forGetter(log -> log.supports)
    ).apply(instance, SupportLog::new));

    private final Map<UUID, Support> supports;

    public SupportLog() {
        this(Map.of());
    }

    private SupportLog(Map<UUID, Support> supports) {
        this.supports = new HashMap<>(supports);
    }

    public Optional<Support> support(UUID supporterId) {
        return Optional.ofNullable(supports.get(supporterId));
    }

    public Set<UUID> supporters() {
        return Set.copyOf(supports.keySet());
    }

    public Set<UUID> supportersSince(long earliestTick) {
        Set<UUID> recent = new HashSet<>();
        supports.forEach((supporterId, support) -> {
            if (support.lastSupportTick() >= earliestTick) recent.add(supporterId);
        });
        return recent;
    }

    public boolean isEmpty() {
        return supports.isEmpty();
    }

    void recordHealing(UUID supporterId, float healing, long gameTick) {
        merge(supporterId, new Support(healing, Set.of(), gameTick));
    }

    void recordEffect(UUID supporterId, ResourceLocation effectId, long gameTick) {
        merge(supporterId, new Support(0.0f, Set.of(effectId), gameTick));
    }

    private void merge(UUID supporterId, Support addition) {
        supports.merge(supporterId, addition, Support::combinedWith);
    }

    public record Support(float healingReceived,
                          Set<ResourceLocation> effectsApplied,
                          long lastSupportTick) {

        static final Codec<Support> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.fieldOf("healing").forGetter(Support::healingReceived),
                ResourceLocation.CODEC.listOf().xmap(Set::copyOf, List::copyOf)
                        .optionalFieldOf("effects", Set.of()).forGetter(Support::effectsApplied),
                Codec.LONG.fieldOf("tick").forGetter(Support::lastSupportTick)
        ).apply(instance, Support::new));

        Support combinedWith(Support later) {
            return new Support(
                    healingReceived + later.healingReceived,
                    union(effectsApplied, later.effectsApplied),
                    Math.max(lastSupportTick, later.lastSupportTick));
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
