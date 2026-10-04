package xiaoshi2022.corpseorigin.event;

import java.util.Set;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.config.CorpseConfig;
import xiaoshi2022.corpseorigin.registry.ModEntities;
import xiaoshi2022.corpseorigin.skill.EvolutionManager;
import xiaoshi2022.corpseorigin.skill.EvolutionTier;

/**
 * 世界威胁等级 —— 玩家越强，尸兄越强。
 * <p>
 * 世界威胁等级 = 服务器全体在线玩家的<b>最高境界等级</b>（人1=1 … 神上=20）。
 * 尸兄系实体（白名单，不含固定设定 BOSS/NPC）在<b>生成与读档加载</b>时按威胁等级
 * 获得属性加成（永久修饰符、幂等覆盖，绝不叠加）：
 * <ul>
 *   <li>最大生命 / 攻击伤害：每 1 级威胁 +{@code hpPerLevel}/{@code damagePerLevel}（乘基础值）</li>
 *   <li>护甲：每 1 级威胁 +{@code armorPerLevel} 点</li>
 * </ul>
 * 威胁等级变化（新玩家带着更高境界进服 / 玩家突破）时向全服广播。
 * 总闸与数值见 config/corpseorigin.json → spawn.worldThreat（GUI"世界威胁等级"页）。
 */
public final class WorldThreatManager {

    private WorldThreatManager() {}

    private static final Identifier HP_ID = CorpseOrigin.id("world_threat_hp");
    private static final Identifier DAMAGE_ID = CorpseOrigin.id("world_threat_damage");
    private static final Identifier ARMOR_ID = CorpseOrigin.id("world_threat_armor");

    /** 受威胁等级缩放的尸兄系实体白名单 —— 固定设定的 BOSS（尔多兽王/穆博士）与 NPC（天博士/达摩）不进 */
    private static final Set<EntityType<?>> SCALABLE = Set.of(
            ModEntities.LOWER_LEVEL_ZB, ModEntities.AOTUMAN_ZB, ModEntities.ZB_WORM,
            ModEntities.CORPSE_MAGGOT, ModEntities.RED_FIRE_ANT, ModEntities.BULLET_ANT,
            ModEntities.MULTI_HEAD_CORPSE_WORM, ModEntities.COCO_ZOMBIE, ModEntities.COCO_ZOMBIE_X,
            ModEntities.FROG_ZBR_MC, ModEntities.GECKO_ZBR, ModEntities.MOSQUITO_ZBR,
            ModEntities.RAVEN_ZBR,
            ModEntities.VAMPIRE_BAT, ModEntities.CORPSE_FISH_EGG);

    /** 上次广播过的威胁等级；-1 = 本 tick 周期还没算过（服务器刚启动，静默设定不广播） */
    private static int lastThreat = -1;

    public static void initialize() {
        // 实体加入服务器世界（自然生成 / 事件召唤 / 刷怪蛋 / 指令 / 读档）时按当前威胁加成。
        // 挂载点在 mixin WorldThreatMixin（ServerLevel.addFreshEntity）—— 26.2 的 fabric-api
        // 已移除 ServerEntityEvents.ENTITY_LOAD，mixin 是覆盖所有路径的唯一入口。
        // 每 10 秒重算世界威胁，变化时广播
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 200 != 0) return;
            int threat = computeThreatLevel(server);
            if (lastThreat >= 0 && threat != lastThreat) broadcast(server, threat, threat > lastThreat);
            lastThreat = threat;
        });
    }

    /** 世界威胁等级 = 在线玩家最高境界（无玩家时为 1），受 maxLevelsCounted 钳制 */
    public static int computeThreatLevel(MinecraftServer server) {
        var cfg = CorpseConfig.get().spawn.worldThreat;
        if (!cfg.enabled) return 1;
        int max = 1;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            int lvl = EvolutionManager.getLevel(PlayerCharacterData.get(p).getEarnedPoints(p.getUUID()));
            if (lvl > max) max = lvl;
        }
        return Math.min(max, Math.max(1, cfg.maxLevelsCounted));
    }

    /** 给尸兄挂威胁加成：永久修饰符 + 固定 id，addOrReplace 幂等（读档重复触发不叠加）。仅白名单实体生效 */
    public static void apply(LivingEntity entity, ServerLevel level) {
        if (!SCALABLE.contains(entity.getType())) return;
        var cfg = CorpseConfig.get().spawn.worldThreat;
        if (!cfg.enabled) return;
        int threat = computeThreatLevel(level.getServer());
        if (threat <= 1) return;
        int steps = threat - 1;

        float beforeMax = entity.getMaxHealth();
        multiplied(entity, Attributes.MAX_HEALTH, HP_ID, cfg.hpPerLevel * steps);
        multiplied(entity, Attributes.ATTACK_DAMAGE, DAMAGE_ID, cfg.damagePerLevel * steps);
        added(entity, Attributes.ARMOR, ARMOR_ID, cfg.armorPerLevel * steps);

        // 未受伤的怪（生成即满血）把血补到新上限；带伤读档的怪只提高上限不回血
        if (Math.abs(entity.getHealth() - beforeMax) < 0.5F) entity.setHealth(entity.getMaxHealth());
    }

    private static void multiplied(LivingEntity e, Holder<Attribute> attr, Identifier id, double amount) {
        if (amount <= 0) return;
        AttributeInstance inst = e.getAttribute(attr);
        if (inst != null) inst.addOrReplacePermanentModifier(
                new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    private static void added(LivingEntity e, Holder<Attribute> attr, Identifier id, double amount) {
        if (amount <= 0) return;
        AttributeInstance inst = e.getAttribute(attr);
        if (inst != null) inst.addOrReplacePermanentModifier(
                new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
    }

    private static void broadcast(MinecraftServer server, int threat, boolean rising) {
        Component realm = EvolutionTier.formatFullName(threat)
                .copy().withColor(EvolutionTier.colorOf(threat));
        String key = rising ? "message.corpseorigin.threat_up" : "message.corpseorigin.threat_down";
        for (ServerPlayer p : server.getPlayerList().getPlayers())
            p.sendSystemMessage(Component.translatable(key, realm));
    }
}
