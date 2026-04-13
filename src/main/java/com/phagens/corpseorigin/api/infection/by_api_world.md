# 尸兄模组感染API文档

## 概述

尸兄模组现在提供了完整的感染系统API，允许外部模组注册自定义的感染逻辑。这使得任何模组的生物都可以被感染为尸兄，或者被感染为其他自定义实体。

## 主要功能

1. **实体感染注册系统** - 简单的实体类型映射
2. **高级感染API** - 完全自定义的感染处理器
3. **感染事件系统** - 监听和响应感染过程中的各个阶段
4. **默认感染处理器** - 为未注册的实体提供默认行为

## 快速开始

### 方法1：使用实体感染注册系统（简单）

最简单的方式是使用 `EntityInfectionRegistry`，它提供了实体类型之间的直接映射：

```java
import com.phagens.corpseorigin.api.infection.EntityInfectionRegistry;
import com.phagens.corpseorigin.register.EntityRegistry;
import net.minecraft.world.entity.EntityType;

// 注册：暮色森林的Naga感染后变成低阶尸兄
EntityInfectionRegistry.registerInfection(
    twilightforest.naga,           // 原始实体类型
    EntityRegistry.LOWER_LEVEL_ZB,  // 感染后的实体类型
    1.0f                           // 感染速度（1.0 = 正常速度）
);

// 注册：大型怪物感染后变成龙右（尸王）
EntityInfectionRegistry.registerInfection(
    twilightforest.hydra,
    EntityRegistry.LONGYOU,
    0.5f  // 感染速度较慢
);

// 注册：不创建尸体的感染（直接感染）
EntityInfectionRegistry.registerInfection(
    twilightforest.minotaur,
    EntityRegistry.LOWER_LEVEL_ZB,
    1.0f,
    false  // 不创建尸体，直接感染
);
```

### 方法2：使用高级感染API（灵活）

如果需要更复杂的逻辑，可以实现 `IInfectionHandler` 接口：

```java
import com.phagens.corpseorigin.api.infection.InfectionAPI;
import com.phagens.corpseorigin.api.infection.IInfectionHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class CustomInfectionHandler implements IInfectionHandler {

    @Override
    public boolean canBeInfected(LivingEntity entity) {
        // 自定义逻辑：判断实体是否可以被感染
        String biome = entity.level().getBiome(entity.blockPosition())
            .unwrapKey().location().toString();
        
        // 只有在暮色森林群系中才能被感染
        return biome.contains("twilight_forest");
    }

    @Override
    public LivingEntity createInfectedEntity(Level level, LivingEntity originalEntity) {
        // 根据原实体的属性创建不同类型的尸兄
        if (originalEntity.getMaxHealth() > 100) {
            // 大型怪物感染后变成龙右
            return EntityRegistry.LONGYOU.get().create(level);
        } else if (originalEntity.getMaxHealth() > 50) {
            // 中型怪物感染后变成高级尸兄
            return EntityRegistry.GUIGUN.get().create(level);
        } else {
            // 小型怪物感染后变成低阶尸兄
            return EntityRegistry.LOWER_LEVEL_ZB.get().create(level);
        }
    }

    @Override
    public float getInfectionSpeed(LivingEntity entity) {
        // 血量越高的生物感染越慢
        float healthFactor = Math.min(2.0f, entity.getMaxHealth() / 20.0f);
        return 1.0f / healthFactor;
    }

    @Override
    public void onInfectionComplete(LivingEntity originalEntity, LivingEntity infectedEntity) {
        // 感染完成时的回调
        CorpseOrigin.LOGGER.info("感染完成: {} -> {}", 
            originalEntity.getType(), infectedEntity.getType());
        
        // 可以在这里添加额外效果，例如播放音效、生成粒子等
    }

    @Override
    public void onInfectionProgress(LivingEntity entity, float progress) {
        // 感染进度更新时的回调
        if (progress > 0.5f && progress < 0.51f) {
            // 感染进度过半时的特殊效果
        }
    }

    @Override
    public boolean shouldCreateCorpse(LivingEntity entity) {
        // 判断死亡时是否应该创建尸体
        return true;
    }
}

// 注册自定义处理器
InfectionAPI.registerInfectionHandler(
    twilightforest.ur_ghast,
    new CustomInfectionHandler()
);
```

### 方法3：设置默认感染处理器

为所有未注册的实体设置默认行为：

```java
InfectionAPI.setDefaultInfectionHandler(new DefaultInfectionHandler());
```

## 监听感染事件

你可以监听感染过程中的各种事件：

```java
import com.phagens.corpseorigin.api.infection.InfectionEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = "your_mod_id")
public class InfectionEventHandler {

    @SubscribeEvent
    public static void onInfectionStart(InfectionEvent.InfectionStartEvent event) {
        // 感染开始时触发
        CorpseOrigin.LOGGER.info("感染开始: {}", 
            event.getOriginalEntity().getType());
    }

    @SubscribeEvent
    public static void onInfectionProgress(InfectionEvent.InfectionProgressEvent event) {
        // 感染进度更新时触发
        float progress = event.getInfectionProgress();
        if (progress > 0.5f) {
            // 感染进度过半
        }
    }

    @SubscribeEvent
    public static void onInfectionComplete(InfectionEvent.InfectionCompleteEvent event) {
        // 感染完成时触发
        LivingEntity original = event.getOriginalEntity();
        LivingEntity infected = event.getInfectedEntity();
        
        CorpseOrigin.LOGGER.info("感染完成: {} -> {}", 
            original.getType(), infected.getType());
    }

    @SubscribeEvent
    public static void onCorpseCreate(InfectionEvent.CorpseCreateEvent event) {
        // 尸体创建时触发
        CorpseOrigin.LOGGER.info("创建尸体: {}", 
            event.getOriginalEntity().getType());
    }

    @SubscribeEvent
    public static void onInfectionCheck(InfectionEvent.InfectionCheckEvent event) {
        // 检查实体是否可以被感染时触发
        // 可以动态修改结果
        if (event.getEntity().getType().toString().contains("boss")) {
            event.setCanBeInfected(false); // Boss不能被感染
        }
    }
}
```

## 暮色森林联动示例

以下是一个完整的暮色森林联动示例：

```java
import com.phagens.corpseorigin.api.infection.EntityInfectionRegistry;
import com.phagens.corpseorigin.api.infection.InfectionAPI;
import com.phagens.corpseorigin.register.EntityRegistry;
import net.minecraft.world.entity.EntityType;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

public class TwilightForestCompat {

    public static void init(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            registerInfections();
        });
    }

    private static void registerInfections() {
        // 检查暮色森林是否已加载
        if (!isTwilightForestLoaded()) {
            return;
        }

        // 注册各种暮色森林怪物的感染
        EntityType<?> naga = getTwilightEntityType("twilightforest:naga");
        if (naga != null) {
            EntityInfectionRegistry.registerInfection(
                naga,
                EntityRegistry.LOWER_LEVEL_ZB,
                1.0f
            );
        }

        EntityType<?> hydra = getTwilightEntityType("twilightforest:hydra");
        if (hydra != null) {
            // 九头蛇感染后变成龙右
            EntityInfectionRegistry.registerInfection(
                hydra,
                EntityRegistry.LONGYOU,
                0.3f  // 感染很慢
            );
        }

        EntityType<?> urGhast = getTwilightEntityType("twilightforest:ur_ghast");
        if (urGhast != null) {
            // 使用自定义处理器
            InfectionAPI.registerInfectionHandler(
                urGhast,
                new UrGhastInfectionHandler()
            );
        }

        // 为所有暮色森林怪物设置默认感染
        InfectionAPI.setDefaultInfectionHandler(
            new TwilightForestDefaultHandler()
        );
    }

    private static boolean isTwilightForestLoaded() {
        try {
            Class.forName("twilightforest.TwilightForestMod");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static EntityType<?> getTwilightEntityType(String entityId) {
        try {
            return BuiltInRegistries.ENTITY_TYPE.get(
                ResourceLocation.parse(entityId)
            );
        } catch (Exception e) {
            return null;
        }
    }

    // 自定义处理器示例
    private static class UrGhastInfectionHandler implements IInfectionHandler {
        // 实现接口方法...
    }

    private static class TwilightForestDefaultHandler implements IInfectionHandler {
        // 实现接口方法...
    }
}
```

## API参考

### EntityInfectionRegistry

```java
// 注册简单感染
void registerInfection(EntityType<?> originalType, EntityType<?> infectedType)
void registerInfection(EntityType<?> originalType, EntityType<?> infectedType, float infectionSpeed)
void registerInfection(EntityType<?> originalType, EntityType<?> infectedType, float infectionSpeed, boolean createCorpse)

// 查询方法
boolean canBeInfected(EntityType<?> entityType)
boolean shouldCreateCorpse(EntityType<?> entityType)
EntityType<?> getInfectedType(EntityType<?> originalType)
float getInfectionSpeed(EntityType<?> entityType)
LivingEntity createInfectedEntity(Level level, LivingEntity originalEntity)

// 管理方法
void removeInfection(EntityType<?> entityType)
void clearAll()
int getRegisteredCount()
```

### InfectionAPI

```java
// 注册处理器
void registerInfectionHandler(EntityType<?> entityType, IInfectionHandler handler)
void registerInfectionHandler(String modId, IInfectionHandler handler)
void setDefaultInfectionHandler(IInfectionHandler handler)

// 查询方法
IInfectionHandler getInfectionHandler(LivingEntity entity)
boolean canBeInfected(LivingEntity entity)
boolean shouldCreateCorpse(LivingEntity entity)
LivingEntity createInfectedEntity(Level level, LivingEntity originalEntity)
float getInfectionSpeed(LivingEntity entity)

// 回调方法
void onInfectionComplete(LivingEntity originalEntity, LivingEntity infectedEntity)
void onInfectionProgress(LivingEntity entity, float progress)

// 管理方法
void clearHandlers()
boolean hasHandler(EntityType<?> entityType)
boolean hasDefaultHandler()
```

### IInfectionHandler接口

```java
public interface IInfectionHandler {
    // 判断实体是否可以被感染
    boolean canBeInfected(LivingEntity entity);
    
    // 创建感染后的实体
    LivingEntity createInfectedEntity(Level level, LivingEntity originalEntity);
    
    // 获取感染速度
    float getInfectionSpeed(LivingEntity entity);
    
    // 感染完成时的回调
    void onInfectionComplete(LivingEntity originalEntity, LivingEntity infectedEntity);
    
    // 感染进度更新时的回调（可选）
    default void onInfectionProgress(LivingEntity entity, float progress) {}
    
    // 判断死亡时是否应该创建尸体（可选）
    default boolean shouldCreateCorpse(LivingEntity entity) {
        return canBeInfected(entity);
    }
}
```

## 注意事项

1. **线程安全**：所有API调用都应该在服务器线程或主线程中进行
2. **模组依赖**：如果依赖其他模组，请先检查模组是否已加载
3. **性能考虑**：避免在 `canBeInfected` 和 `getInfectionSpeed` 中进行复杂计算
4. **事件监听**：使用 `@EventBusSubscriber` 注解注册事件监听器
5. **资源清理**：在模组卸载时清理注册的处理器（如果需要）

## 完整示例项目

查看 `InfectionAPIExample.java` 获取更多示例代码和最佳实践。

## 支持

如有问题或建议，请联系尸兄模组开发团队。
