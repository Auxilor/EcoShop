---
title: "How to Make an Item"
sidebar_position: 3
---

An **item** is a single entry in a category's `items` list, the thing a player actually buys or sells. It carries a **buy** price, a **sell** price, or both, a **display** in the menu, and optional **effects** that run on purchase. Items can be physical items, commands, or pure effects, with per-player and server-wide limits. This page covers writing one.

## Quick start

1. Open a category file in `categories/`, or copy one of the example items out of `categories/_example.yml`.
2. Under that category's `items:` list, add an entry with a unique `id`.
3. Set `item:` to what the player receives, e.g. `cooked_mutton`.
4. Add a `buy` block, a `sell` block, or both, each with a `type`, `value`, and `display`.
5. Set the `gui` `row`, `column`, and `page` so the icon has a place in the menu.
6. Save, then run `/ecoshop reload`.
7. Open the shop, find the item, and buy it to confirm it works.

:::tip
`categories/_example.yml` ships ready-made items, including buy-sell, effect, and dynamic-pricing examples, and is **never loaded**, so copy an item out of it into a real category file as your starting point. Items live inside category files, so they are organised by organising those category files.
:::

## Naming and IDs

Unlike shops and categories, an item's ID is the `id:` field you set, not a file name. That ID is what commands and placeholders use, so keep it unique within the category. The `item:` field uses the [Item Lookup System](https://plugins.auxilor.io/the-item-lookup-system) syntax.

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the item will not load.
:::

## The structure of an item

An item is made of four parts:

| Part | What it controls |
| --- | --- |
| **Display** | The icon, name, and lore shown in the menu |
| **Buying** | The buy price, optional alt-buy currency, and purchase limits |
| **Selling** | The sell price and sell limits |
| **Effects and conditions** | Effects on purchase or sale, and conditions that gate the trade |
| **Rotation** | Optional; makes the item eligible to fill a rotating shop slot instead of a fixed one |

Here is a complete item with the common parts in place:

```yaml
# === Display: the icon and name in the menu ===
- id: enchanted_diamond # Internal ID, used in commands and placeholders, never shown to players.
  item: ecoitems:enchanted_diamond # The item bought or sold; uses Item Lookup System syntax.
  name: "&bEnchanted Diamond" # Optional; overrides the icon's name in the menu.
  gui:
    row: 3 # Row in the menu (1-6).
    column: 4 # Column in the menu (1-9).
    page: 1 # Page the icon sits on.

  # === Buying: price and limits to purchase ===
  buy:
    type: coins # Currency type; see the prices system.
    value: 500 # Price; can be a plain number or an expression.
    display: "$%value%" # How the price renders in the menu.
    amount: 8 # Optional; default buy amount, defaults to 1.
    max-at-once: 16 # Optional; cap per purchase, removes the multi-buy GUI.
    limit: 10 # Optional; max times each player can buy, defaults to unlimited.
    global-limit: 100000 # Optional; max buys across all players, defaults to unlimited.
  alt-buy: # Optional second currency the player can choose instead.
    type: crystals
    value: 65
    display: "&b%value% ❖"

  # === Selling: price and limits to sell back ===
  sell:
    type: coins
    value: 100
    display: "$%value%"
    limit: 256 # Optional; max times each player can sell.

  # === Effects and conditions: extra behaviour ===
  buy-effects: # Optional; run on purchase, with or without a physical item.
    - id: run_command
      args:
        command: "lp user %player% parent set iron"
```

### Display

The icon is whatever `item:` resolves to, but you can override its name and lore, or replace the shown icon entirely without changing what the player receives.

```yaml
- id: cooked_mutton
  item: cooked_mutton # What the player actually gets.
  name: "&6&lMutton Chop" # Optional; overrides only the menu name.
  gui:
    row: 1
    column: 4
    page: 1
    show-quick-buy-sell: false # Optional; hides the default quick buy/sell lore.
    display: # Optional; change the shown icon without changing the purchased item.
      item: diamond name:"&bSpecial Diamond"
      lore:
        - "&7A very special diamond!"
      bottom-lore: # Lore under the price, rendered per player, so placeholders work.
        - "&7You have bought &e%playerbuys%&7 of these!"
```

### Buying

The `buy` block sets the price. Only `type`, `value`, and `display` are required; everything after is an optional field you add on top.

```yaml
buy:
  type: coins # Currency; see the prices system.
  value: 500 - (%player_rank% * 25) # Price; expressions are allowed.
  display: "$%value%" # How the price renders in the menu.
```

:::info Values are expressions
`value` and `require` are evaluated as math, so you can use placeholders and arithmetic, e.g. `500 - (%player_rank% * 25)`. See the [Math](https://plugins.auxilor.io/all-plugins/math) and [Prices](https://plugins.auxilor.io/all-plugins/prices) guides.
:::

#### `alt-buy`

A second currency the player can choose instead, with the same fields as `buy`. The menu shows both prices side by side.

```yaml
alt-buy:
  type: crystals
  value: 65
  display: "&b%value% ❖"
```

#### `amount`

The default, or quick-buy, amount handed over per purchase. Defaults to `1`.

```yaml
buy:
  amount: 8
```

#### `max-at-once`

Caps how many a player can buy in one go, and removes the multi-buy GUI. Defaults to unlimited.

```yaml
buy:
  max-at-once: 16
```

#### `limit`

The maximum number of times each player may buy this item. Defaults to unlimited.

```yaml
buy:
  limit: 1
```

#### `global-limit`

The maximum number of times all players combined may buy this item. Defaults to unlimited.

```yaml
buy:
  global-limit: 1
```

#### `require`

A math expression that must hold true for the player to buy. Defaults to always allowed.

```yaml
buy:
  require: "%player_level% >= 10"
```

#### `conditions`

Eco conditions that must pass before the purchase is allowed. The same key works under `sell`.

```yaml
buy:
  conditions:
    - id: has_permission
      args:
        permission: group.iron
```

#### `dynamic-pricing`

Makes this one item's buy price react to demand, overriding the category. Any field you omit inherits from the category block. See [Dynamic Pricing](dynamic-pricing).

```yaml
buy:
  dynamic-pricing:
    enabled: true
    max-increase: 3.0
    max-decrease: 0.5
```

### Selling

The `sell` block mirrors `buy` and lets players sell the item back, with the same required `type`, `value`, and `display`.

```yaml
sell:
  type: coins
  value: 100 + (%player_rank% * 25)
  display: "$%value%"
```

#### `limit`

The maximum number of times each player may sell this item. Defaults to unlimited.

```yaml
sell:
  limit: 256
```

#### `global-limit`

The maximum number of times all players combined may sell this item. Defaults to unlimited.

```yaml
sell:
  global-limit: 100000
```

#### `conditions`

Eco conditions that must pass before the sale is allowed, exactly as under `buy`.

```yaml
sell:
  conditions:
    - id: below_y
      args:
        y: 100
```

#### `dynamic-pricing`

Overrides the category's sell pricing for this item only, inheriting any field you leave out. See [Dynamic Pricing](dynamic-pricing).

```yaml
sell:
  dynamic-pricing:
    enabled: false
```

### Effects and conditions

Effects run extra behaviour on a trade, and conditions gate whether the trade is allowed. An item with `buy-effects` and no `item:` is a pure effect item: it has no physical drop, so it cannot be sold, and it needs a `gui.display` to give the icon something to show.

```yaml
- id: iron_rank
  buy-effects: # Run when bought; here, grant a rank.
    - id: run_command
      args:
        command: "lp user %player% parent set iron"
  sell-effects: # Optional; run when the item is sold.
    - id: broadcast
      args:
        message: "&f%player% sold %item% for &b%value%❖&f!"
  gui:
    display: # Required for effect-only items, since there is no item icon.
      item: diamond_chestplate name:"&fIron Rank"
      lore:
        - "&fGrants the Iron rank."
    row: 3
    column: 6
    page: 2
  buy:
    type: crystals
    value: 65
    display: "&b%value% Crystals ❖"
    conditions: # Must pass for the player to buy.
      - id: has_permission
        args:
          permission: group.iron
          inverse: true
```

:::danger Effects are their own system
Effects and conditions are configured by the shared eco system, not by EcoShop, so the full list of effect and condition IDs and their arguments lives there:

- [Configuring an Effect](https://plugins.auxilor.io/effects/configuring-an-effect)
- [Configuring an Effect Chain](https://plugins.auxilor.io/effects/configuring-a-chain)
:::

### Rotation

An item with a `rotation` block is eligible to fill a rotating slot (marked `r` in the category's mask `pattern`) instead of sitting in one fixed place, only in categories that have `rotation.enabled: true`. This is its own system with its own resolution order (fixed-slot, then always-show, then weighted random); see [Rotating Shops](rotating-shops) for the full reference.

```yaml
rotation:
  enabled: true # Make this item eligible for rotation. Defaults to false.
  weight: 10 # (Optional) Relative chance of being drawn against other weighted items. Defaults to 1.
  always-show: false # (Optional) Fill a slot every rotation instead of competing in the weighted draw.
```

:::warning
Weighted and always-show rotation items must omit `gui.row` / `gui.column` / `gui.page`, or place it on a cell that isn't `r` in the pattern. A `gui` position on an `r` cell turns the item into a fixed-slot item instead, which always returns to that same cell every rotation.
:::

## Internal placeholders

These placeholders are available in an item's `display`, `bottom-lore`, and price `display` fields:

| Placeholder | Value |
| --- | --- |
| `%amount%` | The amount of items the player bought |
| `%value%` | The buy/sell value, for use in a price display |
| `%value_commas%` | The comma-separated buy/sell value |
| `%playerlimit%` | The per-player purchase limit for the item |
| `%playerbuys%` | The number of times the player has bought this item |
| `%globallimit%` | The global purchase limit for the item |
| `%globalbuys%` | The number of times the item has been bought globally |
| `%playerselllimit%` | The per-player sell limit for the item |
| `%playersells%` | The number of times the player has sold this item |
| `%globalselllimit%` | The global sell limit for the item |
| `%globalsells%` | The number of times the item has been sold globally |

:::tip Troubleshooting
- **Item icon is missing or barren?** It is an effect-only item with no `gui.display`. Add a `display` with an `item` and `lore`.
- **Players cannot buy it?** A `require` expression or a `conditions` entry is failing, or a `limit` is reached. Check those against the player.
- **Price shows blank?** The `display` is empty or omits `%value%`. Set `display: "$%value%"`.
:::

<hr/>

## Where to go next

- **Place items in context:** [How to make a Category](how-to-make-a-category) shows the file these items live in.
- **Make prices move:** [Dynamic Pricing](dynamic-pricing) covers the `dynamic-pricing` overrides referenced above.
- **Rotate the stock:** [Rotating Shops](rotating-shops) covers the `rotation` block referenced above in full.
- **Tune global menus:** [Plugin Config](plugin-config) controls the shared buy and sell GUIs.