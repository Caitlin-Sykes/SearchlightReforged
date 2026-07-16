# Light Blocks

Searchlight Reforged includes a variety of light blocks. While they come in different shapes and sizes, they all share a
core set of features and can be interacted with in the same ways.

## Shared Features

All light blocks share the following properties:

- **Dyeable**: Right-click with any standard Minecraft dye to change the block's color.
- **Adjustable Brightness**:
    - Right-click with **Glowstone Dust** to increase brightness.
    - Right-click with **Redstone Dust** to decrease brightness.
    - There are 5 levels: Off, Low, Medium, High, and Ultra.
- **Redstone Reactive**: By default, lights use inverse logic (they turn off when receiving a redstone signal).
- **Shift + Right-click** (with empty hand): Set a custom address for ComputerCraft.

## ComputerCraft Integration

If [CC: Tweaked](../integrations/index) is installed, these blocks act as peripherals, allowing for programmatic
control:

### Methods

- `setBrightness(level)`: Sets the light level (0-4).
- `getBrightness()`: Returns the current brightness level.
- `setColor(color)`: Sets the color using the color name (e.g., "red", "light_blue").
- `getColor()`: Returns the current color name.
- `isLit()`: Returns `true` if the light is currently on.
- `setLit(state)`: Controls the light source state.
    - `"on"`: Forces the light on.
    - `"off"`: Forces the light off.
    - `"release"`: Returns control to default redstone behavior.

When a computer provides instructions, they take priority over the default redstone behavior until `release` is called.

## Available Blocks

- [Wall Light](./wall-light)
- [Corner Light](./corner-light)
- [Centre Light](./centre-light)
- [Edge Light](./edge-light)
- [Colour Lamp](./colour-lamp)
- [Searchlight](./searchlight)
- [Wall Light](./wall-light)
