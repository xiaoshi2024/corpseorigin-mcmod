package xiaoshi2022.corpseorigin.character;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.zuohufa.DetachGuardianSkill;
import xiaoshi2022.corpseorigin.skill.zuohufa.MergeGuardianSkill;
import xiaoshi2022.corpseorigin.skill.zuohufa.MouthSnakeSkill;
import xiaoshi2022.corpseorigin.skill.zuohufa.QiLockSkill;
import xiaoshi2022.corpseorigin.skill.zuohufa.TyrantStrikeSkill;

import java.util.List;

/**
 * 左护法 - 尸王亲信，原著里与一条青龙合体成尸兄，是尸王「四大神宠」之一「青龙」。
 * <p>
 * 角色效果：拿到角色即<b>变成尸兄阵营</b>，并且整具身体换成 {@code zuo_guardian} 蛟龙
 * （见 {@code VARIANT_ZUO_GUARDIAN}）—— 原版玩家模型与盔甲不画，改渲染巨蛇+骑手，
 * 骑手贴图上是玩家本人皮肤（见 {@code ZuoGuardianSkinBuilder}）。
 * <p>
 * 招式取自原著：<b>暴龙一击</b>（武神阁贯注重击）、<b>嘴里吐蛇</b>（金旒龙贯穿）、
 * <b>锁气</b>（被闪瞎也能锁住敌人身上的气）；另有 <b>脱离 / 合体</b> 一对形态技 ——
 * 把蛟龙蜕下来当宠物 BOSS（{@code zuo_flood_long}），也能再收回身上。
 */
public class ZuoHuFa implements ICharacter {

    public static final String ID = "zuohufa";

    private static final List<ISkill> SKILLS = List.of(
            new TyrantStrikeSkill(),
            new MouthSnakeSkill(),
            new QiLockSkill(),
            new DetachGuardianSkill(),
            new MergeGuardianSkill()
    );

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Component getName() {
        return Component.translatable("character.corpseorigin." + ID);
    }

    @Override
    public Component getDescription() {
        return Component.translatable("character.corpseorigin." + ID + ".desc");
    }

    @Override
    public Identifier getIcon() {
        return ICharacter.iconId(ID);
    }

    @Override
    public boolean isPassive() {
        return false;
    }

    @Override
    public List<Component> getTraits() {
        return List.of(
                Component.translatable("character.corpseorigin." + ID + ".trait1"),
                Component.translatable("character.corpseorigin." + ID + ".trait2"),
                Component.translatable("character.corpseorigin." + ID + ".trait3")
        );
    }

    @Override
    public List<ISkill> getSkills() {
        return SKILLS;
    }

    @Override
    public float getInfectionMultiplier() {
        return 0.0f;
    }

    @Override
    public int getMaxInnerPower() {
        return 90;
    }

    @Override
    public void onAcquire(Player player) {
        PlayerCorpseComponent comp = PlayerCorpseComponent.get(player);
        if (comp.isCorpse()) {
            // 本来就是尸兄（感染 / 别的尸兄角色转过来）→ 只改外观变种，别把已有的类型、进化等级、饥饱抹掉
            comp.setVariant(PlayerCorpseComponent.VARIANT_ZUO_GUARDIAN);
        } else {
            PlayerCorpseComponent.setPlayerAsCorpse(player, PlayerCorpseComponent.TYPE_ELITE,
                    PlayerCorpseComponent.VARIANT_ZUO_GUARDIAN);
        }

        // 玩家角色要保留意识，否则一换角色就变成只会本能行动的怪物

        // 基础数值：拿到角色就是蛟龙合体形态那一档（必须在变种设好之后调，它按形态取档）
        applyIfZuoHuFa(player);

        // restoreConsciousness / setVariant 不会触发广播；必须广播：尸兄外观是别的玩家看你时才渲染的
        if (player instanceof ServerPlayer serverPlayer) {
            CorpseNetwork.broadcastPlayerCorpseSync(serverPlayer);
        }
    }

    @Override
    public void onLose(Player player) {
        // 失去左护法身份 → 先摘掉数值加成，再清尸兄状态与蛟龙外观
        removeBaseAttributes(player);
        PlayerCorpseComponent.removeCorpseState(player);
    }

    // ==================== 基础数值 ====================

    /**
     * 人形（蛟龙已脱离在外）的面板。
     * <p>
     * 数值用 {@link AttributeModifier} 套、随时能摘，所以换角色 / 换身体都不会留残值
     * （和尸王 {@code LongYou} 同一套路）。整体比尸王（50 血 / 10 护甲 / 6 攻击）低一档 ——
     * 他是尸王的神宠，不该压过主子。
     */
    public static final double HUMAN_HEALTH = 30.0;
    public static final double HUMAN_ARMOR = 5.0;
    public static final double HUMAN_ATTACK = 3.5;
    public static final double HUMAN_SPEED = 0.105;
    public static final double HUMAN_KNOCKBACK = 0.25;

    /**
     * 蛟龙合体形态的面板：血量比分离时高 <b>5 颗心</b>，攻防也各高一档 ——
     * 龙身是尸王「四大神宠」之一，穿着它当然比赤手空拳的人形能扛。
     * <p>
     * 另外合体形态是<b>水生物</b>：{@code WATER_MOVEMENT_EFFICIENCY} 拉满（水里不被拖着走），
     * 水下呼吸见 {@code LivingEntityBreatheMixin}，游动表现见动画对应表里的 {@code swim}。
     */
    public static final double MERGED_HEALTH = 40.0;
    public static final double MERGED_ARMOR = 8.0;
    public static final double MERGED_ATTACK = 5.0;
    public static final double MERGED_SPEED = 0.115;
    public static final double MERGED_KNOCKBACK = 0.45;
    /** 水下移动效率：0 = 普通人（被水阻拖），1 = 水生物（原版海豚同档） */
    public static final double MERGED_WATER_EFFICIENCY = 1.0;
    /** 合体青龙的实体体型；原版 SCALE 会同步碰撞箱、眼高与第一人称相机。 */
    public static final float MERGED_SCALE = 3.0F;
    // Reach is measured from the eyes, which are roughly 4.9 blocks above the ground.
    public static final double MERGED_ATTACK_RANGE = 6.0;

    private static final Identifier HEALTH_MODIFIER = CorpseOrigin.id("zuohufa_health");
    private static final Identifier ARMOR_MODIFIER = CorpseOrigin.id("zuohufa_armor");
    private static final Identifier ATTACK_MODIFIER = CorpseOrigin.id("zuohufa_attack");
    private static final Identifier SPEED_MODIFIER = CorpseOrigin.id("zuohufa_speed");
    private static final Identifier KNOCKBACK_MODIFIER = CorpseOrigin.id("zuohufa_knockback");
    private static final Identifier WATER_MODIFIER = CorpseOrigin.id("zuohufa_water_efficiency");
    private static final Identifier SCALE_MODIFIER = CorpseOrigin.id("zuohufa_scale");
    private static final Identifier REACH_MODIFIER = CorpseOrigin.id("zuohufa_reach");

    /** 这具身体是不是"左护法身体"（认角色，不认账号） */
    public static boolean isZuoHuFaBody(LivingEntity entity) {
        return entity instanceof ServerPlayer player
                && ID.equals(CharacterManager.getInstance().getPlayerCharacterId(player));
    }

    /** 是左护法就按当前形态套上对应档位的数值，否则摘掉 —— 重复调用安全 */
    public static void applyIfZuoHuFa(LivingEntity entity) {
        if (isZuoHuFaBody(entity) && entity instanceof Player player) {
            applyBaseAttributes(entity, PlayerCorpseComponent.isMutantVariant(player));
        } else {
            removeBaseAttributes(entity);
        }
    }

    /**
     * 把某一档的面板套到这具身体上。
     * <p>
     * 加成量按<b>当前基础值</b>反算，所以不管是玩家本体（20 血）还是别的尸兄身体，
     * 最终都落到同一套数值上；同一个 id 会原地替换，不会叠加。
     *
     * @param merged 现在是蛟龙合体形态吗（true = 合体那一档，false = 人形那一档）
     */
    public static void applyBaseAttributes(LivingEntity entity, boolean merged) {
        setAttribute(entity, Attributes.MAX_HEALTH, HEALTH_MODIFIER, merged ? MERGED_HEALTH : HUMAN_HEALTH);
        setAttribute(entity, Attributes.ARMOR, ARMOR_MODIFIER, merged ? MERGED_ARMOR : HUMAN_ARMOR);
        setAttribute(entity, Attributes.ATTACK_DAMAGE, ATTACK_MODIFIER, merged ? MERGED_ATTACK : HUMAN_ATTACK);
        setAttribute(entity, Attributes.MOVEMENT_SPEED, SPEED_MODIFIER, merged ? MERGED_SPEED : HUMAN_SPEED);
        setAttribute(entity, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_MODIFIER,
                merged ? MERGED_KNOCKBACK : HUMAN_KNOCKBACK);
        // 水生物只在合体档：人形那边回到普通人的 0（这项是 [0,1] 的效率值，拉满就是海豚那档）
        setAttribute(entity, Attributes.WATER_MOVEMENT_EFFICIENCY, WATER_MODIFIER,
                merged ? MERGED_WATER_EFFICIENCY : 0.0D);
        if (merged) {
            setAttribute(entity, Attributes.SCALE, SCALE_MODIFIER, MERGED_SCALE);
            setAttribute(entity, Attributes.ENTITY_INTERACTION_RANGE, REACH_MODIFIER, MERGED_ATTACK_RANGE);
        } else {
            removeAttribute(entity, Attributes.SCALE, SCALE_MODIFIER);
            removeAttribute(entity, Attributes.ENTITY_INTERACTION_RANGE, REACH_MODIFIER);
        }
        entity.refreshDimensions();
    }

    /** 摘掉左护法的数值加成（换角色 / 不再是左护法身体时用） */
    public static void removeBaseAttributes(LivingEntity entity) {
        removeAttribute(entity, Attributes.MAX_HEALTH, HEALTH_MODIFIER);
        removeAttribute(entity, Attributes.ARMOR, ARMOR_MODIFIER);
        removeAttribute(entity, Attributes.ATTACK_DAMAGE, ATTACK_MODIFIER);
        removeAttribute(entity, Attributes.MOVEMENT_SPEED, SPEED_MODIFIER);
        removeAttribute(entity, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_MODIFIER);
        removeAttribute(entity, Attributes.WATER_MOVEMENT_EFFICIENCY, WATER_MODIFIER);
        removeAttribute(entity, Attributes.SCALE, SCALE_MODIFIER);
        removeAttribute(entity, Attributes.ENTITY_INTERACTION_RANGE, REACH_MODIFIER);
        entity.refreshDimensions();
    }

    private static void setAttribute(LivingEntity entity, Holder<Attribute> attribute,
                                     Identifier id, double target) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        double delta = target - instance.getBaseValue();
        AttributeModifier existing = instance.getModifier(id);
        if (existing != null
                && existing.operation() == AttributeModifier.Operation.ADD_VALUE
                && existing.amount() == delta) {
            return;   // 已经是这套数值了，别白改一遍
        }
        instance.addOrReplacePermanentModifier(
                new AttributeModifier(id, delta, AttributeModifier.Operation.ADD_VALUE));
    }

    private static void removeAttribute(LivingEntity entity, Holder<Attribute> attribute, Identifier id) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
        }
    }
}
