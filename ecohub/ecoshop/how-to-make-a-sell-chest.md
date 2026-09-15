---
title: "How to Make a Sell Chest"
sidebar_position: 5
---

A **sell chest** is a placeable chest that automatically sells its contents at shop prices, so players can feed it with hoppers and get paid. A sell chest defines its **item**, how often it **sells**, which items it **sells**, and an optional **hologram** showing what it has earned. This page covers making one from scratch.

## Quick start

1. Open the `/sellchests/` folder inside the EcoShop plugin folder.
2. Copy `_example.yml` and rename it to your sell chest's ID, e.g. `basic.yml`.
3. Set the `name`, `item`, and `lore` for the sell chest.
4. Set `block` and `sell-interval`.
5. Run `/ecoshop reload`, then `/ecoshop givesellchest <player> basic` to hand the sell chest out.
6. Place it, put some sellable items inside, and wait a few seconds to confirm they sell.

:::tip
`_example.yml` is included as a reference and is **never loaded**, so copy or rename it to make a real sell chest. `basic.yml` ships as a simple working sell chest. You can also organise sell chests into subfolders inside `sellchests/`, and they'll still load.
:::

## Naming and IDs

The file name without `.yml` is the sell chest's ID. This is what `/ecoshop givesellchest` references. Every sell chest is also a custom item with the ID `ecoshop:sellchest_<id>`, so you can sell it in a shop, use it in recipes, or give it with effects. The `item` uses the [Item Lookup System](https://plugins.auxilor.io/the-item-lookup-system) format, so you can use vanilla items, custom items, and modifiers.

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the sell chest will not load.
:::

## The structure of a sell chest

| Part | What it controls |
| --- | --- |
| **Item** | The sell chest item, its name, lore, and the block it places |
| **Selling** | How often it sells, what happens to unsellable items, and owner notifications |
| **Filters** | Which items the sell chest sells |
| **Bypass** | Shop checks the sell chest skips |
| **Hologram** | An optional hologram above the chest showing its stats |

```yaml
# === Item: the sell chest item ===
name: "&aSell Chest" # The display name used in messages.
item: chest glint name:"&aSell Chest" # The sell chest item.
lore: # Lore applied to the item.
  - "&7Automatically sells its contents."
  - "&7Sold: &a%sold_items%&7 items for &a%sold_value%"
block: chest # The placed block: chest, trapped_chest, or barrel.

# === Selling: when and how it sells ===
sell-interval: 100 # Ticks between sales.
overflow: keep # keep or stop; what happens when unsellable items are left inside.
notify: true # If the owner gets an actionbar summary of sales.

# === Filters: what the sell chest sells ===
filters:
  shops: [ ] # Only sell items from these shops, empty for all shops.
  whitelist: [ ] # Only sell these items, empty for all items.
  blacklist: [ ] # Never sell these items.

# === Bypass: shop checks the sell chest skips ===
bypass:
  dynamic-pricing: false # Pay the base price and don't move the dynamic price.
  player-limits: false # Ignore sell.limit.
  global-limits: false # Ignore sell.global-limit.

# === Hologram: the stats above the chest ===
hologram:
  enabled: false # If a hologram shows above the chest.
  height: 1.5 # Blocks above the chest the hologram shows.
  lines: # The hologram lines.
    - "%type% &7(%owner%)"
    - "&fSold: &a%sold_items%"
    - "&fEarned: &a%sold_value%"
```

### Item

The sell chest item players place, with its name, lore, and the block it becomes.

```yaml
name: "&aSell Chest" # The display name used in messages.
item: chest glint name:"&aSell Chest" # The sell chest item.
lore: # Lore applied to the item.
  - "&7Automatically sells its contents."
  - "&7Sold: &a%sold_items%&7 items for &a%sold_value%"
block: chest # The placed block: chest, trapped_chest, or barrel.
```

The item always places as `block`, even if `item` is a different material. When a sell chest is broken, it drops its contents and a sell chest item that keeps its lifetime stats, so placing it again carries on from where it left off.

:::info
A sell chest only sells items that are sellable in a shop, at that item's shop price. Every sell rule on the shop item still applies, including `limit`, `global-limit`, `conditions`, and the `ecoshop.sell.<id>` permission. Anything it can't sell stays in the chest.
:::

### Selling

How often the sell chest sells, what happens to items it can't sell, and whether the owner is told about sales.

```yaml
sell-interval: 100 # Ticks between sales.
overflow: keep # keep or stop; what happens when unsellable items are left inside.
notify: true # If the owner gets an actionbar summary of sales.
```

A sell chest only sells when something new has gone into it, from a hopper or a player closing it. `sell-interval` can't be faster than `sell-chests.sweep-interval` in `config.yml`.

With `overflow: stop`, a sell chest that's left holding unsellable items stops accepting items from hoppers until the owner opens and closes it. With `overflow: keep`, hoppers keep filling it.

`notify` sends the owner an actionbar message at most every `sell-chests.notify-every` sales, and only while they're online.

:::warning Boosters don't apply
Sell chests pay the shop price without sell multipliers from boosters or other plugins, so they don't stack with them.
:::

### Filters

Which items the sell chest sells. A rule is either a shop item ID, e.g. `diamond`, or a category, e.g. `category:minerals`.

```yaml
filters:
  shops: [ ] # Only sell items from these shops, empty for all shops.
  whitelist: [ ] # Only sell these items, empty for all items.
  blacklist: [ ] # Never sell these items.
```

An item has to be in one of the `shops` and pass the `whitelist` to sell, and the `blacklist` wins over both. Unlike sell wands, sell chests don't support `multipliers`.

:::tip
A rule for a shop, shop item, or category that doesn't exist is skipped, with a warning in the console on reload.
:::

### Bypass

Shop checks the sell chest skips. A bypassed check is ignored, and sales from the sell chest don't count towards it.

```yaml
bypass:
  dynamic-pricing: false # Pay the base price and don't move the dynamic price.
  player-limits: false # Ignore sell.limit.
  global-limits: false # Ignore sell.global-limit.
```

### Hologram

A hologram above the chest showing what it has sold. It updates after each sale.

```yaml
hologram:
  enabled: false # If a hologram shows above the chest.
  height: 1.5 # Blocks above the chest the hologram shows.
  lines: # The hologram lines.
    - "%type% &7(%owner%)"
    - "&fSold: &a%sold_items%"
    - "&fEarned: &a%sold_value%"
```

:::info
Holograms also need `sell-chests.holograms: true` in `config.yml`, which turns them off for every sell chest at once. A hologram plugin supported by eco is required.
:::

## Ownership and limits

The player who places a sell chest owns it. Only the owner, or players with `ecoshop.sellchest.bypass-owner`, can open or break it, and other players can't use sell wands on it. Sell chests never merge into double chests.

Players can place up to `sell-chests.default-limit` sell chests. Give a player `ecoshop.sellchest.limit.<n>` to change their limit; the highest one they have wins, and `-1` means unlimited. Use `/ecoshop sellchestcount <player> [set <n>]` to check or fix a player's count, e.g. if a sell chest was removed with WorldEdit.

With `sell-chests.explosion-proof: true`, sell chests survive explosions. Otherwise, they drop their contents and the sell chest item like when they're broken.

## Offline selling

By default, a sell chest only sells while its owner is online, and sells anything that built up as soon as they log back in.

With `sell-chests.offline-selling.enabled: true`, sell chests also sell while their owner is offline, but only items that meet all of these:

- The shop item is sold for money (`eco:economy`).
- Its sell `value` is a fixed number, with no placeholders.
- It has no sell `conditions`.
- The owner had `ecoshop.sellchest.offline` when they last logged out.

Anything else stays in the chest until the owner is online. With `summary-on-join: true`, players are told what their sell chests earned while they were away.

:::warning
Offline selling needs an economy plugin that supports paying offline players. If it doesn't, nothing sells offline and a warning is logged.
:::

## Internal placeholders

These placeholders are available in the sell chest's `lore`:

| Placeholder | Value |
| --- | --- |
| `%sold_items%` | The number of items this sell chest has sold |
| `%sold_value%` | The value this sell chest has sold, shown with each price's display, e.g. `$1,500 and 6 Crystals` |

These placeholders are available in `hologram.lines`:

| Placeholder | Value |
| --- | --- |
| `%type%` | The sell chest's `name` |
| `%owner%` | The owner's name |
| `%sold_items%` | The number of items this sell chest has sold |
| `%sold_value%` | The value this sell chest has sold |

:::tip Troubleshooting
- **Sell chest not loading?** Check the file name is lowercase letters, numbers, and underscores only, and that it isn't prefixed with `_`.
- **Nothing sells?** The items aren't sellable in a shop, or a filter, sell limit, sell condition, or permission is stopping them. Sell chests also only work in loaded chunks.
- **Stops selling when the owner logs out?** Offline selling is off, or the item doesn't meet the offline rules above.
- **Can't place another one?** The player has reached their limit; check it with `/ecoshop sellchestcount`.
:::

<hr/>

## Where to go next

- **Items:** make items sellable for sell chests in [How to Make an Item](how-to-make-an-item).
- **Global options:** tune sell speed, limits, and offline selling in [Plugin Config](plugin-config).
- **Commands:** give sell chests and fix counts with [Commands and Permissions](commands-and-permissions).
- **Defaults:** the shipped sell chests live [here](https://github.com/Auxilor/EcoShop/tree/master/eco-core/core-plugin/src/main/resources/sellchests).
