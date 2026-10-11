1.2.1
---
- Fix kept-broken shields being removed from the hand, and kept-broken bows firing PlayerDestroyItemEvent; entities now stop using a broken item on the next tick
- Mending still repairs broken items
- Negative damage (repairs done through hurtAndBreak) works on broken items, and the broken marker is cleared on any repair the next time the item is damaged
- Soulbound stash keeps items with components from removed mods, saves entry by entry, and drops overflow for creative players instead of deleting it


