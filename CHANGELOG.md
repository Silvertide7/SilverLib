1.1.0
---
- Add breakage feature: mods can keep an item broken at zero durability instead of destroyed via ItemBreakingEvent.keepBroken()
- Add ItemKeptBrokenEvent, posted after an item is kept broken
- Broken items can't attack, mine, be used, or grant attributes until repaired
