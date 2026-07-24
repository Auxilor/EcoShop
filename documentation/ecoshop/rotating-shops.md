---
title: "Rotating Shops"
sidebar_position: 4.5
---

Rotating shops let a category swap out a selection of items on a timer, instead of every item sitting in a fixed slot forever. This is a global, server-wide rotation — every player sees the same active items and the same countdown, and the schedule survives restarts. This page covers turning it on, how items are picked each rotation, and how to force or track a rotation.

## How it works

A rotating category dedicates one or more slots in its mask `pattern` to rotation, marked with the character `r` instead of a background number. Every rotation, EcoShop fills those slots from the category's rotation-eligible items, then leaves them alone until the next rotation fires. The whole thing is global and persisted to disk, so it's the same for every player and survives a restart or reload with the countdown intact.

Items can be filled into rotation slots in three ways, resolved in this order:

1. **Fixed-slot** — a rotation item that also has a `gui` position landing on an `r` cell always rotates back into that exact cell.
2. **Always-show** — fills a slot every single rotation, picked before the weighted pool if there's a choice of several.
3. **Weighted random** — the remaining slots are filled by weighted sampling without replacement, so heavier items are more likely to appear, but every draw is a distinct item.

Slots that no rotation item is picked for that round are left empty.

## Enabling it on a category

Add a `rotation` block to the category file, alongside `gui`:

```yaml
rotation:
  enabled: true
  reset-limits: false # (Optional) Reset online players' buy/sell limits for rotating items on every rotation.
  broadcast-message: true # (Optional) Tell every online player when this category rotates. Defaults to true.
  rotation-time:
    interval: 1h # Rotate every 1h. Accepts s/m/h/d.
    # server-time: "04:00" # Rotate daily at this server-local time instead. Mutually exclusive with interval.
```

Only one of `rotation-time.interval` or `rotation-time.server-time` should be set; `interval` rotates on a fixed cadence from whenever the timer last fired, `server-time` rotates once a day at a fixed server-local clock time.

### The rotation broadcast

By default, every online player is sent the `rotated` message (from `lang.yml`) whenever the category rotates:

```yaml
rotated: "&fThe &r%category%&f shop has rotated its stock!"
```

Set `rotation.broadcast-message: false` on a category to rotate silently instead.

Then mark the slots that should rotate in the page's mask `pattern` with `r`:

```yaml
gui:
  pages:
    - page: 1
      mask:
        items:
          - gray_stained_glass_pane
        pattern:
          - "222222222"
          - "2rrrrrrr2" # 7 rotation slots on this row.
          - "222222222"
```

A category with `rotation.enabled: false` (or no `rotation` block at all) behaves exactly as before; `r` in a pattern is only meaningful once rotation is turned on.

## Making an item eligible for rotation

Add a `rotation` block to the item, inside the category's `items` list:

```yaml
items:
  - id: rotation_diamond
    item: diamond
    rotation:
      enabled: true
      weight: 10 # Higher weight = more likely to be drawn relative to other weighted items.
    buy:
      value: 100
      type: coins
      display: "$%value%"
```

:::warning Weighted and always-show items must not have a fixed `gui` position on an `r` cell
`gui.row` / `gui.column` / `gui.page` is what makes an item **fixed-slot** (see below). Give a weighted or always-show rotation item a `gui` position that lands on an `r` cell and it silently becomes fixed-slot instead of joining the pool. Leave `gui` off entirely for weighted and always-show items, or point it at a non-`r` cell.
:::

`weight` defaults to `1` if omitted, so every weighted item is equally likely unless you set weights explicitly. `weight` only matters relative to other weighted items in the same category — it has no effect on always-show or fixed-slot items.

### Always-show items

```yaml
rotation:
  enabled: true
  always-show: true
```

An always-show item fills a slot on every rotation (subject to available capacity), rather than competing in the weighted draw.

### Fixed-slot items

Give a rotation item a `gui` position that lands on an `r` cell, and it claims that exact slot every rotation instead of a random one:

```yaml
- id: rotation_gold_ingot
  item: gold_ingot
  rotation:
    enabled: true
  gui:
    row: 2
    column: 5
    page: 1
```

Fixed-slot items are reserved first, before always-show and weighted items are drawn, so they can never be displaced. If a normal (non-rotating) static item already occupies that same `r` cell, the static item wins and a warning is logged; if two fixed-slot items claim the same cell, the first one registered wins and the rest are skipped with a warning.

## Forcing a rotation

```
/ecoshop rotate <category>
```

Immediately re-rolls the category's rotation slots and restarts its timer from now, without waiting for the scheduled rotation. Requires `ecoshop.command.rotate`. Tab-completes category IDs that have rotation enabled.

## Tracking the countdown

With PlaceholderAPI installed, `%ecoshop_rotation_<category>%` returns a countdown to that category's next rotation, formatted from the `rotation-countdown` (or `rotation-ready` once the timer hits zero) keys in `lang.yml`:

```yaml
rotation-countdown: "%hours%h %minutes%m %seconds%s"
rotation-ready: "Rotating soon..."
```

These two keys live at the **top level** of `lang.yml`, not under `messages:`.

Because a category's `lore:` (the lore on its icon inside the parent shop) is rendered with placeholders enabled, you can drop the countdown straight into it:

```yaml
item: diamond_sword name:"&fRotating Gear"
lore:
  - "&aBuy all the best gear here!"
  - "&7Next rotation: &f%ecoshop_rotation_rotating_gear%"
```

<hr/>

## Where to go next

- **Build the category first:** [How to make a Category](how-to-make-a-category) covers the `gui` and mask `pattern` this feature builds on.
- **Build the items:** [How to make an Item](how-to-make-an-item) covers everything else an item can do besides rotation.
- **Force or track a rotation:** [Commands and Permissions](commands-and-permissions) and [PlaceholderAPI](placeholderapi) list `/ecoshop rotate` and `%ecoshop_rotation_<category>%`.
