---
title: "How to Make a Sell Wand"
sidebar_position: 4
---

A **sell wand** is an item that sells the contents of a container at shop prices when a player right-clicks it. A wand defines its **item**, its **limits**, which items it **sells**, and a left-click **inspect** preview. This page covers making one from scratch.

## Quick start

1. Open the `/sellwands/` folder inside the EcoShop plugin folder.
2. Copy `_example.yml` and rename it to your wand's ID, e.g. `golden.yml`.
3. Set the `name`, `item`, and `lore` for the wand.
4. Set `uses`, `cooldown`, and `multiplier`.
5. Run `/ecoshop reload`, then `/ecoshop givewand <player> golden` to hand the wand out.
6. Put some sellable items in a chest and right-click it with the wand to confirm they sell.

:::tip
`_example.yml` is included as a reference and is **never loaded**, so copy or rename it to make a real wand. `basic.yml` ships as a simple working wand. You can also organise wands into subfolders inside `sellwands/`, and they'll still load.
:::

## Naming and IDs

The file name without `.yml` is the wand's ID. This is what `/ecoshop givewand` references. Every wand is also a custom item with the ID `ecoshop:sellwand_<id>`, so you can sell it in a shop, use it in recipes, or give it with effects. The `item` uses the [Item Lookup System](https://plugins.auxilor.io/the-item-lookup-system) format, so you can use vanilla items, custom items, and modifiers.

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the wand will not load.
:::

## The structure of a sell wand

| Part | What it controls |
| --- | --- |
| **Item** | The wand item, its name, and its lore |
| **Limits** | Uses, cooldown, period limit, and per-use caps |
| **Containers** | Which blocks the wand works on, and whether it needs sneaking |
| **Filters** | Which items the wand sells, and extra multipliers |
| **Bypass** | Shop checks the wand skips |
| **Conditions** | Conditions the player must meet to use the wand |
| **Inspect** | The left-click sale preview |

```yaml
# === Item: the wand item ===
name: "&6Golden Sell Wand" # The display name used in messages.
item: blaze_rod glint name:"&6Golden Sell Wand" # The wand item.
lore: # Lore applied to the wand.
  - "&7Right-click a container to sell its contents."
  - "&7Left-click to preview."
  - "&7Uses left: &e%uses%"
  - "&7Multiplier: &a%multiplier%x"
  - "&7Sold: &a%sold_items%&7 items for &a%sold_value%"

# === Limits: how often and how much the wand sells ===
uses: 100 # Uses before the wand is empty, -1 for infinite.
cooldown: 5 # Seconds between uses, 0 for none.
period-limit:
  uses: 20 # Max uses per period, -1 for none.
  period: 1440 # Period length in minutes.
max-items-per-use: 1728 # Max items sold per use, -1 for none.
max-value-per-use: 50000 # Max money earned per use, -1 for none.
multiplier: 1.5 # Sell price multiplier.
break-when-empty: true # If the wand is removed when it runs out of uses.

# === Containers: where the wand works ===
containers: # Container blocks the wand works on.
  - chest
  - trapped_chest
  - barrel
  - shulker_box
require-sneak: false # If the player must sneak to use the wand.

# === Filters: what the wand sells ===
filters:
  shops: [ ] # Only sell items from these shops, empty for all shops.
  whitelist: [ ] # Only sell these items, empty for all items.
  blacklist: [ ] # Never sell these items.
  multipliers: # Extra multipliers for specific items.
    - rule: "category:minerals"
      multiplier: 2.0
    - rule: "cobblestone"
      multiplier: 0.5

# === Bypass: shop checks the wand skips ===
bypass:
  dynamic-pricing: false # Pay the base price and don't move the dynamic price.
  player-limits: false # Ignore sell.limit.
  global-limits: false # Ignore sell.global-limit.

# === Conditions: who can use the wand ===
conditions: [ ] # Conditions the player must meet to use the wand.

# === Inspect: the left-click preview ===
inspect:
  enabled: true # If left-clicking a container previews the sale.
  display: message # message, hologram, or both.
  duration: 60 # Ticks the hologram stays.
  height: 1.5 # Blocks above the container the hologram shows.
  lines: # The hologram lines.
    - "&fSells &a%amount%&f items"
    - "&ffor &a%price%"
    - "&7%unsold% stacks can't be sold"
```

### Item

The wand item players hold, with its name and lore. The lore is updated after every use, so it can show the wand's remaining uses and lifetime stats.

```yaml
name: "&6Golden Sell Wand" # The display name used in messages.
item: blaze_rod glint name:"&6Golden Sell Wand" # The wand item.
lore: # Lore applied to the wand.
  - "&7Right-click a container to sell its contents."
  - "&7Uses left: &e%uses%"
  - "&7Sold: &a%sold_items%&7 items for &a%sold_value%"
```

:::info
A wand only sells items that are sellable in a shop, at that item's shop price. Every sell rule on the shop item still applies, including `limit`, `global-limit`, `conditions`, and the `ecoshop.sell.<id>` permission. Anything the wand can't sell stays in the container.
:::

### Limits

How often a wand can be used, and how much it sells each time.

```yaml
uses: 100 # Uses before the wand is empty, -1 for infinite.
cooldown: 5 # Seconds between uses, 0 for none.
period-limit:
  uses: 20 # Max uses per period, -1 for none.
  period: 1440 # Period length in minutes.
max-items-per-use: 1728 # Max items sold per use, -1 for none.
max-value-per-use: 50000 # Max money earned per use, -1 for none.
multiplier: 1.5 # Sell price multiplier.
break-when-empty: true # If the wand is removed when it runs out of uses.
```

A use is only spent when something sells, so right-clicking an empty chest costs nothing. When a wand runs out of uses it's removed, or if `break-when-empty` is `false`, it stays and stops working. The `period-limit` window starts on the first use and resets once `period` minutes have passed. `max-value-per-use` only counts items sold for money (`eco:economy` prices).

The `multiplier` stacks with everything else, so an item's final price is:

```text
base price × dynamic pricing × boosters × wand multiplier × filter multiplier
```

:::warning
`cooldown` and `period-limit` are tracked per player **per wand type**, not per wand item. Swapping to another wand of the same type doesn't get around them.
:::

### Containers

Which blocks the wand works on. Right-clicking any other block behaves normally.

```yaml
containers: # Container blocks the wand works on.
  - chest
  - trapped_chest
  - barrel
  - shulker_box
require-sneak: false # If the player must sneak to use the wand.
```

`shulker_box` matches every colour of shulker box, and a double chest sells both halves. Wands respect claim and protection plugins, so a player can't sell from a container they aren't allowed to open. Set `require-sneak: true` if players should still be able to open containers with a wand in hand.

### Filters

Which items the wand sells, and extra multipliers for specific ones. A rule is either a shop item ID, e.g. `diamond`, or a category, e.g. `category:minerals`.

```yaml
filters:
  shops: [ ] # Only sell items from these shops, empty for all shops.
  whitelist: [ ] # Only sell these items, empty for all items.
  blacklist: [ ] # Never sell these items.
  multipliers: # Extra multipliers for specific items.
    - rule: "category:minerals"
      multiplier: 2.0
    - rule: "cobblestone"
      multiplier: 0.5
```

An item has to be in one of the `shops` and pass the `whitelist` to sell, and the `blacklist` wins over both. If more than one multiplier rule matches an item, a shop item rule wins over a category rule.

:::tip
A rule for a shop, shop item, or category that doesn't exist is skipped, with a warning in the console on reload.
:::

### Bypass

Shop checks the wand skips. A bypassed check is ignored, and sales from the wand don't count towards it.

```yaml
bypass:
  dynamic-pricing: false # Pay the base price and don't move the dynamic price.
  player-limits: false # Ignore sell.limit.
  global-limits: false # Ignore sell.global-limit.
```

### Conditions

Conditions the player must meet to use the wand, checked on every use.

```yaml
conditions:
  - id: has_permission
    args:
      permission: group.vip
```

:::info
Conditions are configured by the shared eco system, not by EcoShop. See [Configuring an Effect](https://plugins.auxilor.io/effects/configuring-an-effect) for the full list of conditions and their arguments.
:::

### Inspect

Left-clicking a container with the wand previews the sale without selling anything, so no uses or cooldown are spent.

```yaml
inspect:
  enabled: true # If left-clicking a container previews the sale.
  display: message # message, hologram, or both.
  duration: 60 # Ticks the hologram stays.
  height: 1.5 # Blocks above the container the hologram shows.
  lines: # The hologram lines.
    - "&fSells &a%amount%&f items"
    - "&ffor &a%price%"
    - "&7%unsold% stacks can't be sold"
```

`message` sends the `sellwand.inspect` message from `lang.yml`, and `hologram` shows `lines` above the container, visible only to that player. The previewed price doesn't include boosters, since those are only applied when the sale happens.

## Internal placeholders

These placeholders are available in the wand's `lore`:

| Placeholder | Value |
| --- | --- |
| `%uses%` | The wand's remaining uses, or `∞` if infinite |
| `%max_uses%` | The wand type's `uses`, or `∞` if infinite |
| `%multiplier%` | The wand's `multiplier` |
| `%sold_items%` | The number of items this wand has sold |
| `%sold_value%` | The value this wand has sold, shown with each price's display, e.g. `$1,500 and 6 Crystals` |

These placeholders are available in `inspect.lines`:

| Placeholder | Value |
| --- | --- |
| `%amount%` | The number of items that would sell |
| `%price%` | The value those items would sell for |
| `%unsold%` | The number of stacks that can't be sold |

:::tip Troubleshooting
- **Wand not loading?** Check the file name is lowercase letters, numbers, and underscores only, and that it isn't prefixed with `_`.
- **Right-clicking opens the container?** The block isn't in `containers`, or `require-sneak` is `true` and the player isn't sneaking.
- **Nothing sells?** The items aren't sellable in a shop, or a filter, sell limit, sell condition, or permission is stopping them.
- **"You can't use a sell wand on this container"?** A protection plugin is denying access to that container.
:::

<hr/>

## Where to go next

- **Items:** make items sellable for wands in [How to Make an Item](how-to-make-an-item).
- **Commands:** give wands with [Commands and Permissions](commands-and-permissions).
- **Defaults:** the shipped sell wands live [here](https://github.com/Auxilor/EcoShop/tree/master/eco-core/core-plugin/src/main/resources/sellwands).
