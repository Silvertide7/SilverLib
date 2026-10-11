1.2.0
---
- Add soulbound feature: mods mark items kept on death via SoulboundCheckEvent.keep(); items return to their original slot after respawn
- Soulbound items survive revive mods cancelling the death, and drop at the player instead of vanishing when the inventory is full
- Loosen the NeoForge requirement to 21.1.228 or newer
- Broken items lose their enchantments until repaired, stop blocking or brushing the moment they break, and dispensers eject them instead of using them
- The broken marker is removed once an item is repaired

1.1.0
---
- Add breakage feature: mods can keep an item broken at zero durability instead of destroyed via ItemBreakingEvent.keepBroken()
- Add ItemKeptBrokenEvent, posted after an item is kept broken
- Broken items can't attack, mine, be used, or grant attributes until repaired
