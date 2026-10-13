package xiaoshi2022.corpseorigin.effect;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.CorpseBrother;
import xiaoshi2022.corpseorigin.character.MortalCharacter;
import xiaoshi2022.corpseorigin.character.NewChapterCharacter;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import xiaoshi2022.corpseorigin.entity.HybridZombie;

import xiaoshi2022.corpseorigin.entity.ZombieKin;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.registry.ModEffects;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BYeffect extends MobEffect {

    /** 感染源记录 */
    private static final Map<UUID, UUID> infectionSource = new HashMap<>();

    /** ✅ 记录每个目标的初始感染总时长（tick） */
    private static final Map<UUID, Integer> TOTAL_DURATION = new HashMap<>();

    public BYeffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public void onEffectAdded(LivingEntity livingEntity, int amplifier) {
        super.onEffectAdded(livingEntity, amplifier);
        if (!livingEntity.level().isClientSide()) {
            // ✅ 如果没有记录，用当前效果时长补一个（兜底）
            if (!TOTAL_DURATION.containsKey(livingEntity.getUUID())) {
                MobEffectInstance instance = livingEntity.getEffect(ModEffects.QIANS);
                if (instance != null) {
                    TOTAL_DURATION.put(livingEntity.getUUID(), instance.getDuration());
                }
            }
            CorpseOrigin.LOGGER.info("感染效果添加到 {}，将在效果结束时变异",
                    livingEntity.getName().getString());
        }
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // ✅ 每 10 tick 更新一次感染度；最后一 tick 必须触发转化
        return duration % 10 == 0 || duration <= 1;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity livingEntity, int amplifier) {
        MobEffectInstance instance = livingEntity.getEffect(ModEffects.QIANS);
        if (instance == null) {
            return true;
        }

        int remaining = instance.getDuration();

        // ===== 最后一 tick：转化 =====
        if (remaining <= 1) {
            performTransformation(livingEntity, level);

            // 清理 + 清零感染度
            TOTAL_DURATION.remove(livingEntity.getUUID());
            infectionSource.remove(livingEntity.getUUID());

            if (livingEntity instanceof ServerPlayer player) {
                PlayerCorpseComponent comp = PlayerCorpseComponent.get(player);
                comp.setInfection(0);
                CorpseNetwork.sendInfectionSync(player);
            }
            return true;
        }

        // ===== 中间：按比例更新感染度（只对玩家）=====
        if (livingEntity instanceof ServerPlayer player) {
            Integer total = TOTAL_DURATION.get(player.getUUID());
            if (total == null || total <= 0) {
                // 兜底：没有记录就跳过
                return true;
            }

            int infection = (int) ((total - remaining) * 100.0 / total);
            infection = Math.max(0, Math.min(100, infection));

            PlayerCorpseComponent comp = PlayerCorpseComponent.get(player);
            comp.setInfection(infection);
            CorpseNetwork.sendInfectionSync(player);
        }

        return true;
    }

    /**
     * 执行转化逻辑
     */
    private void performTransformation(LivingEntity livingEntity, ServerLevel serverLevel) {
        if (!livingEntity.isAlive()) return;
        if (livingEntity instanceof ServerPlayer player) {
            convertPlayerToCorpse(player);
        } else if (livingEntity instanceof Mob mob && canInfect(mob)) {
            xiaoshi2022.corpseorigin.growth.NativeCorpseState.transform(mob, serverLevel);
        }
    }

    private void convertPlayerToCorpse(ServerPlayer player) {
        PlayerCorpseComponent.setPlayerAsCorpse(player, 1);

        // ★ 凡人被感染成尸兄 → 自动转入「尸兄」角色。
        //   尸兄的进化效果（技能树、进化等级联动）都挂在这个角色上，
        //   所以不能只是"阵营变了、角色还是凡人"。
        //   鬼棍·人类同理：感染期满 → 转为「鬼棍·尸兄」（天级精英尸兄，保留意识）。
        //   ⚠️ 必须放在 setPlayerAsCorpse 之后：onAcquire 看到"已经是尸兄"的角色不会
        //   重写状态，感染给的尸兄类型 / 变种才保得住。
        CharacterManager characters = CharacterManager.getInstance();
        boolean guigunTransformed = false;
        if (characters.getPlayerCharacter(player) instanceof MortalCharacter) {
            characters.setPlayerCharacter(player, CorpseBrother.ID);
        } else if (NewChapterCharacter.GUIGUN_HUMAN_ID.equals(characters.getPlayerCharacterId(player))) {
            characters.setPlayerCharacter(player, NewChapterCharacter.GUIGUN_CORPSE_ID);
            guigunTransformed = true;
        }

        boolean hasConsciousness = PlayerCorpseComponent.get(player).hasInnateConsciousness();

        CorpseNetwork.broadcastPlayerCorpseSync(player);
        player.level().broadcastEntityEvent(player, (byte) 35);

        if (guigunTransformed) {
            player.sendOverlayMessage(Component.translatable("message.corpseorigin.b_yeffect.text_03"));
            CorpseOrigin.LOGGER.info("玩家 {} 感染期满，鬼棍化为尸兄！", player.getName().getString());
        } else if (hasConsciousness) {
            player.sendOverlayMessage(Component.translatable("message.corpseorigin.b_yeffect.text_01"));
            CorpseOrigin.LOGGER.info("玩家 {} 已转化为尸族！幸运地保留了意识！", player.getName().getString());
        } else {
            player.sendOverlayMessage(Component.translatable("message.corpseorigin.b_yeffect.text_02"));
            CorpseOrigin.LOGGER.info("玩家 {} 已转化为尸族！失去了人类意识...", player.getName().getString());
        }
    }

    // ==================== 静态方法 ====================

    public static void applyInfection(LivingEntity target, ServerLevel serverLevel) {
        applyInfection(target, serverLevel, null);
    }

    public static void applyInfection(LivingEntity target, ServerLevel serverLevel, UUID sourceUUID) {
        if (serverLevel == null) return;
        applyInfection(target, serverLevel, 60 + serverLevel.getRandom().nextInt(241), sourceUUID);
    }

    public static void applyInfection(LivingEntity target, ServerLevel serverLevel,
                                      int durationTicks, UUID sourceUUID) {
        if (serverLevel == null || !canInfect(target) || target.hasEffect(ModEffects.QIANS)
                || xiaoshi2022.corpseorigin.growth.NativeCorpseState.incubating(target)) return;
        int duration = Math.max(1, durationTicks);
        if (sourceUUID != null) infectionSource.put(target.getUUID(), sourceUUID);
        TOTAL_DURATION.put(target.getUUID(), duration);
        if (target instanceof Mob) {
            // Persist generation before incubation so unloading/restarting cannot lose it.
            String prefix = HybridZombie.INFECTION_GENERATION + "_pending:";
            for (String tag : java.util.List.copyOf(target.entityTags()))
                if (tag.startsWith(prefix)) target.removeTag(tag);
            Entity source = sourceUUID == null ? null : serverLevel.getEntity(sourceUUID);
            xiaoshi2022.corpseorigin.growth.NativeCorpseState.captureSource(target, source);
            int generation = source == null ? 1 : Math.max(1, HybridZombie.generation(source) + 1);
            target.addTag(prefix + generation);
        }
        boolean applied = target.addEffect(new MobEffectInstance(ModEffects.QIANS, duration, 0, false, true, true));
        if (!applied && target instanceof Mob) {
            // Ender dragon/wither reject status effects; retain their original immunities.
            target.setAttached(xiaoshi2022.corpseorigin.growth.NativeCorpseState.INCUBATION, duration);
        } else if (!applied) {
            infectionSource.remove(target.getUUID());
            TOTAL_DURATION.remove(target.getUUID());
        }
    }

    public static boolean canInfect(LivingEntity target) {
        if (target == null || !target.isAlive() || ZombieKin.isZombieKin(target)) return false;
        if (target instanceof ServerPlayer player)
            return !player.isCreative() && !player.isSpectator() && !PlayerCorpseComponent.isCorpse(player);
        return target instanceof Mob
                && "minecraft".equals(EntityType.getKey(target.getType()).getNamespace());
    }

    public static UUID getInfectionSource(UUID targetUUID) {
        return infectionSource.get(targetUUID);
    }

    public static void clearInfectionSource(UUID targetUUID) {
        infectionSource.remove(targetUUID);
    }

    /** ✅ 外部清理（玩家退出/effect 被强制移除时调用） */
    public static void clearTotalDuration(UUID targetUUID) {
        TOTAL_DURATION.remove(targetUUID);
    }
}
