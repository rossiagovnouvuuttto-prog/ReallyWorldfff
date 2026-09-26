package dev.nova.client.module;

import net.minecraft.text.Text;

public enum Category {
    COMBAT("category.nova_client.combat"),
    MOVEMENT("category.nova_client.movement"),
    VISUAL("category.nova_client.visual"),
    PLAYER("category.nova_client.player"),
    MISC("category.nova_client.misc"),
    SETTINGS("category.nova_client.settings");

    private final String translationKey;

    Category(String translationKey) {
        this.translationKey = translationKey;
    }

    public Text displayName() {
        return Text.translatable(translationKey);
    }
}
