package xiaoshi2022.corpseorigin.character;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import xiaoshi2022.corpseorigin.entity.CloneAvatarEntity;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.registry.ModItems;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.longyou.*;
import xiaoshi2022.corpseorigin.skill.zhaoritian.TianGangKeySkill;

import java.util.List;

/**
 * 龙右 - 尸王
 * <p>
 * 成为龙右即直接变为满级尸兄（尸王类型），并保留意识。
 * <p>
 * 含《尸巢之战篇》设定的不死髅体、雷电骨架技能，以及"尸水之源"——
 * 走过的水源会被污染成尸水（被动，实装在 {@code LongYouEventHandler}）。
 */
public class LongYou implements ICharacter {

    public static final String ID = "longyou";

    private static final List<ISkill> SKILLS = List.of(
            new TianGangSkill(TianGangSkill.Form.ZHI), new TianGangSkill(TianGangSkill.Form.JI),
            new TianGangSkill(TianGangSkill.Form.LI), new TianGangSkill(TianGangSkill.Form.YU),
            new TianGangSkill(TianGangSkill.Form.QI), new TianGangSkill(TianGangSkill.Form.HUI),
            new TianGangSkill(TianGangSkill.Form.MIE), new TianGangSkill(TianGangSkill.Form.WU),
            new TianGangSkill(TianGangSkill.Form.SHEN), new TianGangSkill(TianGangSkill.Form.NIPO),
            new TianGangSkill(TianGangSkill.Form.POGANG), new TianGangSkill(TianGangSkill.Form.TIANGANGPO),
            new UndyingChestSkill(),
            // 雷电系：吸收电鳗获得的「雷电之力」是基础，雷鳗与球状闪电都由它解锁
            new ThunderPowerSkill(),
            new CorpseKingThunderSkill(),
            new BallLightningSkill(),
            new CorpseKingInfrasoundSkill(),
            new WaterPollutionSkill(),
            // 感染领域：龙右展开可调半径的感染领域，范围内的原版怪物被周期性感染为半尸兄
            new InfectionDomainSkill(),
            new CorpseBrotherRallySkill(),
            new NestSenseSkill(),
            // 换身体的两手：缩进原体 / 重塑一具新身体（旧身体都会蜕成分身）
            new GoldenCicadaShellSkill(),
            new FleshReshapeSkill(),
            new FleshAbandonSkill(), new XuanwuBodySkill(), new JingangInfantConvergenceSkill(),
            // 天罡匙：龙右作为尸王也能掌握这件神兵（原著中天罡匙是神兵，不是赵日天专属道具）
            new TianGangKeySkill()
    );

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Component getName() {
        return Component.translatable("character.corpseorigin.longyou");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("character.corpseorigin.longyou.desc");
    }

    @Override
    public Identifier getIcon() {
        return ICharacter.iconId("longyou");
    }

    @Override
    public boolean isPassive() {
        return false;
    }

    @Override
    public List<Component> getTraits() {
        return List.of(
                Component.translatable("character.corpseorigin.longyou.trait1"),
                Component.translatable("character.corpseorigin.longyou.trait2"),
                Component.translatable("character.corpseorigin.longyou.trait3")
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

    /** 龙右（尸王）：千年修为，内力深厚，上限 200 */
    @Override
    public int getMaxInnerPower() {
        return 200;
    }

    // ==================== 尸王的基础数值 ====================

    /** 血量：尸王拥有 200 点生命，高于重做后赵日天的 100 点。 */
    public static final double BASE_HEALTH = 200.0;
    /** 护甲（裸装）：不死髅体本身的防御力，可再被「玄武体」之类的技能临时拉高 */
    public static final double BASE_ARMOR = 10.0;
    /** 徒手攻击力：强过普通玩家（1），弱于下界合金剑（8），给武器留出空间 */
    public static final double BASE_ATTACK_DAMAGE = 6.0;
    /** 移动速度：略快于普通玩家（0.1），但不宜过快，保持追击压迫感 */
    public static final double BASE_MOVEMENT_SPEED = 0.12;
    /** 击退抗性：体型高大且实力碾压，普通攻击撼不动 */
    public static final double BASE_KNOCKBACK_RESISTANCE = 0.5;

    private static final Identifier HEALTH_MODIFIER = CorpseOrigin.id("longyou_base_health");
    private static final Identifier ARMOR_MODIFIER = CorpseOrigin.id("longyou_base_armor");
    private static final Identifier ATTACK_MODIFIER = CorpseOrigin.id("longyou_base_attack");
    private static final Identifier SPEED_MODIFIER = CorpseOrigin.id("longyou_base_speed");
    private static final Identifier KNOCKBACK_MODIFIER = CorpseOrigin.id("longyou_base_knockback");

    /**
     * 把尸王的基础数值套到这具身体上。
     * <p>
     * 用 {@link AttributeModifier} 而不是直接改 baseValue：修饰符能随时摘掉，
     * 不用记住"原本是多少"，玩家换角色、分身换身体时不会留下残值。
     * <p>
     * 加成量按<b>当前基础值</b>反算，所以不管是玩家（血量 20 / 护甲 0 / 攻击 1）
     * 还是尸兄克隆体（继承低等尸兄的 20 / 2 / 3），最终都落到同一套数值上。
     * 重复调用安全：同一个 id 会原地替换，不会叠加。
     */
    public static void applyBaseAttributes(LivingEntity entity) {
        setAttribute(entity, Attributes.MAX_HEALTH, HEALTH_MODIFIER, BASE_HEALTH);
        setAttribute(entity, Attributes.ARMOR, ARMOR_MODIFIER, BASE_ARMOR);
        setAttribute(entity, Attributes.ATTACK_DAMAGE, ATTACK_MODIFIER, BASE_ATTACK_DAMAGE);
        setAttribute(entity, Attributes.MOVEMENT_SPEED, SPEED_MODIFIER, BASE_MOVEMENT_SPEED);
        setAttribute(entity, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_MODIFIER, BASE_KNOCKBACK_RESISTANCE);
    }

    /** 摘掉尸王的基础数值加成（换角色 / 不再是龙右身体时用） */
    public static void removeBaseAttributes(LivingEntity entity) {
        removeAttribute(entity, Attributes.MAX_HEALTH, HEALTH_MODIFIER);
        removeAttribute(entity, Attributes.ARMOR, ARMOR_MODIFIER);
        removeAttribute(entity, Attributes.ATTACK_DAMAGE, ATTACK_MODIFIER);
        removeAttribute(entity, Attributes.MOVEMENT_SPEED, SPEED_MODIFIER);
        removeAttribute(entity, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_MODIFIER);
    }

    // ==================== 原体的体术 ====================

    /** 原体的跳跃力加成（玩家基础 0.42 之上再加这么多） */
    public static final double ORIGINAL_BODY_JUMP_BONUS = 0.35;
    /** 原体的安全落距：给到这个数就基本等于摔不伤（真身靠身法，不靠蛮力） */
    public static final double ORIGINAL_BODY_SAFE_FALL = 100.0;

    private static final Identifier JUMP_MODIFIER = CorpseOrigin.id("original_body_jump");
    private static final Identifier SAFE_FALL_MODIFIER = CorpseOrigin.id("original_body_safe_fall");

    /**
     * 原体的体术：<b>跳得高、摔不伤</b>。
     * <p>
     * 拇指大小的真身打不了肉搏，但身法极强 —— 跳得比正常人高一大截，落地也不摔伤。
     * 不是原体就摘掉这些加成，所以换回正常身体时不会残留。
     * 重复调用安全。
     *
     * @param originalBody 这个实体当前是不是正处在「原体」形态（体型被缩小过）
     */
    public static void applyOriginalBodyAgility(LivingEntity entity, boolean originalBody) {
        AttributeInstance jump = entity.getAttribute(Attributes.JUMP_STRENGTH);
        if (jump != null) {
            jump.removeModifier(JUMP_MODIFIER);
            if (originalBody) {
                jump.addOrReplacePermanentModifier(new AttributeModifier(
                        JUMP_MODIFIER, ORIGINAL_BODY_JUMP_BONUS, AttributeModifier.Operation.ADD_VALUE));
            }
        }

        if (originalBody) {
            setAttribute(entity, Attributes.SAFE_FALL_DISTANCE, SAFE_FALL_MODIFIER, ORIGINAL_BODY_SAFE_FALL);
        } else {
            removeAttribute(entity, Attributes.SAFE_FALL_DISTANCE, SAFE_FALL_MODIFIER);
        }
    }

    /**
     * 把体型复位成正常大小，并摘掉原体体术。
     * <p>
     * <b>死亡重生必须走一遍</b>：换身体时体型是写在 {@code SCALE} 属性上的，而重生会把属性一起带过去 ——
     * 死在"拇指原体"里的话，重生之后玩家会一直是个小人儿（正常死亡夺舍那条路走 {@code apply()}，
     * 会按新身体重写体型，所以只有"没有备用身体 → 原版重生"这一条会漏）。
     */
    public static void resetBodySize(LivingEntity entity) {
        AttributeInstance scale = entity.getAttribute(Attributes.SCALE);
        if (scale != null) {
            scale.setBaseValue(1.0F);
        }
        applyOriginalBodyAgility(entity, false);
    }

    /**
     * 这个实体是不是"龙右身体"。
     * <p>
     * 认的是<b>身体</b>而不是账号：玩家把意识转移进龙右克隆体后依然是尸王，
     * 走动的龙右分身实体也算。
     */
    public static boolean isLongYouBody(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            return ID.equals(CharacterManager.getInstance().getPlayerCharacterId(player));
        }
        return entity instanceof CloneAvatarEntity avatar && avatar.isCorpseKingBody();
    }

    /** 是龙右身体就套上基础数值，否则摘掉 —— 重复调用安全 */
    public static void applyIfLongYou(LivingEntity entity) {
        if (isLongYouBody(entity)) {
            applyBaseAttributes(entity);
        } else {
            removeBaseAttributes(entity);
        }
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

    /**
     * 这位玩家是不是「尸水之源」本人 —— 完全免疫尸水（角色特质：{@code trait3}）。
     * <p>
     * 尸水的中毒/感染判定散在三处（{@code InfectedWaterFluid}、{@code InfectedWaterBlock}、
     * {@code ByWaterBottleItem}），统一走这里，免得以后漏改一处。
     * <p>
     * 注意这是<b>角色特质</b>，不需要学会技能：是龙右就生效。
     */
    public static boolean isImmuneToInfectedWater(Player player) {
        return player != null
                && ID.equals(CharacterManager.getInstance().getPlayerCharacterId(player));
    }

    @Override
    public void onAcquire(Player player) {
        // ✅ 成为龙右 → 直接变为尸王（满级 + 有意识）
        // ★ 变种指定为"不长尸眼骨骼"：他是尸王、有自己的一整套外观（专属服装），
        //   不该再往脸上糊一层尸兄的多眼骨骼
        PlayerCorpseComponent.setPlayerAsCorpse(player, PlayerCorpseComponent.TYPE_KING,
                PlayerCorpseComponent.VARIANT_NO_EXOSKELETON);
        PlayerCorpseComponent comp = PlayerCorpseComponent.get(player);
        comp.setEvolutionLevel(PlayerCorpseComponent.MAX_EVOLUTION_LEVEL);
        comp.restoreConsciousness();

        // ✅ 尸王的基础数值：血量 200 / 护甲 10 / 徒手 6 / 移速 0.12 / 击退抗性 50%
        applyBaseAttributes(player);
        // ✅ 自动换上尸王专属服装（原先穿的盔甲掉在脚下，不销毁）
        equipCorpseKingCloth(player);
        // 换上这具身体就是满状态（否则 20 血量的本体切过来只有 20/200）
        player.setHealth(player.getMaxHealth());

        // ⚠️ setEvolutionLevel/restoreConsciousness 不会触发同步，需显式补一次。
        //    必须用广播：尸兄外观是别的玩家看你时才渲染的，只发给自己别人看不到。
        if (player instanceof ServerPlayer serverPlayer) {
            CorpseNetwork.broadcastPlayerCorpseSync(serverPlayer);
        }
    }

    /**
     * 尸王原体的大小倍率 —— 原著里那颗真身只有<b>拇指大小</b>。
     * <p>
     * 原版 {@code SCALE} 会被 {@code LivingEntity#sanitizeScale} 夹在 [1/16, 16]，
     * 取 0.15 刚好还在夹取范围内、又是"小人儿"的观感；想更小可以直接往下调，
     * 但视角会越来越贴地（进原体靠技能「金蝉脱壳」，出来靠「血肉重塑」）。
     */
    public static final float ORIGINAL_BODY_SCALE = 0.15F;

    /**
     * 变尸王时自动换上专属服装（头盔 / 胸甲 / 护腿，模型里没有靴子几何）。
     * <p>
     * 原先穿的盔甲<b>掉在脚下</b>而不是直接清掉 —— 自动穿戴不该顺手毁掉玩家的装备。
     */
    private static void equipCorpseKingCloth(Player player) {
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS};
        ItemStack[] cloth = {
                new ItemStack(ModItems.LONGYOU_CLOTH_HELMET.get()),
                new ItemStack(ModItems.LONGYOU_CLOTH_CHESTPLATE.get()),
                new ItemStack(ModItems.LONGYOU_CLOTH_LEGGINGS.get())
        };

        for (int i = 0; i < slots.length; i++) {
            ItemStack old = player.getItemBySlot(slots[i]);
            if (!old.isEmpty()) {
                // 退回背包而不是直接丢掉：自动穿戴不该顺手毁掉玩家的装备
                player.getInventory().placeItemBackInInventory(old.copy());
            }
            player.setItemSlot(slots[i], cloth[i]);
        }
    }

    @Override
    public void onLose(Player player) {
        // 失去尸王身份 → 基础数值加成 / 原体体术一并摘掉（血量超出新上限时原版会自动收回来）
        removeBaseAttributes(player);
        applyOriginalBodyAgility(player, false);
        // 失去龙右身份 → 清除尸兄状态（removeCorpseState 内部已同步）
        PlayerCorpseComponent.removeCorpseState(player);
    }
}
