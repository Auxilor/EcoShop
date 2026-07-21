---
title: "Commands and Permissions"
sidebar_position: 6
---

This page lists every EcoShop command, the permission it needs, and the extra permission nodes that gate opening shops and trading specific items. Shop commands you define in shop files are separate; these are the built-in ones.

## Commands

| Command | Description | Permission |
| --- | --- | --- |
| `/ecoshop reload` | Reloads the plugin | `ecoshop.command.reload` |
| `/ecoshop resetbuys <player/all> <id/all>` | Resets how many times a player has bought an item | `ecoshop.command.resetbuys` |
| `/ecoshop resetsells <player/all> <id/all>` | Resets how many times a player has sold an item | `ecoshop.command.resetsells` |
| `/ecoshop resetdynamicpricing <id/all>` | Resets the dynamic pricing counters for an item | `ecoshop.command.resetdynamicpricing` |
| `/ecoshop rotate <category>` | Immediately re-rolls a rotating category's slots and restarts its timer | `ecoshop.command.rotate` |
| `/sell` | Opens the mass-sell GUI | `ecoshop.command.sell` |
| `/sell hand` | Sells the items in the player's hand | `ecoshop.command.sell.hand` |
| `/sell all` | Sells all sellable items in the player's inventory | `ecoshop.command.sell.all` |
| `/sell handall` | Sells the items in hand and all sellable items at once | `ecoshop.command.sell.handall` |

## Additional permissions

| Permission | Description |
| --- | --- |
| `ecoshop.open.<id>` | Required to open the shop with that ID |
| `ecoshop.buy.<id>` | Restricts buying the item with that ID to players who hold it |
| `ecoshop.sell.<id>` | Restricts selling the item with that ID to players who hold it |

<hr/>

## Where to go next

- **Reset pricing:** [Dynamic Pricing](dynamic-pricing) explains what `resetdynamicpricing` clears.
- **Force a rotation:** [Rotating Shops](rotating-shops) explains what `/ecoshop rotate` re-rolls.
- **Configure /sell:** [Plugin Config](plugin-config) covers the mass-sell GUI those commands open.
- **Build a shop:** [How to make a Shop](how-to-make-a-shop) is the place to start.
