package xiaoshi2022.corpseorigin.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.block.entity.CNChessZbrsBlockEntity;
import xiaoshi2022.corpseorigin.block.entity.CloneChamberBlockEntity;
import xiaoshi2022.corpseorigin.block.entity.MosquitoEggsBlockEntity;
import xiaoshi2022.corpseorigin.block.entity.QiXingGuanBlockEntity;
import xiaoshi2022.corpseorigin.block.entity.ZBRFleshBlockEntity;

import java.util.Set;

public final class ModBlockEntities {

    // 26.2 已移除 BlockEntityType.Builder，直接 new：参数为 构造器引用 + 关联方块集合
    public static final BlockEntityType<CloneChamberBlockEntity> CLONE_CHAMBER =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    CorpseOrigin.id("clone_chamber"),
                    new BlockEntityType<>(CloneChamberBlockEntity::new, Set.of(ModBlocks.CLONE_CHAMBER))
            );

    /** 尸兄肉块（GeckoLib 动画方块实体） */
    public static final BlockEntityType<ZBRFleshBlockEntity> ZBR_FLESH =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    CorpseOrigin.id("zbr_flesh"),
                    new BlockEntityType<>(ZBRFleshBlockEntity::new, Set.of(ModBlocks.ZBR_FLESH))
            );

    /** 象棋尸兄（GeckoLib 动画方块实体） */
    public static final BlockEntityType<CNChessZbrsBlockEntity> CN_CHESS_ZBRS =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    CorpseOrigin.id("cn_chess_zbrs"),
                    new BlockEntityType<>(CNChessZbrsBlockEntity::new, Set.of(ModBlocks.CN_CHESS_ZBRS))
            );

    /** 蚊子尸兄卵（GeckoLib 动画方块实体：idle / wriggle / incubate 按 age 分段） */
    public static final BlockEntityType<MosquitoEggsBlockEntity> MOSQUITO_ZBR_EGGS =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    CorpseOrigin.id("mosquito_zbr_eggs"),
                    new BlockEntityType<>(MosquitoEggsBlockEntity::new, Set.of(ModBlocks.MOSQUITO_ZBR_EGGS))
            );

    /** 七星棺（GeckoLib 动画方块实体：idle / open） */
    public static final BlockEntityType<QiXingGuanBlockEntity> QI_XING_GUAN =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    CorpseOrigin.id("qi_xing_guan"),
                    new BlockEntityType<>(QiXingGuanBlockEntity::new, Set.of(ModBlocks.QI_XING_GUAN))
            );

    private ModBlockEntities() {
    }

    public static void init() {
        CorpseOrigin.LOGGER.info("CorpseOrigin block entities registered");
    }
}
