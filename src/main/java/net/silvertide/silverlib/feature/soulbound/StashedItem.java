package net.silvertide.silverlib.feature.soulbound;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

record StashedItem(int inventorySlot, ItemStack stack) {
    private static final Logger LOGGER = LogUtils.getLogger();

    static final Codec<StashedItem> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("inventory_slot").forGetter(StashedItem::inventorySlot),
            ItemStack.CODEC.fieldOf("stack").forGetter(StashedItem::stack)
    ).apply(instance, StashedItem::new));

    static final Codec<List<StashedItem>> LIST_CODEC_SKIPPING_UNREADABLE_ITEMS = new Codec<>() {
        @Override
        public <T> DataResult<Pair<List<StashedItem>, T>> decode(DynamicOps<T> ops, T input) {
            return ops.getStream(input).map(elements -> Pair.of(elements.flatMap(element -> decodeOrSkip(ops, element).stream()).toList(), input));
        }

        @Override
        public <T> DataResult<T> encode(List<StashedItem> stash, DynamicOps<T> ops, T prefix) {
            return CODEC.listOf().encode(stash, ops, prefix);
        }
    };

    private static <T> Optional<StashedItem> decodeOrSkip(DynamicOps<T> ops, T element) {
        DataResult<StashedItem> result = CODEC.parse(ops, element);
        result.error().ifPresent(error -> LOGGER.warn("Dropping unreadable soulbound stash entry: {}", error.message()));
        return result.result();
    }
}
