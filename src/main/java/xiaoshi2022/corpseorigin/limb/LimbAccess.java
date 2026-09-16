package xiaoshi2022.corpseorigin.limb;

import net.minecraft.world.entity.player.Player;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.HeiXiaoFei;
import xiaoshi2022.corpseorigin.character.LongYou;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 断肢再生的身份门槛。
 * <p>
 * 需要两个条件同时成立：
 * <ul>
 *   <li><b>角色</b>在 {@link #CAPABLE_CHARACTERS} 白名单里</li>
 *   <li><b>阵营</b>是尸兄（PLAYER_CORPSE.is_corpse，且未伪装）</li>
 * </ul>
 * 二者是两套正交系统：角色决定技能树与进化点，尸兄身份决定阵营 / 外观 / 饥饿。
 * 黑小飞的 {@code onAcquire} 不会写入尸兄状态（只有龙右会），
 * 所以"黑小飞 + 尸兄"必须靠尸水感染等他途获得尸兄身份。
 * <p>
 * 新角色想接入，在 {@link #registerDefaults()} 里加一行 {@link #allowCharacter} 即可。
 * <p>
 * ⚠️ 角色 ID 只在服务端可信；客户端只缓存自己的角色，因此渲染侧不能自行判断这个条件，
 * 必须依赖服务端下发的 limb_mask。
 */
public final class LimbAccess {

    private static final Set<String> CAPABLE_CHARACTERS = new LinkedHashSet<>();

    private static boolean defaultsRegistered;

    private LimbAccess() {
    }

    /** 把某个角色加进"能断肢再生"的白名单 */
    public static void allowCharacter(String characterId) {
        CAPABLE_CHARACTERS.add(characterId);
    }

    public static Set<String> capableCharacters() {
        return Collections.unmodifiableSet(CAPABLE_CHARACTERS);
    }

    public static void registerDefaults() {
        if (defaultsRegistered) {
            return;
        }
        defaultsRegistered = true;

        allowCharacter(HeiXiaoFei.ID);
        // 尸王：不死髅体，手脚炸碎也能再生（走尸王 3 秒档）
        allowCharacter(LongYou.ID);
        // 只想要黑小飞的话，删掉上面 LongYou 那行
    }

    /** 是否为黑小飞角色（服务端可信，客户端只对自己有效） */
    public static boolean isHeiXiaoFei(Player player) {
        return HeiXiaoFei.ID.equals(
                CharacterManager.getInstance().getPlayerCharacterId(player));
    }

    /** 角色是否在白名单里 */
    public static boolean isCapableCharacter(Player player) {
        return CAPABLE_CHARACTERS.contains(
                CharacterManager.getInstance().getPlayerCharacterId(player));
    }

    /** 是否有资格断肢 / 再生 */
    public static boolean canDismember(Player player) {
        if (!PlayerCorpseComponent.isCorpse(player)) {
            return false;   // 不是尸兄阵营
        }
        if (PlayerCorpseComponent.get(player).isDisguised()) {
            return false;   // 伪装成人形，不露残肢
        }
        return isCapableCharacter(player);
    }
}
