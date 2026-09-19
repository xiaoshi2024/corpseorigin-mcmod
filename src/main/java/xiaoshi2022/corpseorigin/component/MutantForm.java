package xiaoshi2022.corpseorigin.component;

import net.minecraft.world.entity.player.Player;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.KaiWeiNai;
import xiaoshi2022.corpseorigin.character.ZuoHuFa;
import xiaoshi2022.corpseorigin.config.CorpseConfig;

/**
 * 「左护法变异体形态」的双端判定。
 * <p>
 * 形态本身是服务端状态（{@code PLAYER_CORPSE} 附件里的变种号），所以：
 * <ul>
 *   <li><b>服务端</b>直接读附件 —— 碰撞箱、拾取这些逻辑只在这里做判定；</li>
 *   <li><b>客户端</b>读网络包缓存（{@link xiaoshi2022.corpseorigin.client.CorpseOriginClient#corpseDataCache}）。</li>
 * </ul>
 * 客户端引用只写在 {@code isClientSide} 分支里，服务端不会加载那个类。
 * <p>
 * 判定条件与渲染那边完全一致（尸兄 + 非伪装 + 变种 3 + 配置开关），
 * 免得出现"客户端画着变异体、服务端却不认"的错位。
 */
public final class MutantForm {

    private MutantForm() {
    }

    /** 这位玩家现在是不是左护法变异体形态 */
    public static boolean isMutant(Player player) {
        if (player == null) {
            return false;
        }
        if (player.level().isClientSide()) {
            return xiaoshi2022.corpseorigin.client.renderer.player.MutantBodyRenderData
                    .isMutantBody(player.getUUID());
        }
        if (!CorpseConfig.get().mutantBody.enabled) {
            return false;
        }
        return PlayerCorpseComponent.isMutantVariant(player);
    }

    /**
     * 是不是"连多段碰撞箱一起算"的变异体形态。
     * <p>
     * 只有在开了多段碰撞箱时，玩家自己的原版箱子才会被设成不可被选中 ——
     * 关掉碰撞箱就等于回到老行为（本体照样能被打到），不会出现"打不到人"的死角。
     */
    public static boolean isMutantWithHitboxes(Player player) {
        return CorpseConfig.get().mutantBody.hitboxes.enabled && isMutant(player);
    }

    /**
     * 形态自洽修正：<b>专属外观只属于对应角色</b>。
     * <p>
     * 尸兄的"外观变种"跟着<b>身体</b>走，而角色跟着<b>意识</b>走 —— 两者在换身这类路径上会打架：
     * 克隆仓培育出来的身体角色会被清成凡人，尸兄数据却是从本体整个复制的，
     * 于是出现「凡人却长着蛟龙身体」（换进这具身体的人明明不是左护法）。
     * <p>
     * 所以在"身体被应用到玩家身上"的汇合点（{@code ServerPlayerShellMixin#apply}）和登录时各修一次：
     * <ul>
     *   <li>变种是蛟龙、角色却不是左护法 → 降级回普通尸兄那套外观；</li>
     *   <li>变种是开胃奶背挂、角色却不是开胃奶 → 同上。</li>
     * </ul>
     * <p>
     * 只改数据、不发包：调用方紧跟着就会广播（换身 {@code apply} 末尾、登录补发），
     * 让它顺手带上修正后的数据即可。
     *
     * @return 是否改动了数据
     */
    public static boolean reconcile(Player player) {
        if (player == null || player.level().isClientSide()) {
            return false;   // 判定与修正都在服务端做，客户端只认同步下来的数据
        }
        String characterId = CharacterManager.getInstance().getPlayerCharacterId(player);

        if (PlayerCorpseComponent.isMutantVariant(player)
                && !ZuoHuFa.ID.equals(characterId)) {
            PlayerCorpseComponent.get(player).setVariant(0);
            return true;
        }
        if (PlayerCorpseComponent.isNiunaiVariant(player)
                && !KaiWeiNai.ID.equals(characterId)) {
            PlayerCorpseComponent.get(player).setVariant(0);
            return true;
        }
        return false;
    }
}
