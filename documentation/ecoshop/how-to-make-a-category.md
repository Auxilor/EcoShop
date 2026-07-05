---
title: "How to Make a Category"
sidebar_position: 2
---

A **category** is the actual shop screen, the menu where players buy and sell; a [shop](how-to-make-a-shop) is just a doorway to one or more of them. Each category is one file holding an **icon**, an optional **dynamic pricing** block, the **GUI** layout, and a list of **items**. This page covers building one from scratch.

## Quick start

1. Open the `categories/` folder in your EcoShop config directory.
2. Copy `_example.yml` and rename the copy, e.g. `gear.yml`. The file name (without `.yml`) becomes the category ID.
3. Set the `item:` icon and the `gui.title` players will see.
4. Add at least one entry under `items:` (see [How to make an Item](how-to-make-an-item)).
5. Reference this category from a shop, by ID, under that shop's `categories` or `direct-category`.
6. Save, then run `/ecoshop reload`.
7. Open the shop in game and click through to the category; your items should appear.

:::tip
`_example.yml` is included as a reference and is **never loaded**, so copy or rename it to make a real category. You can also organise categories into subfolders inside `categories/`, and they'll still load.
:::

## Naming and IDs

The file name without `.yml` is the category's ID, and that is what shops reference and what placeholders use. The `item:` icon uses the [Item Lookup System](https://hub.auxilor.io/wiki/eco/the-item-lookup-system-the-item-lookup-system) syntax. One category can sit in as many shops as you like.

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the category will not load.
:::

## The structure of a category

A category is made of four parts:

| Part | What it controls |
| --- | --- |
| **Category info** | The icon, lore, and access permission for the category |
| **Dynamic pricing** | Optional demand-based price changes for every item inside |
| **GUI** | The menu's size, title, navigation, and page layout |
| **Items** | The items players actually buy and sell |

Here is a complete category with every part in place:

```yaml
# === Category info: icon, lore, access ===
item: diamond_sword name:"&fExample Category" # The icon shown for this category in a shop.
lore: # Lore lines on that icon.
  - "&aBuy all the best gear here!"
permission: ecoshop.category.permission1 # Optional; permission required to open the category.

# === Dynamic pricing: demand-based prices ===
dynamic-pricing:
  enabled: false # Off by default; see the Dynamic Pricing page for the full system.
  max-increase: 1.5 # Cap prices at 150% of base.
  max-decrease: 0.5 # Floor prices at 50% of base.
  buy:
    enabled: true
    formula: "%base_price% * (1 + 0.0781 * log(1 + max(%buys% - %sells%, 0)) - 0.0781 * log(1 + max(%sells% - %buys%, 0)))"

# === GUI: the category menu ===
gui:
  rows: 6 # Height of the menu (1-6).
  title: "Demo Category" # The menu title. Supports %page% and %max_page% placeholders.
  forwards-arrow:
    item: arrow name:"&fNext Page"
    row: 6
    column: 6
  backwards-arrow:
    item: arrow name:"&fPrevious Page"
    row: 6
    column: 4
  pages:
    - page: 1
      mask: # Decorative background.
        items:
          - gray_stained_glass_pane
          - black_stained_glass_pane
        pattern: # 0 empty, 1 first mask item, 2 second mask item.
          - "222222222"
          - "211111112"
          - "211111112"
          - "211111112"
          - "211111112"
          - "222222222"
      custom-slots: [ ] # Extra decorative or command slots.

# === Items: what players buy and sell ===
items: [] # Each entry is a shop item; see How to make an Item.
```

### Category info

These fields decide how the category looks in its parent shop and who may open it.

```yaml
item: diamond_sword name:"&fExample Category" # The icon shown for this category in a shop.
lore: # Lore lines on that icon.
  - "&aBuy all the best gear here!"
permission: ecoshop.category.permission1 # Optional; without it the category is open to everyone.
```

### Dynamic pricing

This optional block makes every item in the category respond to server-wide demand, with caps on how far prices can move.

```yaml
dynamic-pricing:
  enabled: false # Turn the whole system on or off for this category.
  max-increase: 1.5 # Highest a price may reach, as a multiple of base (1.5 = 150%).
  max-decrease: 0.5 # Lowest a price may reach, as a multiple of base (0.5 = 50%).
  buy:
    enabled: true # Apply dynamic pricing to the buy price.
    formula: "%base_price% * (1 + 0.0781 * log(1 + max(%buys% - %sells%, 0)) - 0.0781 * log(1 + max(%sells% - %buys%, 0)))"
```

Dynamic pricing is a system of its own, including decay, per-price-type formulas, and per-item overrides. See [Dynamic Pricing](dynamic-pricing) for the full reference.

### GUI

The GUI block lays out the category menu itself. See [GUI Options](https://hub.auxilor.io/wiki/eco/pages) for the mask, pattern, and page fields.

```yaml
gui:
  rows: 6 # Height of the menu (1-6).
  title: "Demo Category" # The menu title. Supports %page% and %max_page% placeholders.
  forwards-arrow: # Hidden on the last page.
    item: arrow name:"&fNext Page"
    row: 6
    column: 6
  backwards-arrow: # Hidden on the first page.
    item: arrow name:"&fPrevious Page"
    row: 6
    column: 4
  pages:
    - page: 1
      custom-slots: [ ] # Add as many pages as you want by appending to this list.
```

### Items

The `items` list is the point of the category. Each entry is a shop item with its own buy and sell options and GUI position.

```yaml
items:
  - id: cooked_mutton # See How to make an Item for the full item format.
    item: cooked_mutton
    buy:
      type: coins
      value: 20
      display: "$%value%"
    gui:
      row: 1
      column: 4
      page: 1
```

Building items is a topic on its own; see [How to make an Item](how-to-make-an-item).

:::tip Troubleshooting
- **Category icon missing from the shop?** The shop's `categories` `id` does not match this file's name. Make them match.
- **Players cannot open the category?** A `permission` is set that they do not have. Grant it or remove the line.
- **An item does not show up?** Its `gui` row, column, or page is outside the menu, or the `items` list is empty. Check the coordinates against `gui.rows`.
:::

<hr/>

## Where to go next

- **Stock the shelves:** [How to make an Item](how-to-make-an-item) is the next step.
- **React to demand:** [Dynamic Pricing](dynamic-pricing) explains the pricing block above in full.
- **Wire it up:** [How to make a Shop](how-to-make-a-shop) shows how shops point at this category.