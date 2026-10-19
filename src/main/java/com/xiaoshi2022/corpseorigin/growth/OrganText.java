package com.xiaoshi2022.corpseorigin.growth;

import net.minecraft.network.chat.Component;

/** Keep translatable components intact across the network; resolve only on the client. */
public final class OrganText {
    private OrganText() {}
    public static Component text(String key, Object... args) { return Component.translatable("organ_editor.corpseorigin." + key, args); }
    public static String label(String key, Object... args) { return text(key, args).getString(); }
    public static Failure error(String key, Object... args) { return new Failure(text(key, args)); }
    public static PackFailure io(String key, Object... args) { return new PackFailure(text(key, args)); }
    public static Component failure(Exception e) { return e instanceof Failure f ? f.component : e instanceof PackFailure f ? f.component : text("error", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()); }
    public static final class PackFailure extends java.io.IOException {
        public final Component component;
        private PackFailure(Component component) { super(component.toString()); this.component = component; }
    }
    public static final class Failure extends IllegalArgumentException {
        public final Component component;
        private Failure(Component component) { super(component.toString()); this.component = component; }
    }
}
