/**
 * 尸兄肉块方块类 - 尸巢的建筑方块
 *
 * 【功能说明】
 * 1. 动画效果：具有呼吸般的脉动动画，模拟活体组织的特性
 * 2. 尸巢组件：作为尸巢的基本构成单元，可以被尸兄玩家召唤生成
 * 3. 生物特性：虽然是方块，但具有类似生物的视觉效果
 *
 * 【尸巢背景 - 原著设定】
 * 尸巢是漫画《尸兄》中的核心地点，是由巨型尸兄形成的肉山建筑。
 * 尸巢外形类似水立方，但有棱角，外部由肉块和触手构成。
 * 内部中空，是一个巨大的空间，充满肉壁、血管和各种生物组织。
 * 尸巢会吸引各方尸兄进入并通过吞噬他们来生长。
 * 尸兄玩家可以觉醒技能，当拥有足够多尸兄奴仆时，
 * 可以命令奴仆们自我吞噬生成肉块，逐渐组成尸巢。
 *
 * 【尸巢结构特点】
 * - 外部：肉块外壳，厚度2-3层
 * - 内部：中空空间，可供玩家和尸兄活动
 * - 结构：类似水立方但有棱角，不规则的多面体结构
 * - 连接处：肉块之间有血管连接，形成生物网络
 *
 * 【动画系统】
 * - idle动画：持续的呼吸脉动效果
 * - 使用GeckoLib实现复杂的3D动画效果
 *
 * @author Phagens
 * @version 2.0 - 重构为原著设定
 */
package com.phagens.corpseorigin.block.custom;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.block.entity.ZBRFleshBlockEntity;
import com.phagens.corpseorigin.entity.LowerLevelZbEntity;
import com.phagens.corpseorigin.register.EntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * 尸兄肉块方块主类
 * 继承Block实现基础方块功能，实现EntityBlock接口以支持方块实体
 */
public class ZBRFleshBlock extends Block implements EntityBlock {

    // 尸巢结构尺寸配置
    private static final int OUTER_WALL_THICKNESS = 2; // 外壳厚度
    private static final int MIN_INTERIOR_SIZE = 5;    // 内部最小空间

    public ZBRFleshBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(2.0f, 4.0f)       // 硬度2.0，爆炸抗性4.0
                .sound(SoundType.SLIME_BLOCK) // 史莱姆音效，模拟血肉质感
                .mapColor(MapColor.COLOR_RED) // 地图显示为红色
                .noOcclusion()               // 不遮挡光线
                .randomTicks()               // 启用随机tick
        );
    }

    @Override
    public void onPlace(BlockState blockstate, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(blockstate, level, pos, oldState, moving);
        level.scheduleTick(pos, this, 40);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Block.box(0, 0, 0, 16, 16, 16);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Block.box(0, 0, 0, 16, 16, 16);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource source) {
        BlockEntity entity = level.getBlockEntity(pos);
        level.scheduleTick(pos, this, 40);

        if (entity instanceof ZBRFleshBlockEntity fleshEntity) {
//            CorpseOrigin.LOGGER.info("尸巢肉块tick检查，位置: {}, 当前击杀数: {}/{}",
//                    pos, fleshEntity.getKills(), ZBRFleshBlockEntity.REQUIRED_KILLS);

            if (fleshEntity.getKills() >= ZBRFleshBlockEntity.REQUIRED_KILLS) {
                // 生成内部中空的尸巢结构
                generateHollowNest(level, pos);
                // 销毁当前肉块方块
                level.destroyBlock(pos, false);
            }
        }
    }

    /**
     * 生成内部中空的尸巢结构（原著设定）
     * 外形类似水立方但有棱角，内部中空
     */
    private void generateHollowNest(ServerLevel level, BlockPos centerPos) {
        try {
            RandomSource random = RandomSource.create();

            // 随机选择尸巢大小（决定外部尺寸和内部空间）
            NestSize nestSize = selectNestSize(random);

            CorpseOrigin.LOGGER.info("开始生成内部中空的尸巢，尺寸: {}, 中心位置: {}", nestSize.name(), centerPos);

            // 生成外部肉块外壳（空心结构）
            generateOuterShell(level, centerPos, nestSize, random);

            // 生成内部装饰（血管、肉柱、触手等）
            generateInteriorDecoration(level, centerPos, nestSize, random);

            // 生成尸巢入口
            generateEntrance(level, centerPos, nestSize, random);

            // 生成尸巢内部的生物群系特效
            generateNestAtmosphere(level, centerPos, nestSize);

            CorpseOrigin.LOGGER.info("成功生成内部中空的尸巢，尺寸: {}", nestSize.name());

        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("生成尸巢结构失败", e);
        }
    }

    /**
     * 选择尸巢大小
     */
    private NestSize selectNestSize(RandomSource random) {
        double roll = random.nextDouble();
        if (roll < 0.33) {
            return NestSize.SMALL;   // 7x7x7 外部，内部3x3x3
        } else if (roll < 0.66) {
            return NestSize.MEDIUM;  // 11x11x11 外部，内部5x5x5
        } else {
            return NestSize.LARGE;   // 15x15x15 外部，内部7x7x7
        }
    }

    /**
     * 生成外部外壳（空心结构，类似水立方但有棱角）
     */
    private void generateOuterShell(ServerLevel level, BlockPos centerPos, NestSize size, RandomSource random) {
        BlockState fleshState = getFleshBlockState();
        int outerRadius = size.getOuterRadius();
        int wallThickness = OUTER_WALL_THICKNESS;

        Set<BlockPos> shellBlocks = new HashSet<>();

        // 生成不规则的多面体外壳（类似水立方但有棱角）
        for (int x = -outerRadius; x <= outerRadius; x++) {
            for (int y = -outerRadius; y <= outerRadius; y++) {
                for (int z = -outerRadius; z <= outerRadius; z++) {
                    BlockPos pos = centerPos.offset(x, y, z);

                    // 判断是否在外壳范围内
                    if (isInShell(x, y, z, outerRadius, wallThickness, random)) {
                        shellBlocks.add(pos);
                        level.setBlock(pos, fleshState, 3);

                        // 随机添加触手装饰（凸起）
                        if (random.nextDouble() < 0.1) {
                            addTentacleDecoration(level, pos, random);
                        }
                    }
                }
            }
        }

        CorpseOrigin.LOGGER.info("生成外壳完成，共放置 {} 个肉块方块", shellBlocks.size());
    }

    /**
     * 判断位置是否在外壳中（空心结构判断）
     */
    private boolean isInShell(int x, int y, int z, int radius, int thickness, RandomSource random) {
        // 计算曼哈顿距离，创造有棱角的形状（类似水立方）
        int distance = Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z)));

        // 外壳范围：距离在 (radius - thickness) 到 radius 之间
        boolean inOuterLayer = distance <= radius && distance > radius - thickness;

        // 添加不规则变化（让外壳不是完美的立方体，而是有棱角的不规则形状）
        if (inOuterLayer) {
            // 随机在一些位置产生凹陷或凸起
            double irregularity = Math.sin(x * 0.5) * Math.cos(y * 0.5) * Math.sin(z * 0.5);
            int adjustedRadius = radius + (int)(irregularity * 2);
            return Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z))) <= adjustedRadius;
        }

        return inOuterLayer;
    }

    /**
     * 添加触手装饰（外壳上的凸起）
     */
    private void addTentacleDecoration(ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState fleshState = getFleshBlockState();

        // 随机方向添加触手
        int direction = random.nextInt(6);
        BlockPos offsetPos = pos;

        for (int i = 1; i <= random.nextInt(2, 5); i++) {
            switch (direction) {
                case 0 -> offsetPos = pos.offset(0, i, 0);
                case 1 -> offsetPos = pos.offset(0, -i, 0);
                case 2 -> offsetPos = pos.offset(i, 0, 0);
                case 3 -> offsetPos = pos.offset(-i, 0, 0);
                case 4 -> offsetPos = pos.offset(0, 0, i);
                case 5 -> offsetPos = pos.offset(0, 0, -i);
            }

            if (level.isEmptyBlock(offsetPos)) {
                level.setBlock(offsetPos, fleshState, 3);
            } else {
                break;
            }
        }
    }

    /**
     * 生成内部装饰（肉壁、血管、肉柱等）
     */
    private void generateInteriorDecoration(ServerLevel level, BlockPos centerPos, NestSize size, RandomSource random) {
        BlockState fleshState = getFleshBlockState();
        int interiorMin = -size.getInteriorRadius();
        int interiorMax = size.getInteriorRadius();

        // 1. 生成肉壁装饰（墙壁上的血管纹理）
        generateWallVessels(level, centerPos, interiorMin, interiorMax, random);

        // 2. 生成肉柱（支撑结构）
        generateFleshPillars(level, centerPos, size, random);

        // 3. 生成地面的肉瘤和肉垫
        generateFloorDetails(level, centerPos, interiorMin, interiorMax, random);

        // 4. 生成天花板的垂吊触手
        generateCeilingTentacles(level, centerPos, interiorMin, interiorMax, random);
    }

    /**
     * 生成墙壁血管纹理
     */
    private void generateWallVessels(ServerLevel level, BlockPos centerPos, int min, int max, RandomSource random) {
        BlockState fleshState = getFleshBlockState();

        for (int x = min; x <= max; x++) {
            for (int y = min; y <= max; y++) {
                for (int z = min; z <= max; z++) {
                    // 只在墙壁边缘生成（x或z达到边界）
                    if ((Math.abs(x) == max || Math.abs(z) == max) && Math.abs(y) < max) {
                        // 随机生成血管状线条（用肉块模拟）
                        if (random.nextDouble() < 0.3) {
                            BlockPos pos = centerPos.offset(x, y, z);
                            level.setBlock(pos, fleshState, 3);
                        }
                    }
                }
            }
        }
    }

    /**
     * 生成肉柱（支撑内部空间的柱子）
     */
    private void generateFleshPillars(ServerLevel level, BlockPos centerPos, NestSize size, RandomSource random) {
        BlockState fleshState = getFleshBlockState();
        int interiorRadius = size.getInteriorRadius();
        int pillarCount = size == NestSize.SMALL ? 2 : (size == NestSize.MEDIUM ? 4 : 6);

        for (int i = 0; i < pillarCount; i++) {
            // 随机柱子位置（不挡路）
            double angle = (2 * Math.PI * i) / pillarCount + random.nextDouble() * 0.5;
            int pillarX = (int)(Math.cos(angle) * (interiorRadius - 2));
            int pillarZ = (int)(Math.sin(angle) * (interiorRadius - 2));

            // 从地面到天花板
            for (int y = -interiorRadius; y <= interiorRadius; y++) {
                BlockPos pillarPos = centerPos.offset(pillarX, y, pillarZ);

                // 柱子宽度：3x3
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dz == 0) continue;
                        BlockPos pos = pillarPos.offset(dx, 0, dz);
                        if (Math.abs(pos.getX() - centerPos.getX()) <= interiorRadius &&
                                Math.abs(pos.getZ() - centerPos.getZ()) <= interiorRadius) {
                            level.setBlock(pos, fleshState, 3);
                        }
                    }
                }
                level.setBlock(pillarPos, fleshState, 3);
            }
        }
    }

    /**
     * 生成地面细节（肉瘤、肉垫等）
     */
    private void generateFloorDetails(ServerLevel level, BlockPos centerPos, int min, int max, RandomSource random) {
        BlockState fleshState = getFleshBlockState();
        int floorY = -max;

        for (int x = min; x <= max; x++) {
            for (int z = min; z <= max; z++) {
                BlockPos floorPos = centerPos.offset(x, floorY, z);
                level.setBlock(floorPos, fleshState, 3);

                // 添加肉瘤
                if (random.nextDouble() < 0.15) {
                    BlockPos lumpPos = floorPos.above();
                    level.setBlock(lumpPos, fleshState, 3);

                    // 更大的肉瘤
                    if (random.nextDouble() < 0.3) {
                        level.setBlock(lumpPos.above(), fleshState, 3);
                    }
                }
            }
        }
    }

    /**
     * 生成天花板垂吊触手
     */
    private void generateCeilingTentacles(ServerLevel level, BlockPos centerPos, int min, int max, RandomSource random) {
        BlockState fleshState = getFleshBlockState();
        int ceilingY = max;

        for (int x = min; x <= max; x++) {
            for (int z = min; z <= max; z++) {
                if (random.nextDouble() < 0.2) {
                    BlockPos ceilingPos = centerPos.offset(x, ceilingY, z);
                    level.setBlock(ceilingPos, fleshState, 3);

                    // 向下延伸触手
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
    }

    /**
     * 生成尸巢入口
     */
    private void generateEntrance(ServerLevel level, BlockPos centerPos, NestSize size, RandomSource random) {
        int outerRadius = size.getOuterRadius();

        // 随机选择一面墙作为入口
        int side = random.nextInt(4);
        int entranceWidth = size == NestSize.SMALL ? 2 : (size == NestSize.MEDIUM ? 3 : 4);
        int entranceHeight = size == NestSize.SMALL ? 3 : (size == NestSize.MEDIUM ? 4 : 5);

        BlockPos entrancePos = centerPos;

        switch (side) {
            case 0 -> { // 北面
                entrancePos = centerPos.offset(0, 0, -outerRadius);
                for (int x = -entranceWidth/2; x <= entranceWidth/2; x++) {
                    for (int y = -entranceHeight/2; y <= entranceHeight/2; y++) {
                        BlockPos pos = entrancePos.offset(x, y, 0);
                        level.destroyBlock(pos, false);
                    }
                }
            }
            case 1 -> { // 南面
                entrancePos = centerPos.offset(0, 0, outerRadius);
                for (int x = -entranceWidth/2; x <= entranceWidth/2; x++) {
                    for (int y = -entranceHeight/2; y <= entranceHeight/2; y++) {
                        BlockPos pos = entrancePos.offset(x, y, 0);
                        level.destroyBlock(pos, false);
                    }
                }
            }
            case 2 -> { // 西面
                entrancePos = centerPos.offset(-outerRadius, 0, 0);
                for (int z = -entranceWidth/2; z <= entranceWidth/2; z++) {
                    for (int y = -entranceHeight/2; y <= entranceHeight/2; y++) {
                        BlockPos pos = entrancePos.offset(0, y, z);
                        level.destroyBlock(pos, false);
                    }
                }
            }
            case 3 -> { // 东面
                entrancePos = centerPos.offset(outerRadius, 0, 0);
                for (int z = -entranceWidth/2; z <= entranceWidth/2; z++) {
                    for (int y = -entranceHeight/2; y <= entranceHeight/2; y++) {
                        BlockPos pos = entrancePos.offset(0, y, z);
                        level.destroyBlock(pos, false);
                    }
                }
            }
        }

        CorpseOrigin.LOGGER.info("生成尸巢入口在侧面: {}", side);
    }

    /**
     * 生成尸巢内部氛围特效
     */
    private void generateNestAtmosphere(ServerLevel level, BlockPos centerPos, NestSize size) {
        int interiorRadius = size.getInteriorRadius();

        // 生成红色迷雾粒子效果
        for (int i = 0; i < 100; i++) {
            double x = centerPos.getX() + (Math.random() - 0.5) * interiorRadius * 2;
            double y = centerPos.getY() + (Math.random() - 0.5) * interiorRadius * 2;
            double z = centerPos.getZ() + (Math.random() - 0.5) * interiorRadius * 2;

            level.sendParticles(
                    ParticleTypes.CRIMSON_SPORE,
                    x, y, z,
                    1, 0.1, 0.1, 0.1, 0
            );
        }

        // 播放尸巢生成音效
        level.playSound(
                null, centerPos.getX(), centerPos.getY(), centerPos.getZ(),
                SoundEvents.WITHER_SPAWN,
                SoundSource.BLOCKS, 1.0f, 0.8f
        );
    }

    /**
     * 获取肉块方块状态
     */
    private BlockState getFleshBlockState() {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("corpseorigin", "zbr_flesh")
        ).defaultBlockState();
    }

    /**
     * 实体进入时的处理（贡献击杀数）
     */
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (entity instanceof LowerLevelZbEntity zbEntity) {
            if (blockEntity instanceof ZBRFleshBlockEntity fleshEntity) {
                fleshEntity.addKills();
                CorpseOrigin.LOGGER.info("尸兄贡献击杀数，当前击杀数: {}", fleshEntity.getKills());

                // 播放血肉吞噬音效
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.playSound(
                            null, pos.getX(), pos.getY(), pos.getZ(),
                            SoundEvents.SLIME_SQUISH,
                            SoundSource.BLOCKS, 1.0f, 0.5f
                    );
                }
            }
        }

        super.entityInside(state, level, pos, entity);
    }

    /**
     * 反击机制（防御入侵者）
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity instanceof ZBRFleshBlockEntity fleshEntity && fleshEntity.getOwner() != null) {
            if (level instanceof ServerLevel serverLevel) {
                // 检查破坏者是否是主人
                java.util.List<Player> players = level.getEntitiesOfClass(
                        Player.class,
                        new AABB(pos).inflate(5)
                );

                if (!players.isEmpty()) {
                    Player player = players.get(0);
                    if (!player.getUUID().equals(fleshEntity.getOwner())) {
                        counterAttack(serverLevel, pos, fleshEntity.getOwner(), player);
                    }
                }
            }
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }

    /**
     * 反击机制 - 召唤尸兄奴仆
     */
    private void counterAttack(ServerLevel level, BlockPos pos, java.util.UUID ownerUUID, Player attacker) {
        try {
            AABB searchBox = new AABB(pos).inflate(32);
            java.util.List<LowerLevelZbEntity> existingZombies = level.getEntitiesOfClass(
                    LowerLevelZbEntity.class, searchBox
            );

            // 命令周围尸兄攻击
            for (LowerLevelZbEntity zombie : existingZombies) {
                zombie.setTarget(attacker);
            }

            // 生成援军
            if (existingZombies.size() < 5) {
                net.minecraft.world.entity.EntityType<LowerLevelZbEntity> zombieType = EntityRegistry.LOWER_LEVEL_ZB.get();
                for (int i = 0; i < 3; i++) {
                    double spawnX = pos.getX() + (Math.random() - 0.5) * 8;
                    double spawnY = pos.getY() + 1;
                    double spawnZ = pos.getZ() + (Math.random() - 0.5) * 8;

                    LowerLevelZbEntity zombie = zombieType.create(level);
                    if (zombie != null) {
                        zombie.moveTo(spawnX, spawnY, spawnZ, 0, 0);
                        zombie.setTarget(attacker);
                        level.addFreshEntity(zombie);

                        // 特效
                        level.sendParticles(ParticleTypes.DRAGON_BREATH,
                                spawnX, spawnY, spawnZ, 10, 0, 0, 0, 0);
                    }
                }
            }

            // 警告音效
            level.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                    SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 2.0f, 0.6f);

            CorpseOrigin.LOGGER.info("尸巢被入侵，尸兄奴仆反击！");
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("反击失败", e);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ZBRFleshBlockEntity(pos, state);
    }

    /**
     * 尸巢尺寸枚举
     */
    private enum NestSize {
        SMALL(3, 3, 1),   // 外部半径7，内部半径3，厚度2
        MEDIUM(5, 5, 2),  // 外部半径11，内部半径5，厚度3
        LARGE(7, 7, 3);   // 外部半径15，内部半径7，厚度4

        private final int interiorRadius;
        private final int outerRadius;
        private final int wallThickness;

        NestSize(int interiorRadius, int outerRadius, int wallThickness) {
            this.interiorRadius = interiorRadius;
            this.outerRadius = outerRadius;
            this.wallThickness = wallThickness;
        }

        public int getInteriorRadius() { return interiorRadius; }
        public int getOuterRadius() { return outerRadius; }
        public int getWallThickness() { return wallThickness; }
    }
}