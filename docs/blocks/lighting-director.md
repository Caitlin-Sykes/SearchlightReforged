# Wireless Lighting Director

![Lighting Director](/images/blocks/lighting-director.png)

The central hub for wireless light management.

## Functionality

Manages up to 64 linked lights wirelessly.

## Linker Card

Used to pair lights to the director.

1. Right-click **Director** with a card to pair.
2. Right-click **Light Blocks** with the paired card to add/remove them.

## Redstone

Can be used to trigger global states (if implemented via ComputerCraft).

## Recipe

<RecipeGrid recipe="lighting_director" />

## ComputerCraft Support

Acts as a peripheral to allow programmatic control over all linked lights.

### Methods

- `getLinkedLights()`: Returns a table of all linked lights indexed by their address (or default label). Each entry
  includes:
    - `index`: 1-based slot index in the director.
    - `x`, `y`, `z`: Coordinates of the light block.
    - `active`: `true` if the light block exists and is loaded.
    - `type`: The light block type, or `"broken"`.
    - `lit`: Current boolean lit state (`true`/`false`).
    - `light_request`: Current override state (`"on"`, `"off"`, or `"release"`).
    - `brightness`: Current brightness level name (e.g., `"medium"`, `"high"`).
    - `color`: Current color name (e.g., `"white"`, `"red"`).
    - `address`: Custom address assigned to the light.
- `setLight(target, options)`: Updates settings for a linked light by its 1-based slot index (`number`) or address
  (`string`). Returns `true` if found and updated.
    - `options` table supports:
        - `color`: Sets the light color (e.g., `"red"`, `"light_blue"`).
        - `brightness`: Sets the brightness level (number `0-4` or name e.g. `"low"`, `"medium"`, `"high"`, `"ultra"`).
        - `lit`: Controls the light source state (`true`/`false`, or `"on"`, `"off"`, `"release"`).
- `setLights(bulkOptions)`: Updates multiple lights simultaneously using a table where keys are slot indices or
  addresses and values are `options` tables. Returns `true`.
- `removeLight(target)`: Unlinks and removes a light by its 1-based slot index (`number`) or address (`string`). Returns
  `true` if removed.
- `clearLights()`: Removes all linked lights from the director.
