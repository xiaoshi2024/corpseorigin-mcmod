package com.xiaoshi2022.corpseorigin.compat.jade;

import com.xiaoshi2022.corpseorigin.CorpseOrigin;
import com.xiaoshi2022.corpseorigin.growth.EvolutionTier;
import com.xiaoshi2022.corpseorigin.growth.NativeCorpseState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade（WAILA）联动 —— 准星指着被感染的生物时显示「境界」。
 * <p>
 * 进化等级由 {@code CorpseOriginNetwork} 同步到客户端（同步包高 8 位），
 * 未同步到等级时按人1处理。Jade 为可选依赖：未安装时本类不会被加载。
 */
@WailaPlugin(CorpseOrigin.MODID)
public class CorpseWailaPlugin implements IWailaPlugin {

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(RealmLineProvider.INSTANCE, Entity.class);
    }

    /** 仅客户端加载的实体信息行提供器。 */
    static final class RealmLineProvider implements IEntityComponentProvider {

        static final RealmLineProvider INSTANCE = new RealmLineProvider();
        private static final ResourceLocation UID =
                new ResourceLocation(CorpseOrigin.MODID, "realm_line");

        private RealmLineProvider() {
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            Entity target = accessor.getEntity();
            if (!(target instanceof Mob mob) || !NativeCorpseState.transformed(mob)) return;

            int syncedLevel = NativeCorpseState.clientEvolutionLevel(mob);
            // 刚感染、同步包还没到时至少显示人1
            final int level = syncedLevel <= 0 ? 1 : syncedLevel;

            Component realm = EvolutionTier.formatFullName(level).copy()
                    .withStyle(style -> style.withColor(TextColor.fromRgb(EvolutionTier.colorOf(level))));
            tooltip.add(Component.translatable("message.corpseorigin.jade_realm", realm));
        }
    }
}
