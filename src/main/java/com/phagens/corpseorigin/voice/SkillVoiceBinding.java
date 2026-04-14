package com.phagens.corpseorigin.voice;

import net.minecraft.resources.ResourceLocation;

/**
 * 技能语音绑定
 * 将语音指令与技能关联
 */
public class SkillVoiceBinding {
    
    private final ResourceLocation skillId;
    private final String voiceCommand;  // 语音指令关键词
    private final String displayName;   // 显示名称
    
    public SkillVoiceBinding(ResourceLocation skillId, String voiceCommand, String displayName) {
        this.skillId = skillId;
        this.voiceCommand = voiceCommand.toLowerCase();
        this.displayName = displayName;
    }
    
    /**
     * 获取技能ID
     */
    public ResourceLocation getSkillId() {
        return skillId;
    }
    
    /**
     * 获取语音指令
     */
    public String getVoiceCommand() {
        return voiceCommand;
    }
    
    /**
     * 获取显示名称
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 检查语音输入是否匹配此绑定
     */
    public boolean matches(String voiceInput) {
        if (voiceInput == null || voiceInput.isEmpty()) {
            return false;
        }
        
        String normalized = voiceInput.toLowerCase().trim();
        
        // 完全匹配
        if (normalized.equals(voiceCommand)) {
            return true;
        }
        
        // 包含匹配（语音输入包含指令关键词）
        if (normalized.contains(voiceCommand)) {
            return true;
        }
        
        // 模糊匹配（计算相似度）
        double similarity = calculateSimilarity(normalized, voiceCommand);
        return similarity >= 0.8; // 80%相似度阈值
    }
    
    /**
     * 计算两个字符串的相似度（Levenshtein距离）
     */
    private double calculateSimilarity(String s1, String s2) {
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) return 1.0;
        
        int distance = levenshteinDistance(s1, s2);
        return 1.0 - ((double) distance / maxLength);
    }
    
    /**
     * 计算Levenshtein距离
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(
                        dp[i - 1][j] + 1,      // 删除
                        dp[i][j - 1] + 1),     // 插入
                        dp[i - 1][j - 1] + cost // 替换
                );
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    @Override
    public String toString() {
        return "SkillVoiceBinding{" +
                "skillId=" + skillId +
                ", voiceCommand='" + voiceCommand + '\'' +
                ", displayName='" + displayName + '\'' +
                '}';
    }
}
