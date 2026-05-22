package com.phagens.corpseorigin.GongFU.GongFaZL;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum GongFaCategory implements StringRepresentable {
    
    GF("gf", Component.translatable("gongfa.category.gf"), 0xFF4A90E2),
    XM("xm", Component.translatable("gongfa.category.xm"), 0xFFE74C3C),
    YN("yn", Component.translatable("gongfa.category.yn"), 0xFF2ECC71),
    FB("fb", Component.translatable("gongfa.category.fb"), 0xFFF39C12),
    ST("st", Component.translatable("gongfa.category.st"), 0xFFE67E22),
    SG("sg", Component.translatable("gongfa.category.sg"), 0xFF3498DB),
    SZ("sz", Component.translatable("gongfa.category.sz"), 0xFF9B59B6),
    TFST("tfst", Component.translatable("gongfa.category.tfst"), 0xFFF1C40F),
    QY("qy", Component.translatable("gongfa.category.qy"), 0xFF34495E),
    UNIVERSAL("universal", Component.translatable("gongfa.category.universal"), 0xFF95A5A6);
    
    private final String name;
    private final Component displayName;
    private final int color;
    
    GongFaCategory(String name, Component displayName, int color) {
        this.name = name;
        this.displayName = displayName;
        this.color = color;
    }
    
    public String getName() {
        return name;
    }
    
    public Component getDisplayName() {
        return displayName;
    }
    
    public int getColor() {
        return color;
    }
    
    public static GongFaCategory byName(String name) {
        for (GongFaCategory category : values()) {
            if (category.name.equalsIgnoreCase(name)) {
                return category;
            }
        }
        return UNIVERSAL;
    }
    
    @Override
    public @NotNull String getSerializedName() {
        return name;
    }
}
