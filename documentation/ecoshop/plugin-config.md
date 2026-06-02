---
title: "Plugin Config"
sidebar_position: 5
---

`config.yml` holds the plugin-wide settings: storage, the global shop-item lore, and the shared buy, buy-more, sell, sell-more, and mass-sell menus. It lives at `/plugins/EcoShop/config.yml`. After editing it, run `/ecoshop reload` to apply your changes.

:::warning
Changing `use-local-storage` switches the storage backend, so it does not take effect on a reload; restart the server after changing it, or buy and sell data may not load correctly.
:::

## Default config.yml

```yaml
# Even if eco is set up to use a database, this forces EcoShop to save to
# local storage, disabling cross-server sync.
use-local-storage: false

shop-items:
  register-permissions: false # Show shop permissions in permission plugins; slows reloads, so off by default.
  sell-strict-match: true # Sold items must match the shop item exactly (name, lore, enchants); false matches only material.
  global-bottom-lore: # Lore shown on every shop icon, off by default.
    buy: [ ] # Shown under buyable items, e.g. "&e&oLeft Click to buy".
    sell: [ ] # Shown under sellable items, e.g. "&e&oRight Click to sell".
    always: [ ] # Shown under every item.

buy-menu:
  rows: 6 # Height of the menu (1-6).
  title: "Buying %item%" # Menu title; %item% is the item being bought.
  mask: # Decorative background.
    items: # Mask materials, referenced by the pattern below.
      - black_stained_glass_pane
      - gray_stained_glass_pane
    pattern: # 0 empty, 1 first mask item, 2 second mask item.
      - "111111111"
      - "122202221"
      - "122222221"
      - "122222221"
      - "122020221"
      - "111101111"
  item: # Where the item being bought is shown.
    row: 2
    column: 5
  buy-more: # Button to open the buy-more menu.
    row: 6
    column: 5
    item: emerald 64 unbreaking:1 hide_enchants name:"&aBuy More"
  cancel: # Button to close without buying.
    row: 5
    column: 4
    item: barrier name:"&cCancel"
  confirm: # Button to confirm the purchase.
    row: 5
    column: 6
    item: diamond unbreaking:1 hide_enchants name:"&aConfirm"
  add-buttons: # Buttons that change the amount by a fixed step.
    - add: 1 # +1 button.
      row: 4
      column: 6
      item: lime_concrete 1 name:"&a+1"
    - add: 10 # +10 button.
      row: 4
      column: 7
      item: lime_concrete 10 name:"&a+10"
    - add: -1 # -1 button.
      row: 4
      column: 4
      item: red_concrete 1 name:"&c-1"
    - add: -10 # -10 button.
      row: 4
      column: 3
      item: red_concrete 10 name:"&c-10"
  set-buttons: # Buttons that set the amount to a fixed value.
    - set: 1 # Set to one.
      row: 4
      column: 2
      item: red_concrete 1 name:"&cSet to one"
    - set: 64 # Set to a full stack.
      row: 4
      column: 8
      item: lime_concrete 64 name:"&aSet to full stack"
  custom-slots: [ ] # Extra decorative or command slots.

buy-more:
  rows: 2 # Height of the menu (1-6).
  title: "Buying more %item%" # Menu title.
  mask: # Decorative background.
    items:
      - black_stained_glass_pane
    pattern: # 0 empty, 1 first mask item.
      - "000000000"
      - "111101111"
  item-name: "&fBuy &a%stacks%&f stacks" # Name template for each amount button; %stacks% is the stack count.
  amounts: # One button per stack count.
    - stacks: 1 # Buy 1 stack.
      row: 1
      column: 1
    - stacks: 2 # Buy 2 stacks.
      row: 1
      column: 2
    - stacks: 3 # Buy 3 stacks.
      row: 1
      column: 3
    - stacks: 4 # Buy 4 stacks.
      row: 1
      column: 4
    - stacks: 5 # Buy 5 stacks.
      row: 1
      column: 5
    - stacks: 6 # Buy 6 stacks.
      row: 1
      column: 6
    - stacks: 7 # Buy 7 stacks.
      row: 1
      column: 7
    - stacks: 8 # Buy 8 stacks.
      row: 1
      column: 8
    - stacks: 9 # Buy 9 stacks.
      row: 1
      column: 9
  back: # Button back to the buy menu.
    row: 2
    column: 5
    item: arrow 1 name:"&cGo Back"
  custom-slots: [ ] # Extra decorative or command slots.

sell-menu:
  rows: 6 # Height of the menu (1-6).
  title: "Selling %item%" # Menu title; %item% is the item being sold.
  mask: # Decorative background.
    items:
      - black_stained_glass_pane
      - gray_stained_glass_pane
    pattern: # 0 empty, 1 first mask item, 2 second mask item.
      - "111111111"
      - "122202221"
      - "122222221"
      - "122222221"
      - "122020221"
      - "111101111"
  item: # Where the item being sold is shown.
    row: 2
    column: 5
  sell-more: # Button to open the sell-more menu.
    row: 6
    column: 5
    item: emerald 64 unbreaking:1 hide_enchants name:"&aSell More"
  cancel: # Button to close without selling.
    row: 5
    column: 4
    item: barrier name:"&cCancel"
  confirm: # Button to confirm the sale.
    row: 5
    column: 6
    item: diamond unbreaking:1 hide_enchants name:"&aConfirm"
  sell-all-button: # Button that sells every matching item the player holds.
    enabled: true
    item: hopper name:"&aSell All"
    lore: # %amount% and %price% are filled per player.
      - "&7Sell every matching item you have"
      - "&7Amount: &e%amount%"
      - "&7Price: &e%price%"
    row: 3
    column: 5
  add-buttons: # Buttons that change the amount by a fixed step.
    - add: 1 # +1 button.
      row: 4
      column: 6
      item: red_concrete 1 name:"&c+1"
    - add: 10 # +10 button.
      row: 4
      column: 7
      item: red_concrete 10 name:"&c+10"
    - add: -1 # -1 button.
      row: 4
      column: 4
      item: lime_concrete 1 name:"&a-1"
    - add: -10 # -10 button.
      row: 4
      column: 3
      item: lime_concrete 10 name:"&a-10"
  set-buttons: # Buttons that set the amount to a fixed value.
    - set: 1 # Set to one.
      row: 4
      column: 2
      item: lime_concrete 1 name:"&aSet to one"
    - set: 64 # Set to a full stack.
      row: 4
      column: 8
      item: red_concrete 64 name:"&cSet to full stack"
  custom-slots: [ ] # Extra decorative or command slots.

sell-more:
  rows: 2 # Height of the menu (1-6).
  title: "Selling more %item%" # Menu title.
  mask: # Decorative background.
    items:
      - black_stained_glass_pane
    pattern: # 0 empty, 1 first mask item.
      - "000000000"
      - "111101111"
  item-name: "&fSell &a%stacks%&f stacks" # Name template for each amount button.
  amounts: # One button per stack count.
    - stacks: 1 # Sell 1 stack.
      row: 1
      column: 1
    - stacks: 2 # Sell 2 stacks.
      row: 1
      column: 2
    - stacks: 3 # Sell 3 stacks.
      row: 1
      column: 3
    - stacks: 4 # Sell 4 stacks.
      row: 1
      column: 4
    - stacks: 5 # Sell 5 stacks.
      row: 1
      column: 5
    - stacks: 6 # Sell 6 stacks.
      row: 1
      column: 6
    - stacks: 7 # Sell 7 stacks.
      row: 1
      column: 7
    - stacks: 8 # Sell 8 stacks.
      row: 1
      column: 8
    - stacks: 9 # Sell 9 stacks.
      row: 1
      column: 9
  back: # Button back to the sell menu.
    row: 2
    column: 5
    item: arrow 1 name:"&cGo Back"
  custom-slots: [ ] # Extra decorative or command slots.

# The mass-sell GUI opened by /sell.
sell-gui:
  rows: 6 # Height of the menu (1-6).
  title: Sell Items # Menu title.
  custom-slots: # The drop slot players fill with items to sell.
    - row: 6
      column: 9
      item: paper 1 unbreaking:1 hide_enchants name:"&aDrop items in here to sell them!"
      lore: # Everything dropped in is sold when the menu closes.
        - "&fWhen you close this menu, all"
        - "&fitems inside it will be sold!"
```

<hr/>

## Where to go next

- **Build shops:** [How to make a Shop](how-to-make-a-shop) covers the per-shop configs these menus wrap.
- **Run commands:** [Commands and Permissions](commands-and-permissions) lists `/ecoshop reload` and the `/sell` commands.
- **Default files:** the shipped configs live in the [EcoShop repository](https://github.com/Auxilor/EcoShop/tree/master/eco-core/core-plugin/src/main/resources).