package xiaoshi2022.corpseorigin.character;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.kaiweinai.ChrysanthemumShieldSkill;
import xiaoshi2022.corpseorigin.skill.kaiweinai.DogEyeCannonSkill;

import java.util.List;

/**
 * 开胃奶 - 拥有黑骑士体质的白小飞表弟。
 * <p>
 * 选中即尸兄化，并走 {@link PlayerCorpseComponent#VARIANT_NIUNAIX} 变种：不长通用尸眼骨骼，
 * 改在背后挂 {@code niunaix} 那套（触角 / 捆仙索 / 菊花盾），原版模型与盔甲照常渲染。
 */
public class KaiWeiNai implements ICharacter {

    public static final String ID = "kaiweinai";

    // ==================== 「拦腰斩断」（黑骑士体质）时轴 ====================
    // 三条时长都定在这里：服务端用它算广播窗口，客户端用它算"现在该播哪一段"。
    // 前两条动画各 2 秒（见 geckolib/animations/entity/niunai_link_player.animation.json）。

    /** 断开动画 {@code broken_off} 的时长（tick）：2 秒 */
    public static final int NIUNAI_LINK_BREAK_TICKS = 40;
    /** 断开后保持截断姿态的时长（tick）：1 分钟，之后才开始接回 */
    public static final int NIUNAI_LINK_HOLD_TICKS = 1200;
    /** 接回动画 {@code link} 的时长（tick）：2 秒 */
    public static final int NIUNAI_LINK_RESTORE_TICKS = 40;

    /** 整个「拦腰斩断」过程的时长（tick）—— 服务端就广播这个数 */
    public static final int NIUNAI_LINK_TOTAL_TICKS =
            NIUNAI_LINK_BREAK_TICKS + NIUNAI_LINK_HOLD_TICKS + NIUNAI_LINK_RESTORE_TICKS;

    private static final List<ISkill> SKILLS = List.of(
            new ChrysanthemumShieldSkill(),
            new DogEyeCannonSkill()
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
        return 0.4f;
    }

    @Override
    public void onAcquire(Player player) {
        // 选中即尸兄化，走「背挂」变种：算尸兄、不长通用尸眼骨骼，外观交给 niunaix 背挂
        PlayerCorpseComponent.setPlayerAsCorpse(player, PlayerCorpseComponent.TYPE_NORMAL,
                PlayerCorpseComponent.VARIANT_NIUNAIX);

        // 玩家角色要保留意识，否则一换角色就变成只会本能行动的怪物
        PlayerCorpseComponent comp = PlayerCorpseComponent.get(player);

        // restoreConsciousness 不会触发同步，补一次；
        // 必须广播：尸兄外观是别的玩家看你时才渲染的。
        if (player instanceof ServerPlayer serverPlayer) {
            CorpseNetwork.broadcastPlayerCorpseSync(serverPlayer);
        }
    }

    @Override
    public void onLose(Player player) {
        PlayerCorpseComponent.removeCorpseState(player);
    }
}
