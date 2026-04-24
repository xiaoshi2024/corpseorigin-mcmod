package com.phagens.corpseorigin;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec SPEC;

    private static ModConfigSpec.BooleanValue enableVoiceTrigger;
    private static ModConfigSpec.BooleanValue showSkillIcon;
    private static ModConfigSpec.DoubleValue similarityThreshold;
    private static ModConfigSpec.ConfigValue<String> voiceLanguage;

    static {
        BUILDER.comment("General settings").push("general");

        enableVoiceTrigger = BUILDER
                .comment("Enable voice trigger system")
                .translation("corpseorigin.config.enableVoiceTrigger")
                .define("enableVoiceTrigger", true);

        showSkillIcon = BUILDER
                .comment("Show selected skill icon with cooldown indicator")
                .translation("corpseorigin.config.showSkillIcon")
                .define("showSkillIcon", true);

        BUILDER.pop();

        // 语音设置组
        BUILDER.comment("Voice Recognition Settings").push("voice");

        similarityThreshold = BUILDER
                .comment("Similarity threshold (considered a match if similarity exceeds this value)")
                .translation("corpseorigin.config.similarityThreshold")
                .defineInRange("similarityThreshold", 0.75, 0.0, 1.0);

        voiceLanguage = BUILDER
                .comment("Voice recognition language (zh-CN, en-US, ja-JP)")
                .translation("corpseorigin.config.voiceLanguage")
                .define("voiceLanguage", "zh-CN");

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static boolean enableVoiceTrigger() {
        return enableVoiceTrigger != null && enableVoiceTrigger.get();
    }

    public static boolean showSkillIcon() {
        return showSkillIcon != null && showSkillIcon.get();
    }

    public static double getSimilarityThreshold() {
        return similarityThreshold != null ? similarityThreshold.get() : 0.75;
    }

    public static String getVoiceLanguage() {
        return voiceLanguage != null ? voiceLanguage.get() : "zh-CN";
    }
}