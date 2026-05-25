---
title: Dynamic Pricing
sidebar_position: 4
---

## Dynamic Pricing

Dynamic pricing allows shop item prices to fluctuate automatically based on how many times items have been bought and sold across the entire server. As players buy more of an item, the price rises; as players sell more, the price falls.

Prices are driven by configurable math formulas and are clamped between a minimum and maximum percentage of the item's base price.

## Category-Level Configuration

Dynamic pricing is configured per category in the category config file. It applies to every item in that category unless overridden per item.

```yaml
dynamic-pricing:
  enabled: false # Whether dynamic pricing is active for this category.
  max-increase: 1.5 # The maximum price as a multiplier of base price (1.5 = 150%).
  max-decrease: 0.5 # The minimum price as a multiplier of base price (0.5 = 50%).

  decay:
    enabled: false # Whether price decay is active for this category.
    rate: 0.1      # Percentage of counters removed per period (0.1 = 10%).
    period: 1440   # Period in minutes (1440 = 24 hours).

  buy:
    enabled: true
    formula: "%base_price% * (1 + 0.0781 * log(1 + max(%buys% - %sells%, 0)) - 0.0781 * log(1 + max(%sells% - %buys%, 0)))"

  alt-buy:
    enabled: true
    formula: "%base_price% * (1 + 0.0781 * log(1 + max(%buys% - %sells%, 0)) - 0.0781 * log(1 + max(%sells% - %buys%, 0)))"

  sell:
    enabled: true
    formula: "%base_price% * (1 + 0.0781 * log(1 + max(%buys% - %sells%, 0)) - 0.0781 * log(1 + max(%sells% - %buys%, 0)))"
```

### `enabled`

Whether dynamic pricing is active for this category. When `false`, all item prices remain at their configured base values.

### `max-increase`

The maximum price multiplier. A value of `1.5` means prices can rise to at most 150% of the item's base price.

### `max-decrease`

The minimum price multiplier. A value of `0.5` means prices can fall to at most 50% of the item's base price.

### Per-price-type options

Each price type (`buy`, `alt-buy`, `sell`) supports:

| Key | Description |
|---|---|
| `enabled` | Whether dynamic pricing is active for this price type. Defaults to the category `enabled` value. |
| `max-increase` | Override `max-increase` for this price type only. |
| `max-decrease` | Override `max-decrease` for this price type only. |
| `formula` | The math formula used to calculate the price. |

```yaml
dynamic-pricing:
  enabled: true
  max-increase: 1.5
  max-decrease: 0.5
  buy:
    max-increase: 2.0 # Buy price can go higher than sell
    formula: "%base_price% * (1 + 0.0781 * log(1 + max(%buys% - %sells%, 0)) - 0.0781 * log(1 + max(%sells% - %buys%, 0)))"
  sell:
    enabled: false # Disable dynamic pricing for sell price in this category
```

## Formula Placeholders

The following placeholders are available for use in formulas:

| Placeholder | Description |
|---|---|
| `%base_price%` | The item's configured base price value |
| `%buys%` | Total number of times this item has been bought across the whole server |
| `%sells%` | Total number of times this item has been sold across the whole server |

Read here for more info on supported operators and functions: [Math](https://plugins.auxilor.io/all-plugins/math).

## Default Formula

The default formula responds to **net demand** — the difference between buys and sells. Only the net position matters, so buying and selling cancel each other out exactly:

- **600 net buys** (e.g. 600 bought, 0 sold) → price reaches `max-increase`
- **600 net sells** (e.g. 0 bought, 600 sold) → price reaches `max-decrease`
- **Equal buys and sells** → price stays at base, regardless of volume

```
%base_price% * (1 + 0.0781 * log(1 + max(%buys% - %sells%, 0)) - 0.0781 * log(1 + max(%sells% - %buys%, 0)))
```

To change how many net transactions it takes to reach the cap, adjust the `0.0781` coefficient:

```
coefficient = (max-increase - 1) / ln(1 + target_transactions)
```

For example, to reach the cap at 1000 net buys with `max-increase: 1.5`:
```
coefficient = 0.5 / ln(1001) ≈ 0.0724
```

## Decay

By default, dynamic pricing counters persist indefinitely — prices stay elevated or depressed until enough opposite transactions occur. Decay gradually reduces both counters over time, drifting prices back toward their base values even when transaction activity slows down.

```yaml
dynamic-pricing:
  enabled: true
  decay:
    enabled: true  # Whether decay is active for this category.
    rate: 0.1      # 10% of counters removed per period.
    period: 1440   # Period in minutes (default: 1440 = 24 hours).
```

### `decay.enabled`

Whether decay is active for this category. Defaults to `false`.

### `decay.rate`

The percentage of the buy and sell counters removed each period. A value of `0.1` reduces both counters by 10% every period.

### `decay.period`

How often decay is applied, in minutes. Defaults to `1440` (24 hours).

### How decay works

Both the buy and sell counters are multiplied by `(1 - rate)` each period. Since the formula uses net demand `(buys - sells)`, reducing both counters proportionally also reduces the net demand — pulling prices back toward base. Equal buying and selling still cancels out, but prolonged inactivity will always eventually return prices to base.

Decay is category-level only and is inherited by all items in the category, including those with per-item pricing overrides.

To manually reset dynamic pricing counters for an item, use `/ecoshop resetdynamicpricing <id/all>`.

## Per-Item Overrides

Individual items can override the category dynamic pricing settings. Per-item overrides are placed inside the `buy`, `alt-buy`, or `sell` sections of the item config alongside `value` and `type`.

Any field not specified inherits from the category `dynamic-pricing` block. Decay is category-level only and cannot be overridden per item — it is always inherited from the category.

```yaml
- id: diamond
  item: diamond
  gui:
    column: 5
    row: 3
    page: 1
  buy:
    value: 100
    type: coins
    display: "$%value%"
    dynamic-pricing: # Override buy dynamic pricing for this item only
      enabled: true
      max-increase: 3.0 # This item's buy price can go up to 300% of base
      max-decrease: 0.5
      formula: "%base_price% * (1 + 0.0781 * log(1 + max(%buys% - %sells%, 0)) - 0.0781 * log(1 + max(%sells% - %buys%, 0)))"
  sell:
    value: 50
    type: coins
    display: "$%value%"
    dynamic-pricing: # Override sell dynamic pricing for this item only
      enabled: false # Disable sell dynamic pricing for this item
```

Read here for more info on configuring items: [How to make an Item](https://plugins.auxilor.io/ecoshop/how-to-make-an-item).