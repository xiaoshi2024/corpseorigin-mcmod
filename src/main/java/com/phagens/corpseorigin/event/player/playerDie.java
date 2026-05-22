package com.phagens.corpseorigin.event.player;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.GongFU.GongFaZL.BaseGongFaItem;
import com.phagens.corpseorigin.GongFU.GongFaZL.GongFaData;
import com.phagens.corpseorigin.GongFU.GongFaZL.GongFaSkillManager;
import com.phagens.corpseorigin.GongFU.ModUtlis.GongFUDataUtlis;
import com.phagens.corpseorigin.GongFU.PackGongFu.Paket.SyncSlotConfigPacket;
import com.phagens.corpseorigin.Item.YaoJi.Sagent;
import com.phagens.corpseorigin.skill.ISkillHandler;
import com.phagens.corpseorigin.skill.SkillAttachment;
import com.phagens.corpseorigin.skill.SkillHandler;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.phagens.corpseorigin.GongFU.ModUtlis.GongFUDataUtlis.applyGongFaAttributes;

/**
 * 玩家死亡事件处理器 - 处理玩家死亡时的数据保存和重生恢复
 *
 * 【功能说明】
 * 1. 玩家死亡时保存功法数据到服务器内存缓存
 * 2. 玩家重生时从缓存恢复功法数据
 * 3. 死亡时移除所有CorpseOrigin添加的属性修饰符
 * 4. 每100tick检查并自动学习功法技能
 *
 * 【数据保存机制】
 * - 使用ConcurrentHashMap作为服务器内存缓存
 * - 死亡时保存GongFuContainer NBT数据
 * - 重生时恢复到新玩家实体
 * - 恢复后清理缓存
 *
 * 【属性清理】
 * - 死亡时调用Sagent.removeAllPlayerAttributes()
 * - 移除所有药剂和功法添加的属性修饰符
 *
 * 【自动学习】
 * - 每100tick（5秒）检查一次
 * - 遍历玩家功法容器中的所有功法物品
 * - 根据功法数据自动学习对应的技能
 * - 发送提示消息给玩家
 *
 * 【关联系统】
 * - GongFUDataUtlis: 功法数据工具类
 * - GongFaSkillManager: 功法技能管理
 * - SkillAttachment: 技能数据管理
 * - Sagent: 药剂系统
 *
 * @author Phagens
 * @version 1.0
 */
@EventBusSubscriber(modid = CorpseOrigin.MODID)
public class playerDie {

    /** 服务器级别的内存缓存 - 存储死亡玩家的功法数据 */
    private static final Map<String, CompoundTag> DEATH_BACKUP = new ConcurrentHashMap<>();
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) {

            CompoundTag playerData = player.getPersistentData();

            // 保存功法容器数据
            CompoundTag gongFuData = playerData.getCompound("GongFuContainer");
            if (!gongFuData.isEmpty()) {
                DEATH_BACKUP.put(player.getUUID().toString(), gongFuData.copy());
            }

            // 保存槽位配置数据
            CompoundTag slotData = playerData.getCompound("GongFuSlotData");
            if (!slotData.isEmpty()) {
                DEATH_BACKUP.put(player.getUUID().toString() + "_SlotData", slotData.copy());
            }

            // 玩家死亡时移除所有CorpseOrigin添加的属性修饰符
            Sagent.removeAllPlayerAttributes(player);
        }

    }
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event){
        Player original = event.getOriginal();
        Player newPlayer = event.getEntity();

        String uuidStr = original.getUUID().toString();

        // 恢复功法容器数据
        if (DEATH_BACKUP.containsKey(uuidStr)) {
            CompoundTag backupData = DEATH_BACKUP.get(uuidStr);
            newPlayer.getPersistentData().put("GongFuContainer", backupData);
            DEATH_BACKUP.remove(uuidStr);
        }

        // 恢复槽位配置数据
        String slotKey = uuidStr + "_SlotData";
        if (DEATH_BACKUP.containsKey(slotKey)) {
            CompoundTag slotBackup = DEATH_BACKUP.get(slotKey);
            newPlayer.getPersistentData().put("GongFuSlotData", slotBackup);
            DEATH_BACKUP.remove(slotKey);

            // 同步槽位配置到客户端
            if (!newPlayer.level().isClientSide && newPlayer instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                SyncSlotConfigPacket packet =
                        SyncSlotConfigPacket.create(serverPlayer);
                PacketDistributor.sendToPlayer(serverPlayer, packet);

                CorpseOrigin.LOGGER.info("【玩家重生】已恢复并同步槽位配置数据");
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide) {
            applyGongFaAttributes(player);
            if (player.tickCount % 100 == 0) {
                checkAndLearnGongFuSkills(player);
            }
        }
    }

    private static void checkAndLearnGongFuSkills(Player player) {
        // 获取玩家的技能处理器
        ISkillHandler handler = SkillAttachment.getSkillHandler(player);
        if (handler == null) {
            return;  // 没有技能处理器（可能是普通人）
        }

        // 获取修行容器中的所有物品
        NonNullList<ItemStack> gongFuItems = GongFUDataUtlis.getGongFuItems(player);

        for (ItemStack stack : gongFuItems) {
            // 检查是否是功法物品
            if (!stack.isEmpty() && stack.getItem() instanceof BaseGongFaItem gongFaItem) {
                // 从物品中读取功法数据
                GongFaData data = gongFaItem.getDataFromItem(stack);
                if (data == null) {
                    continue;  // 没有有效的功法数据
                }

                // 获取对应的技能 ID
                ResourceLocation skillId = GongFaSkillManager.getInstance()
                        .getGongFuSkillId(data.getTypeId(), data.getRarity(), data.getCeng());

                if (skillId == null) {
                    CorpseOrigin.LOGGER.warn("未找到功法技能 ID: {}_{}_{}",
                            data.getTypeId(), data.getRarity(), data.getCeng());
                    continue;  // 技能未注册
                }

                // 检查是否已经学习了该技能
                if (!handler.hasLearned(skillId)) {
                    // 尝试学习技能
                    var result = ((SkillHandler) handler).learnGongFuSkill(skillId);

                    if (result == ISkillHandler.LearnResult.SUCCESS ||
                            result == ISkillHandler.LearnResult.ALREADY_LEARNED) {
                        // 发送提示消息给玩家
                        String skillNames = String.join(", ", data.getSkills());
                        player.sendSystemMessage(Component.literal(
                                "§a§l✦ 习得功法技艺：§r§6" + skillNames +
                                        " §7(§e" + data.getName() + "§7)"
                        ));

                        CorpseOrigin.LOGGER.info("玩家 {} 自动习得功法技能：{} -> {}",
                                player.getName().getString(), data.getTypeId(), skillId);
                    } else {
                        CorpseOrigin.LOGGER.warn("玩家 {} 学习功法技能失败：{} - {}",
                                player.getName().getString(), data.getTypeId(), result);
                    }
                }
            }
        }
    }

}



