package com.xiaoshi2022.corpseorigin.client;

import com.xiaoshi2022.corpseorigin.CorpseOrigin;
import com.xiaoshi2022.corpseorigin.growth.EvolutionTier;
import com.xiaoshi2022.corpseorigin.growth.NativeCorpseState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 尸兄名称前的境界标签（如「地2 僵尸」）。
 * <p>
 * 进化等级由 {@code CorpseOriginNetwork} 同步包的高 8 位带过来，
 * 只在原版本来就会显示名称牌的情况下生效（自定义名 / 准星指向 / Boss）。
 */
@Mod.EventBusSubscriber(modid = CorpseOrigin.MODID, value = Dist.CLIENT)
public final class ClientRealmNameTags {

    private ClientRealmNameTags() {
    }

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        int level = NativeCorpseState.clientEvolutionLevel(living);
        if (level <= 0) return;

        Component realm = EvolutionTier.formatFullName(level).copy()
                .withStyle(style -> style.withColor(TextColor.fromRgb(EvolutionTier.colorOf(level))));
        event.setContent(Component.empty()
                .append(realm)
                .append(Component.literal(" "))
                .append(event.getContent()));
    }
}
