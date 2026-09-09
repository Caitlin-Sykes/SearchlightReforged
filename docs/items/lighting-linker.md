# Lighting Linker

![Lighting Linker Item - an item used to link different lighting blocks together](/images/items/lighting_linker.png "Lighting Linker")

## Description

An item used to link different lighting blocks together.

### Usage Instructions

#### To Link New Lights

1. Shift + Right Click on your director block to set it as the target.
2. Right click on your target block to link it to the director block.

#### To Manage Existing Links

1. Right Click on your Director Block![Lighting Director GUI](/images/gui/blocks/lighting_director_gui.png)
2. If you wish to unlink a light, you can press the unlink button

### Methods

- `getLinkedLights()`: Returns a table of all linked lights indexed by their address (or default label). Each entry includes:
    - `index`: 1-based slot index in the director.
    - `x`, `y`, `z`: Coordinates of the light block.
    - `active`: `true` if the light block exists and is loaded.
    - `type`: The light block type, or `"broken"`.
    - `lit`: Current boolean lit state (`true`/`false`).
    - `light_request`: Current override state (`"on"`, `"off"`, or `"release"`).
    - `brightness`: Current brightness level name (e.g., `"medium"`, `"high"`).
    - `color`: Current color name (e.g., `"white"`, `"red"`).
    - `address`: Custom address assigned to the light.
- `setLight(target, options)`: Updates settings for a linked light by its 1-based slot index (`number`) or address (`string`). Returns `true` if found and updated.
    - `options` table supports:
        - `color`: Sets the light color (e.g., `"red"`, `"light_blue"`).
        - `brightness`: Sets the brightness level (number `0-4` or name e.g. `"low"`, `"medium"`, `"high"`, `"ultra"`).
        - `lit`: Controls the light source state (`true`/`false`, or `"on"`, `"off"`, `"release"`).
- `setLights(bulkOptions)`: Updates multiple lights simultaneously using a table where keys are slot indices or addresses and values are `options` tables. Returns `true`.
- `removeLight(target)`: Unlinks and removes a light by its 1-based slot index (`number`) or address (`string`). Returns `true` if removed.
- `clearLights()`: Removes all linked lights from the director.

## Recipe

![A crafting recipe for a lighting linker](/images/recipes/lighting_linker_recipe.png "Lighting Linker Recipe")

## Credits

- [amathieson](https://github.com/amathieson) for the model, texture and coding implementation.
