package dev.nova.client;

import dev.nova.client.module.ModuleRegistry;
import dev.nova.client.module.ToggleableNovaModule;
import dev.nova.client.ui.NovaScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class NovaKeybinds {
    public static final String CATEGORY = "key.category.nova_client.controls";

    private static final KeyBinding OPEN_MENU = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.nova_client.open_menu",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            CATEGORY
    ));

    private static final KeyBinding TOGGLE_KILLAURA = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.nova_client.toggle_killaura",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY
    ));

    private NovaKeybinds() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_MENU.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new NovaScreen());
                }
            }
            while (TOGGLE_KILLAURA.wasPressed()) {
                if (ModuleRegistry.byId("killaura") instanceof ToggleableNovaModule killAura) {
                    killAura.toggle();
                }
            }
        });
    }

    public static Text openMenuKeyText() {
        return OPEN_MENU.getBoundKeyLocalizedText();
    }

    public static Text killAuraKeyText() {
        return TOGGLE_KILLAURA.getBoundKeyLocalizedText();
    }
}
