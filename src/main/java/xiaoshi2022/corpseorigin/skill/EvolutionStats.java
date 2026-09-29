package xiaoshi2022.corpseorigin.skill;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-authoritative realm attributes with role-specific health/attack biases.
 * RealmRules supplies the nonlinear tier baseline; independent practice/purchased
 * ranks add diminishing growth. The old linear profiles remain as the fallback
 * when realm.enabled is false. Fixed modifier ids make reconciliation idempotent.
 */
public final class EvolutionStats {

    private EvolutionStats() {
    }

    // ==================== 修饰符 id（固定，全局唯一） ====================

    private static final Identifier HEALTH_MODIFIER = CorpseOrigin.id("evo_health");
    private static final Identifier ARMOR_MODIFIER = CorpseOrigin.id("evo_armor");
    private static final Identifier ATTACK_MODIFIER = CorpseOrigin.id("evo_attack");
    private static final Identifier SPEED_MODIFIER = CorpseOrigin.id("evo_speed");
    private static final Identifier KNOCKBACK_MODIFIER = CorpseOrigin.id("evo_knockback");

    /**
     * 成长原型 —— 括号内是<b>满级（20 级）总加成</b>，1 级为 0，中间线性增长。
     * <p>
     * 综合预算参考（血×1 + 攻×8 + 甲×4 + 速×500 + 抗击退×40）：
     * 均衡 123 / 铁壁 136 / 狂战 124 / 刺客 109 / 迅捷 111 / 巨神 162。
     * 巨神预算偏高，只给体型巨大的 BOSS 级角色（左护法 / 尸巢之子），
     * 因为他们的基础面板里没有速度这一项，预算天然向血甲倾斜。
     */
    public enum GrowthProfile {
        /** 均衡：血 40 / 攻 4 / 甲 6 / 速 +0.03 / 抗击退 0.3 —— 没短板，没特长（默认） */
        BALANCED(40.0, 4.0, 6.0, 0.03, 0.30),
        /** 铁壁：血 60 / 攻 2 / 甲 10 / 速 0 / 抗击退 0.5 —— 扛伤型 */
        TANK(60.0, 2.0, 10.0, 0.00, 0.50),
        /** 狂战：血 30 / 攻 8 / 甲 3 / 速 +0.02 / 抗击退 0.2 —— 进攻型 */
        BERSERKER(30.0, 8.0, 3.0, 0.02, 0.20),
        /** 刺客：血 24 / 攻 6 / 甲 2 / 速 +0.05 / 抗击退 0.1 —— 高敏刺杀型 */
        ASSASSIN(24.0, 6.0, 2.0, 0.05, 0.10),
        /** 迅捷：血 30 / 攻 4 / 甲 4 / 速 +0.05 / 抗击退 0.2 —— 游走辅助型 */
        SWIFT(30.0, 4.0, 4.0, 0.05, 0.20),
        /** 巨神：血 70 / 攻 5 / 甲 8 / 速 0 / 抗击退 0.5 —— 巨型 BOSS 专属 */
        TITAN(70.0, 5.0, 8.0, 0.00, 0.50);

        private final double maxHealth;
        private final double maxAttack;
        private final double maxArmor;
        private final double maxSpeed;
        private final double maxKnockback;

        GrowthProfile(double maxHealth, double maxAttack, double maxArmor,
                      double maxSpeed, double maxKnockback) {
            this.maxHealth = maxHealth;
            this.maxAttack = maxAttack;
            this.maxArmor = maxArmor;
            this.maxSpeed = maxSpeed;
            this.maxKnockback = maxKnockback;
        }

        /** 当前等级进度 [0,1]：1 级 0，MAX_LEVEL 级 1 */
        private static double progress(int level) {
            int clamped = Math.max(1, Math.min(EvolutionManager.MAX_LEVEL, level));
            return (clamped - 1.0) / (EvolutionManager.MAX_LEVEL - 1);
        }
    }

    // ==================== 角色 → 成长原型 映射 ====================

    private static final Map<String, GrowthProfile> PROFILE_MAP = new HashMap<>();

    static {
        populateProfileMap();
    }

    /**
     * 集中映射表 —— 想让某角色走哪条成长路线只改这里。
     * 没列出的角色一律 {@link GrowthProfile#BALANCED}，新加角色不会漏。
     */
    private static void populateProfileMap() {
        // ---------- 主角 / 尸王 ----------
        PROFILE_MAP.put("baixiaofei", GrowthProfile.BERSERKER);   // 白小飞：越战越勇
        PROFILE_MAP.put("heixiaofei", GrowthProfile.ASSASSIN);    // 黑小飞：黑化敏捷
        PROFILE_MAP.put("longyou", GrowthProfile.BALANCED);       // 龙右：基础面板已极强，成长均衡
        PROFILE_MAP.put("shichaozhizi", GrowthProfile.TITAN);     // 尸巢之子：万人气血巨躯
        PROFILE_MAP.put("zhaoritian", GrowthProfile.BERSERKER);   // 赵日天：战力爆表

        // ---------- 炎黄特能队 / 人类方 ----------
        PROFILE_MAP.put("kaiweinai", GrowthProfile.TANK);         // 开胃奶：菊花盾前排
        PROFILE_MAP.put("xiaoyanzi", GrowthProfile.BERSERKER);    // 小言子
        PROFILE_MAP.put("xiaolu", GrowthProfile.SWIFT);           // 小鹿：灵活
        PROFILE_MAP.put("muxi", GrowthProfile.TANK);              // 木犀：土系重甲
        PROFILE_MAP.put("yanyan", GrowthProfile.BERSERKER);       // 炎燕：火系强攻
        PROFILE_MAP.put("formation_metal", GrowthProfile.BERSERKER); // 五行·金
        PROFILE_MAP.put("formation_water", GrowthProfile.SWIFT);     // 五行·水
        PROFILE_MAP.put("formation_earth", GrowthProfile.TANK);      // 五行·土

        // ---------- 东瀛 / 米国欧盟 ----------
        PROFILE_MAP.put("fengmohuitailang", GrowthProfile.ASSASSIN); // 风魔灰太郎：忍术
        PROFILE_MAP.put("k", GrowthProfile.ASSASSIN);                // 黑暗议会 K
        PROFILE_MAP.put("heianhui_suicong", GrowthProfile.ASSASSIN);

        // ---------- 尸王阵营的尸兄 ----------
        PROFILE_MAP.put("zuohufa", GrowthProfile.TITAN);          // 左护法：合体青龙巨躯
        PROFILE_MAP.put("tianxianbaobao_zb", GrowthProfile.TANK); // 天线宝宝尸兄
        PROFILE_MAP.put("jingang_zb", GrowthProfile.TANK);        // 金刚尸兄
        PROFILE_MAP.put("chongmu", GrowthProfile.TANK);           // 虫母
        PROFILE_MAP.put("xiongxing_zb", GrowthProfile.TANK);      // 熊型尸兄
        PROFILE_MAP.put("bianselong_zb", GrowthProfile.ASSASSIN); // 变色龙尸兄
        PROFILE_MAP.put("hujie", GrowthProfile.ASSASSIN);         // 狐姐
        PROFILE_MAP.put("qingwa_zb", GrowthProfile.SWIFT);        // 青蛙尸兄
        PROFILE_MAP.put("chongqun", GrowthProfile.SWIFT);         // 虫群：数量游走
    }

    /** 某角色走哪条成长路线（未配置 = 均衡） */
    public static GrowthProfile profileOf(String characterId) {
        return PROFILE_MAP.getOrDefault(characterId, GrowthProfile.BALANCED);
    }

    // ==================== 核心：按当前角色 + 等级重算进化加成 ====================

    /**
     * 按玩家当前角色与进化等级，把进化属性加成重算一遍（原地替换，重复调用安全）。
     * <p>
     * 调用时机：登录、重生、换角色后、获得进化点（可能升级）后、调试指令改点后。
     */
    public static void reconcile(ServerPlayer player) {
        xiaoshi2022.corpseorigin.component.PlayerCorpseComponent.syncEvolvedEye(player);
        String roleId = CharacterManager.getInstance().getPlayerCharacterId(player);
        GrowthProfile profile = profileOf(roleId);

        PlayerCharacterData data = PlayerCharacterData.get(player);
        int earned = data.getEarnedPoints(player.getUUID());
        int level = EvolutionManager.getLevel(earned);
        double p = GrowthProfile.progress(level);

        float oldMax = player.getMaxHealth();
        if (xiaoshi2022.corpseorigin.growth.RealmProgression.config().enabled) {
            double hp = xiaoshi2022.corpseorigin.growth.RealmProgression.healthBonus(player) * (.75 + profile.maxHealth / 160);
            double attack = xiaoshi2022.corpseorigin.growth.RealmProgression.attackBonus(player) * (.75 + profile.maxAttack / 16);
            applyModifier(player, Attributes.MAX_HEALTH, HEALTH_MODIFIER, hp);
            applyModifier(player, Attributes.ATTACK_DAMAGE, ATTACK_MODIFIER, attack);
            applyModifier(player, Attributes.ARMOR, ARMOR_MODIFIER, Math.min(30, (level-1)*2));
            applyModifier(player, Attributes.MOVEMENT_SPEED, SPEED_MODIFIER,
                    xiaoshi2022.corpseorigin.growth.RealmRules.speed(level,xiaoshi2022.corpseorigin.growth.RealmProgression.rank(player,"agility")));
            applyModifier(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_MODIFIER, Math.min(1,(level-1)*.08));
            applyModifier(player, Attributes.ARMOR_TOUGHNESS, CorpseOrigin.id("evo_toughness"), Math.min(20,(level-1)*1.2));
            applyModifier(player, Attributes.ENTITY_INTERACTION_RANGE, CorpseOrigin.id("evo_reach"), Math.min(8,(level-1)*.3));
            applyModifier(player, Attributes.BLOCK_BREAK_SPEED, CorpseOrigin.id("evo_mining"), Math.min(100,(level-1)*2));
            applyModifier(player, Attributes.SAFE_FALL_DISTANCE, CorpseOrigin.id("evo_fall"), level>=9 ? 10000 : 0);
            if (player.isAlive() && player.getMaxHealth()>oldMax) player.heal(player.getMaxHealth()-oldMax);
            if (player.getHealth()>player.getMaxHealth()) player.setHealth(player.getMaxHealth());
            return;
        }
        applyModifier(player, Attributes.ARMOR_TOUGHNESS, CorpseOrigin.id("evo_toughness"), 0);
        applyModifier(player, Attributes.ENTITY_INTERACTION_RANGE, CorpseOrigin.id("evo_reach"), 0);
        applyModifier(player, Attributes.BLOCK_BREAK_SPEED, CorpseOrigin.id("evo_mining"), 0);
        applyModifier(player, Attributes.SAFE_FALL_DISTANCE, CorpseOrigin.id("evo_fall"), 0);

        applyModifier(player, Attributes.MAX_HEALTH, HEALTH_MODIFIER, profile.maxHealth * p);
        applyModifier(player, Attributes.ARMOR, ARMOR_MODIFIER, profile.maxArmor * p);
        applyModifier(player, Attributes.ATTACK_DAMAGE, ATTACK_MODIFIER, profile.maxAttack * p);
        applyModifier(player, Attributes.MOVEMENT_SPEED, SPEED_MODIFIER, profile.maxSpeed * p);
        applyModifier(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_MODIFIER, profile.maxKnockback * p);
        if (player.getHealth()>player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    /**
     * 获得进化点之后调用：重算加成，若因此升级，把<b>新增的血量上限即时补上</b>
     * （升级 = 实打实变肉，而不是顶着一截空血条）；并返回新等级是否高于旧等级。
     *
     * @param levelBefore 加点之前的进化等级
     * @return 是否发生了升级（调用方可据此播提示 / 音效）
     */
    public static boolean reconcileAfterPointGain(ServerPlayer player, int levelBefore) {
        float maxHealthBefore = player.getMaxHealth();
        reconcile(player);

        UUID uuid = player.getUUID();
        int levelAfter = EvolutionManager.getLevel(
                PlayerCharacterData.get(player).getEarnedPoints(uuid));
        if (levelAfter <= levelBefore) {
            return false;
        }

        float gained = player.getMaxHealth() - maxHealthBefore;
        if (!xiaoshi2022.corpseorigin.growth.RealmProgression.config().enabled && gained > 0.0F && player.isAlive()) {
            player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + gained));
        }
        return true;
    }

    /**
     * 加一条进化修饰符；数额接近 0 时摘掉（1 级无加成，不留空修饰符）。
     */
    private static void applyModifier(ServerPlayer player, Holder<Attribute> attribute,
                                      Identifier id, double amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        if (Math.abs(amount) < 1.0E-4) {
            instance.removeModifier(id);
            return;
        }
        AttributeModifier existing = instance.getModifier(id);
        if (existing != null
                && existing.operation() == AttributeModifier.Operation.ADD_VALUE
                && Math.abs(existing.amount() - amount) < 1.0E-12) {
            return;   // 数值没变，别白改
        }
        instance.addOrReplacePermanentModifier(
                new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
    }
}
