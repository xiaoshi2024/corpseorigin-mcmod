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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.CorpseBrother;
import xiaoshi2022.corpseorigin.character.MortalCharacter;
import xiaoshi2022.corpseorigin.character.NewChapterCharacter;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import xiaoshi2022.corpseorigin.entity.HybridZombie;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;
import xiaoshi2022.corpseorigin.entity.ZombieKin;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.registry.ModEffects;
import xiaoshi2022.corpseorigin.registry.ModEntities;

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
        if (livingEntity instanceof net.minecraft.world.entity.animal.fish.AbstractFish fish) {
            xiaoshi2022.corpseorigin.growth.CorpseInfection.transformFish(fish, serverLevel);
            return;
        }
        if (livingEntity instanceof Villager villager) {
            convertVillagerToZb(villager, serverLevel);
        } else if (livingEntity instanceof ServerPlayer player) {
            convertPlayerToCorpse(player);
        } else if (livingEntity instanceof Monster monster) {
            // 原版怪物被感染领域或龙右近战感染后 → 转换为半尸兄（混血尸兄）
            convertMonsterToHybridZb(monster, serverLevel);
        }
    }

    private void convertVillagerToZb(Villager villager, ServerLevel serverLevel) {
        try {
            LowerLevelZbEntity zb = new LowerLevelZbEntity(ModEntities.LOWER_LEVEL_ZB, serverLevel);
            zb.setPos(villager.getX(), villager.getY(), villager.getZ());
            zb.setYRot(villager.getYRot());
            zb.setXRot(villager.getXRot());

            String villagerName = villager.getName().getString();
            zb.setPlayerSkinName(villagerName);
            zb.setCustomName(villager.getCustomName());
            zb.setCustomNameVisible(villager.isCustomNameVisible());

            UUID sourceUUID = infectionSource.get(villager.getUUID());
            if (sourceUUID != null) {
                CorpseOrigin.LOGGER.info("村民 {} 转化为尸兄，感染源: {}", villagerName, sourceUUID);
            }

            villager.remove(Entity.RemovalReason.CHANGED_DIMENSION);
            serverLevel.addFreshEntity(zb);

            CorpseOrigin.LOGGER.info("村民 {} 成功变异为尸兄", villagerName);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("村民转化失败: {}", e.getMessage());
        }
    }

    /**
     * 把原版怪物（{@link Monster} 子类：僵尸/末影人/苦力怕/骷髅/蜘蛛等）感染为"半尸兄"。
     * <p>
     * 与 {@link #convertVillagerToZb} 同模式新建 {@link LowerLevelZbEntity}，复制位置/朝向/自定义名，
     * 但额外通过 {@link HybridZombie#tagAsHybridZb} 写入三个标签：
     * <ul>
     *   <li>{@link HybridZombie#ORIGINAL_SPECIES}：原宿主的实体类型 id（如 minecraft:enderman）</li>
     *   <li>{@link HybridZombie#HYBRID_GRADE}：{@link HybridZombie#GRADE_HALF}（混血尸兄）</li>
     *   <li>{@link HybridZombie#INFECTION_GENERATION}：1（被龙右本体感染）</li>
     * </ul>
     * 感染代数 ≥2 的实体（同族传播感染得到）在调用方传入时已累加，本方法直接写入。
     * <p>
     * 半尸兄仍属尸族（{@link ZombieKin#isZombieKin} 对 LowerLevelZbEntity 仍返回 true），
     * 但能继续参与"同族传播"——攻击原版同类时按 spreadChance 概率感染对方。
     * <p>
     * <b>皮肤叠加</b>（后续 task，不在本期范围）：客户端 renderer 通过 CombinedSkinBuilder
     * 在 LowerLevelZbEntity 皮肤上叠加原怪物皮肤，体现"尸兄小黑/尸兄苦力怕"的视觉效果。
     * 当前转换后先用 LowerLevelZbEntity 默认皮肤，标签系统先打通。
     */
    private void convertMonsterToHybridZb(Monster monster, ServerLevel serverLevel) {
        try {
            LowerLevelZbEntity zb = new LowerLevelZbEntity(ModEntities.LOWER_LEVEL_ZB, serverLevel);
            zb.setPos(monster.getX(), monster.getY(), monster.getZ());
            zb.setYRot(monster.getYRot());
            zb.setXRot(monster.getXRot());

            // 保留原怪物的自定义名（命名牌命名的怪物仍能识别）
            if (monster.getCustomName() != null) {
                zb.setCustomName(monster.getCustomName());
                zb.setCustomNameVisible(monster.isCustomNameVisible());
            }

            // 半尸兄标签：original_species / hybrid_grade / infection_generation
            String speciesKey = EntityType.getKey(monster.getType()).toString();
            // 感染代数：默认 1（龙右本体或感染领域直接感染）；
            // 若怪物身上有同族传播时预标记的代数，取出并累加
            int generation = 1;
            String pendingPrefix = HybridZombie.INFECTION_GENERATION + "_pending:";
            for (String tag : monster.entityTags()) {
                if (tag.startsWith(pendingPrefix)) {
                    try {
                        generation = Integer.parseInt(tag.substring(pendingPrefix.length()));
                    } catch (NumberFormatException ignored) {
                        // 标记损坏，保持默认 1
                    }
                    break;
                }
            }
            HybridZombie.tagAsHybrid(zb, speciesKey, HybridZombie.GRADE_HALF, generation);

            // 半尸兄视觉标识：显示尸眼骨骼（shieye/group2~7）
            // <p>
            // 不需要做"原怪物皮肤叠加"——半尸兄作为 LowerLevelZbEntity，
            // 天然继承 LowerLevelZbRenderer 的渲染链路（含 ZbOrganLayer 器官骨骼层），
            // 只需让它显示"尸眼"作为感染标识，玩家一看就知道这是被感染的原版怪物。
            // 原宿主类型仍通过 HybridZombie 的 NBT 标签保留，便于同族传播与 /data get 查看。
            zb.setCorpseEye(true);

            UUID sourceUUID = infectionSource.get(monster.getUUID());
            if (sourceUUID != null) {
                CorpseOrigin.LOGGER.info("原版怪物 {} 感染转化为半尸兄（原宿主={}），感染源: {}",
                        monster.getName().getString(), speciesKey, sourceUUID);
            } else {
                CorpseOrigin.LOGGER.info("原版怪物 {} 感染转化为半尸兄（原宿主={}）",
                        monster.getName().getString(), speciesKey);
            }

            monster.remove(Entity.RemovalReason.CHANGED_DIMENSION);
            serverLevel.addFreshEntity(zb);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("原版怪物 {} 感染转化为半尸兄失败: {}",
                    monster.getName().getString(), e.getMessage());
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
        if (target == null || serverLevel == null) return;
        if (target.hasEffect(ModEffects.QIANS)) return;

        int duration = 60 + serverLevel.getRandom().nextInt(241);

        if (sourceUUID != null) {
            infectionSource.put(target.getUUID(), sourceUUID);
        }

        // ✅ 记录总时长（必须在 addEffect 之前或之后都行，只要在 effect tick 之前）
        TOTAL_DURATION.put(target.getUUID(), duration);

        target.addEffect(new MobEffectInstance(
                ModEffects.QIANS,
                duration,
                0,
                false,
                true,
                true
        ));

        CorpseOrigin.LOGGER.info("感染效果已应用到 {}，将在 {} 秒后变异",
                target.getName().getString(), duration / 20);
    }

    public static void applyInfection(LivingEntity target, ServerLevel serverLevel,
                                      int durationTicks, UUID sourceUUID) {
        if (target == null || serverLevel == null) return;
        if (target.hasEffect(ModEffects.QIANS)) return;

        if (sourceUUID != null) {
            infectionSource.put(target.getUUID(), sourceUUID);
        }

        // ✅ 记录总时长
        TOTAL_DURATION.put(target.getUUID(), durationTicks);

        target.addEffect(new MobEffectInstance(
                ModEffects.QIANS,
                durationTicks,
                0,
                false,
                true,
                true
        ));
    }

    public static boolean canInfect(LivingEntity target) {
        if (target instanceof net.minecraft.world.entity.animal.fish.AbstractFish)
            return !xiaoshi2022.corpseorigin.entity.ZombieKin.isZombieKin(target);
        if (target instanceof Villager) {
            return true;
        }
        if (target instanceof ServerPlayer player) {
            return !PlayerCorpseComponent.isCorpse(player);
        }
        // 原版怪物（Monster 子类：僵尸/末影人/苦力怕/骷髅/蜘蛛等）：
        // 不是已尸化的尸族、也不是已在感染中的 → 可以被感染领域或龙右近战感染为半尸兄
        if (target instanceof Monster mon && !ZombieKin.isZombieKin(mon)) {
            return true;
        }
        return false;
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
