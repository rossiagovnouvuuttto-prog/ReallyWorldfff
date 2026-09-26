# NOVA Client — Fabric 1.21.11

Client-only NOVA client for **Minecraft Java Edition 1.21.11**.

## Interface

The real in-game `NovaScreen` is implemented in the dark three-column style from the provided reference image:

- NOVA logo in the header.
- Rounded search field at the top.
- Header Settings and Close buttons.
- Left navigation: Combat, Movement, Visual, Player, Misc, Settings.
- Large rounded module panel in the center.
- Permanent right-side **Menu settings** panel.
- Accent palette: purple, blue, cyan, green, yellow, orange, red and pink.
- Menu scale control (50–125%).
- Animations toggle.
- Current menu/KillAura key bindings displayed in the settings panel.
- Reset settings button.
- Scrolling for the category list, module list and settings panel when the available GUI area is too small.
- Responsive layout that is recomputed from Minecraft's logical GUI size and the configured NOVA scale.

## Layout 1.0.7

The menu is drawn in a fixed design space taken from the reference screenshot
(a 1065x577 window on a 1536x864 screen) and scaled as one piece, so it keeps the
same proportions on any resolution and any Minecraft GUI scale:

- The world stays visible behind the menu (no blur, no dark overlay).
- Vector NOVA logo, rounded search field, gear and close buttons.
- Category rows with vector icons; the selected row has an accent outline and glow.
- Empty-state cube icon in the module panel; module cards with toggles and scrolling.
- Settings panel: accent palette, menu scale slider (50–125%), animations toggle,
  "Open menu" and "Toggle KillAura" bindings, reset button.
- `R` toggles a module registered with the id `killaura` (rebindable in Controls).

## Controls

- Client command `/123` opens NOVA locally and is not sent to the server.
- Right Shift opens NOVA and can be rebound in Minecraft Controls.
- Esc or the `×` header button closes NOVA.
- `R` toggles KillAura and can be rebound in Minecraft Controls.

## Configuration

Interface settings are stored in `.minecraft/config/nova_client.json`.

If the file is absent, defaults are created. If it is invalid, NOVA falls back to defaults and attempts to preserve the unreadable file as `nova_client.broken.json`.

## Toolchain pinned in this project

- Java 21
- Minecraft 1.21.11
- Fabric Loader 0.19.5
- Fabric API 0.141.5+1.21.11
- Yarn mappings 1.21.11+build.6
- Fabric Loom 1.14.1 (`net.fabricmc.fabric-loom-remap`)
- Gradle 9.2.0 through Gradle Wrapper

## Build

Windows:

```bat
gradlew.bat build
```

Linux/macOS:

```sh
./gradlew build
```

After a successful build, the remapped installable mod is expected at:

```text
build/libs/nova-client-1.0.7.jar
```

The current preparation environment has Java 21 but cannot resolve `services.gradle.org`, so it cannot download Gradle and therefore cannot perform the real Fabric build or launch Minecraft. `BUILD_STATUS.md` records exactly what was and was not checked. No fake JAR is included.

## Install after building

1. Install Java 21.
2. Install Fabric Loader for Minecraft 1.21.11.
3. Put Fabric API for 1.21.11 and `nova-client-1.0.7.jar` into `.minecraft/mods`.
4. Start the Fabric 1.21.11 profile.
5. Open NOVA with `/123` or Right Shift.

The mod declares `environment: client`; the server does not need NOVA installed.
## Movement modules

The Movement category now contains four toggleable client modules:

- Sprint — keeps sprint enabled while moving forward.
- Velocity — scales locally applied player velocity.
- Timer — changes the client tick multiplier.
- InventoryMove — drives movement input while inventory-style screens are open.

They are registered through `BuiltinModules`, so the existing NOVA screen discovers and renders them automatically from `ModuleRegistry`.


## Visual modules

Added to the Visual category:
- ESP
- Tracers
- StorageESP
- Fullbright
- NoRender
- Zoom (hold Z while enabled)

The uploaded Visual sources were integrated into the existing NOVA module registry and adapted where the Minecraft/Fabric 1.21.11 APIs differ from the supplied code.

## Extra Visual modules

Added Visual modules from the latest supplied source set:
- NameTags
- ItemESP
- NoHurtCam
- CustomCrosshair
- BlockOutline

The supplied 1.21-style render snippets were adapted for Minecraft 1.21.11 APIs: NameTags modifies the 1.21.11 entity render state, CustomCrosshair uses the existing DrawContext passed to InGameHud, and BlockOutline targets the 1.21.11 drawBlockOutline color/line-width arguments.


## UI Match 1.0.1
- Reworked the in-game NOVA screen to match the provided reference more closely.
- Darker translucent panels and purple border glow.
- Selected categories now use a dark fill with a bright accent outline instead of a solid purple block.
- Larger vector NOVA logo, larger category/module labels, fixed-width centered search field, larger header spacing, and a proper empty-state cube icon.
- Keeps the Timer runtime fix and shared keybind category fix from the previous build.

## 1.0.2 integration

The latest combat update moves KillAura silent rotation away from temporary local camera rotation and into a dedicated `MixinSendMovement` hook. The previous Minecraft 1.21.11 Timer and Velocity compatibility fixes remain in place, as do the Visual modules and the photo-matched NOVA interface.


## 1.0.4 additions
- KillAura real client rotation (smooth/GCD-snapped) from the supplied module.
- ESP depth test disabled while drawing boxes so ESP is visible through blocks.
- Kept the Minecraft 1.21.11-compatible NameTags render-state hook and the fixed Timer mixin.
- Removed MixinSendMovement because packet-only silent rotation is no longer used.
