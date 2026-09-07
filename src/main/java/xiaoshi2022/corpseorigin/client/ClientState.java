package xiaoshi2022.corpseorigin.client;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 客户端状态缓存（由 S2C 包驱动更新）
 */
public final class ClientState {

    private ClientState() {
    }

    /** 当前角色ID */
    public static String characterId = "mortal";
    /** 累计进化点 */
    public static int earnedPoints = 0;
    /** 可用进化点 */
    public static int availablePoints = 0;
    /** 击杀数 */
    public static int kills = 0;
    /** 感染度 0-100 */
    public static int infection = 0;
    /** 已学技能ID集合（字符串路径） */
    public static final Set<String> learnedSkills = new HashSet<>();
    /** 技能冷却结束时间戳（毫秒） */
    public static final Map<String, Long> cooldownEnds = new HashMap<>();

    public static void applyEvolution(int earned, int available, int kills, byte[] learnedBytes) {
        ClientState.earnedPoints = earned;
        ClientState.availablePoints = available;
        ClientState.kills = kills;
        learnedSkills.clear();
        String joined = new String(learnedBytes, StandardCharsets.UTF_8);
        if (!joined.isEmpty()) {
            learnedSkills.addAll(java.util.Arrays.asList(joined.split("\n")));
        }
    }

    public static boolean hasLearned(String skillPath) {
        return learnedSkills.contains(skillPath);
    }

    public static int getCooldownRemaining(String skillPath) {
        Long end = cooldownEnds.get(skillPath);
        if (end == null) {
            return 0;
        }
        long remaining = end - System.currentTimeMillis();
        if (remaining <= 0) {
            cooldownEnds.remove(skillPath);
            return 0;
        }
        return (int) (remaining / 50);
    }

    public static void applyCooldown(String skillPath, int ticks) {
        if (ticks > 0) {
            cooldownEnds.put(skillPath, System.currentTimeMillis() + ticks * 50L);
        } else {
            cooldownEnds.remove(skillPath);
        }
    }
}
