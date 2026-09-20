package xiaoshi2022.corpseorigin.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.block.entity.ZBRFleshBlockEntity;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;
import xiaoshi2022.corpseorigin.registry.ModBlocks;
import xiaoshi2022.corpseorigin.registry.ModEntities;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.ShiChaoZhiZi;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import xiaoshi2022.corpseorigin.skill.longyou.CorpseNestDimension;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayDeque;

/**
 * 尸兄肉块 —— 尸巢的基本建筑方块。
 * <p>
 * 【原著设定】尸巢是漫画《尸兄》里的核心地点：由巨型尸兄形成的肉山建筑，外形像水立方但有棱角，
 * 外壳由肉块与触手构成、内部中空。设定上尸巢会吸引各方尸兄进入，靠吞噬它们生长；
 * 尸兄玩家攒够奴仆后让奴仆自我吞噬生成肉块，慢慢堆出尸巢。
 * <p>
 * 【本方块的作用】作为尸巢的基本单元：尸兄踩在上面会"贡献"击杀数，
 * 攒够 {@link ZBRFleshBlockEntity#REQUIRED_KILLS} 就在原地炸出一座内部中空的尸巢；
 * 被非主人破坏时会召唤周围尸兄反击。
 * <p>
 * 【移植说明】从 1.21.1 版移植到 26.2：
 * <ul>
 *   <li>26.2 的 {@code RenderShape} 只剩 {@code INVISIBLE / MODEL}，
 *       所以外观全部交给 GeckoLib 的方块实体渲染器（同 {@code CloneChamberBlock} 的做法）；</li>
 *   <li>{@code entityInside} 多了 {@code InsideBlockEffectApplier} 与 {@code isMoving} 两个参数；</li>
 *   <li>1.21.1 的 {@code onRemove} 在 26.2 已被 {@code affectNeighborsAfterRemoval} 取代，
 *       而那个时点方块实体已经没了、拿不到肉块主人 —— 反击逻辑改挂在
 *       {@link #playerWillDestroy}（玩家破坏时，方块与方块实体都还在），语义不变；</li>
 *   <li>取"肉块方块状态"原来走注册表名字符串，这里直接引 {@link ModBlocks#ZBR_FLESH}。</li>
 * </ul>
 */
public class ZBRFleshBlock extends Block implements EntityBlock {

    /** 外壳厚度 */
    private static final int OUTER_WALL_THICKNESS = 2;

    /** 26.2 起 VoxelShape 用 0~1 的归一化坐标（1 = 一整格） */
    private static final VoxelShape FULL_CUBE = Shapes.box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);

    public ZBRFleshBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        // 26.2 没有 ENTITYBLOCK_ANIMATED 了：不画原版模型，全部交给 GeckoLib 的 BER
        // Corpse nests can contain thousands of these blocks. The baked model is
        // chunk-batched instead of animating and drawing every block separately.
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FULL_CUBE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FULL_CUBE;
    }

    /** 尸巢之子主动吸食相连的尸巢结构，每块计作一位尸兄。 */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof ZBRFleshBlockEntity flesh
                && flesh.isCorpseNestGateway()) {
            CorpseNestDimension.enter(serverPlayer);
            return InteractionResult.SUCCESS;
        }
        if (level.isClientSide() || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        // Only the Young Cult Leader (Son of the Corpse Nest) can absorb flesh blocks.
        if (!ShiChaoZhiZi.ID.equals(CharacterManager.getInstance().getPlayerCharacterId(player))) {
            return InteractionResult.PASS;
        }

        PlayerCorpseComponent corpse = PlayerCorpseComponent.get(player);
        int remaining = Math.max(0, 1000 - corpse.getKills());
        boolean preserveBlocks = level.dimension().equals(CorpseNestDimension.KEY);
        int absorbed = absorbConnectedFlesh((ServerLevel) level, pos, remaining, !preserveBlocks);
        corpse.addKills(absorbed);
        player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
                "message.corpseorigin.shichaozhizi.absorb_progress",
                corpse.getKills(), 1000));
        ((ServerLevel) level).sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                Math.min(80, absorbed), 1.5, 1.5, 1.5, 0.08);
        level.playSound(null, pos, SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 1.0F, 0.7F);
        return InteractionResult.SUCCESS;
    }

    private static int absorbConnectedFlesh(ServerLevel level, BlockPos start, int limit, boolean removeBlocks) {
        if (limit <= 0) return 0;
        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        pending.add(start.immutable());
        int absorbed = 0;

        while (!pending.isEmpty() && absorbed < limit) {
            BlockPos current = pending.removeFirst();
            if (!visited.add(current) || !level.getBlockState(current).is(ModBlocks.ZBR_FLESH)) continue;
            if (removeBlocks) level.destroyBlock(current, false);
            absorbed++;
            for (var direction : net.minecraft.core.Direction.values()) {
                BlockPos next = current.relative(direction);
                if (!visited.contains(next)) pending.addLast(next.immutable());
            }
        }
        return absorbed;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        level.scheduleTick(pos, this, 40);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.scheduleTick(pos, this, 40);

        if (level.getBlockEntity(pos) instanceof ZBRFleshBlockEntity flesh
                && flesh.getKills() >= ZBRFleshBlockEntity.REQUIRED_KILLS) {
            // 攒够了 → 原地炸出一座内部中空的尸巢，然后自身消失
            generateHollowNest(level, pos);
            level.destroyBlock(pos, false);
        }
    }

    /** 尸兄踩在肉块上 = 自我吞噬，贡献一点击杀数 */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                InsideBlockEffectApplier effectApplier, boolean isMoving) {
        super.entityInside(state, level, pos, entity, effectApplier, isMoving);

        if (level.isClientSide() || !(entity instanceof LowerLevelZbEntity)) {
            return;
        }

        if (level.getBlockEntity(pos) instanceof ZBRFleshBlockEntity flesh) {
            flesh.addKills();
            level.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                    SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 1.0F, 0.5F);
        }
    }

    /**
     * 被玩家破坏时的反击：破坏者不是主人 → 号令周围尸兄扑上去，并现场补几只援军。
     * <p>
     * 1.21.1 版把这段放在 {@code onRemove} 里；26.2 的 {@code affectNeighborsAfterRemoval}
     * 触发时方块实体已经没了、读不到主人，所以换成这里（同样是"有人来拆"的时点）。
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()
                && level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof ZBRFleshBlockEntity flesh
                && flesh.getOwner() != null
                && !player.getUUID().equals(flesh.getOwner())) {
            counterAttack(serverLevel, pos, player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ZBRFleshBlockEntity(pos, state);
    }

    // ==================== 尸巢生成 ====================

    private void generateHollowNest(ServerLevel level, BlockPos centerPos) {
        try {
            RandomSource random = RandomSource.create();
            NestSize nestSize = selectNestSize(random);

            CorpseOrigin.LOGGER.info("开始生成内部中空的尸巢，尺寸: {}, 中心位置: {}", nestSize.name(), centerPos);

            generateOuterShell(level, centerPos, nestSize, random);
            generateInteriorDecoration(level, centerPos, nestSize, random);
            generateEntrance(level, centerPos, nestSize, random);
            generateNestAtmosphere(level, centerPos, nestSize);

            CorpseOrigin.LOGGER.info("成功生成内部中空的尸巢，尺寸: {}", nestSize.name());
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("生成尸巢结构失败", e);
        }
    }

    private NestSize selectNestSize(RandomSource random) {
        double roll = random.nextDouble();
        if (roll < 0.33) {
            return NestSize.SMALL;
        }
        if (roll < 0.66) {
            return NestSize.MEDIUM;
        }
        return NestSize.LARGE;
    }

    /** 外壳：半径判定 + 正弦扰动，做出有水立方味道但不规则的多面体，内部留空 */
    private void generateOuterShell(ServerLevel level, BlockPos centerPos, NestSize size, RandomSource random) {
        BlockState fleshState = ModBlocks.ZBR_FLESH.defaultBlockState();
        int outerRadius = size.getOuterRadius();
        Set<BlockPos> shellBlocks = new HashSet<>();

        for (int x = -outerRadius; x <= outerRadius; x++) {
            for (int y = -outerRadius; y <= outerRadius; y++) {
                for (int z = -outerRadius; z <= outerRadius; z++) {
                    if (!isInShell(x, y, z, outerRadius, OUTER_WALL_THICKNESS)) {
                        continue;
                    }

                    BlockPos pos = centerPos.offset(x, y, z);
                    shellBlocks.add(pos);
                    level.setBlock(pos, fleshState, 3);

                    if (random.nextDouble() < 0.1) {
                        addTentacleDecoration(level, pos, fleshState, random);
                    }
                }
            }
        }

        CorpseOrigin.LOGGER.info("生成外壳完成，共放置 {} 个肉块方块", shellBlocks.size());
    }

    private boolean isInShell(int x, int y, int z, int radius, int thickness) {
        int distance = Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z)));
        return distance <= radius && distance > radius - thickness;
    }

    /** 外壳上的触手凸起 */
    private void addTentacleDecoration(ServerLevel level, BlockPos pos, BlockState fleshState, RandomSource random) {
        int direction = random.nextInt(6);
        BlockPos offsetPos;

        for (int i = 1; i <= random.nextInt(2, 5); i++) {
            offsetPos = switch (direction) {
                case 0 -> pos.offset(0, i, 0);
                case 1 -> pos.offset(0, -i, 0);
                case 2 -> pos.offset(i, 0, 0);
                case 3 -> pos.offset(-i, 0, 0);
                case 4 -> pos.offset(0, 0, i);
                default -> pos.offset(0, 0, -i);
            };

            if (level.isEmptyBlock(offsetPos)) {
                level.setBlock(offsetPos, fleshState, 3);
            } else {
                break;
            }
        }
    }

    private void generateInteriorDecoration(ServerLevel level, BlockPos centerPos, NestSize size, RandomSource random) {
        int min = -size.getInteriorRadius();
        int max = size.getInteriorRadius();

        generateWallVessels(level, centerPos, min, max, random);
        generateFleshPillars(level, centerPos, size, random);
        generateFloorDetails(level, centerPos, min, max, random);
        generateCeilingTentacles(level, centerPos, min, max, random);
    }

    private void generateWallVessels(ServerLevel level, BlockPos centerPos, int min, int max, RandomSource random) {
        BlockState fleshState = ModBlocks.ZBR_FLESH.defaultBlockState();

        for (int x = min; x <= max; x++) {
            for (int y = min; y <= max; y++) {
                for (int z = min; z <= max; z++) {
                    if ((Math.abs(x) == max || Math.abs(z) == max) && Math.abs(y) < max
                            && random.nextDouble() < 0.3) {
                        level.setBlock(centerPos.offset(x, y, z), fleshState, 3);
                    }
                }
            }
        }
    }

    private void generateFleshPillars(ServerLevel level, BlockPos centerPos, NestSize size, RandomSource random) {
        BlockState fleshState = ModBlocks.ZBR_FLESH.defaultBlockState();
        int interiorRadius = size.getInteriorRadius();
        int pillarCount = size == NestSize.SMALL ? 2 : (size == NestSize.MEDIUM ? 4 : 6);

        for (int i = 0; i < pillarCount; i++) {
            double angle = (2 * Math.PI * i) / pillarCount + random.nextDouble() * 0.5;
            int pillarX = (int) (Math.cos(angle) * (interiorRadius - 2));
            int pillarZ = (int) (Math.sin(angle) * (interiorRadius - 2));

            for (int y = -interiorRadius; y <= interiorRadius; y++) {
                BlockPos pillarPos = centerPos.offset(pillarX, y, pillarZ);

                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dz == 0) {
                            continue;
                        }
                        BlockPos pos = pillarPos.offset(dx, 0, dz);
                        if (Math.abs(pos.getX() - centerPos.getX()) <= interiorRadius
                                && Math.abs(pos.getZ() - centerPos.getZ()) <= interiorRadius) {
                            level.setBlock(pos, fleshState, 3);
                        }
                    }
                }
                level.setBlock(pillarPos, fleshState, 3);
            }
        }
    }

    private void generateFloorDetails(ServerLevel level, BlockPos centerPos, int min, int max, RandomSource random) {
        BlockState fleshState = ModBlocks.ZBR_FLESH.defaultBlockState();
        int floorY = -max;

        for (int x = min; x <= max; x++) {
            for (int z = min; z <= max; z++) {
                BlockPos floorPos = centerPos.offset(x, floorY, z);
                level.setBlock(floorPos, fleshState, 3);

                if (random.nextDouble() < 0.15) {
                    BlockPos lumpPos = floorPos.above();
                    level.setBlock(lumpPos, fleshState, 3);

                    if (random.nextDouble() < 0.3) {
                        level.setBlock(lumpPos.above(), fleshState, 3);
                    }
                }
            }
        }
    }

    private void generateCeilingTentacles(ServerLevel level, BlockPos centerPos, int min, int max, RandomSource random) {
        BlockState fleshState = ModBlocks.ZBR_FLESH.defaultBlockState();
        int ceilingY = max;

        for (int x = min; x <= max; x++) {
            for (int z = min; z <= max; z++) {
                if (random.nextDouble() >= 0.2) {
                    continue;
                }

                level.setBlock(centerPos.offset(x, ceilingY, z), fleshState, 3);

                int tentacleLength = random.nextInt(1, 4);
                for (int i = 1; i <= tentacleLength; i++) {
                    BlockPos tentaclePos = centerPos.offset(x, ceilingY - i, z);
                    if (Math.abs(tentaclePos.getY() - centerPos.getY()) > min) {
                        level.setBlock(tentaclePos, fleshState, 3);
                    } else {
                        break;
                    }
                }
            }
        }
    }

    private void generateEntrance(ServerLevel level, BlockPos centerPos, NestSize size, RandomSource random) {
        int outerRadius = size.getOuterRadius();
        int side = random.nextInt(4);
        int entranceWidth = size == NestSize.SMALL ? 2 : (size == NestSize.MEDIUM ? 3 : 4);
        int entranceHeight = size == NestSize.SMALL ? 3 : (size == NestSize.MEDIUM ? 4 : 5);

        BlockPos entrancePos = switch (side) {
            case 0 -> centerPos.offset(0, 0, -outerRadius);
            case 1 -> centerPos.offset(0, 0, outerRadius);
            case 2 -> centerPos.offset(-outerRadius, 0, 0);
            default -> centerPos.offset(outerRadius, 0, 0);
        };

        for (int a = -entranceWidth / 2; a <= entranceWidth / 2; a++) {
            for (int y = -entranceHeight / 2; y <= entranceHeight / 2; y++) {
                BlockPos pos = (side <= 1)
                        ? entrancePos.offset(a, y, 0)
                        : entrancePos.offset(0, y, a);
                level.destroyBlock(pos, false);
            }
        }

        CorpseOrigin.LOGGER.info("生成尸巢入口在侧面: {}", side);
    }

    private void generateNestAtmosphere(ServerLevel level, BlockPos centerPos, NestSize size) {
        int interiorRadius = size.getInteriorRadius();

        for (int i = 0; i < 100; i++) {
            double x = centerPos.getX() + (Math.random() - 0.5) * interiorRadius * 2;
            double y = centerPos.getY() + (Math.random() - 0.5) * interiorRadius * 2;
            double z = centerPos.getZ() + (Math.random() - 0.5) * interiorRadius * 2;

            level.sendParticles(ParticleTypes.CRIMSON_SPORE, x, y, z, 1, 0.1, 0.1, 0.1, 0);
        }

        level.playSound(null, centerPos.getX(), centerPos.getY(), centerPos.getZ(),
                SoundEvents.WITHER_SPAWN, SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    // ==================== 反击 ====================

    private void counterAttack(ServerLevel level, BlockPos pos, Player attacker) {
        try {
            List<LowerLevelZbEntity> existingZombies = level.getEntitiesOfClass(
                    LowerLevelZbEntity.class, new AABB(pos).inflate(32));

            for (LowerLevelZbEntity zombie : existingZombies) {
                zombie.setTarget(attacker);
            }

            if (existingZombies.size() < 5) {
                for (int i = 0; i < 3; i++) {
                    double spawnX = pos.getX() + (Math.random() - 0.5) * 8;
                    double spawnY = pos.getY() + 1;
                    double spawnZ = pos.getZ() + (Math.random() - 0.5) * 8;

                    LowerLevelZbEntity zombie = ModEntities.LOWER_LEVEL_ZB.create(level, EntitySpawnReason.EVENT);
                    if (zombie != null) {
                        zombie.snapTo(spawnX, spawnY, spawnZ, 0.0F, 0.0F);
                        zombie.setTarget(attacker);
                        level.addFreshEntity(zombie);

                        level.sendParticles(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F),
                                spawnX, spawnY, spawnZ, 10, 0.0, 0.0, 0.0, 0.0);
                    }
                }
            }

            level.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                    SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 2.0F, 0.6F);

            CorpseOrigin.LOGGER.info("尸巢被入侵，尸兄奴仆反击！");
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("反击失败", e);
        }
    }

    /** 尸巢尺寸 */
    private enum NestSize {
        SMALL(3, 7),    // 内部半径 3，外部半径 7
        MEDIUM(5, 11),
        LARGE(7, 15);

        private final int interiorRadius;
        private final int outerRadius;

        NestSize(int interiorRadius, int outerRadius) {
            this.interiorRadius = interiorRadius;
            this.outerRadius = outerRadius;
        }

        public int getInteriorRadius() {
            return this.interiorRadius;
        }

        public int getOuterRadius() {
            return this.outerRadius;
        }
    }
}
