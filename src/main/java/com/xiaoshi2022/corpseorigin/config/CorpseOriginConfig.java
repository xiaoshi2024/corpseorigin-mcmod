package com.xiaoshi2022.corpseorigin.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 客户端配置。
 * <p>
 * 目前只暴露尸水的渲染色调，方便按材质调整颜色（ARGB 十六进制）。
 */
public final class CorpseOriginConfig {

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<String> FLUID_TINT;

    private static final int DEFAULT_TINT = 0xFF884422;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("CorpseOrigin client rendering").push("fluid");
        FLUID_TINT = builder
                .comment("尸水渲染色调，ARGB 十六进制（例如 FF884422 = 不透明棕红色，FFFFFFFF = 原贴图色）")
                .define("infectedWaterTint", "FF884422");
        builder.pop();
        SPEC = builder.build();
    }

    private CorpseOriginConfig() {}

    /** 读取当前配置的尸水色调；配置未加载或格式非法时回退默认值。 */
    public static int fluidTint() {
        try {
            return (int) Long.parseLong(FLUID_TINT.get().trim(), 16);
        } catch (RuntimeException e) {
            return DEFAULT_TINT;
        }
    }
}
