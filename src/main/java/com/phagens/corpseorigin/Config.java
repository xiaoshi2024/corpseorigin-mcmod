package com.phagens.corpseorigin;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static final ModConfigSpec SPEC = BUILDER.build();

    private static ModConfigSpec.BooleanValue enableVoiceTrigger;
    private static ModConfigSpec.BooleanValue showSkillIcon;
// ... existing code ...

    public static void init() {
        BUILDER.comment("General settings").push("General");

        // 定义配置项并赋值给字段
        enableVoiceTrigger = BUILDER
                .comment("Enable voice trigger system")
                .define("enableVoiceTrigger", true);

        showSkillIcon = BUILDER
                .comment("Show selected skill icon with cooldown indicator")
                .define("showSkillIcon", true);

        BUILDER.pop();
    }

    public static boolean enableVoiceTrigger() {
        return enableVoiceTrigger != null && enableVoiceTrigger.get();
    }



    public static boolean showSkillIcon() {
        return showSkillIcon != null && showSkillIcon.get();
    }

}
