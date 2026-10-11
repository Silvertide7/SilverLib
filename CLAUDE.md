# SilverLib

A NeoForge Minecraft mod (1.21.1, Java 21) that is a **library, not a gameplay mod**. It records
things other mods need to know and exposes them. It changes nothing on its own: no drops, no XP, no
credit, no rewards. Every gameplay decision belongs to a consumer.

Consumers so far: Realms of Reverie (FTB Quests kill credit, Project MMO XP sharing) and a monster
hunter mod (per-entity-type damage unlocks).

## Features

### `feature/combat` — combat contribution ledger

Records who contributed to an entity's death and how much, plus who supported those players.

Three serialized data attachments:

| Attachment | On | Holds |
|---|---|---|
| `combat_log` | any `LivingEntity` | `totalDamageTaken` (every source) and `Map<UUID, Contribution>` for player-attributed damage, **since the entity was last at full health** |
| `combat_support` | `Player` | who healed or buffed them, and when (`copyOnDeath`) |
| `summon_owner` | any `Entity` | generic hook for mods whose summons implement neither `OwnableEntity` nor `TraceableEntity` |

`Contribution` carries `damageDealt`, `indirectDamage`, `damageMitigated`, `effectsApplied` and
`lastContributionTick`. The public entry point is `CombatTracking`.

**The three damage figures are deliberately distinct and must stay that way:**

- `damageDealt` — HP+absorption actually removed, **overkill clamped**. Recorded on
  `LivingDamageEvent.Pre` (NOT `Post`) because in `actuallyHurt` the order is armour reduction →
  magic reduction → **Pre** → absorption reduction → health applied → Post. At `Pre` both
  `getHealth()` and `getAbsorptionAmount()` are still pre-hit; at `Post` the pre-hit health is
  unrecoverable because `setHealth` clamps to 0.
- `indirectDamage` — sourceless damage (burning, poison, wither ticks carry a `DamageSource` with no
  attacker) attributed to whoever last hit the entity directly, within `INDIRECT_ATTRIBUTION_TICKS`.
  The window expires on **direct** hits only, so "tag it and let lava finish it" stops being
  attributed. Kept separate so `nonPlayerShare()` can stay strict while `unattributedShare()` is
  lenient — a Fire Aspect kill must not read as 30% environmental.
- `damageMitigated` — only `ARMOR`, `ENCHANTMENTS`, `MOB_EFFECTS` and `INNATE_RESISTANCE`
  reductions. **Not** `INVULNERABILITY` or shield-blocked, which are subtracted before `Pre` and
  would otherwise record an i-frame hit as "armour ate your damage".

**An engagement ends when the entity is whole again.** Two triggers, because health can reach
`maxHealth` without `heal()` ever running (a max-health attribute shrink clamps via `setHealth`; the
ender dragon's crystals call `setHealth` directly): a `LivingHealEvent` that would reach full clears
the log eagerly, and a hit landing on an entity already at full HP with no absorption clears any log
it still carries before recording — if it is whole and carrying damage, that damage is stale by
definition. The absorption guard keeps hits that only chew a shield at full HP accumulating as real
damage. That is a statement about state, not the clock: if it is back to full, nothing from
the previous fight stuck and there is nothing to apportion; if it is not, whatever it carries really
did contribute to its eventual death. A time-gap reset was rejected because it wipes a boss fight
during an invulnerable phase. Healing is only ever a reset trigger, never a number — a healing
offset was tried and cannot be apportioned once time-ordering is lost.

Accepted residue: both `LivingDamageEvent.Pre` and `LivingHealEvent` are handled at `LOWEST`, and a
same-priority listener registered after ours that cancels or shrinks the event leaves us having
already recorded (or cleared). NeoForge offers no "after all LOWEST" hook, so this is documented
rather than fixed. Within one engagement every hit counts as dealt even if healed back, the
standard damage-meter convention. The error is bounded by healing inside a single fight. A running
total also **cannot be windowed after the fact** — `lastContributionTick` tells you who took part
recently, not how much of their damage was recent; time-sliced amounts would need bucketed storage
and no consumer needs them.

### `feature/breakage` — keep items broken instead of destroying them

Lets consumers stop a damageable item from vanishing when its durability runs out. The item stays
at max damage, unusable, until it is repaired. SilverLib never keeps an item on its own; a
consumer has to ask.

`mixin/ItemStackMixin` hooks `ItemStack.hurtAndBreak` at the `shrink(1)` call and hands off to
`BreakageHooks`, which posts `ItemBreakingEvent`. If any listener calls `keepBroken()` (and no
listener emptied the stack), location-based enchantment effects such as Soul Speed are stopped while they are still visible, the
stack gets the `silver_lib:kept_broken` marker, damage is pinned at max, the break sound plays,
`ItemKeptBrokenEvent` is posted, and the destroy is skipped. `onBreak` must stay skipped: vanilla's
callbacks post `PlayerDestroyItemEvent`, which consumers treat as "the item is gone". That is why
this is a cancelling `@Inject` and not a `@WrapOperation` on `shrink` alone. A listener can only ask
to keep the item, never to force a destroy, so "keep" always wins between consumers.

`BrokenItems.isBroken(stack)` is the facade: marker present **and** damage at max. The marker stops
SilverLib from hijacking items other mods deliberately leave at max damage. `setDamageValue`
removes it once damage drops below max, so repaired stacks match fresh ones again; repairs that
write `DataComponents.DAMAGE` directly leave it until the next `hurtAndBreak`, which clears it at
HEAD. Stackable damageable items cannot exist (vanilla rejects them), so a kept stack is always one
item.

While broken: no further durability loss (positive damage cancelled at `hurtAndBreak` HEAD;
negative damage still repairs); attack, item use and interact events cancelled for non-creative
players; `LivingEntityUseItemEvent.Start` cancelled for any entity, and an entity still using a
broken item stops on its next `EntityTickEvent.Pre`; attribute modifiers cleared in
`ItemAttributeModifierEvent` and every enchantment except `REPAIR_WITH_XP` ones removed in
`GetEnchantmentLevelEvent`, both at `LOWEST` so other mods' bonuses go too (Mending stays so a
broken item can still be repaired with experience); and dispensers eject a broken item instead of
using it (`mixin/DispenserBlockMixin`), so broken shears or flint and steel do nothing.

The stop-using is deliberately deferred to the next tick, never done inside `hurtAndBreak`: vanilla
callers check `useItem.isEmpty()` right after damaging (`Player.hurtCurrentlyUsedShield` then clears
the hand, `LivingEntity.releaseUsingItem` posts `PlayerDestroyItemEvent`), and `stopUsingItem()`
empties `useItem`.

Mixins call `@ApiStatus.Internal` hook classes (`BreakageHooks`, `SoulboundHooks`) so the marker
component, the stash attachment and `StashedItem` can stay package-private. Mixins live in their own
subpackage because a Mixin config package may contain only mixins; each feature with mixins has its
own `silver_lib.<feature>.mixins.json`.

### `feature/soulbound` — keep items through death

Consumers decide which items a player keeps on death; SilverLib moves them safely. Nothing is
soulbound unless a consumer says so.

`SoulboundItems.isSoulbound(player, stack)` is the facade. It posts `SoulboundCheckEvent`; any
listener calling `keep()` makes the stack soulbound. It is an event, not a component, because rules
like "attuned to *this* player" depend on who is dying.

`mixin/PlayerMixin` injects at `Player.dropEquipment` HEAD. That runs only from `dropAllDeathLoot`,
after every `LivingDeathEvent` listener has had its chance to cancel, so a revive mod cancelling the
death never strands items, and it runs before Curse of Vanishing destroys anything. It does nothing
with `keepInventory` on (vanilla keeps everything). Each soulbound slot is moved into the
`soulbound_stash` player attachment as `StashedItem(inventorySlot, stack)` (combined `Inventory`
index: 0-35 main, 36-39 armour, 40 offhand), appended to anything already stashed, never
overwriting. The attachment is serialized (survives a disconnect on the death screen) and
`copyOnDeath`, so the respawned player carries it.

`PlayerRespawnEvent` empties the stash in two passes: first every stack whose slot is empty goes
back to it, then the displaced ones go into any free slot (only attempted when there is room,
because `Inventory.add` silently zeroes the stack for infinite-materials players), and if the
inventory is full they drop at
the player's feet through `ServerPlayer.drop(stack, false, false)`, which skips `ItemTossEvent` so a
protection mod cancelling tosses cannot delete them. Two passes stop a displaced item from taking a
slot another stashed item owns. The stash is removed before placing, so an overflow drop cannot
duplicate. The stash encodes and decodes entry by entry: a stack that cannot be saved is logged and
skipped, a stack with a component from a removed mod loads without that component (vanilla's
`resultOrPartial` behaviour), and only an entry that does not load at all (unknown item) is dropped,
so one bad entry never wipes the rest. Restoring happens at
respawn, not in `Clone`, because the new player is positioned by then and an overflow drop lands at
their feet instead of world spawn.

Accepted gaps: grave mods that scrape the inventory inside `LivingDeathEvent` itself still see
soulbound items first. Curios slots are the consumer's job (SilverLib never hard-depends on Curios):
add a `DropRulesEvent` override using `SoulboundItems.isSoulbound`.

## Project Structure

```
src/main/java/net/silvertide/silverlib/
  SilverLib.java          @Mod entry point. Stays thin: a list of XFeature.init(modEventBus) calls.
  core/                   Shared infrastructure only. Nothing feature-specific. Created when needed.
  feature/<name>/         One vertical slice, one Java package.
```

**The rule: everything that changes together lives together.** A feature folder holds every part of
that feature — attachments, events, data classes, its public API, and its integrations with other
mods. If adding a capability to a feature makes you edit files in three other folders, the split is
wrong.

Each feature exposes `public static void init(IEventBus)` on an `XFeature` class, called from
`SilverLib`, and owns its own registries. One registry class per registry type per feature.

**`core/` is for things with no single owner**: genuinely shared infrastructure, or code used by
several features. It is not a staging area — if something has an obvious home, it goes in the
feature. Do not create it empty; add it when something actually belongs there.

**One package per feature is a correctness constraint here, not just tidiness.** Mutators on data
classes are package-private so the public read API cannot be used to corrupt state, and an
`api`/`internal` split would force those mutators public. Keep a feature's classes in one package.

### Library API rules

- A feature's public surface is one facade class (`CombatTracking`). Consumers should not need
  anything else.
- Reads must be safe on entities nothing ever touched — return an empty log, never null.
- Never hard-depend on another mod. Where the library cannot observe attribution itself, expose a
  recording method and let the consuming mod wire the integration. `CombatTracking.recordHealing`
  and `setSummonOwner` exist for exactly this, so no Iron's Spells code ships here.
- Name recording methods so they cannot be misused: `recordDamageOutsideVanillaHurt` is blunt
  because anything that went through `hurt()` is already recorded and a second call would corrupt
  the ratios.

## Build & Run

```bash
./gradlew build                # build the mod jar
./gradlew runClient            # smoke test
./check/check_combat_log.sh    # standalone damage-accounting assertions, no Minecraft classpath
./gradlew runGameTestServer    # in-game tests (breakage, soulbound death and respawn)
```

In-game tests live in the `gametest` source set (`src/gametest/`), which every dev run loads but
the published jar does not. Standalone checks live in `check/`. They compile the pure logic against a plain `javac` and assert
with `AssertionError`. Non-trivial maths (shares, clamping, attribution windows) should leave one
behind; Minecraft-coupled code is verified in game.

---

# Reusable Engineering Standards

## Code Style

**Never write comments.** No inline `//` comments, no `/* */` blocks, no javadoc, no leading
explanatory headers on methods or fields. Code must be self-documenting through naming alone.

- Variable names describe what the value *is*; method names describe what they *do* and under what
  conditions.
- Extract a well-named helper method instead of writing a comment to explain a block.
- Constants get descriptive names that encode their meaning and unit.
- If a name would need a comment to explain it, rename it until it doesn't.

**Never leave dead code.** No unused methods, fields, classes, parameters, or imports. No "escape
hatch" or "just in case" code. No commented-out blocks. If it's not called, delete it — git history
is the archive.

## Code Review

When asked to review code, do a "pass", check for issues, or audit a recent change, do **two**
passes in order:

1. **Self-audit first.** Read the diff yourself. Fix the obvious — dead code, comments, naming,
   anything violating the Code Style rules. Report findings.
2. **Then spawn an independent reviewer** with only the diff and the goal, no context about why the
   choices were made. That catches the bugs you would otherwise rationalize away.

Do not skip step 2 because step 1 looked clean — the value of the independent reviewer is exactly
that it doesn't share your blind spots. And re-review after fixing: fixes written against correct
findings are not themselves reviewed until someone looks at them.

**Verify upstream behaviour, don't recall it.** Decompile the actual jar with `javap` before
asserting what a NeoForge, Minecraft or third-party API does. Several bugs in this codebase's
history compiled cleanly and failed only at runtime.

## Version Control

**The user handles commits in git.** Never run `git add`, `git commit`, or `git push` — and don't
suggest it — unless explicitly asked. Wrap up by reporting what changed.
