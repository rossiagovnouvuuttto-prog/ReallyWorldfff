# NOVA 1.0.2 — integration status

Target: Minecraft 1.21.11 / Fabric / Java 21.

Integrated from the latest supplied files:
- packet-only KillAura yaw/pitch spoof state;
- `MixinSendMovement` for outgoing movement rotation values;
- InventoryMove kept on the existing 1.21.11 input implementation;
- existing Visual modules, UI-match layout, keybind-category fix and Timer runtime fix preserved.

Important merge decisions:
- The newly supplied `MixinRenderTickCounter` was **not** copied because it is the older `@ModifyVariable` implementation that previously caused a fatal startup Mixin injection failure on Minecraft 1.21.11.
- The newly supplied `MixinEntityVelocity` was **not** copied because the project already contains the 1.21.11 `Vec3d` signature that compiled successfully in Termux.
- Existing Visual mixins/modules were not removed when merging the shorter supplied mixin/module lists.

Build in Termux:

```bash
./gradlew clean build --no-daemon -Dorg.gradle.jvmargs="-Xmx4G -Xms512M"
```

Expected remapped jar:
`build/libs/nova-client-1.0.2.jar`

This environment has not run the full Gradle build for this revision; verify in Termux and send the compiler/runtime log if anything fails.


### 1.0.4 merge
Merged supplied real-rotation KillAura and through-wall ESP behavior. The supplied legacy NameTags/ESP API calls were not copied verbatim where they conflict with the already verified 1.21.11 mappings; equivalent behavior is kept on the 1.21.11-compatible hooks.


## 1.0.6
- Removed obsolete RenderSystem.disableDepthTest/depthMask/enableDepthTest calls from ESP for Minecraft 1.21.11 compatibility.
- UI pixel-match changes retained.

## 1.0.7
- NOVA menu redrawn in a fixed design space matching the reference screenshot and scaled as a whole.
- No blur/dark overlay behind the menu; "Toggle KillAura" binding row (default `R`); scale range 50–125%.
- Not compiled in this environment (Fabric/Mojang repositories unreachable). Rendering was checked
  against stubbed Minecraft classes only; run `./gradlew build` and send the log if anything fails.
