package com.phagens.corpseorigin.voice;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 语音模板管理器
 * 允许玩家录制自己的语音模板，然后进行匹配
 * 在客户端运行（包括单人游戏）
 */
@OnlyIn(Dist.CLIENT)
public class VoiceTemplateManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SAMPLE_RATE = 16000;
    private static final int SAMPLE_SIZE_IN_BITS = 16;
    private static final int CHANNELS = 1;
    private static final boolean SIGNED = true;
    private static final boolean BIG_ENDIAN = false;
    
    private static VoiceTemplateManager instance;
    private final Map<String, byte[]> templates = new HashMap<>();
    private final Path templateDir;
    
    private VoiceTemplateManager() {
        Path tempTemplateDir = null;
        try {
            // 使用游戏目录下的 voice_templates 文件夹
            tempTemplateDir = Paths.get(Minecraft.getInstance().gameDirectory.getAbsolutePath(), "voice_templates");
            loadTemplates(tempTemplateDir);
            LOGGER.info("VoiceTemplateManager initialized with directory: {}", tempTemplateDir);
        } catch (Exception e) {
            LOGGER.error("Failed to initialize VoiceTemplateManager", e);
            tempTemplateDir = Paths.get(Minecraft.getInstance().gameDirectory.getAbsolutePath(), "voice_templates");
        }
        this.templateDir = tempTemplateDir;
    }
    
    public static synchronized VoiceTemplateManager getInstance() {
        if (instance == null) {
            instance = new VoiceTemplateManager();
        }
        return instance;
    }
    
    /**
     * 加载所有语音模板
     */
    private void loadTemplates(Path dir) {
        try {
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
                LOGGER.info("Created voice template directory: {}", dir);
            }
            
            File[] files = dir.toFile().listFiles((d, name) -> name.endsWith(".wav"));
            if (files != null) {
                for (File file : files) {
                    String skillName = file.getName().replace(".wav", "");
                    byte[] audioData = Files.readAllBytes(file.toPath());
                    templates.put(skillName, audioData);
                    LOGGER.info("Loaded voice template for skill: {}", skillName);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load voice templates", e);
        }
    }
    
    /**
     * 录制语音模板
     */
    public void recordTemplate(String skillName, int durationMs) {
        LOGGER.info("Starting voice template recording for skill: {} (duration: {}ms)", skillName, durationMs);
        
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_IN_BITS, CHANNELS, SIGNED, BIG_ENDIAN);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            
            if (!AudioSystem.isLineSupported(info)) {
                LOGGER.error("Microphone not supported for format: {}", format);
                showMessage("§c麦克风不支持");
                return;
            }
            
            TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            
            showMessage("§e开始录制 " + skillName + " 语音模板...");
            LOGGER.info("Microphone opened and started recording");
            
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            long startTime = System.currentTimeMillis();
            
            while (System.currentTimeMillis() - startTime < durationMs) {
                int count = line.read(buffer, 0, buffer.length);
                if (count > 0) {
                    out.write(buffer, 0, count);
                }
            }
            
            line.stop();
            line.close();
            
            byte[] audioData = out.toByteArray();
            LOGGER.info("Recording completed: {} bytes", audioData.length);
            
            // 检查录音长度
            if (audioData.length < 1000) {
                showMessage("§c录音时间太短或没有检测到声音");
                LOGGER.warn("Recording too short: {} bytes", audioData.length);
                return;
            }
            
            // 保存模板
            Path filePath = templateDir.resolve(skillName + ".wav");
            Files.write(filePath, audioData);
            templates.put(skillName, audioData);
            
            showMessage("§a语音模板 " + skillName + " 录制完成!");
            LOGGER.info("Voice template saved for skill: {} ({} bytes)", skillName, audioData.length);
            
        } catch (Exception e) {
            LOGGER.error("Failed to record voice template for skill: {}", skillName, e);
            showMessage("§c录制失败: " + e.getMessage());
        }
    }
    
    /**
     * 匹配语音
     */
    public String matchVoice(byte[] audioData) {
        if (templates.isEmpty()) {
            LOGGER.warn("No voice templates loaded");
            return null;
        }
        
        String bestMatch = null;
        double bestSimilarity = 0.0;
        
        for (Map.Entry<String, byte[]> entry : templates.entrySet()) {
            double similarity = calculateSimilarity(audioData, entry.getValue());
            LOGGER.debug("Similarity with {}: {}", entry.getKey(), similarity);
            
            if (similarity > bestSimilarity && similarity > 0.6) { // 阈值 0.6
                bestSimilarity = similarity;
                bestMatch = entry.getKey();
            }
        }
        
        if (bestMatch != null) {
            LOGGER.info("Best match: {} (similarity: {})", bestMatch, bestSimilarity);
        }
        
        return bestMatch;
    }
    
    /**
     * 计算两个音频的相似度（简化版DTW算法）
     */
    private double calculateSimilarity(byte[] audio1, byte[] audio2) {
        // 提取音频特征（简化版：使用音量包络）
        double[] features1 = extractFeatures(audio1);
        double[] features2 = extractFeatures(audio2);
        
        // 使用动态时间规整（DTW）计算相似度
        return dtwDistance(features1, features2);
    }
    
    /**
     * 提取音频特征（音量包络）
     */
    private double[] extractFeatures(byte[] audioData) {
        int frameSize = 1024;
        int numFrames = audioData.length / frameSize;
        double[] features = new double[numFrames];
        
        for (int i = 0; i < numFrames; i++) {
            double sum = 0;
            for (int j = 0; j < frameSize; j += 2) {
                int idx = i * frameSize + j;
                if (idx + 1 < audioData.length) {
                    short sample = (short) ((audioData[idx + 1] << 8) | (audioData[idx] & 0xFF));
                    sum += Math.abs(sample);
                }
            }
            features[i] = sum / (frameSize / 2);
        }
        
        // 归一化
        double max = 0;
        for (double f : features) {
            if (f > max) max = f;
        }
        if (max > 0) {
            for (int i = 0; i < features.length; i++) {
                features[i] /= max;
            }
        }
        
        return features;
    }
    
    /**
     * 动态时间规整（DTW）距离
     */
    private double dtwDistance(double[] seq1, double[] seq2) {
        int n = seq1.length;
        int m = seq2.length;
        
        // 限制最大长度差异
        if (Math.abs(n - m) > Math.max(n, m) * 0.3) {
            return 0.0; // 长度差异太大，不匹配
        }
        
        double[][] dtw = new double[n + 1][m + 1];
        
        // 初始化
        for (int i = 0; i <= n; i++) {
            dtw[i][0] = Double.POSITIVE_INFINITY;
        }
        for (int j = 0; j <= m; j++) {
            dtw[0][j] = Double.POSITIVE_INFINITY;
        }
        dtw[0][0] = 0;
        
        // 填充DTW矩阵
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                double cost = Math.abs(seq1[i - 1] - seq2[j - 1]);
                dtw[i][j] = cost + Math.min(dtw[i - 1][j], Math.min(dtw[i][j - 1], dtw[i - 1][j - 1]));
            }
        }
        
        // 转换为相似度（0-1）
        double distance = dtw[n][m];
        double maxDistance = Math.max(n, m);
        double similarity = 1.0 - (distance / maxDistance);
        
        return Math.max(0, similarity);
    }
    
    /**
     * 显示消息
     */
    private void showMessage(String message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.execute(() -> {
                minecraft.player.displayClientMessage(Component.literal(message), true);
            });
        }
    }
    
    /**
     * 检查是否有模板
     */
    public boolean hasTemplates() {
        return !templates.isEmpty();
    }
    
    /**
     * 获取模板数量
     */
    public int getTemplateCount() {
        return templates.size();
    }
}
