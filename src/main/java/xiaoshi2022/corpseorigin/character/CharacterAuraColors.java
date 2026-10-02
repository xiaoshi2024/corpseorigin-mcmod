package xiaoshi2022.corpseorigin.character;

import java.util.Map;

/**
 * 角色气息主色 —— 剑气 / 目击气 / 光束等招式视觉按施法者角色取色。
 * 未配置的角色统一回落到默认剑罡金（0xFFB137，即历史固定色）。
 */
public final class CharacterAuraColors {

    private CharacterAuraColors() {}

    public static final int DEFAULT = 0xFFB137; // 默认剑罡金

    private static final Map<String, Integer> AURA = Map.ofEntries(
            Map.entry("mortal", DEFAULT),
            Map.entry("baixiaofei", 0xFFC428),          // 白小飞：罡金
            Map.entry("heixiaofei", 0xB01E30),          // 黑小飞：尸化暗红
            Map.entry("longyou", 0x50F08C),             // 龙右：幽绿尸气
            Map.entry("shichaozhizi", 0x9B59E0),        // 尸潮之子：诡紫
            Map.entry("kaiweinai", 0xFF78BE),           // 开胃奶：菊花粉紫
            Map.entry("xiaolu", 0x66E08C),              // 小鹿：翠绿
            Map.entry("xiaoyanzi", 0x4AA8FF),           // 小燕子：燕羽蓝
            Map.entry("tianxianbaobao_zb", 0xFFB0D8),   // 天仙宝宝：甜粉
            Map.entry("jingang_zb", 0xC8C8B8),          // 金刚尸兄：石灰
            Map.entry("xiaohui", 0xBFC8D0),             // 小灰：银灰
            Map.entry("tushu", 0xD8A848),               // 图鼠：沙金
            Map.entry("muxi", 0xCD9646),                // 木犀：黄土
            Map.entry("formation_metal", 0xFFEFC0),     // 五行·金：鎏金白
            Map.entry("formation_water", 0x3CC8E8),     // 五行·水：碧水蓝
            Map.entry("formation_earth", 0xB87838),     // 五行·土：厚土棕
            Map.entry("yanyan", 0xFF6A30),              // 炎燕：烈焰橙红
            Map.entry("fengmohuitailang", 0x5A8CFF),    // 风魔灰太郎：忍风靛蓝
            Map.entry("k", 0xDC283C),                   // K：血红
            Map.entry("zhaoritian", 0x28DCDC),          // 赵日天：天罡青
            Map.entry("chongmu", 0xB4E63C),             // 虫母：虫群酸绿
            Map.entry("chongqun", 0xB4E63C),            // 虫群：同色
            Map.entry("hujie", 0xC060C0),               // 胡姐：紫红
            Map.entry("weixin", 0x8CA0B8),              // 威信：钢灰蓝
            Map.entry("corpse_brother", 0x7FA05A),      // 尸兄：浊绿
            Map.entry("zuohufa", 0x8C3050),             // 左护法：暗绛紫
            Map.entry("bianyi_guiyu", 0x3C8C8C),        // 变异怪鱼：深海青
            Map.entry("yanhuang_budui", 0xFF9A2E));     // 炎黄部队：军橙

    /** 按角色 id 取气息主色（0xRRGGBB），未知角色回落默认 */
    public static int aura(String characterId) {
        return characterId == null ? DEFAULT : AURA.getOrDefault(characterId, DEFAULT);
    }

    /** 拆成 {r,g,b}（0~255），供渲染顶点色使用 */
    public static int[] rgb(int color) {
        return new int[]{(color >> 16) & 255, (color >> 8) & 255, color & 255};
    }

    /** 提亮做内芯高光色，clamp 到 255 */
    public static int[] bright(int[] c, double factor) {
        return new int[]{(int)Math.min(255, c[0] * factor),
                (int)Math.min(255, c[1] * factor), (int)Math.min(255, c[2] * factor)};
    }
}
