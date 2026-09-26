package dev.nova.client.module;

public final class BuiltinModules {
    private BuiltinModules() {}

    /**
     * No built-in modules are registered. The menu shell (categories, search,
     * settings panel) still works -- every category simply renders its
     * empty-state placeholder until modules are registered here.
     */
    public static void register() {
    }
}
