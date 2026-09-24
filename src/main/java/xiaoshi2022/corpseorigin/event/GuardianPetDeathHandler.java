package xiaoshi2022.corpseorigin.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.entity.ZuoFloodLongEntity;

import java.util.UUID;

/**
 * 青龙宠物死亡追踪。
 * <p>
 * 蛟龙（{@link ZuoFloodLongEntity}）是永久宠物，只有两条消失途径：被主人用「合体」收回
 * （走 {@code discard()}，不触发死亡事件），或<b>被击杀</b>（触发本处理器）。
 * 被击杀时在主人的存档数据里打上 {@code guardianLost} 标记 —— 左护法的「唤龙」技能
 * 凭这个标记判定"有一条殒落的青龙可以复活"。
 * <p>
 * 主人是否在线无所谓：标记按 UUID 直接写进 {@link PlayerCharacterData}（随世界存档），
 * 主人下次上线照样能用。
 */
public final class GuardianPetDeathHandler {

    private GuardianPetDeathHandler() {
    }

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register(GuardianPetDeathHandler::onDeath);
    }

    private static void onDeath(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof ZuoFloodLongEntity dragon)) {
            return;
        }
        UUID ownerId = dragon.getOwnerUUID();
        if (ownerId == null || !(entity.level().getServer() instanceof net.minecraft.server.MinecraftServer server)) {
            return;
        }
        PlayerCharacterData.get(server).setGuardianLost(ownerId, true);

        // 主人在线就即时给个提示，让他知道可以用「唤龙」召回（消耗 100 点气血）
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
        if (owner != null) {
            owner.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
                    "skill.corpseorigin.revive_guardian.pet_lost"));
        }
    }
}
