package xiaoshi2022.corpseorigin.character;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.Holder;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.jingang_zb.PeelShellSkill;
import xiaoshi2022.corpseorigin.skill.shichaozhizi.BloodLotusArmorSkill;
import xiaoshi2022.corpseorigin.skill.shichaozhizi.GroundBurrowSkill;
import xiaoshi2022.corpseorigin.skill.shichaozhizi.SonOfCorpseNestSkill;
import xiaoshi2022.corpseorigin.skill.shichaozhizi.ThousandEyesSkill;
import xiaoshi2022.corpseorigin.skill.longyou.FleshAbandonSkill;
import xiaoshi2022.corpseorigin.skill.longyou.XuanwuBodySkill;

import java.util.List;

/**
 * 尸巢之子 - 少教主，《尸巢之战篇》的最终隐藏 BOSS。
 */
public class ShiChaoZhiZi implements ICharacter {

    public static final String ID = "shichaozhizi";
    /**
     * 二阶段<b>模型本体</b>的渲染倍率 —— 体型、视高、碰撞箱的唯一开关。
     * <p>
     * geo 里模型自带约 646 像素（≈ 40 格）高，必须整体缩一次才像个人形：
     * 0.32 ≈ 13 格、0.1 ≈ 4 格（见 {@code LivingEntityRendererSubmitMixin}）。
     * 改这一个数，下面 {@link #SECOND_FORM_EYE_HEIGHT} 与 {@link #SECOND_FORM_HITBOX_HEIGHT}
     * 会自动跟着走，不用再对第二次数。
     */
    public static final float SECOND_FORM_MODEL_SCALE = 0.32F;
    /** 模型最高点（像素）—— 触须顶端 574.4；碰撞箱从脚底往上长，所以用它换算高度。 */
    private static final float SECOND_FORM_TOP_PIXELS = 574.4F;
    /**
     * 模型<b>躯干</b>外沿宽度（像素）—— {@code torso} + {@code chest} 合并后的 X 外沿 203.2
     * （Z 只有 127.68，取大者；麦块箱子 XZ 是正方形）。
     * <p>
     * 刻意只量躯干、不量整具模型：整具模型 X 外沿有 384.2（尾巴甩到 -202.6、手伸到 +181.6），
     * 那属于附肢 —— 原版 BOSS 的箱子同样只罩身体（劫掠兽不算角、监守者不算胳膊）。
     */
    private static final float SECOND_FORM_BODY_WIDTH_PIXELS = 203.2F;
    /** 模型里头部所在的模型像素高度（{@code Head} 骨骼挂点 495、方块 495~503，取中间 499）。 */
    private static final float SECOND_FORM_HEAD_PIXELS = 499.0F;
    /**
     * 二阶段的碰撞箱宽 × 高（格）—— 都跟模型视觉尺寸对齐：
     * {@code 0.32 × 203.2 / 16 ≈ 4.06} 格宽、{@code 0.32 × 574.4 / 16 ≈ 11.5} 格高。
     * <p>
     * 刻意<b>不用</b> {@code Attributes.SCALE} 撑体型：那个属性会把宽度、眼高、相机、模型一起放大
     * （0.6 宽的玩家会变成 10 格宽的塔，渲染还得再补一套抵消逻辑）。这里只把箱子的尺寸钉住 ——
     * 见 {@code PlayerDimensionsMixin}。
     */
    public static final float SECOND_FORM_HITBOX_WIDTH =
            SECOND_FORM_MODEL_SCALE * SECOND_FORM_BODY_WIDTH_PIXELS / 16.0F;
    public static final float SECOND_FORM_HITBOX_HEIGHT =
            SECOND_FORM_MODEL_SCALE * SECOND_FORM_TOP_PIXELS / 16.0F;
    /**
     * 二阶段的视高（格）—— 视角就架在模型头部，同样由倍率换算：
     * {@code 0.32 × 499 / 16 ≈ 9.98} 格（缩到 0.1 时自动变成约 3.12 格）。
     * 由 {@code PlayerDimensionsMixin} 写进实体尺寸里，相机、准星原点和视线判定都用它。
     */
    public static final float SECOND_FORM_EYE_HEIGHT =
            SECOND_FORM_MODEL_SCALE * SECOND_FORM_HEAD_PIXELS / 16.0F;
    /**
     * 二阶段的起跳速度（原版玩家是 0.42）。
     * <p>
     * 折算跳高 ≈ {@code 1.25 × (v / 0.42)²}：<b>0.7 ≈ 3.5 格</b>（约 2.8 倍玩家，平地上落地还不吃摔伤）。
     * 之前给的 1.9 能跳 25 格往上，落下来直接把自己摔爆 —— 别再往上加了。
     */
    public static final double SECOND_FORM_JUMP_STRENGTH = 0.7;
    public static final double SECOND_FORM_HEALTH = 500.0;
    public static final double SECOND_FORM_ARMOR = 20.0;
    public static final double SECOND_FORM_ATTACK_RANGE = SECOND_FORM_EYE_HEIGHT + 2.0;

    private static final Identifier HEALTH_MODIFIER = CorpseOrigin.id("shichao_second_form_health");
    private static final Identifier ARMOR_MODIFIER = CorpseOrigin.id("shichao_second_form_armor");
    private static final Identifier KNOCKBACK_MODIFIER = CorpseOrigin.id("shichao_second_form_knockback");
    private static final Identifier JUMP_MODIFIER = CorpseOrigin.id("shichao_second_form_jump");
    private static final Identifier REACH_MODIFIER = CorpseOrigin.id("shichao_second_form_reach");

    private static final List<ISkill> SKILLS = List.of(
            new SonOfCorpseNestSkill(),
            new xiaoshi2022.corpseorigin.skill.longyou.NestSenseSkill(),
            new BloodLotusArmorSkill(),
            new GroundBurrowSkill(),
            new ThousandEyesSkill()
            // 进入龙右不死髅体后解锁的躯体技能
//            new XuanwuBodySkill(),
//            new PeelShellSkill()
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
        if (!PlayerCorpseComponent.isCorpse(player)) {
            PlayerCorpseComponent.setPlayerAsCorpse(player, PlayerCorpseComponent.TYPE_ELITE,
                    PlayerCorpseComponent.VARIANT_NO_EXOSKELETON);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            CorpseNetwork.broadcastPlayerCorpseSync(serverPlayer);
        }
    }

    @Override
    public void onLose(Player player) {
        removeSecondFormAttributes(player);
        PlayerCorpseComponent.removeCorpseState(player);
    }

    public static void applySecondFormAttributes(LivingEntity entity) {
        setTarget(entity, Attributes.MAX_HEALTH, HEALTH_MODIFIER, SECOND_FORM_HEALTH);
        setTarget(entity, Attributes.ARMOR, ARMOR_MODIFIER, SECOND_FORM_ARMOR);
        setTarget(entity, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_MODIFIER, 1.0);
        setTarget(entity, Attributes.JUMP_STRENGTH, JUMP_MODIFIER, SECOND_FORM_JUMP_STRENGTH);
        setTarget(entity, Attributes.ENTITY_INTERACTION_RANGE, REACH_MODIFIER, SECOND_FORM_ATTACK_RANGE);
        entity.refreshDimensions();
    }

    public static void removeSecondFormAttributes(LivingEntity entity) {
        remove(entity, Attributes.MAX_HEALTH, HEALTH_MODIFIER);
        remove(entity, Attributes.ARMOR, ARMOR_MODIFIER);
        remove(entity, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_MODIFIER);
        remove(entity, Attributes.JUMP_STRENGTH, JUMP_MODIFIER);
        remove(entity, Attributes.ENTITY_INTERACTION_RANGE, REACH_MODIFIER);
        entity.refreshDimensions();
        if (entity.getHealth() > entity.getMaxHealth()) entity.setHealth(entity.getMaxHealth());
    }

    /**
     * 这位玩家现在是不是第二形态 —— 双端判定。
     * <p>
     * 形态本身是服务端状态（{@code PLAYER_CORPSE} 附件里的变种号），客户端读网络包缓存。
     * 判定条件与渲染那边（{@code ShiChaoBodyRenderData}）保持一致，免得出现
     * "客户端画着巨人、服务端却不给箱子"的错位。
     */
    public static boolean isSecondForm(Player player) {
        if (player == null) {
            return false;
        }
        if (player.level().isClientSide()) {
            // 客户端引用只写在这个分支里，服务端不会加载那个类
            return xiaoshi2022.corpseorigin.client.renderer.player.ShiChaoBodyRenderData
                    .isActive(player.getUUID());
        }
        return PlayerCorpseComponent.isShiChaoVariant(player);
    }

    public static void reconcileSecondForm(LivingEntity entity) {
        if (entity instanceof Player player
                && PlayerCorpseComponent.get(player).getVariant()
                == PlayerCorpseComponent.VARIANT_SHICHAOZHIZI) {
            applySecondFormAttributes(entity);
        } else {
            removeSecondFormAttributes(entity);
        }
    }

    private static void setTarget(LivingEntity entity, Holder<Attribute> attribute,
                                  Identifier id, double target) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) return;
        instance.addOrReplacePermanentModifier(new AttributeModifier(
                id, target - instance.getBaseValue(), AttributeModifier.Operation.ADD_VALUE));
    }

    private static void remove(LivingEntity entity, Holder<Attribute> attribute, Identifier id) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) instance.removeModifier(id);
    }
}
