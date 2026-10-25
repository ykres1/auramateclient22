# AuraMateClient (Fabric, Minecraft 1.21.11)

One client mod with everything built in (no other mods needed except Fabric API).

Open the panel with **Right Shift** (rebindable). The panel and its settings windows have the snowy mountain + moving aurora as their backdrop, with a sky-blue title (the **See-through Panel** setting controls how strongly the aurora shows through). Every feature has an on/off switch.
**Right-click** a row (or an Armor HUD / Effect Timer on screen) to open its settings window:
change its options, and for the HUD elements move them (drag the window by its title bar too).
You can also left-drag the Armor HUD / Effect Timer straight on screen while the panel is open.

## Optimizers
- **Lunge Swap** (G): wind charge, swap to Lunge spear, jab. Delay stepper.
- **Fast Shield**: shields block after 3 ticks (restart to apply).
- **Wind Charge Opt**: shorter wind charge cooldown, set in ticks (restart to apply).
- **Cobweb Optimizer**: hold right-click with cobwebs to place one every tick.
- **Elytra Swap** (H): instant chestplate / elytra swap.
- **Crystal Optimizer**: hitting an end crystal removes it on your screen right away.
- **Anchor Optimizer**: exploding a charged respawn anchor (outside the Nether) removes it on your screen right away, so you can place the next one.
- **Anchor Fast Place**: hold right-click with respawn anchors to place one every tick.

## Visuals
Fullbright, Damage Tint, Totem Tweaks, See Hearts (`/iseeyourhearts [on|off]`), Title Logo,
**Aurora Mountain** (menu background on the title, singleplayer, multiplayer and options screens: snow-covered mountains under a moving aurora), See-through Panel.

## HUD
Effect Timer, Armor HUD, Spotify Now Playing.

Settings are saved in `config/auramateclient.properties`.

All files sit in one folder (no src/ needed). Build with GitHub Actions
(.github/workflows/main.yml) and use the jar from the artifact.

Install: put the jar and Fabric API in `mods/`.

To add another feature: make a `Feature`, register it in `Features.java`; it gets a row automatically.
