---
title: How to make an Item
sidebar_position: 3
---
# Items
Items are everything that can be bought or sold in the shop. They can be real items or commands, single-purchase, limited purchase, buy only, sell only, both, they can be bought with 2 different currency types - the point is, there's a lot of options to wrap your head around.

These items go into your category config, read here for more into: [categories](https://plugins.auxilor.io/ecoshop/how-to-make-a-category).

# How to add items

## Simple buy-sell item

Let's start with a really simple shop item - if you're making a standard buy-sell shop, this is what most of your items will look like:

```yaml
- id: cooked_mutton # The internal ID of the item, used in commands and placeholders.
  item: cooked_mutton # The item shown in the shop, read here for more info: https://plugins.auxilor.io/the-item-lookup-system
  buy: # The buy options, read below for more info.
    type: coins # The currency type, read here for more info: https://plugins.auxilor.io/all-plugins/prices
    value: 20 # The price, read here for more info: https://plugins.auxilor.io/all-plugins/prices
    display: $%value% # The price display, read here for more info: https://plugins.auxilor.io/all-plugins/prices
  sell: # Same as above, but for selling the item back to the shop.
    type: coins
    value: 10
    display: $%value%
  gui: # The GUI options
    column: 4 # The column to display the item in (1-9)
    row: 1 # The row to display the item in (1-6)
    page: 1 # The page to display the item on
```

## Buy Effect Items

An effect item has no physical item, and when bought, it runs the configured effects. These are configured very similarly to the standard buy-sell items, but with a `buy-effects` section instead `item`.
Because effects are not a physical item, they cannot have a `sell` option, but they still have most of the other options.

With effect items, you could have an item that gives the player a potion effect when they buy it, or even an item that runs a custom effect chain to do something really cool.

```yaml
- id: my_effect_item 
  buy-effects: # The effects to run when the item is bought, read here for more info: https://plugins.auxilor.io/effects/configuring-an-effect
    - id: potion_effect
      args:
        effect: speed
        level: 1
        duration: 2000
  buy:
    value: 65
    type: crystals
    display: "&b%value% Crystals ❖"
  gui: 
    display: # Effects are not items, so you need to configure a display item here.
      item: nether_star name:"&fCool Effect Item" # The item shown in the shop, read here for more info: https://plugins.auxilor.io/the-item-lookup-system
      lore: # The lore of the item shown in the shop.
        - "&fBuy me to do something cool!"
    column: 6
    row: 3
    page: 1
```

# Additional Config Options

## General

### `name`

Overrides the display name of the item shown in the shop GUI. If not set, the name is inherited from the item itself.

```yaml
- id: cooked_mutton
  item: cooked_mutton
  name: "&6&lMutton Chop"
```

### `buy-effects`

Effects to run when the item is bought. Can be added to any item alongside a regular `item:`, not just effect-only items. Read here for more info: [Configuring an Effect](https://plugins.auxilor.io/effects/configuring-an-effect).

```yaml
buy-effects:
  - id: broadcast
    args:
      message: "&f%player% just bought %item%!"
```

## Buying (Applies to alt-buy too)

### Alt-Buy

EcoShop supports buying items with multiple currencies using the `alt-buy` options. All the options that work with `buy` also apply to `alt-buy`. These are configured the same way, using the [price](https://plugins.auxilor.io/all-plugins/prices) system.

```yaml
    alt-buy:
      value: 65
      type: crystals
      display: "&b%value%❖"
```
A dual currency item will show both prices in the shop, and the player can choose which one to buy with. This looks like this in the config:

```yaml
- id: cooked_mutton
  item: cooked_mutton
  buy:
    type: coins
    value: 20
    display: 
  alt-buy:
    type: crystals
    value: 65
    display: "&b%value%❖"
```

### `conditions`

Any conditions that must be met to buy the item. Read here for more info: [Configuring a Condition](https://plugins.auxilor.io/effects/configuring-a-condition).

```yaml
buy:
  conditions:
    - id: has_permission
      args:
        permission: group.iron  
```

### `require`

An expression that must evaluate to true for the player to be allowed to buy the item. Read here for more info: [Math](https://plugins.auxilor.io/all-plugins/math).

```yaml
buy:
  require: "%player_level% >= 10"
```

### `limit`

The max times a player can buy this item.

```yaml
buy:
  limit: 1
```

### `global-limit`

The max times all players can buy this item.

```yaml
buy:
  global-limit: 1
```

### `max-at-once`

The max amount of this item a player can buy at once. (Removes the multi-buy GUI).

```yaml
buy:
  max-at-once: 1
```

### `amount`

The amount of items to be bought at once.

```yaml
buy:
  amount: 32
```

### `dynamic-pricing`

Override the category-level dynamic pricing settings for this item's buy price. Any field not set here inherits from the category `dynamic-pricing` block.

```yaml
buy:
  dynamic-pricing:
    enabled: true
    max-increase: 3.0
    max-decrease: 0.5
    formula: "%base_price% * (1 + 0.0781 * log(1 + %buys%) - 0.0781 * log(1 + %sells%))"
```

Read here for more info: [Dynamic Pricing](https://plugins.auxilor.io/ecoshop/dynamic-pricing).

## Sell

### `conditions`

Any conditions that must be met to sell the item. Read here for more info: [Configuring a Condition](https://plugins.auxilor.io/effects/configuring-a-condition).

```yaml
sell:
  conditions:
    - id: has_permission
      args:
        permission: group.iron
```

### `limit`

The max times a player can sell this item.

```yaml
sell:
  limit: 1
```

### `global-limit`

The max times all players can sell this item.

```yaml
sell:
  global-limit: 1
```

### `dynamic-pricing`

Override the category-level dynamic pricing settings for this item's sell price. Any field not set here inherits from the category `dynamic-pricing` block.

```yaml
sell:
  dynamic-pricing:
    enabled: true
    max-increase: 1.5
    max-decrease: 0.3
    formula: "%base_price% * (1 + 0.0781 * log(1 + %buys%) - 0.0781 * log(1 + %sells%))"
```

Read here for more info: [Dynamic Pricing](https://plugins.auxilor.io/ecoshop/dynamic-pricing).

### `sell-effects`

Effects that run when the item is sold. Read here for more info: [Configuring an Effect](https://plugins.auxilor.io/effects/configuring-an-effect).

```yaml
sell-effects:
  - id: broadcast
    args:
      message: "&f%player%&r&f has sold &r%item%&r&f for &b%value%❖&f!"
```

## GUI

### `show-quick-buy-sell`

By default, quick buy/sell lore is shown on item slots. Set to `false` to hide it.

```yaml
gui:
  show-quick-buy-sell: false
```

### `display`

Overrides the item shown in the shop GUI slot without affecting the item given to the player on purchase. Useful for adding custom lore or a different icon to any item, not just effect items.

```yaml
gui:
  display:
    item: diamond name:"&bSpecial Diamond" # The item shown in the shop GUI
    lore:
      - "&7A very special diamond!"
```

### `display.bottom-lore`

Lore appended below the price information on the shop slot. Rendered per-player, so placeholders are supported.

```yaml
gui:
  display:
    bottom-lore:
      - "&7You have bought &e%playerbuys%&7 of these!"
```

## Internal Placeholders

| Placeholder         | Value                                                       |
|---------------------|-------------------------------------------------------------|
| `%amount%`          | The amount of items the player bought                       |
| `%value%`           | The buy/sell value, to use in price display                 |
| `%value_commas%`    | The comma separated buy/sell value, to use in price display |
| `%playerlimit%`     | The per-player purchase limit for the item                  |
| `%playerbuys%`      | The amount of times the player has bought this item         |
| `%globallimit%`     | The global purchase limit for the item                      |
| `%globalbuys%`      | The amount of times the item has been bought globally       |
| `%playerselllimit%` | The per-player sell limit for the item                      |
| `%playersells%`     | The amount of times the player has sold this item           |
| `%globalselllimit%` | The global sell limit for the item                          |
| `%globalsells%`     | The amount of times the item has been sold globally         |