package xiaoshi2022.corpseorigin.character;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.heixiaofei.*;

import java.util.List;

/**
 * 黑小飞 - 白小飞的克隆体，《尸巢之战篇》本篇主角。
 * <p>
 * 8 项核心技能：杀势、黑暗虹吸、黑金心脏、断臂攻击、虎爪/飞蜂轮、
 * 恶犬出笼、圆舞、杀戮化形·身外化身。
 */
public class HeiXiaoFei implements ICharacter {

    public static final String ID = "heixiaofei";

    private static final List<ISkill> SKILLS = List.of(
            new SlaughterMomentumSkill(),
            new DarkSiphonSkill(),
            new BlackGoldHeartSkill(),
            new SeveredArmStrikeSkill(),
            new TigerClawBeeWheelSkill(),
            new HoundUnleashedSkill(),
            new RoundDanceSkill(),
            new KillingIncarnationSkill()
    );

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Component getName() {
        return Component.translatable("character.corpseorigin.heixiaofei");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("character.corpseorigin.heixiaofei.desc");
    }

    @Override
    public Identifier getIcon() {
        return ICharacter.iconId("heixiaofei");
    }

    @Override
    public boolean isPassive() {
        return false;
    }

    @Override
    public List<Component> getTraits() {
        return List.of(
                Component.translatable("character.corpseorigin.heixiaofei.trait1"),
                Component.translatable("character.corpseorigin.heixiaofei.trait2"),
                Component.translatable("character.corpseorigin.heixiaofei.trait3")
        );
    }

    @Override
    public List<ISkill> getSkills() {
        return SKILLS;
    }

    @Override
    public float getInfectionMultiplier() {
        return 0.4f;
    }

    /**
     * 失去黑小飞身份 → 断肢再生能力一并失效，清掉断肢状态并重新广播，
     * 否则客户端会一直用断肢模型渲染这具身体。
     */
    @Override
    public void onLose(Player player) {
        if (PlayerCorpseComponent.get(player).clearLimbs()
                && player instanceof ServerPlayer serverPlayer) {
            CorpseNetwork.broadcastPlayerCorpseSync(serverPlayer);
        }
    }
}
