---
title: "Commands and Permissions"
sidebar_position: 5
---

| Command                                    | Description                                           | Permission                     |
|--------------------------------------------|-------------------------------------------------------|--------------------------------|
| `/ecoshop reload`                          | Reloads the plugin                                    | `ecoshop.command.reload`       |
| `/ecoshop resetbuys <player/all> <id/all>` | Resets how many times a player has bought an item     | `ecoshop.command.resetbuys`    |
| `/ecoshop resetsells <player/all> <id/all>` | Resets how many times a player has sold an item       | `ecoshop.command.resetsells`   |
| `/ecoshop resetdynamicpricing <id/all>` | Resets the dynamic pricing counters for an item       | `ecoshop.command.resetdynamicpricing` |
| `/sell`                                    | Opens the Sell GUI                                    | `ecoshop.command.sell`         |
| `/sell hand`                               | Sells items in the player's hand                      | `ecoshop.command.sell.hand`    |
| `/sell all`                                | Sells all sellable items in the player's inventory    | `ecoshop.command.sell.all`     |
| `/sell handall`                            | Sells items in hand and all sellable items (combined) | `ecoshop.command.sell.handall` |

### Additional Permissions

| Permission                       | Description                                                    |
|----------------------------------|----------------------------------------------------------------|
| `ecoshop.open.<id>`              | Opening the shop requires this permission                      |
| `ecoshop.buy.<id>`               | Restrict purchase of specific items to players with permission |
| `ecoshop.sell.<id>`              | Restrict selling of specific items to players with permission  |
