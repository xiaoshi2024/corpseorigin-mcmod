# CorpseOrigin MCA 兼容模组设计方案

## 概述

将 CorpseOrigin 模组中的 MCA（Minecraft Comes Alive）相关功能完全解耦，创建一个独立的兼容模组 `CorpseOriginMCACompat`，实现与 MCA 模组的无缝联动。

## 架构设计

```
┌─────────────────────────────────────────────────────────────────┐
│                    CorpseOrigin (主模组)                          │
│  ├── 不包含任何 MCA 依赖                                          │
│  ├── McaZombieEntity - 仅存储数据，无交互逻辑                     │
│  ├── mobInteract() - 空实现或返回 PASS                           │
│  └── 独立运行，不受 MCA 影响                                      │
└─────────────────────────────────────────────────────────────────┘
                              ▲
                              │ 依赖 (compileOnly)
                              │
┌─────────────────────────────────────────────────────────────────┐
│              CorpseOriginMCACompat (联动扩展模组)                 │
│  ├── 依赖 CorpseOrigin + MCA                                     │
│  ├── McaCompatZombieEntity - 继承 McaZombieEntity                │
│  │   └── 完整实现 VillagerLike 接口                              │
│  ├── 实体注册与替换                                               │
│  ├── 村民死亡事件转换                                             │
│  └── 提供完整的 MCA 交互功能                                      │
└─────────────────────────────────────────────────────────────────┘
```

## 文件结构

```
CorpseOriginMCACompat/
├── build.gradle
├── src/main/
│   ├── java/com/phagens/corpseorigin/compat/
│   │   ├── CorpseOriginMCACompat.java          # 模组主类
│   │   ├── Config.java                          # 配置类
│   │   ├── entity/
│   │   │   └── McaCompatZombieEntity.java       # 兼容尸兄实体
│   │   ├── event/
│   │   │   └── CompatEventHandler.java          # 事件处理
│   │   └── registry/
│   │       └── CompatEntities.java              # 实体注册
│   └── resources/
│       ├── META-INF/
│       │   └── neoforge.mods.toml               # 模组描述文件
│       └── assets/
│           └── corpseorigin_mca_compat/
│               └── lang/
│                   └── zh_cn.json               # 语言文件
```

## 核心代码

### 1. build.gradle

```gradle
plugins {
    id 'java'
    id 'net.neoforged.gradle.userdev' version '7.0.80'
}

base {
    archivesName = "corpseorigin-mca-compat"
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

repositories {
    mavenCentral()
    maven {
        name = "TerraformersMC"
        url = "https://maven.terraformersmc.com/"
    }
    maven {
        name = "CurseMaven"
        url = "https://cursemaven.com"
    }
}

dependencies {
    implementation "net.neoforged:neoforge:${neo_version}"
    
    // 依赖主模组
    implementation project(":CorpseOrigin")
    
    // 依赖 MCA (CurseForge)
    // 需要替换为实际的 curse maven 坐标
    implementation "curse.maven:mca-${mca_curse_id}:${mca_file_id}"
    
    // 可选依赖
    compileOnly "mcp.mobius.waila:wthit-neo:12.10.2"
}

tasks.named('compileJava', JavaCompile) {
    options.encoding = 'UTF-8'
}
```

### 2. neoforge.mods.toml

```toml
modLoader="javafml"
loaderVersion="[4,)"
license="All Rights Reserved"

[[mods]]
modId="corpseorigin_mca_compat"
version="1.0.0"
displayName="CorpseOrigin MCA Compat"
logoFile="logo.png"
authors="Phagens"
description='''
CorpseOrigin 模组与 Minecraft Comes Alive 的兼容扩展。
提供尸兄村民与 MCA 村民系统的完整联动。
'''

[[dependencies.corpseorigin_mca_compat]]
    modId="neoforge"
    type="required"
    versionRange="[21.1,)"
    ordering="NONE"
    side="BOTH"

[[dependencies.corpseorigin_mca_compat]]
    modId="corpseorigin"
    type="required"
    versionRange="[0.0.5,)"
    ordering="AFTER"
    side="BOTH"

[[dependencies.corpseorigin_mca_compat]]
    modId="mca"
    type="required"
    versionRange="[7.7.7,)"
    ordering="AFTER"
    side="BOTH"
```

### 3. CorpseOriginMCACompat.java

```java
package com.phagens.corpseorigin.compat;

import com.phagens.corpseorigin.compat.registry.CompatEntities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(CorpseOriginMCACompat.MODID)
public class CorpseOriginMCACompat {
    public static final String MODID = "corpseorigin_mca_compat";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
    
    public CorpseOriginMCACompat(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("CorpseOrigin MCA Compat 模组初始化");
        
        // 注册实体
        CompatEntities.ENTITIES.register(modEventBus);
        
        // 注册配置
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
```

### 4. McaCompatZombieEntity.java

```java
package com.phagens.corpseorigin.compat.entity;

import com.phagens.corpseorigin.entity.mca.McaZombieEntity;
import net.conczin.mca.entity.VillagerLike;
import net.conczin.mca.entity.ai.Genetics;
import net.conczin.mca.entity.ai.Traits;
import net.conczin.mca.entity.ai.brain.VillagerBrain;
import net.conczin.mca.entity.ai.relationship.AgeState;
import net.conczin.mca.entity.interaction.VillagerCommandHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class McaCompatZombieEntity extends McaZombieEntity implements VillagerLike<McaCompatZombieEntity> {
    
    private VillagerCommandHandler interactions;
    private Genetics genetics;
    private Traits traits;
    private VillagerBrain<McaCompatZombieEntity> brain;
    
    public McaCompatZombieEntity(EntityType<? extends McaZombieEntity> type, Level level) {
        super(type, level);
    }
    
    @Override
    public VillagerCommandHandler getInteractions() {
        if (interactions == null) {
            interactions = new VillagerCommandHandler(this);
        }
        return interactions;
    }
    
    @Override
    public Genetics getGenetics() {
        if (genetics == null) {
            genetics = new Genetics(this);
            restoreGeneticsFromData();
        }
        return genetics;
    }
    
    @Override
    public Traits getTraits() {
        if (traits == null) {
            traits = new Traits(this);
        }
        return traits;
    }
    
    @Override
    public VillagerBrain<McaCompatZombieEntity> getVillagerBrain() {
        if (brain == null) {
            brain = new VillagerBrain<>(this);
        }
        return brain;
    }
    
    @Override
    public float getInfectionProgress() {
        return 0.8f;
    }
    
    @Override
    public boolean isInfected() {
        return true;
    }
    
    @Override
    public AgeState getAgeState() {
        return AgeState.ADULT;
    }
    
    @Override
    public boolean isBurned() {
        return false;
    }
    
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            return getInteractions().interactAt(serverPlayer, this.position(), hand);
        }
        return InteractionResult.PASS;
    }
    
    private void restoreGeneticsFromData() {
        String data = this.entityData.get(DATA_MCA_ENTITY_DATA);
        if (data != null && !data.isEmpty() && genetics != null) {
            try {
                CompoundTag tag = TagParser.parseTag(data);
                // 恢复基因数据
                if (tag.contains("SkinGene")) {
                    // 应用皮肤基因
                }
                if (tag.contains("Gender")) {
                    // 应用性别
                }
            } catch (Exception e) {
                // 忽略解析错误
            }
        }
    }
    
    // 从原实体转换
    public static McaCompatZombieEntity fromMcaZombie(McaZombieEntity zombie) {
        EntityType<McaCompatZombieEntity> type = CompatEntities.MCA_COMPAT_ZOMBIE.get();
        McaCompatZombieEntity newZombie = type.create(zombie.level());
        
        if (newZombie != null) {
            newZombie.setPos(zombie.getX(), zombie.getY(), zombie.getZ());
            newZombie.setYRot(zombie.getYRot());
            newZombie.setXRot(zombie.getXRot());
            newZombie.setCustomName(zombie.getCustomName());
            
            // 复制同步数据
            newZombie.entityData.set(DATA_MCA_ENTITY_DATA, zombie.entityData.get(DATA_MCA_ENTITY_DATA));
            newZombie.entityData.set(DATA_MCA_PLAYER_NAME, zombie.entityData.get(DATA_MCA_PLAYER_NAME));
            
            // 复制内部数据
            if (zombie.getMcaVillagerLike() != null) {
                newZombie.setMcaVillagerLike(zombie.getMcaVillagerLike());
            }
        }
        
        return newZombie;
    }
    
    // 从 MCA 村民转换
    public static McaCompatZombieEntity fromMcaVillager(net.conczin.mca.entity.VillagerEntityMCA villager) {
        EntityType<McaCompatZombieEntity> type = CompatEntities.MCA_COMPAT_ZOMBIE.get();
        McaCompatZombieEntity zombie = type.create(villager.level());
        
        if (zombie != null) {
            zombie.setPos(villager.getX(), villager.getY(), villager.getZ());
            zombie.setYRot(villager.getYRot());
            zombie.setXRot(villager.getXRot());
            zombie.setCustomName(villager.getCustomName());
            
            // 保存 MCA 村民数据
            CompoundTag tag = villager.getGenetics().serializeNBT();
            tag.putString("Gender", villager.getGenetics().getGender().toString());
            zombie.entityData.set(DATA_MCA_ENTITY_DATA, tag.toString());
            zombie.entityData.set(DATA_MCA_PLAYER_NAME, villager.getGenetics().getGender().toString());
        }
        
        return zombie;
    }
}
```

### 5. CompatEntities.java

```java
package com.phagens.corpseorigin.compat.registry;

import com.phagens.corpseorigin.compat.CorpseOriginMCACompat;
import com.phagens.corpseorigin.compat.entity.McaCompatZombieEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class CompatEntities {
    
    public static final DeferredRegister<EntityType<?>> ENTITIES = 
        DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, CorpseOriginMCACompat.MODID);
    
    public static final Supplier<EntityType<McaCompatZombieEntity>> MCA_COMPAT_ZOMBIE = 
        ENTITIES.register("mca_compat_zombie", () -> EntityType.Builder
            .of(McaCompatZombieEntity::new, MobCategory.MONSTER)
            .sized(0.6f, 1.95f)
            .build("mca_compat_zombie"));
}
```

### 6. CompatEventHandler.java

```java
package com.phagens.corpseorigin.compat.event;

import com.phagens.corpseorigin.compat.entity.McaCompatZombieEntity;
import com.phagens.corpseorigin.entity.mca.McaZombieEntity;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@EventBusSubscriber
public class CompatEventHandler {
    
    @SubscribeEvent
    public static void onVillagerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof VillagerEntityMCA villager && !villager.level().isClientSide) {
            McaCompatZombieEntity zombie = McaCompatZombieEntity.fromMcaVillager(villager);
            villager.level().addFreshEntity(zombie);
            zombie.setPos(villager.position());
        }
    }
    
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        // 将现有的 McaZombieEntity 替换为 McaCompatZombieEntity
        if (!event.getLevel().isClientSide && event.getEntity() instanceof McaZombieEntity zombie 
            && !(zombie instanceof McaCompatZombieEntity)) {
            McaCompatZombieEntity compatZombie = McaCompatZombieEntity.fromMcaZombie(zombie);
            event.getLevel().addFreshEntity(compatZombie);
            compatZombie.setPos(zombie.position());
            zombie.discard();
        }
    }
}
```

### 7. Config.java

```java
package com.phagens.corpseorigin.compat;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    
    public static final ModConfigSpec SPEC;
    
    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        
        builder.comment("CorpseOrigin MCA Compat 配置").push("general");
        
        // 配置选项
        builder.pop();
        
        SPEC = builder.build();
    }
}
```

## 主模组修改

在主模组的 `McaZombieEntity` 中简化 `mobInteract`：

```java
@Override
public InteractionResult mobInteract(Player player, InteractionHand hand) {
    // 主模组不处理交互，由兼容模组覆盖
    return InteractionResult.PASS;
}
```

## 使用方式

1. **仅安装 CorpseOrigin**：尸兄存在，但无 MCA 交互
2. **安装 CorpseOrigin + MCA**：尸兄存在，但无 MCA 交互（避免崩溃）
3. **安装 CorpseOrigin + MCA + CorpseOriginMCACompat**：完整的 MCA 联动体验

## 优点

| 特性 | 说明 |
|------|------|
| **完全解耦** | 主模组不知道 MCA 存在 |
| **类型安全** | 联动模组直接使用 MCA API，无需反射 |
| **无崩溃** | 实体替换逻辑确保兼容性 |
| **可独立发布** | 联动模组可以单独在 CurseForge 发布 |
| **易于维护** | 各自独立更新，互不影响 |