---
title: "How to Make a Shop"
sidebar_position: 1
---

A **shop** is the top-level menu a player opens with a **command**; it holds a set of **categories**, either as a paged picker or as a single category the player lands in directly. This page covers creating a shop file, naming it, and laying out its parts.

## Quick start

1. Open the `shops/` folder in your EcoShop config directory.
2. Copy `_example.yml` and rename the copy, e.g. `shop.yml`. The file name (without `.yml`) becomes the shop ID.
3. Set `command:` to the command you want players to run, e.g. `shop`.
4. Under `pages:`, list the `categories` you want, each with a `row` and `column`. The category IDs must match real [category](how-to-make-a-category) files.
5. Save, then run `/ecoshop reload`.
6. Run your shop command in game; the category picker should open.

:::tip
`_example.yml` is included as a reference and is **never loaded**, so copy or rename it to make a real shop. You can also organise shops into subfolders inside `shops/`, and they'll still load.
:::

## Naming and IDs

The file name without `.yml` is the shop's ID. That ID is what you use in commands, effects, and placeholders. Item fields like the arrows below use the [Item Lookup System](https://plugins.auxilor.io/the-item-lookup-system) syntax.

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the shop will not load.
:::

## The structure of a shop

A shop is made of four parts:

| Part | What it controls |
| --- | --- |
| **Shop info** | The GUI title and the command that opens the shop |
| **Navigation** | The forward and back arrows between pages |
| **Broadcasts and sounds** | Purchase announcements and the click, buy, and sell sounds |
| **Categories** | Which categories the shop shows, and whether it is paged or direct |

Here is a complete shop with every part in place:

```yaml
# === Shop info: name and command ===
title: Demo Shop # The GUI title shown at the top of the shop.
command: demoshop # The command players run to open this shop.

# === Navigation: page arrows ===
forwards-arrow: # Shown on every page except the last.
  item: arrow name:"&fNext Page" # Item syntax comes from the Item Lookup System.
  row: 6
  column: 6
backwards-arrow: # Shown on every page except the first.
  item: arrow name:"&fPrevious Page"
  row: 6
  column: 4

# === Broadcasts and sounds: feedback on actions ===
buy-broadcasts: # Announce purchases to the whole server; good for /buy menus.
  enabled: true
  message: "&b%player%&f bought &r%item%&f!" # Supports %player%, %item%, and %amount%.
  sound:
    enabled: true
    sound: ui_toast_challenge_complete
    pitch: 1.5
    volume: 2
    category: players
click-sound: # Played when a player clicks any icon in this shop.
  enabled: true
  sound: block_stone_button_click_on
  pitch: 1
  volume: 1
  category: UI
buy-sound: # Played on a successful purchase.
  enabled: true
  sound: entity_player_levelup
  pitch: 2
  volume: 1
  category: players
sell-sound: # Played on a successful sale.
  enabled: true
  sound: block_amethyst_block_place
  pitch: 1.5
  volume: 1
  category: players

# === Categories: what the shop contains ===
rows: 3 # Height of the category-picker GUI (1-6).
pages: # Add as many pages as you want.
  - page: 1
    mask: # Decorative filler behind the categories.
      items:
        - gray_stained_glass_pane
        - black_stained_glass_pane
      pattern: # 0 empty, 1 first mask item, 2 second mask item.
        - "222222222"
        - "211111112"
        - "211000112"
        - "211000112"
        - "211111112"
        - "222222222"
    categories: # Where each category icon sits in the GUI.
      - id: example
        row: 3
        column: 3
      - id: example_2
        row: 4
        column: 6
    custom-slots: [ ] # Extra decorative or command slots.
```

### Shop info

The title and command are the only required fields a shop must have.

```yaml
title: Demo Shop # The GUI title shown at the top of the shop.
command: demoshop # The command players run to open this shop.
```

### Navigation

The arrows move players between pages and hide themselves when there is nowhere to go.

```yaml
forwards-arrow: # Hidden on the last page.
  item: arrow name:"&fNext Page"
  row: 6
  column: 6
backwards-arrow: # Hidden on the first page.
  item: arrow name:"&fPrevious Page"
  row: 6
  column: 4
```

### Broadcasts and sounds

These give feedback when players act in the shop. Broadcasts go to the whole server; the three sounds play locally for the buyer. See the [Sound Configs](https://plugins.auxilor.io/all-plugins/sounds) guide for the sound fields.

```yaml
buy-broadcasts:
  enabled: true # Announce every purchase in this shop to the server.
  message: "&b%player%&f bought &r%item%&f!" # Supports %player%, %item%, and %amount%.
  sound: # A sound played alongside the broadcast.
    enabled: true
    sound: ui_toast_challenge_complete
    pitch: 1.5
    volume: 2
    category: players
click-sound: # Played when an icon is clicked.
  enabled: true
  sound: block_stone_button_click_on
  pitch: 1
  volume: 1
  category: UI
```

### Categories

A shop reaches its categories in one of two ways. Use a **paged** list when the shop is a hub of many categories, e.g. a typical `/shop`:

```yaml
rows: 3
pages:
  - page: 1
    categories: # Place each category icon in the picker.
      - id: example
        row: 3
        column: 3
      - id: example_2
        row: 4
        column: 6
```

Use `direct-category` instead when the shop should drop the player straight into one category, e.g. a single-page `/bossshop`:

```yaml
direct-category: example_category # Opens this category immediately, with no picker.
```

:::info One category, many shops
The same category can appear in any number of shops. EcoShop tracks which shop a player opened the category from, so that shop's sounds and broadcasts apply even when two shops share a category.
:::

## Internal placeholders

These placeholders are available inside `buy-broadcasts.message`:

| Placeholder | Value |
| --- | --- |
| `%player%` | The name of the player who bought the item |
| `%item%` | The display name of the item that was bought |
| `%amount%` | The amount the player bought |

:::tip Troubleshooting
- **Shop command does nothing?** The file is still named `_example.yml`, or you have not reloaded. Rename it to a real ID and run `/ecoshop reload`.
- **A category slot is empty?** The `id` under `categories` does not match a category file name. Check the category's file name in `categories/`.
- **Arrows never appear?** There is only one page, so there is nowhere to navigate. Add a second page to see them.
:::

<hr/>

## Where to go next

- **Fill your shop:** [How to make a Category](how-to-make-a-category) is the next step; shops are empty without categories.
- **Add things to sell:** [How to make an Item](how-to-make-an-item) covers the items inside a category.
- **Global menus:** [Plugin Config](plugin-config) configures the shared buy, sell, and mass-sell GUIs.
