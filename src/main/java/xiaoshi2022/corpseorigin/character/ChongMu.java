package xiaoshi2022.corpseorigin.character;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.chongmu.BagCaptureSkill;
import xiaoshi2022.corpseorigin.skill.chongmu.SummonSwarmSkill;

import java.util.List;

/**
 * 虫母 - 人肉森林的掌控者，虫群灾害的源头。
 */
public class ChongMu implements ICharacter {

    public static final String ID = "chongmu";

    /**
     * 虫母形态的实体体型倍率。
     * <p>
     * 用原版 {@code Attributes.SCALE} 一个数把三件事一起撑起来：<b>碰撞箱、眼高（含第一人称相机）、
     * 以及渲染倍率</b>。生物形态的渲染路径（{@code LivingEntityRendererSubmitMixin} 里的
     * {@code corpseorigin$submitSalmon}）本来就会乘一次 {@code avatar.scale}，所以模型跟着一起变大，
     * 不需要另写抵消逻辑；而 {@code Attributes.SCALE} 又是"渲染与碰撞箱一起变"的
     * （同左护法合体形态的做法，见 {@link ZuoHuFa#MERGED_SCALE}）。
     * <p>
     * 参考值：{@code chongmu.geo.json} 本体约 2.7 格高（含摆动部件到 4.5 格），
     * 而 1.0 时碰撞箱还是玩家那套 0.6×1.8，看着"模型比箱子大一圈"。
     * 想再大/再小只改这一个数。
     */
    public static final float BODY_SCALE = 1.4F;

    private static final Identifier SCALE_MODIFIER = CorpseOrigin.id("chongmu_scale");

    private static final List<ISkill> SKILLS = List.of(
            new SummonSwarmSkill(),
            new BagCaptureSkill()
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

    // ==================== 体型 ====================

    /** 拿到虫母角色 → 套上体型（{@code onAcquire} 会在角色数据写好后调用） */
    @Override
    public void onAcquire(Player player) {
        applyIfChongMu(player);
    }

    /**
     * 失去虫母身份 → 摘掉体型。
     * <p>
     * ⚠️ 这里必须<b>无条件</b>摘：{@code CharacterManager#setPlayerCharacter} 是先调 {@code onLose}、
     * 之后才改写 characterId，所以此刻 {@link #applyIfChongMu} 查到的还是 chongmu，不会自己摘。
     */
    @Override
    public void onLose(Player player) {
        removeAttribute(player, Attributes.SCALE, SCALE_MODIFIER);
        player.refreshDimensions();
    }

    /**
     * 是虫母就按 {@link #BODY_SCALE} 套上体型，不是就摘掉 —— 重复调用安全
     * （同一 id 的修饰符原地替换，不会叠加，也不会和别的形态的体型打架）。
     * <p>
     * 用<b>修饰符</b>而不是直接改基础值：换身体时 {@code ServerPlayerShellMixin} 会用身体快照
     * 覆盖 {@code SCALE} 的基础值，修饰符不受影响、能稳定叠加在快照之上。
     */
    public static void applyIfChongMu(LivingEntity entity) {
        if (entity instanceof Player player
                && ID.equals(CharacterManager.getInstance().getPlayerCharacterId(player))) {
            setAttribute(entity, Attributes.SCALE, SCALE_MODIFIER, BODY_SCALE);
        } else {
            removeAttribute(entity, Attributes.SCALE, SCALE_MODIFIER);
        }
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
