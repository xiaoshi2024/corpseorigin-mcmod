//package xiaoshi2022.corpseorigin.infection;
//
//import net.minecraft.core.BlockPos;
//import net.minecraft.network.chat.Component;
//import net.minecraft.server.level.ServerLevel;
//import net.minecraft.server.level.ServerPlayer;
//import net.minecraft.sounds.SoundEvents;
//import net.minecraft.sounds.SoundSource;
//import net.minecraft.tags.FluidTags;
//import net.minecraft.world.effect.MobEffectInstance;
//import net.minecraft.world.effect.MobEffects;
//import net.minecraft.world.entity.player.Player;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.level.block.state.BlockState;
//import xiaoshi2022.corpseorigin.CorpseOrigin;
//import xiaoshi2022.corpseorigin.character.CharacterManager;
//import xiaoshi2022.corpseorigin.character.ICharacter;
//import xiaoshi2022.corpseorigin.network.CorpseNetwork;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.UUID;
//
//public class InfectionManager {
//
//    private static final int EFFECT_CHECK_INTERVAL = 40;
//    private static final int SYNC_INTERVAL = 20;
//    private static final int CONTACT_TICKS_PER_POINT = 20;
//    private static final int DECAY_INTERVAL_TICKS = 100;
//
//    private final Map<UUID, Integer> contactTicks = new HashMap<>();
//    private final Map<UUID, Integer> effectTimer = new HashMap<>();
//    private final Map<UUID, Integer> syncTimer = new HashMap<>();
//    private final Map<UUID, Integer> decayTimer = new HashMap<>();
//
//    private static InfectionManager instance;
//
//    private InfectionManager() {
//    }
//
//    public static InfectionManager getInstance() {
//        if (instance == null) {
//            instance = new InfectionManager();
//        }
//        return instance;
//    }
//
//    // ==================== 查询 ====================
//
//    public static int getInfection(Player player) {
//        // 修复：通过 player.level() 获取 ServerLevel，再获取 Server
//        if (player.level() instanceof ServerLevel serverLevel) {
//            return PlayerInfectionData.get(serverLevel.getServer()).get(player.getUUID());
//        }
//        return 0;
//    }
//
//    public static float getInfectionPercent(Player player) {
//        return getInfection(player) / (float) PlayerInfectionData.MAX_INFECTION;
//    }
//
//    // ==================== 修改 ====================
//
//    public static void addInfection(Player player, int amount, UUID source) {
//        if (player.level().isClientSide() || amount <= 0) {
//            return;
//        }
//        ICharacter character = CharacterManager.getInstance().getPlayerCharacter(player);
//        float multiplier = character != null ? character.getInfectionMultiplier() : 1.0f;
//        int actual = Math.round(amount * multiplier);
//        if (actual <= 0) {
//            return;
//        }
//
//        // 修复：通过 player.level() 获取 ServerLevel，再获取 PlayerInfectionData
//        if (player.level() instanceof ServerLevel serverLevel) {
//            PlayerInfectionData data = PlayerInfectionData.get(serverLevel.getServer());
//            data.set(player.getUUID(), data.get(player.getUUID()) + actual);
//            if (player instanceof ServerPlayer sp) {
//                CorpseNetwork.sendInfectionSync(sp, data.get(sp.getUUID()));
//            }
//        }
//    }
//
//    public static void clearInfection(Player player) {
//        if (player.level().isClientSide()) {
//            return;
//        }
//        if (player.level() instanceof ServerLevel serverLevel) {
//            PlayerInfectionData data = PlayerInfectionData.get(serverLevel.getServer());
//            data.set(player.getUUID(), 0);
//            if (player instanceof ServerPlayer sp) {
//                CorpseNetwork.sendInfectionSync(sp, 0);
//            }
//        }
//    }
//
//    public static void reduceInfection(Player player, int amount) {
//        if (player.level().isClientSide()) {
//            return;
//        }
//        if (player.level() instanceof ServerLevel serverLevel) {
//            PlayerInfectionData data = PlayerInfectionData.get(serverLevel.getServer());
//            data.set(player.getUUID(), Math.max(0, data.get(player.getUUID()) - amount));
//            if (player instanceof ServerPlayer sp) {
//                CorpseNetwork.sendInfectionSync(sp, data.get(sp.getUUID()));
//            }
//        }
//    }
//
//    // ==================== 尸水标记 ====================
//
//    public static void markWater(ServerLevel level, BlockPos pos) {
//        InfectionData.get(level).markWaterInfected(pos);
//    }
//
//    public static int markWaterAround(ServerLevel level, BlockPos center, int radius) {
//        return InfectionData.get(level).markArea(level, center, radius);
//    }
//
//    public static int purifyWaterAround(ServerLevel level, BlockPos center, int radius) {
//        return InfectionData.get(level).clearArea(center, radius);
//    }
//
//    public static boolean isInfectedWater(ServerLevel level, BlockPos pos) {
//        return InfectionData.get(level).isWaterInfected(pos);
//    }
//
//    // ==================== Tick ====================
//
//    public void tick(Player player) {
//        // 修复：检查 player.level() 是否为 ServerLevel
//        if (player.level().isClientSide() || !player.isAlive()) {
//            return;
//        }
//        if (!(player.level() instanceof ServerLevel level)) {
//            return;
//        }
//        if (!(player instanceof ServerPlayer sp)) {
//            return;
//        }
//
//        UUID uuid = player.getUUID();
//        PlayerInfectionData data = PlayerInfectionData.get(level.getServer());
//        int infection = data.get(uuid);
//
//        boolean inInfectedWater = isInInfectedWater(level, player);
//
//        // 接触尸水累积
//        if (inInfectedWater) {
//            int ticks = contactTicks.merge(uuid, 1, Integer::sum);
//            if (ticks >= CONTACT_TICKS_PER_POINT) {
//                contactTicks.put(uuid, 0);
//                ICharacter character = CharacterManager.getInstance().getPlayerCharacter(player);
//                float multiplier = character != null ? character.getInfectionMultiplier() : 1.0f;
//                if (multiplier > 0 && infection < PlayerInfectionData.MAX_INFECTION) {
//                    data.set(uuid, infection + 1);
//                }
//            }
//            decayTimer.put(uuid, 0);
//        } else {
//            contactTicks.put(uuid, 0);
//            int decay = decayTimer.merge(uuid, 1, Integer::sum);
//            if (decay >= DECAY_INTERVAL_TICKS) {
//                decayTimer.put(uuid, 0);
//                if (infection > 0) {
//                    data.set(uuid, infection - 1);
//                }
//            }
//        }
//
//        // 分级效果
//        int updated = data.get(uuid);
//        if (updated > 0) {
//            int timer = effectTimer.merge(uuid, 1, Integer::sum);
//            if (timer >= EFFECT_CHECK_INTERVAL) {
//                effectTimer.put(uuid, 0);
//                applyInfectionEffects(player, updated);
//            }
//        } else {
//            effectTimer.put(uuid, 0);
//        }
//
//        // 同步给客户端
//        int sync = syncTimer.merge(uuid, 1, Integer::sum);
//        if (sync >= SYNC_INTERVAL) {
//            syncTimer.put(uuid, 0);
//            CorpseNetwork.sendInfectionSync(sp, data.get(uuid));
//        }
//    }
//
//    private boolean isInInfectedWater(ServerLevel level, Player player) {
//        BlockPos feet = player.blockPosition();
//        BlockPos eye = BlockPos.containing(player.getEyePosition());
//        for (BlockPos pos : new BlockPos[]{feet, eye}) {
//            BlockState state = level.getBlockState(pos);
//            if (state.getFluidState().is(FluidTags.WATER) && isInfectedWater(level, pos)) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    private void applyInfectionEffects(Player player, int infection) {
//        if (infection >= PlayerInfectionData.MAX_INFECTION) {
//            player.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1, false, true));
//            player.sendOverlayMessage(Component.translatable("message.corpseorigin.infection.critical"));
//            player.level().playSound(null, player.blockPosition(),
//                    SoundEvents.ZOMBIE_AMBIENT, SoundSource.PLAYERS, 0.6f, 0.6f);
//            if (player.level() instanceof ServerLevel serverLevel) {
//                PlayerInfectionData.get(serverLevel.getServer()).set(player.getUUID(), 60);
//            }
//        } else if (infection >= 60) {
//            player.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0, false, true));
//        } else if (infection >= 30) {
//            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 120, 1, false, true));
//        }
//    }
//
//    // ==================== 清理 ====================
//
//    public void cleanupPlayer(UUID uuid) {
//        contactTicks.remove(uuid);
//        effectTimer.remove(uuid);
//        syncTimer.remove(uuid);
//        decayTimer.remove(uuid);
//    }
//}