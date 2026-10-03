package xiaoshi2022.corpseorigin.compat.jade;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.ClientState;
import xiaoshi2022.corpseorigin.entity.RealmRated;
import xiaoshi2022.corpseorigin.skill.EvolutionTier;

/**
 * Jade（WAILA）联动 —— 准星指着谁就显示谁的境界：
 * <ul>
 *   <li>玩家：本地玩家显示自己的境界（客户端缓存 {@link ClientState#evolutionLevel}）；
 *       其他玩家显示服务器广播的境界（{@code PlayerRealmSyncS2C} → {@link ClientState#otherPlayerRealms}）</li>
 *   <li>实现了 {@link RealmRated} 的实体（天博士/达摩/青蛙/壁虎/蚊群核心等）：
 *       按设定境界显示；普通尸兄查 {@code ZbRatings} 基础评级表，颜色沿用境界阶层配色</li>
 * </ul>
 * Jade 为可选依赖（modCompileOnly）：玩家未装 Jade 时本类不会被加载；
 * 专用服务器上 {@code registerClient} 不触发，内部类也不加载，无客户端类泄露风险。
 */
public class CorpseWailaPlugin implements IWailaPlugin {

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(RealmLineProvider.INSTANCE, Entity.class);
    }

    /** 仅客户端加载（在 registerClient 首次触碰时初始化） */
    static final class RealmLineProvider implements IEntityComponentProvider {
        static final RealmLineProvider INSTANCE = new RealmLineProvider();

        private RealmLineProvider() {
        }

        @Override
        public Identifier getUid() {
            return CorpseOrigin.id("realm_line");
        }

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            Entity target = accessor.getEntity();
            int level = -1;
            if (target instanceof RealmRated rated) {
                level = rated.corpseRealmLevel();
            } else if (target instanceof net.minecraft.client.player.LocalPlayer) {
                level = ClientState.evolutionLevel;   // 自己：客户端本地缓存
            } else if (target instanceof net.minecraft.world.entity.player.Player player) {
                level = ClientState.otherPlayerRealms.getOrDefault(player.getUUID(), -1); // 别人：服务器广播缓存
            }
            if (level <= 0) {
                level = xiaoshi2022.corpseorigin.entity.ZbRatings.of(target.getType()); // 普通尸兄：基础评级表
            }
            if (level > 0) {
                tooltip.add(Component.translatable("message.corpseorigin.jade_realm",
                        EvolutionTier.formatFullName(level).copy().withColor(EvolutionTier.colorOf(level))));
            }
        }
    }
}
