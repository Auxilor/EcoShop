---
title: "Dynamic Pricing"
sidebar_position: 4
---

Dynamic pricing lets shop prices move on their own in response to how much players buy and sell across the whole server. By the end of this page you will understand how the system reacts to demand, how to configure it on a category, how to write and tune the formula, how decay pulls prices back over time, and how to override any of it on a single item.

## How dynamic pricing works

Prices rise when an item is bought and fall when it is sold, server-wide. Every purchase and sale updates two counters for the item, a buy counter and a sell counter. A formula turns those counters into a multiplier on the item's base price, and the result is clamped between a configured floor and ceiling so prices can never run away.

The default formula reacts to **net demand**, i.e. buys minus sells, so:

- Buying and selling in equal numbers cancel out, and the price stays at base regardless of volume.
- A run of net buys pushes the price up toward the ceiling.
- A run of net sells pushes the price down toward the floor.

## Configuring it on a category

Dynamic pricing is set per category, in the category config file, and applies to every item in that category unless an item overrides it. Set `enabled: true` and pick your caps and formulas.

```yaml
dynamic-pricing:
  enabled: false # Master switch for the whole category.
  max-increase: 1.5 # Ceiling, as a multiple of base price (1.5 = 150%).
  max-decrease: 0.5 # Floor, as a multiple of base price (0.5 = 50%).
  buy:
    enabled: true # Apply dynamic pricing to the buy price.
    formula: "%base_price% * (1 + 0.0781 * log(1 + max(%buys% - %sells%, 0)) - 0.0781 * log(1 + max(%sells% - %buys%, 0)))"
  alt-buy:
    enabled: true
    formula: "%base_price% * (1 + 0.0781 * log(1 + max(%buys% - %sells%, 0)) - 0.0781 * log(1 + max(%sells% - %buys%, 0)))"
  sell:
    enabled: true
    formula: "%base_price% * (1 + 0.0781 * log(1 + max(%buys% - %sells%, 0)) - 0.0781 * log(1 + max(%sells% - %buys%, 0)))"
```

Each price type (`buy`, `alt-buy`, `sell`) can carry its own `enabled`, `max-increase`, `max-decrease`, and `formula`, overriding the category-wide values for that one price type:

```yaml
dynamic-pricing:
  enabled: true
  max-increase: 1.5
  max-decrease: 0.5
  buy:
    max-increase: 2.0 # The buy price may climb higher than other prices.
    formula: "%base_price% * (1 + 0.0781 * log(1 + max(%buys% - %sells%, 0)) - 0.0781 * log(1 + max(%sells% - %buys%, 0)))"
  sell:
    enabled: false # No dynamic pricing on the sell price in this category.
```

This block lives in the category file; see [How to make a Category](how-to-make-a-category) for where it sits.

## Writing a formula

The formula is plain math that returns the new price, evaluated whenever the price is shown. It has three placeholders to work with:

| Placeholder | Value |
| --- | --- |
| `%base_price%` | The item's configured base price |
| `%buys%` | Total server-wide buys of this item |
| `%sells%` | Total server-wide sells of this item |

See the [Math](https://hub.auxilor.io/wiki/eco/math) guide for the operators and functions you can use. The shipped default is:

```text
%base_price% * (1 + 0.0781 * log(1 + max(%buys% - %sells%, 0)) - 0.0781 * log(1 + max(%sells% - %buys%, 0)))
```

It uses `max(..., 0)` on each side so that only the winning direction (net buys or net sells) moves the price, and the logarithm makes early transactions matter more than later ones.

## Tuning how fast prices move

The `0.0781` coefficient controls how many net transactions it takes to reach a cap; lower it to make prices move more slowly, raise it to make them move faster. With the default, roughly 600 net buys reach `max-increase` and 600 net sells reach `max-decrease`.

To target a specific number of transactions, compute the coefficient from your `max-increase`:

```text
coefficient = (max-increase - 1) / ln(1 + target_transactions)
```

For example, to reach `max-increase: 1.5` at 1000 net buys:

```text
coefficient = 0.5 / ln(1001), i.e. about 0.0724
```

## Decay

Decay slowly returns prices toward base when activity dies down. Without it, the buy and sell counters persist forever, so a price stays elevated or depressed until enough opposite transactions arrive. Decay multiplies both counters by `(1 - rate)` every period, shrinking net demand and drifting the price home.

```yaml
dynamic-pricing:
  enabled: true
  decay:
    enabled: true # Off by default.
    rate: 0.1 # Fraction of both counters removed each period (0.1 = 10%).
    period: 1440 # Period length in minutes (1440 = 24 hours).
```

Because both counters shrink proportionally, equal buying and selling still cancels out, but a long quiet spell always pulls the price back to base. Decay is category-level only: it is inherited by every item in the category, including items with their own pricing overrides, and cannot be set per item. To wipe an item's counters immediately, run `/ecoshop resetdynamicpricing <id/all>`.

## Overriding pricing per item

A single item can override the category's pricing by placing a `dynamic-pricing` block inside its `buy`, `alt-buy`, or `sell` section, alongside `value` and `type`. Any field you leave out inherits from the category block.

```yaml
- id: diamond
  item: diamond
  gui:
    row: 3
    column: 5
    page: 1
  buy:
    value: 100
    type: coins
    display: "$%value%"
    dynamic-pricing: # Override the buy price for this item only.
      enabled: true
      max-increase: 3.0 # This item's buy price can reach 300% of base.
      max-decrease: 0.5
      formula: "%base_price% * (1 + 0.0781 * log(1 + max(%buys% - %sells%, 0)) - 0.0781 * log(1 + max(%sells% - %buys%, 0)))"
  sell:
    value: 50
    type: coins
    display: "$%value%"
    dynamic-pricing:
      enabled: false # This item's sell price stays fixed.
```

See [How to make an Item](how-to-make-an-item) for the rest of the item format.

<hr/>

## Where to go next

- **Set it up:** [How to make a Category](how-to-make-a-category) is where the category-level block lives.
- **Override per item:** [How to make an Item](how-to-make-an-item) covers per-item pricing.
- **Reset counters:** [Commands and Permissions](commands-and-permissions) lists `/ecoshop resetdynamicpricing`.