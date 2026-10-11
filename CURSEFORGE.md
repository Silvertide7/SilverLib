# SilverLib

SilverLib is a shared library for Silvertide mods. It holds the code and features that more than one of my mods needs, so each mod can stay focused on its own gameplay instead of re-implementing the same plumbing.

**On its own, SilverLib changes nothing.** It adds no items, blocks, drops, XP, or rewards. It only records information and exposes it through an API for other mods to read. Install it because another mod requires it.

## What it currently does

### Combat contribution tracking

SilverLib keeps a ledger of who contributed to a mob's death and how much. Other mods use this to answer questions like "who deserves credit for this kill?" or "how should this XP be split?" fairly, instead of giving everything to whoever landed the last hit.

For every living entity it records, since the entity was last at full health:

- **Total damage taken** from every source, including the environment and other mobs.
- **Per-player damage dealt**, with overkill clamped so hitting a mob with 1 HP for 100 damage counts as 1.
- **Indirect damage** such as fire, poison, and wither ticks, attributed to the player who last hit the mob directly. A Fire Aspect kill is credited to the player, not to "the environment".
- **Damage mitigated** by the mob's armor, enchantments, and effects, so a heavily armored fight still reflects the effort put in.
- **Effects applied** to the mob by each player.
- **Summons and projectiles** are attributed to their owner, so a summoner's wolf or a mage's spell counts as the player's contribution.

For every player it also records who healed or buffed them, so support players can be credited by mods that care about it.

Once a mob is healed back to full health the ledger is cleared. A fight that never stuck does not carry over into the next one.

### Broken instead of destroyed

Mods using SilverLib can ask for an item to be kept broken when its durability runs out instead of vanishing. A broken item sits at zero durability: it can't attack, mine, be used, or grant attributes until it's repaired. SilverLib only does this when another mod asks; on its own no item behaves differently.

### Soulbound items

Mods using SilverLib can mark items as soulbound so a player keeps them on death. Kept items go back to the slot they came from, or into the inventory if that slot is taken, and drop at the player's feet rather than vanish if the inventory is full. A death cancelled by a revive mod never loses them.

### For mod developers

The public entry points are `CombatTracking`, and `BrokenItems` plus `ItemBreakingEvent` / `ItemKeptBrokenEvent` for breakage, and `SoulboundItems` plus `SoulboundCheckEvent` for soulbound. Reads are always safe: an entity nothing ever touched returns an empty log, never null.

SilverLib never hard-depends on other mods. Where it cannot observe something itself (custom healing, spell summons, damage that bypasses vanilla `hurt()`), it exposes recording methods so your mod can feed the ledger directly.

## Mods using SilverLib

- Realms of Reverie
- Silvertide monster hunter mod (in development)

## Requirements

- Minecraft 1.21.1
- NeoForge
- Java 21
