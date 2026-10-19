package com.xiaoshi2022.corpseorigin.event;

import com.xiaoshi2022.corpseorigin.growth.CorpseInfection;
import com.xiaoshi2022.corpseorigin.registry.ModFluids;
import com.xiaoshi2022.corpseorigin.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Forge 事件总线入口（1.20.1 Forge 版本）。
 * <p>
 * 感染触发、NativeCorpseState tick 等 Server-only 逻辑都挂在这里。
 * 与 26.2 Fabric 版本的 ServerLivingEntityEvents 对应。
 */
public final class ModEvents {
    private ModEvents() {}

    /** Forge LivingHurtEvent：每次 LivingEntity 受伤后，攻击者是尸族则按概率感染。 */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) return;
        CorpseInfection.onLivingHurt(target, event.getSource().getEntity() instanceof LivingEntity ? (LivingEntity) event.getSource().getEntity() : null, event.getAmount(), event.isCanceled());
    }

    /** 玩家开始追踪实体时补发尸变渲染状态（后进入世界的玩家也能看到）。 */
    @SubscribeEvent
    public static void onStartTracking(net.minecraftforge.event.entity.player.PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof net.minecraft.world.entity.Mob mob
                && event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player
                && com.xiaoshi2022.corpseorigin.growth.NativeCorpseState.infected(mob)) {
            com.xiaoshi2022.corpseorigin.network.CorpseOriginNetwork.syncTo(player, mob);
        }
    }

    /** 客户端卸载世界时清空同步缓存，避免 UUID 复用导致误渲染。 */
    @SubscribeEvent
    public static void onLevelUnload(net.minecraftforge.event.level.LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            com.xiaoshi2022.corpseorigin.growth.NativeCorpseState.clearClientState();
        }
    }

    /** 【临时诊断】生成一只感染苦力怕，验证骨骼渲染。 */
    @SubscribeEvent
    public static void onDiagServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        var level = server.overworld();
        if (level == null) return;
        DIAG_TICKS++;
        if (DIAG_TICKS == 80) {
            var player = level.players().stream().findFirst().orElse(null);
            if (player == null) return;
            var creeper = net.minecraft.world.entity.EntityType.CREEPER.create(level);
            if (creeper == null) return;
            var look = player.getLookAngle();
            var eye = player.getEyePosition();
            creeper.moveTo(eye.x + look.x * 3.0D, eye.y + look.y * 3.0D - 1.2D, eye.z + look.z * 3.0D,
                    0.0F, 0.0F);
            level.addFreshEntity(creeper);
            DIAG_ENTITY = creeper.getUUID();
            com.xiaoshi2022.corpseorigin.effect.BYeffect.applyInfection(creeper, level, 40, null);
            com.xiaoshi2022.corpseorigin.CorpseOrigin.LOGGER.info("[diag2] spawned creeper {}", DIAG_ENTITY);
        }
        if (DIAG_ENTITY != null && DIAG_TICKS % 40 == 0) {
            var entity = level.getEntity(DIAG_ENTITY);
            if (entity instanceof LivingEntity living) {
                var instance = living.getEffect(com.xiaoshi2022.corpseorigin.registry.ModEffects.QIANS.get());
                com.xiaoshi2022.corpseorigin.CorpseOrigin.LOGGER.info(
                        "[diag2] server t={} infected={} infinite={} amp={} tags={}",
                        DIAG_TICKS,
                        com.xiaoshi2022.corpseorigin.growth.NativeCorpseState.infected(living),
                        instance != null && instance.isInfiniteDuration(),
                        instance == null ? -99 : instance.getAmplifier(),
                        living.getTags());
            }
        }
    }

    private static int DIAG_TICKS = 0;
    private static java.util.UUID DIAG_ENTITY = null;

    /**
     * 玻璃瓶舀尸水：右击尸水水源 → 获得尸水瓶（bywater_bottle）。
     * <p>
     * 原版 BottleItem 只识别 {@code minecraft:water}，所以这里自行做 SOURCE_ONLY 射线检测，
     * 兼容生存 / 创造两种模式（创造模式下客户端方块射线不会命中流体）。
     */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.GLASS_BOTTLE)) return;

        Level level = event.getLevel();
        Vec3 eye = player.getEyePosition();
        double reach = player.getAttributeValue(ForgeMod.BLOCK_REACH.get());
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(reach));
        BlockHitResult hit = level.clip(new ClipContext(eye, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, player));
        if (hit.getType() != HitResult.Type.BLOCK) return;

        BlockPos pos = hit.getBlockPos();
        FluidState fluid = level.getFluidState(pos);
        if (fluid.getType() != ModFluids.INFECTED_WATER.get() || !fluid.isSource()) return;

        if (!level.isClientSide()) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            ItemStack bottle = new ItemStack(ModItems.BYWATER_BOTTLE.get());
            if (!player.getInventory().add(bottle)) {
                player.drop(bottle, false);
            }
            level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
    }
}
