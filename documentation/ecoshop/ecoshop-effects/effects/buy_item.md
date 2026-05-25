# `buy_item`
:::infoRequires:
EcoShop
:::

:::dangerTriggered Effect
This effect requires a [Trigger](https://plugins.auxilor.io/effects/all-triggers) to activate.
:::

Buys a shop item for the player

# Effect Syntax
```yaml
- id: buy_item
  args:
    item: example_item # The shop item ID
    type: buy # The buy type: buy or alt_buy
    amount: 1 # (Optional) The amount to buy, defaults to 1
    category: example_category # (Optional) Restrict to a specific category ID
    price: 10.0 # (Optional) Override the price value
    bypass-limits: false # (Optional) Bypass buy limits, defaults to false
  ...other config (eg triggers, filters, mutators, etc)
```