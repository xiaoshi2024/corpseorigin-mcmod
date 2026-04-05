package com.phagens.corpseorigin.skill;

import com.mojang.serialization.Codec;
import com.phagens.corpseorigin.CorpseOrigin;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * 技能数据附件类 - 管理玩家技能数据的持久化
 *
 * 【功能说明】
 * 1. 使用NeoForge的Attachment系统存储玩家技能数据
 * 2. 支持数据序列化和反序列化（保存到NBT）
 * 3. 支持死亡后数据保留（copyOnDeath）
 * 4. 处理客户端-服务器数据同步
 *
 * 【数据流】
 * 1. 服务端：玩家数据变更 -> 保存到Attachment -> 同步到客户端
 * 2. 客户端：接收同步包 -> 更新本地数据 -> UI更新
 * 3. 死亡/重生：copyOnDeath复制数据 -> 重新应用被动技能
 *
 * 【重要机制】
 * - 使用Codec进行数据序列化
 * - 延迟加载：首次访问时才创建SkillHandler
 * - 玩家引用管理：反序列化后需要重新设置玩家引用
 * - 被动技能重应用：登录/重生后自动重新应用
 *
 * 【关联系统】
 * - SkillHandler: 技能数据处理实现
 * - SyncSkillDataPacket: 客户端同步数据包
 * - SkillEventHandler: 处理登录/重生事件
 *
 * @author Phagens
 * @version 1.0
 */
public class SkillAttachment {

    /** 附件类型注册器 */
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, CorpseOrigin.MODID);

    /** 技能处理器附件类型 */
    public static final Supplier<AttachmentType<SkillHandler>> SKILL_HANDLER = ATTACHMENT_TYPES.register(
            "skill_handler",
            () -> AttachmentType.<SkillHandler>builder(() -> new SkillHandler(null))
                    .serialize(SkillHandler.CODEC)
                    .copyOnDeath() // 死亡后保留数据
                    .build()
    );

    /**
     * 获取玩家的技能处理器
     * 自动处理创建、反序列化、玩家引用设置等问题
     *
     * 【处理流程】
     * 1. 获取玩家数据附件中的SkillHandler
     * 2. 如果为null，创建新的SkillHandler
     * 3. 如果玩家引用为null（反序列化后），重新设置
     * 4. 重新应用被动技能（如果是服务端）
     *
     * @param player 目标玩家
     * @return 玩家的技能处理器
     */
    public static ISkillHandler getSkillHandler(Player player) {
        if (player == null) return null;

        SkillHandler handler = player.getData(SKILL_HANDLER);

        if (handler == null) {
//            CorpseOrigin.LOGGER.debug("为玩家 {} 创建新的技能处理器", player.getName().getString());
            handler = new SkillHandler(player);
            player.setData(SKILL_HANDLER, handler);
        } else if (handler.getPlayer() == null) {
            handler.setPlayer(player);

            // 客户端和服务端都打印日志
            if (player.level().isClientSide) {
//                CorpseOrigin.LOGGER.info("【客户端】为玩家 {} 设置技能处理器，已学习 {} 个技能，进化点: {}",
//                        player.getName().getString(),
//                        handler.getLearnedSkills().size(),
//                        handler.getEvolutionPoints());
            } else {
//                CorpseOrigin.LOGGER.info("【服务端】为玩家 {} 设置技能处理器，已学习 {} 个技能，进化点: {}",
//                        player.getName().getString(),
//                        handler.getLearnedSkills().size(),
//                        handler.getEvolutionPoints());

                // 只有在服务端才重新应用被动技能
                handler.reapplyPassiveSkills();
            }
        }

        return handler;
    }
}