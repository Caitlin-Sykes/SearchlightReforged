# Mod Integrations

Searchlight Reforged works better with friends.

## CC: Tweaked

Full peripheral support for advanced automation.

- **Lighting Director**: Exposes `getLinkedLights()`, `setLight(index/address, options)`, and `setLights(table)`.
- **Searchlights/Light Blocks**: Can be given individual addresses for direct control.
- **Options**: Supports changing `color`, `brightness`, and `lit` state via Lua.

## Jade / WTHIT

On-screen information for easier management.

- Displays the current **address** of the block.
- Shows **brightness** levels and **color** info at a glance.
- Indicates if a block is currently **linked** to a Director.
