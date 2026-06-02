---
title: "PlaceholderAPI"
sidebar_position: 7
---

With PlaceholderAPI installed, EcoShop exposes per-item placeholders you can use in any plugin that reads them, e.g. scoreboards, holograms, or chat. They all follow the format `%ecoshop_<category>_<item>_<stat>%`, where `<category>` is the category ID and `<item>` is the item ID. This page lists the available stats, grouped by buy pricing, buy limits, sell pricing, and sell limits.

## Buy pricing

| Placeholder | Description |
| --- | --- |
| `%ecoshop_<category>_<item>_buy_base%` | The base buy price value |
| `%ecoshop_<category>_<item>_buy_base_display%` | The base buy price, formatted for display |
| `%ecoshop_<category>_<item>_buy_price%` | The effective buy price (base with multipliers) |
| `%ecoshop_<category>_<item>_buy_price_display%` | The effective buy price, formatted for display |
| `%ecoshop_<category>_<item>_alt_buy_base%` | The base alt-buy price value |
| `%ecoshop_<category>_<item>_alt_buy_base_display%` | The base alt-buy price, formatted for display |
| `%ecoshop_<category>_<item>_alt_buy_price%` | The effective alt-buy price (base with multipliers) |
| `%ecoshop_<category>_<item>_alt_buy_price_display%` | The effective alt-buy price, formatted for display |

## Buy limits

| Placeholder | Description |
| --- | --- |
| `%ecoshop_<category>_<item>_buy_limit%` | The per-player buy limit (`-1` if unlimited) |
| `%ecoshop_<category>_<item>_buy_global_limit%` | The global buy limit (`-1` if unlimited) |
| `%ecoshop_<category>_<item>_buy_amount%` | The amount the player has bought |
| `%ecoshop_<category>_<item>_buy_global_amount%` | The total amount bought globally |
| `%ecoshop_<category>_<item>_buy_remaining%` | The amount the player can still buy |
| `%ecoshop_<category>_<item>_buy_global_remaining%` | The amount remaining in the global limit (`-1` if unlimited) |

## Sell pricing

| Placeholder | Description |
| --- | --- |
| `%ecoshop_<category>_<item>_sell_base%` | The base sell price value |
| `%ecoshop_<category>_<item>_sell_base_display%` | The base sell price, formatted for display |
| `%ecoshop_<category>_<item>_sell_price%` | The effective sell price (base with multipliers) |
| `%ecoshop_<category>_<item>_sell_price_display%` | The effective sell price, formatted for display |

## Sell limits

| Placeholder | Description |
| --- | --- |
| `%ecoshop_<category>_<item>_sell_limit%` | The per-player sell limit (`-1` if unlimited) |
| `%ecoshop_<category>_<item>_sell_global_limit%` | The global sell limit (`-1` if unlimited) |
| `%ecoshop_<category>_<item>_sell_amount%` | The amount the player has sold |
| `%ecoshop_<category>_<item>_sell_global_amount%` | The total amount sold globally |
| `%ecoshop_<category>_<item>_sell_remaining%` | The amount the player can still sell |
| `%ecoshop_<category>_<item>_sell_global_remaining%` | The amount remaining in the global sell limit (`-1` if unlimited) |

<hr/>

## Where to go next

- **Find your IDs:** [How to make an Item](how-to-make-an-item) shows where the `<item>` ID comes from.
- **Pricing stats:** [Dynamic Pricing](dynamic-pricing) explains the base-versus-effective price split.
- **Internal placeholders:** the [How to make an Item](how-to-make-an-item) page also lists the `%value%`-style placeholders for use inside shop lore.
