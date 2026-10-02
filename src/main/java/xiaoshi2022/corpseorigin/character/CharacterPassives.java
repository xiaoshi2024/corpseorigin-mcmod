package xiaoshi2022.corpseorigin.character;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import xiaoshi2022.corpseorigin.CorpseOrigin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 角色被动光环 —— 特殊属性列表（原版效果 + 属性修饰符，每秒续期）：
 * <ul>
 *   <li>白小飞：村庄英雄（炎黄特能的群众基础）</li>
 *   <li>黑小飞：伤害抗性 I（尸化耐受高）+ 不祥之兆（尸化气场，进村招袭击）</li>
 *   <li>龙右：光照 &lt; 8 时力量 II（黑暗王者）+ 8 格尸潮威压（敌对缓速）</li>
 *   <li>开胃奶：击退抗性 + 爆炸击退抗性（菊花盾稳如泰山）</li>
 *   <li>木犀：护甲韧性 + 挖掘加速（土系重甲）</li>
 *   <li>风魔灰太郎：跳跃提升 II + 潜行隐身（忍术匿踪）</li>
 *   <li>K：夜视（黑暗议会）</li>
 *   <li>赵日天：生命 &lt; 50% 时力量 II（战意爆发）</li>
 *   <li>虫母：嗜甜 —— 食用蜂蜜瓶/甜浆果回内力（见 {@link #onMeal}）</li>
 * </ul>
 * 切换角色时清除全部被动残留（效果 + 修饰符），防止跨角色污染。
 */
public final class CharacterPassives {

    private CharacterPassives() {}

    private static final Identifier KB_ID = CorpseOrigin.id("passive_kaiweinai_knockback");
    private static final Identifier EXPLOSION_KB_ID = CorpseOrigin.id("passive_kaiweinai_explosion_kb");
    private static final Identifier TOUGHNESS_ID = CorpseOrigin.id("passive_muxi_toughness");
    private static final Identifier MINING_ID = CorpseOrigin.id("passive_muxi_mining");

    /** 本系统可能施加的全部效果（换角色时逐一清除；阈值短续期，断供即失效） */
    private static final List<Holder<MobEffect>> PASSIVE_EFFECTS = List.of(
            MobEffects.HERO_OF_THE_VILLAGE, MobEffects.RESISTANCE, MobEffects.BAD_OMEN,
            MobEffects.STRENGTH, MobEffects.NIGHT_VISION, MobEffects.JUMP_BOOST, MobEffects.INVISIBILITY);

    private static final Map<UUID, String> LAST_ROLE = new HashMap<>();

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0) return;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) tick(p);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((h, s) -> LAST_ROLE.remove(h.player.getUUID()));
    }

    private static void tick(ServerPlayer p) {
        String role = CharacterManager.getInstance().getPlayerCharacterId(p);
        String last = LAST_ROLE.put(p.getUUID(), role);
        if (!role.equals(last)) clear(p);
        if (p.isAlive() && !p.isSpectator()) apply(p, role);
    }

    private static void apply(ServerPlayer p, String role) {
        switch (role) {
            case "baixiaofei" -> buff(p, MobEffects.HERO_OF_THE_VILLAGE, 0, 60);
            case "heixiaofei" -> {
                buff(p, MobEffects.RESISTANCE, 0, 60);
                buff(p, MobEffects.BAD_OMEN, 0, 60);
            }
            case "longyou" -> {
                if (p.level().getMaxLocalRawBrightness(p.blockPosition()) < 8)
                    buff(p, MobEffects.STRENGTH, 1, 60);
                else p.removeEffect(MobEffects.STRENGTH);
                // 尸潮威压：限定 8 格半径 + 每秒一次，避免大范围空间扫描拖垮服务器
                for (Monster m : p.level().getEntitiesOfClass(Monster.class, p.getBoundingBox().inflate(8)))
                    m.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0, true, false, true));
            }
            case "kaiweinai" -> {
                attr(p, Attributes.KNOCKBACK_RESISTANCE, KB_ID, .5);
                attr(p, Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, EXPLOSION_KB_ID, .5);
            }
            case "muxi" -> {
                attr(p, Attributes.ARMOR_TOUGHNESS, TOUGHNESS_ID, 4);
                attr(p, Attributes.BLOCK_BREAK_SPEED, MINING_ID, 2);
            }
            case "fengmohuitailang" -> {
                buff(p, MobEffects.JUMP_BOOST, 1, 60);
                if (p.isCrouching()) buff(p, MobEffects.INVISIBILITY, 0, 60);
                else p.removeEffect(MobEffects.INVISIBILITY);
            }
            case "k" -> buff(p, MobEffects.NIGHT_VISION, 0, 400); // 400t：低于 200t 图标会闪烁
            case "zhaoritian" -> {
                if (p.getHealth() < p.getMaxHealth() * .5) buff(p, MobEffects.STRENGTH, 1, 60);
                else p.removeEffect(MobEffects.STRENGTH);
            }
            default -> { }
        }
    }

    /** 换角色时清残留：被动效果逐一摘除 + 属性修饰符移除（不同 id，不伤养成系统加成） */
    private static void clear(ServerPlayer p) {
        for (Holder<MobEffect> effect : PASSIVE_EFFECTS) p.removeEffect(effect);
        removeModifier(p, Attributes.KNOCKBACK_RESISTANCE, KB_ID);
        removeModifier(p, Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, EXPLOSION_KB_ID);
        removeModifier(p, Attributes.ARMOR_TOUGHNESS, TOUGHNESS_ID);
        removeModifier(p, Attributes.BLOCK_BREAK_SPEED, MINING_ID);
    }

    /** 虫母嗜甜：进食蜂蜜瓶 / 甜浆果回内力（由 RealmProgression.onMeal 转发，服务器侧） */
    public static void onMeal(ServerPlayer p, ItemStack stack) {
        if (!"chongmu".equals(CharacterManager.getInstance().getPlayerCharacterId(p))) return;
        int qi = stack.is(net.minecraft.world.item.Items.HONEY_BOTTLE) ? 200
                : stack.is(net.minecraft.world.item.Items.SWEET_BERRIES) ? 50 : 0;
        if (qi > 0 && InnerPowerManager.getMaxInnerPower(p) > 0) InnerPowerManager.regen(p, qi);
    }

    /** 短续期挂 buff：ambient 无粒子、图标可见；每秒重挂，断供自动消失 */
    private static void buff(ServerPlayer p, Holder<MobEffect> effect, int amplifier, int duration) {
        p.addEffect(new MobEffectInstance(effect, duration, amplifier, true, false, true));
    }

    private static void attr(ServerPlayer p, Holder<Attribute> attribute, Identifier id, double amount) {
        AttributeInstance instance = p.getAttribute(attribute);
        if (instance == null) return;
        AttributeModifier existing = instance.getModifier(id);
        if (existing != null
                && existing.operation() == AttributeModifier.Operation.ADD_VALUE
                && Math.abs(existing.amount() - amount) < 1.0E-12) return;
        instance.addOrReplacePermanentModifier(
                new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
    }

    private static void removeModifier(ServerPlayer p, Holder<Attribute> attribute, Identifier id) {
        AttributeInstance instance = p.getAttribute(attribute);
        if (instance != null) instance.removeModifier(id);
    }
}
