1.2.2
---
- Zero-damage hits on a broken item no longer re-trigger the break
- Creative players' displaced soulbound items always drop instead of partly vanishing
- Broken-item use stopping now runs on the server only (fixes a blocking desync for remote creative players)

1.2.1
---
- Fix kept-broken shields being removed from the hand, and kept-broken bows firing PlayerDestroyItemEvent; entities now stop using a broken item on the next tick
- Mending repairs broken items at its full rate again
- Negative damage (repairs done through hurtAndBreak) works on broken items, and the broken marker is cleared on any repair the next time the item is damaged
- Soulbound stash keeps items with components from removed mods, saves entry by entry, and drops overflow for creative players instead of deleting it


