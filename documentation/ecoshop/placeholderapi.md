---
title: "PlaceholderAPI"
sidebar_position: 6
---

Placeholders are per-item and use the format `%ecoshop_<category>_<item>_<stat>%`, where `<category>` is the category ID and `<item>` is the item ID.

## Buy Pricing

| Placeholder                                        | Description                                              |
|----------------------------------------------------|----------------------------------------------------------|
| `%ecoshop_<category>_<item>_buy_base%`             | The base buy price value                                 |
| `%ecoshop_<category>_<item>_buy_base_display%`     | The base buy price, formatted for display                |
| `%ecoshop_<category>_<item>_buy_price%`            | The effective buy price (base × multipliers)             |
| `%ecoshop_<category>_<item>_buy_price_display%`    | The effective buy price, formatted for display           |
| `%ecoshop_<category>_<item>_alt_buy_base%`         | The base alt-buy price value                             |
| `%ecoshop_<category>_<item>_alt_buy_base_display%` | The base alt-buy price, formatted for display            |
| `%ecoshop_<category>_<item>_alt_buy_price%`        | The effective alt-buy price (base × multipliers)         |
| `%ecoshop_<category>_<item>_alt_buy_price_display%`| The effective alt-buy price, formatted for display       |

## Buy Limits

| Placeholder                                        | Description                                              |
|----------------------------------------------------|----------------------------------------------------------|
| `%ecoshop_<category>_<item>_buy_limit%`            | The per-player buy limit (`-1` if unlimited)             |
| `%ecoshop_<category>_<item>_buy_global_limit%`     | The global buy limit (`-1` if unlimited)                 |
| `%ecoshop_<category>_<item>_buy_amount%`           | The amount the player has bought                         |
| `%ecoshop_<category>_<item>_buy_global_amount%`    | The total amount bought globally                         |
| `%ecoshop_<category>_<item>_buy_remaining%`        | The amount the player can still buy                      |
| `%ecoshop_<category>_<item>_buy_global_remaining%` | The amount remaining in the global limit (`-1` if unlimited) |

## Sell Pricing

| Placeholder                                        | Description                                              |
|----------------------------------------------------|----------------------------------------------------------|
| `%ecoshop_<category>_<item>_sell_base%`            | The base sell price value                                |
| `%ecoshop_<category>_<item>_sell_base_display%`    | The base sell price, formatted for display               |
| `%ecoshop_<category>_<item>_sell_price%`           | The effective sell price (base × multipliers)            |
| `%ecoshop_<category>_<item>_sell_price_display%`   | The effective sell price, formatted for display          |

## Sell Limits

| Placeholder                                         | Description                                              |
|-----------------------------------------------------|----------------------------------------------------------|
| `%ecoshop_<category>_<item>_sell_limit%`            | The per-player sell limit (`-1` if unlimited)            |
| `%ecoshop_<category>_<item>_sell_global_limit%`     | The global sell limit (`-1` if unlimited)                |
| `%ecoshop_<category>_<item>_sell_amount%`           | The amount the player has sold                           |
| `%ecoshop_<category>_<item>_sell_global_amount%`    | The total amount sold globally                           |
| `%ecoshop_<category>_<item>_sell_remaining%`        | The amount the player can still sell                     |
| `%ecoshop_<category>_<item>_sell_global_remaining%` | The amount remaining in the global sell limit (`-1` if unlimited) |