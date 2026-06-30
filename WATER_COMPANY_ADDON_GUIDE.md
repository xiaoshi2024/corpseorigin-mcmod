# 《尸兄：饮水公司》扩展模组开发手册

## 目录

1. [概述](#1-概述)
2. [模组架构](#2-模组架构)
3. [API接口详解](#3-api接口详解)
4. [自定义尸水流体](#4-自定义尸水流体)
5. [管道系统](#5-管道系统)
6. [村庄水槽系统](#6-村庄水槽系统)
7. [村民口渴系统](#7-村民口渴系统)
8. [矿泉水（龙氏）](#8-矿泉水龙氏)
9. [完整示例代码](#9-完整示例代码)
10. [调试与测试](#10-调试与测试)

---

## 1. 概述

《尸兄：饮水公司》是 `corpseorigin` 模组的扩展模组，主要功能：

- **管道系统**：抽取和传输污染的尸水
- **村庄水槽**：村庄内自动生成的水源容器
- **村民口渴**：村民自动饮水机制
- **矿泉水（龙氏）**：安全水源物品

**核心设计理念**：通过 `WaterCompanyAPI` 接口与本体模组解耦，实现两种模式的无缝切换：

| 模式 | 描述 |
|------|------|
| **无扩展模组** | 使用普通水（`Fluids.WATER`），通过 `InfectionData` 判断污染状态 |
| **有扩展模组** | 使用自定义尸水流体（`CorpseWaterFluid`），管道抽取时自动转换 |

---

## 2. 模组架构

```
corpsewatercompany/
├── src/main/java/com/phagens/corpsewatercompany/
│   ├── CorpseWaterCompany.java          # 主类
│   ├── api/                             # API桥接层
│   │   └── CorpseOriginBridge.java      # 调用本体API的工具类
│   ├── fluid/                           # 自定义流体
│   │   ├── CorpseWaterFluid.java        # 尸水流体类
│   │   ├── CorpseWaterBlock.java        # 尸水方块
│   │   └── FluidRegistry.java           # 流体注册
│   ├── pipe/                            # 管道系统
│   │   ├── PipeBlock.java               # 管道方块
│   │   ├── PipeBlockEntity.java         # 管道方块实体
│   │   └── PipeRenderer.java            # 管道渲染
│   ├── village/                         # 村庄系统
│   │   ├── VillageWaterTroughBlock.java # 水槽方块
│   │   ├── VillageWaterTroughBlockEntity.java
│   │   ├── VillagerThirstGoal.java      # 村民口渴AI
│   │   └── VillageStructureInjector.java # 村庄结构注入
│   ├── item/                            # 物品
│   │   ├── LongShiMineralWater.java     # 矿泉水（龙氏）
│   │   └── CorpseWaterBucket.java       # 尸水桶（扩展版）
│   └── event/                           # 事件处理
│       └── WaterPollutionEventHandler.java
└── src/main/resources/
    ├── assets/corpsewatercompany/
    │   ├── lang/
    │   │   ├── en_us.json
    │   │   └── zh_cn.json
    │   ├── models/
    │   │   ├── block/
    │   │   └── item/
    │   └── textures/
    │       ├── block/
    │       └── item/
    └── data/corpsewatercompany/
        ├── recipes/
        └── tags/
```

---

## 3. API接口详解

### 3.1 接口总览

| 接口 | 路径 | 用途 |
|------|------|------|
| `IWaterPollutionProvider` | `com.phagens.corpseorigin.api.watercompany` | 查询水源污染状态 |
| `IInfectionTrigger` | `com.phagens.corpseorigin.api.watercompany` | 触发实体感染 |
| `ICorpseWaterHandler` | `com.phagens.corpseorigin.api.watercompany` | 尸水流体交互（核心） |
| `WaterCompanyAPI` | `com.phagens.corpseorigin.api.watercompany` | 统一入口 |

### 3.2 IWaterPollutionProvider

```java
// 获取接口实例
IWaterPollutionProvider provider = WaterCompanyAPI.getPollutionProvider();

// 判断位置水是否被污染（生物群系污染 + 七星棺材感染）
boolean isPolluted = provider.isWaterPolluted(level, pos);

// 获取污染等级（0-15）
int level = provider.getPollutionLevel(level, pos);

// 判断是否在死寂生物群系
boolean isBiomePolluted = provider.isBiomePolluted(level, pos);

// 判断是否被七星棺材明确感染
boolean isExplicitlyInfected = provider.isWaterExplicitlyInfected(level, pos);

// 标记/清除污染
provider.markWaterAsPolluted(level, pos);
provider.markWaterAsClean(level, pos);
```

### 3.3 IInfectionTrigger

```java
// 获取接口实例
IInfectionTrigger trigger = WaterCompanyAPI.getInfectionTrigger();

// 触发感染（多种重载）
trigger.triggerInfection(villager, level);                  // 默认时长
trigger.triggerInfection(villager, level, 400);             // 指定时长（20秒）
trigger.triggerInfection(villager, level, sourceUUID);      // 指定来源
trigger.triggerInfection(villager, level, 400, sourceUUID); // 完整参数

// 查询感染状态
boolean canInfect = trigger.canInfect(villager);
boolean isInfected = trigger.isInfected(villager);

// 移除感染
trigger.removeInfection(villager);
```

### 3.4 ICorpseWaterHandler（核心桥接）

```java
// 获取接口实例
ICorpseWaterHandler handler = WaterCompanyAPI.getCorpseWaterHandler();

// 获取尸水流体（扩展模组替换后返回自定义流体）
Fluid corpseWater = handler.getCorpseWaterFluid();

// 判断是否为尸水
boolean isCorpseWater = handler.isCorpseWater(fluid);

// 创建尸水物品
ItemStack bottle = handler.createCorpseWaterBottle();
ItemStack bucket = handler.createCorpseWaterBucket();

// 转换水状态
handler.convertWaterToCorpseWater(level, pos);
handler.convertCorpseWaterToCleanWater(level, pos);

// 管道抽取桥接方法（核心）
// 如果位置水源被感染 → 返回 CorpseWaterFluid 的 FluidStack
// 如果位置水源未感染 → 返回普通水的 FluidStack
FluidStack extracted = handler.extractFluidFromPosition(level, pos, 1000);

// 判断位置是否为尸水源头
boolean isSource = handler.isPositionCorpseWaterSource(level, pos);
```

### 3.5 替换默认实现

扩展模组需要在初始化时替换默认实现：

```java
// 在扩展模组主类的构造函数或 commonSetup 中
public class CorpseWaterCompany {
    
    public CorpseWaterCompany(IEventBus modEventBus) {
        // ... 注册流程 ...
        
        // 替换 ICorpseWaterHandler 实现
        WaterCompanyAPI.setCorpseWaterHandler(new CustomCorpseWaterHandler());
        
        // 可选：替换其他接口实现
        // WaterCompanyAPI.setPollutionProvider(new CustomPollutionProvider());
        // WaterCompanyAPI.setInfectionTrigger(new CustomInfectionTrigger());
    }
}
```

---

## 4. 自定义尸水流体

### 4.1 流体类型注册

```java
public class FluidRegistry {
    
    public static final DeferredRegister<FluidType> FLUID_TYPES = 
        DeferredRegister.create(Registries.FLUID_TYPE, "corpsewatercompany");
    
    public static final DeferredRegister<Fluid> FLUIDS = 
        DeferredRegister.create(Registries.FLUID, "corpsewatercompany");
    
    // 尸水流体类型
    public static final DeferredHolder<FluidType, FluidType> CORPSE_WATER_TYPE = 
        FLUID_TYPES.register("corpse_water", () -> new BaseFluidType(
            FluidType.Properties.create()
                .descriptionId("fluid.corpsewatercompany.corpse_water")
                .color(0x8B0000)           // 暗红色
                .lightLevel(0)
                .viscosity(1000)
                .density(1000)
                .temperature(300)
                .canConvertToSource(true)
                .supportsBoating(true)
                .fallDistanceModifier(0.5f)
                .pushEntity(true)
        ));
    
    // 尸水（源）
    public static final DeferredHolder<Fluid, FlowingFluid> CORPSE_WATER = 
        FLUIDS.register("corpse_water", () -> new CorpseWaterFluid.Source());
    
    // 流动尸水
    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_CORPSE_WATER = 
        FLUIDS.register("flowing_corpse_water", () -> new CorpseWaterFluid.Flowing());
}
```

### 4.2 流体类实现

```java
public abstract class CorpseWaterFluid extends FlowingFluid {
    
    @Override
    public Fluid getSource() {
        return FluidRegistry.CORPSE_WATER.get();
    }
    
    @Override
    public Fluid getFlowing() {
        return FluidRegistry.FLOWING_CORPSE_WATER.get();
    }
    
    @Override
    public Item getBucket() {
        return ItemRegistry.CORPSE_WATER_BUCKET.get();
    }
    
    @Override
    protected boolean canConvertToSource() {
        return false;
    }
    
    @Override
    protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
        BlockEntity blockentity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        Block.dropResources(state, level, pos, blockentity);
    }
    
    @Override
    public int getSlopeFindDistance(LevelReader level) {
        return 4;
    }
    
    @Override
    public int getDropOff(LevelReader level) {
        return 1;
    }
    
    @Override
    public void entityInside(FluidState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (entity instanceof LivingEntity living && !level.isClientSide()) {
            // 通过 API 触发感染
            WaterCompanyAPI.getInfectionTrigger().triggerInfection(living, (ServerLevel) level);
        }
    }
    
    @Override
    protected BlockState createLegacyBlock(FluidState state) {
        return BlockRegistry.CORPSE_WATER_BLOCK.get().defaultBlockState()
            .setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
    }
    
    @Override
    public boolean isSame(Fluid fluid) {
        return fluid == FluidRegistry.CORPSE_WATER.get() || 
               fluid == FluidRegistry.FLOWING_CORPSE_WATER.get();
    }
    
    @Override
    public FluidType getFluidType() {
        return FluidRegistry.CORPSE_WATER_TYPE.get();
    }
    
    // 源流体
    public static class Source extends CorpseWaterFluid {
        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }
        
        @Override
        public int getAmount(FluidState state) {
            return 8;
        }
        
        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }
    
    // 流动流体
    public static class Flowing extends CorpseWaterFluid {
        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }
        
        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }
        
        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }
}
```

### 4.3 尸水方块

```java
public class CorpseWaterBlock extends LiquidBlock {
    
    public CorpseWaterBlock(Fluid fluid, Properties properties) {
        super(fluid, properties);
    }
}
```

### 4.4 自定义 ICorpseWaterHandler 实现

```java
public class CustomCorpseWaterHandler implements ICorpseWaterHandler {
    
    @Override
    public Fluid getCorpseWaterFluid() {
        return FluidRegistry.CORPSE_WATER.get();
    }
    
    @Override
    public boolean isCorpseWater(Fluid fluid) {
        return fluid == FluidRegistry.CORPSE_WATER.get() || 
               fluid == FluidRegistry.FLOWING_CORPSE_WATER.get();
    }
    
    @Override
    public ItemStack createCorpseWaterBottle() {
        // 扩展模组可以使用自己的尸水瓶物品
        return new ItemStack(ItemRegistry.CORPSE_WATER_BOTTLE.get());
    }
    
    @Override
    public ItemStack createCorpseWaterBucket() {
        return new ItemStack(ItemRegistry.CORPSE_WATER_BUCKET.get());
    }
    
    @Override
    public boolean canBeConvertedToCorpseWater(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getFluidState().is(Fluids.WATER);
    }
    
    @Override
    public void convertWaterToCorpseWater(ServerLevel level, BlockPos pos) {
        // 使用本体API标记感染
        WaterCompanyAPI.getPollutionProvider().markWaterAsPolluted(level, pos);
        // 同时替换方块为自定义尸水
        level.setBlock(pos, BlockRegistry.CORPSE_WATER_BLOCK.get().defaultBlockState(), 3);
    }
    
    @Override
    public void convertCorpseWaterToCleanWater(ServerLevel level, BlockPos pos) {
        WaterCompanyAPI.getPollutionProvider().markWaterAsClean(level, pos);
        level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
    }
    
    @Override
    public int getCorpseWaterEnergy(ServerLevel level, BlockPos pos) {
        return WaterCompanyAPI.getPollutionProvider().getPollutionLevel(level, pos);
    }
    
    @Override
    public void setCorpseWaterEnergy(ServerLevel level, BlockPos pos, int energy) {
        // 委托给本体API
        InfectionData.get(level).setWaterEnergy(pos, energy);
    }
    
    @Override
    public int getMaxCorpseWaterEnergy() {
        return 15;
    }
    
    @Override
    public FluidStack extractFluidFromPosition(ServerLevel level, BlockPos pos, int amount) {
        if (isPositionCorpseWaterSource(level, pos)) {
            return new FluidStack(FluidRegistry.CORPSE_WATER.get(), amount);
        }
        return new FluidStack(Fluids.WATER, amount);
    }
    
    @Override
    public boolean isPositionCorpseWaterSource(ServerLevel level, BlockPos pos) {
        return WaterCompanyAPI.getPollutionProvider().isWaterPolluted(level, pos);
    }
}
```

---

## 5. 管道系统

### 5.1 管道方块

```java
public class PipeBlock extends Block {
    
    public PipeBlock(Properties properties) {
        super(properties);
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PipeBlockEntity(pos, state);
    }
    
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, 
                                Block neighborBlock, BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, isMoving);
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PipeBlockEntity pipe) {
                pipe.updateConnections();
            }
        }
    }
}
```

### 5.2 管道方块实体（核心）

```java
public class PipeBlockEntity extends BlockEntity {
    
    private final FluidStorageHandler fluidStorage;
    private int transferRate = 100; // 每tick传输量
    
    public PipeBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.PIPE_BLOCK_ENTITY.get(), pos, state);
        this.fluidStorage = new FluidStorageHandler(1000) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
    }
    
    // 注册流体能力
    @Override
    public <T> T getCapability(Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return ForgeCapabilities.FLUID_HANDLER.cast(fluidStorage);
        }
        return super.getCapability(cap, side);
    }
    
    // 管道传输逻辑
    public void tick() {
        if (level == null || level.isClientSide()) return;
        
        // 如果存储中有流体，尝试向相邻管道传输
        if (!fluidStorage.isEmpty()) {
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = worldPosition.relative(dir);
                BlockEntity neighborBe = level.getBlockEntity(neighborPos);
                
                if (neighborBe instanceof PipeBlockEntity neighborPipe) {
                    FluidStack toTransfer = fluidStorage.drain(
                        transferRate, IFluidHandler.FluidAction.SIMULATE);
                    
                    if (!toTransfer.isEmpty()) {
                        int filled = neighborPipe.fluidStorage.fill(
                            toTransfer, IFluidHandler.FluidAction.EXECUTE);
                        if (filled > 0) {
                            fluidStorage.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                        }
                    }
                }
            }
        }
    }
    
    // 从水源抽取流体
    public void extractFromSource(BlockPos sourcePos) {
        if (level == null || level.isClientSide()) return;
        
        // 使用本体API的桥接方法
        FluidStack extracted = WaterCompanyAPI.getCorpseWaterHandler()
            .extractFluidFromPosition((ServerLevel) level, sourcePos, transferRate);
        
        if (!extracted.isEmpty()) {
            fluidStorage.fill(extracted, IFluidHandler.FluidAction.EXECUTE);
        }
    }
    
    public void updateConnections() {
        // 更新管道连接状态（用于渲染）
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put("Fluid", fluidStorage.writeToNBT(new CompoundTag()));
    }
    
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        fluidStorage.readFromNBT(tag.getCompound("Fluid"));
    }
}
```

### 5.3 管道能力注册

在主类中注册管道的流体能力：

```java
public class CorpseWaterCompany {
    
    public CorpseWaterCompany(IEventBus modEventBus) {
        // ...
        
        modEventBus.addListener(this::registerCapabilities);
    }
    
    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
            BlockEntityRegistry.PIPE_BLOCK_ENTITY.get(),
            PipeBlockEntity::new,
            (be, context) -> be.getCapability(context)
        );
    }
}
```

---

## 6. 村庄水槽系统

### 6.1 水槽方块

```java
public class VillageWaterTroughBlock extends Block {
    
    public VillageWaterTroughBlock(Properties properties) {
        super(properties);
    }
    
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VillageWaterTroughBlockEntity(pos, state);
    }
    
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, 
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof VillageWaterTroughBlockEntity trough) {
            ItemStack held = player.getItemInHand(hand);
            
            // 用桶取水
            if (held.getItem() == Items.BUCKET) {
                if (!trough.getFluidStorage().isEmpty()) {
                    FluidStack drained = trough.getFluidStorage().drain(
                        1000, IFluidHandler.FluidAction.EXECUTE);
                    
                    if (!drained.isEmpty()) {
                        ItemStack bucketItem;
                        if (WaterCompanyAPI.getCorpseWaterHandler().isCorpseWater(drained.getFluid())) {
                            bucketItem = new ItemStack(ItemRegistry.CORPSE_WATER_BUCKET.get());
                        } else {
                            bucketItem = new ItemStack(Items.WATER_BUCKET);
                        }
                        
                        if (held.getCount() == 1) {
                            player.setItemInHand(hand, bucketItem);
                        } else {
                            held.shrink(1);
                            if (!player.getInventory().add(bucketItem)) {
                                player.drop(bucketItem, false);
                            }
                        }
                        return InteractionResult.SUCCESS;
                    }
                }
            }
        }
        return InteractionResult.PASS;
    }
}
```

### 6.2 水槽方块实体

```java
public class VillageWaterTroughBlockEntity extends BlockEntity {
    
    private final FluidStorageHandler fluidStorage;
    private int lastPollutionCheckTick = 0;
    
    public VillageWaterTroughBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.WATER_TROUGH_BLOCK_ENTITY.get(), pos, state);
        this.fluidStorage = new FluidStorageHandler(4000) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
        // 初始化时填充普通水
        fluidStorage.fill(new FluidStack(Fluids.WATER, 4000), IFluidHandler.FluidAction.EXECUTE);
    }
    
    @Override
    public <T> T getCapability(Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return ForgeCapabilities.FLUID_HANDLER.cast(fluidStorage);
        }
        return super.getCapability(cap, side);
    }
    
    public void tick() {
        if (level == null || level.isClientSide()) return;
        
        // 定期检查水质污染
        if (level.getGameTime() - lastPollutionCheckTick > 200) { // 每10秒检查一次
            lastPollutionCheckTick = level.getGameTime();
            checkWaterQuality();
        }
    }
    
    private void checkWaterQuality() {
        // 检查水槽是否被污染（通过管道流入的尸水）
        if (!fluidStorage.isEmpty()) {
            Fluid fluid = fluidStorage.getFluidStack().getFluid();
            if (WaterCompanyAPI.getCorpseWaterHandler().isCorpseWater(fluid)) {
                // 水槽中的水是尸水，标记附近村民可能会被感染
            }
        }
    }
    
    public FluidStorageHandler getFluidStorage() {
        return fluidStorage;
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put("Fluid", fluidStorage.writeToNBT(new CompoundTag()));
    }
    
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        fluidStorage.readFromNBT(tag.getCompound("Fluid"));
    }
}
```

### 6.3 村庄结构注入

```java
@SubscribeEvent
public void onStructurePoolAddPiece(StructurePoolAddPieceEvent event) {
    // 只在村庄结构中注入
    if (event.getStructurePool().getKey().location().getNamespace().equals("minecraft") &&
        event.getStructurePool().getKey().location().getPath().contains("village")) {
        
        // 添加水槽结构部件
        event.getPieces().add(new SinglePoolElement(
            new ResourceLocation("corpsewatercompany:village/water_trough"),
            StructureTemplatePool.Projection.RIGID
        ));
    }
}
```

---

## 7. 村民口渴系统

### 7.1 村民口渴AI Goal

```java
public class VillagerThirstGoal extends Goal {
    
    private final Villager villager;
    private final ServerLevel level;
    private BlockPos targetTrough;
    private int thirstLevel = 0; // 0-100
    private int drinkCooldown = 0;
    
    public VillagerThirstGoal(Villager villager) {
        this.villager = villager;
        this.level = (ServerLevel) villager.level();
    }
    
    @Override
    public boolean canUse() {
        if (drinkCooldown > 0) {
            drinkCooldown--;
            return false;
        }
        
        // 村民有一定概率感到口渴
        if (level.getRandom().nextInt(1000) != 0) {
            return false;
        }
        
        // 寻找附近的水槽
        targetTrough = findNearbyTrough();
        return targetTrough != null;
    }
    
    @Override
    public void start() {
        if (targetTrough != null) {
            villager.getNavigation().moveTo(targetTrough.getX(), targetTrough.getY(), targetTrough.getZ(), 1.0);
        }
    }
    
    @Override
    public boolean canContinueToUse() {
        return targetTrough != null && 
               !villager.getNavigation().isDone() &&
               villager.distanceToSqr(Vec3.atCenterOf(targetTrough)) > 2.0;
    }
    
    @Override
    public void tick() {
        if (targetTrough != null && 
            villager.distanceToSqr(Vec3.atCenterOf(targetTrough)) <= 2.0) {
            
            drinkFromTrough();
        }
    }
    
    private BlockPos findNearbyTrough() {
        // 在16格范围内寻找水槽
        return BlockPos.findClosestMatch(
            villager.blockPosition(),
            16,
            4,
            pos -> level.getBlockState(pos).is(BlockRegistry.WATER_TROUGH_BLOCK.get())
        ).orElse(null);
    }
    
    private void drinkFromTrough() {
        BlockEntity be = level.getBlockEntity(targetTrough);
        if (be instanceof VillageWaterTroughBlockEntity trough) {
            FluidStorageHandler storage = trough.getFluidStorage();
            
            if (!storage.isEmpty()) {
                FluidStack fluid = storage.getFluidStack();
                
                // 消耗100mB水
                storage.drain(100, IFluidHandler.FluidAction.EXECUTE);
                
                // 判断水是否被污染
                if (WaterCompanyAPI.getCorpseWaterHandler().isCorpseWater(fluid.getFluid())) {
                    // 喝了尸水，触发感染
                    WaterCompanyAPI.getInfectionTrigger().triggerInfection(villager, level);
                } else {
                    // 喝了干净水，降低口渴值
                    thirstLevel = Math.max(0, thirstLevel - 20);
                }
                
                drinkCooldown = 100; // 5秒冷却
                targetTrough = null;
            }
        }
    }
}
```

### 7.2 注入村民AI

```java
@SubscribeEvent
public void onEntityJoinLevel(EntityJoinLevelEvent event) {
    if (event.getEntity() instanceof Villager villager && !event.getLevel().isClientSide()) {
        // 在村民AI目标列表中添加口渴目标
        // 优先级设为中等，确保村民在空闲时才会去喝水
        villager.goalSelector.addGoal(2, new VillagerThirstGoal(villager));
    }
}
```

---

## 8. 矿泉水（龙氏）

### 8.1 物品实现

```java
public class LongShiMineralWater extends Item {
    
    public LongShiMineralWater(Properties properties) {
        super(properties);
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (!level.isClientSide()) {
            // 饮用效果
            player.removeEffect(EffectRegister.QIANS); // 清除感染
            
            // 添加临时增益
            player.addEffect(new MobEffectInstance(
                MobEffects.REGENERATION,
                200,  // 10秒
                1,
                false,
                true
            ));
            
            player.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_RESISTANCE,
                400,  // 20秒
                0,
                false,
                true
            ));
            
            // 消耗物品
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        
        // 播放饮用音效
        player.playSound(SoundEvents.GENERIC_DRINK, 1.0F, 1.0F);
        
        return InteractionResultHolder.success(stack);
    }
}
```

### 8.2 配方（可选）

```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": [
    "W W",
    " W ",
    "W W"
  ],
  "key": {
    "W": {
      "item": "minecraft:glass_bottle"
    }
  },
  "result": {
    "item": "corpsewatercompany:longshi_mineral_water",
    "count": 3
  }
}
```

---

## 9. 完整示例代码

### 9.1 主类

```java
@Mod("corpsewatercompany")
public class CorpseWaterCompany {
    
    public static final String MODID = "corpsewatercompany";
    public static final Logger LOGGER = LogUtils.getLogger();
    
    public CorpseWaterCompany(IEventBus modEventBus) {
        // 注册流体
        FluidRegistry.FLUID_TYPES.register(modEventBus);
        FluidRegistry.FLUIDS.register(modEventBus);
        
        // 注册方块和物品
        BlockRegistry.BLOCKS.register(modEventBus);
        ItemRegistry.ITEMS.register(modEventBus);
        BlockEntityRegistry.BLOCK_ENTITIES.register(modEventBus);
        
        // 注册事件
        NeoForge.EVENT_BUS.register(this);
        
        // 替换本体API实现
        WaterCompanyAPI.setCorpseWaterHandler(new CustomCorpseWaterHandler());
        
        LOGGER.info("CorpseWaterCompany initialized");
    }
    
    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Villager villager && !event.getLevel().isClientSide()) {
            villager.goalSelector.addGoal(2, new VillagerThirstGoal(villager));
        }
    }
    
    @SubscribeEvent
    public void onStructurePoolAddPiece(StructurePoolAddPieceEvent event) {
        // 村庄结构注入逻辑
    }
}
```

### 9.2 注册类示例

```java
public class BlockRegistry {
    
    public static final DeferredRegister<Block> BLOCKS = 
        DeferredRegister.create(Registries.BLOCK, "corpsewatercompany");
    
    public static final DeferredHolder<Block, Block> CORPSE_WATER_BLOCK = 
        BLOCKS.register("corpse_water", () -> new CorpseWaterBlock(
            FluidRegistry.CORPSE_WATER.get(), 
            Block.Properties.of(Material.WATER)
                .noCollission()
                .strength(100.0F)
                .noDrops()
        ));
    
    public static final DeferredHolder<Block, Block> PIPE_BLOCK = 
        BLOCKS.register("pipe", () -> new PipeBlock(
            Block.Properties.of(Material.METAL)
                .strength(2.0F)
                .sound(SoundType.METAL)
        ));
    
    public static final DeferredHolder<Block, Block> WATER_TROUGH_BLOCK = 
        BLOCKS.register("water_trough", () -> new VillageWaterTroughBlock(
            Block.Properties.of(Material.WOOD)
                .strength(1.0F)
                .sound(SoundType.WOOD)
        ));
}
```

---

## 10. 调试与测试

### 10.1 测试流程

1. **测试管道抽取**：
   - 在被感染的水源旁放置管道
   - 使用管道抽取流体，验证返回的是 `CorpseWaterFluid`

2. **测试村民口渴**：
   - 在村庄附近放置水槽
   - 等待村民感到口渴并前往水槽
   - 验证村民饮用污染水后被感染

3. **测试七星棺材污染**：
   - 在村庄水井中放置七星棺材
   - 等待水井被感染
   - 验证管道抽取的是尸水

4. **测试矿泉水**：
   - 给被感染的村民使用矿泉水
   - 验证感染效果被清除

### 10.2 日志调试

在关键位置添加日志：

```java
// 管道抽取时
CorpseWaterCompany.LOGGER.info("Extracted fluid: {} from {}", 
    extracted.getFluid().toString(), pos);

// 村民饮用时
CorpseWaterCompany.LOGGER.info("Villager {} drank {} water", 
    villager.getUUID(), isCorpseWater ? "polluted" : "clean");
```

### 10.3 常见问题

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| 管道无法抽取尸水 | `extractFluidFromPosition` 返回普通水 | 确保水源已被感染（InfectionData有记录） |
| 村民不喝水 | AI优先级问题 | 调整 `addGoal` 的优先级参数 |
| 流体不流动 | 缺少 `LiquidBlock` | 确保尸水有对应的方块注册 |
| 扩展模组无法访问本体API | 缺少依赖 | 在 `mods.toml` 中声明依赖 `corpseorigin` |

---

## 附录：依赖配置

在 `META-INF/neoforge.mods.toml` 中添加：

```toml
[[mods]]
modId = "corpsewatercompany"
version = "1.0.0"
displayName = "Corpse Water Company"
authors = ["Your Name"]
description = "扩展模组：尸兄饮水公司"

[[dependencies.corpsewatercompany]]
modId = "neoforge"
versionRange = "[21.1,)"
ordering = "NONE"
side = "BOTH"

[[dependencies.corpsewatercompany]]
modId = "minecraft"
versionRange = "[1.21.1,)"
ordering = "NONE"
side = "BOTH"

[[dependencies.corpsewatercompany]]
modId = "corpseorigin"
versionRange = "[1.0.0,)"
ordering = "NONE"
side = "BOTH"
```

---

**文档版本**: 1.0  
**适用版本**: Minecraft 1.21.1 / NeoForge  
**依赖模组**: corpseorigin

---

*本文档基于 corpseorigin 模组的 WaterCompanyAPI 接口编写*
