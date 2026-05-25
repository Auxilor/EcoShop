---
title: How to make a Shop
sidebar_position: 1
---
## Shops
Creating shops is easy if you follow the basic rules: A shop requires [categories](https://plugins.auxilor.io/ecoshop/how-to-make-a-category), and categories require [items](https://plugins.auxilor.io/ecoshop/how-to-make-an-item).
If you follow this guide in order, you will have a shop up and running in no time! If you want to skip ahead, use the links above or in the sidebar.

## How to add shops
Each shop is its own config file, placed in the `/shops/` folder, and you can add or remove them as you please. There's an example config called `_example.yml` to help you out!

The ID of the shop is the file name. This is what you use in commands, effects and placeholders.
ID's must be lowercase letters, numbers, and underscores only.

## Example Shop Config

```yaml
title: "Demo Shop"
command: "demoshop"

forwards-arrow:
  item: arrow name:"&fNext Page"
  row: 6
  column: 6

backwards-arrow:
  item: arrow name:"&fPrevious Page"
  row: 6
  column: 4

buy-broadcasts:
  enabled: true
  message: "&b&lCrystal Shop&r &8»&r %player%&r&f has bought &r%item%&r&ffrom the &bCrystal Shop ❖&f!"
  sound:
    enabled: true
    sound: ui_toast_challenge_complete
    pitch: 1.5
    volume: 2
    category: PLAYERS

click-sound:
  enabled: true
  sound: block_stone_button_click_on
  pitch: 1
  volume: 1
  category: UI

buy-sound:
  enabled: true
  sound: entity_player_levelup
  pitch: 2
  volume: 1
  category: UI

sell-sound:
  enabled: true
  sound: block_amethyst_block_place
  pitch: 1.5
  volume: 1
  category: UI

rows: 3
pages:
  - page: 1
    mask:
      items:
        - gray_stained_glass_pane
        - black_stained_glass_pane
      pattern:
        - "222222222"
        - "211111112"
        - "211000112"
        - "211000112"
        - "211111112"
        - "222222222"
    categories:
      - id: example
        row: 3
        column: 3
      - id: example_2
        row: 4
        column: 6

    custom-slots: [ ]
```

## Understanding all the sections

### The Shop Info Section
```yaml
title: Demo Shop # The GUI title.
command: demoshop # The command to open the shop.
```

### The GUI Section
```yaml
forwards-arrow: # The arrow for switching between pages. If on the last page, this will not show up.
  item: arrow name:"&fNext Page"
  row: 6
  column: 6

backwards-arrow: # The arrow for switching between pages. If on the first page, this will not show up.
  item: arrow name:"&fPrevious Page"
  row: 6
  column: 4
```

### The Broadcasts and Sounds Section
You can read more about configuring sounds in the [Sound Configs](https://plugins.auxilor.io/all-plugins/sounds) guide.
```yaml
buy-broadcasts: # Options for buy broadcasts
  enabled: true # If purchases in this shop should be broadcast to the server, good for /buy menus.
  message: "&b&lCrystal Shop&r &8»&r %player%&r&f has bought &r%item%&r&ffrom the &bCrystal Shop ❖&f!" # Use %player%, %item%, and %amount%
  sound: # Broadcast sound.
    enabled: true # Whether the sound should be played or not.
    sound: ui_toast_challenge_complete # The sound https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html
    pitch: 1.5 # The pitch (0.5 - 2)
    volume: 2 # The volume (0.5 - 2)
    category: PLAYERS # The sound category https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/SoundCategory.html

click-sound: # A sound to be played when clicking an icon in this shop.
  enabled: true
  sound: block_stone_button_click_on
  pitch: 1
  volume: 1
  category: UI

buy-sound: # A sound to be played when buying something in this shop.
  enabled: true
  sound: entity_player_levelup
  pitch: 2
  volume: 1
  category: UI

sell-sound: # A sound to be played when selling something in this shop.
  enabled: true
  sound: block_amethyst_block_place
  pitch: 1.5
  volume: 1
  category: UI
```

### The Categories Section

There are two methods to add [categories](https://plugins.auxilor.io/ecoshop/how-to-make-a-category), first is a direct and second is a list.

A direct-category shop is where you link the shop straight to a category, so when the player opens the shop, they are sent directly to that category. 
This is good for single page shops, such as a boss spawn egg shop (eg: `/boss-shop`)
```yaml
direct-category: example_category # The ID of the category.
```

The second method is to have a list of categories in the shop GUI, this is good for multi-category shops (eg. A typical `/shop`). You can have as many categories as you want in the list, and you can even have the same category in multiple shops if you want.
You need to configure the page, and then the list of categories for that page.

```yaml
# If you want a regular shop that contains multiple categories, use these options here
rows: 3
pages: # All the pages in the preview GUI. You can add as many pages as you want.
  - page: 1
    mask: # Filler items for decoration
      items: # Add as many items as you want
        - gray_stained_glass_pane # Item 1
        - black_stained_glass_pane # Item 2
      pattern:
        - "222222222"
        - "211111112"
        - "211000112"
        - "211000112"
        - "211111112"
        - "222222222"
    categories: # Where to put categories in the GUI
      - id: example # The category ID
        row: 3 # The row
        column: 3 # The column
      - id: example_2
        row: 4
        column: 6

    # Custom GUI slots; see here for a how-to: https://plugins.auxilor.io/all-plugins/pages#custom-gui-slots
    custom-slots: [ ]
```

<hr/>

## Default configs
The default configs can be found [here](https://github.com/Auxilor/EcoShop/blob/main/eco-core/core-plugin/src/main/resources/shops).