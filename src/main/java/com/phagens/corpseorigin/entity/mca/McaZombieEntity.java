package com.phagens.corpseorigin.entity.mca;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.entity.LowerLevelZbEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Optional;

public class McaZombieEntity extends LowerLevelZbEntity {
    private static final EntityDataAccessor<String> DATA_MCA_ENTITY_DATA =
            SynchedEntityData.defineId(McaZombieEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_MCA_PLAYER_NAME =
            SynchedEntityData.defineId(McaZombieEntity.class, EntityDataSerializers.STRING);

    // 客户端皮肤数据缓存（不用于同步，仅用于渲染）
    private Object clientSkinData;
    private String cachedSkinGender;
    private float cachedSkinGene = 0.5f;
    private int cachedSkinIndex = 0;

    private Object mcaVillagerLike;

    public McaZombieEntity(EntityType<? extends LowerLevelZbEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LowerLevelZbEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_MCA_ENTITY_DATA, "");
        builder.define(DATA_MCA_PLAYER_NAME, "");
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);

        // 客户端收到同步数据时，重建皮肤信息
        if (this.level().isClientSide) {
            if (DATA_MCA_ENTITY_DATA.equals(key) || DATA_MCA_PLAYER_NAME.equals(key)) {
                rebuildClientSkinData();
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void rebuildClientSkinData() {
        String entityDataStr = this.entityData.get(DATA_MCA_ENTITY_DATA);
        String genderStr = this.entityData.get(DATA_MCA_PLAYER_NAME);

        if (genderStr != null && !genderStr.isEmpty()) {
            this.cachedSkinGender = genderStr.toLowerCase();
        } else {
            this.cachedSkinGender = "male";
        }

        // 从 NBT 中解析皮肤基因
        if (entityDataStr != null && !entityDataStr.isEmpty()) {
            try {
                CompoundTag tag = TagParser.parseTag(entityDataStr);
                if (tag.contains("SkinGene")) {
                    this.cachedSkinGene = tag.getFloat("SkinGene");
                } else if (tag.contains("Genetics")) {
                    // 尝试从 Genetics 中读取
                    CompoundTag geneticsTag = tag.getCompound("Genetics");
                    if (geneticsTag.contains("skin")) {
                        this.cachedSkinGene = geneticsTag.getFloat("skin");
                    }
                }
                this.cachedSkinIndex = (int) Math.min(4, Math.max(0, this.cachedSkinGene * 5));
            } catch (Exception e) {
                // 使用默认值
            }
        }

        CorpseOrigin.LOGGER.debug("客户端重建皮肤数据: gender={}, skinIndex={}",
                cachedSkinGender, cachedSkinIndex);
    }

    @OnlyIn(Dist.CLIENT)
    public String getClientSkinGender() {
        if (cachedSkinGender == null) {
            rebuildClientSkinData();
        }
        return cachedSkinGender != null ? cachedSkinGender : "male";
    }

    @OnlyIn(Dist.CLIENT)
    public int getClientSkinIndex() {
        if (cachedSkinGender == null) {
            rebuildClientSkinData();
        }
        return cachedSkinIndex;
    }

    @OnlyIn(Dist.CLIENT)
    public boolean hasClientSkinData() {
        return cachedSkinGender != null;
    }

    // 主模组中的 McaZombieEntity.java
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        // 主模组不处理任何交互
        // 联动模组会覆盖这个方法
        return InteractionResult.PASS;
    }

// 删除 sendOpenGuiPacket 和 simpleChat 方法

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putString("MCAEntityData", this.entityData.get(DATA_MCA_ENTITY_DATA));
        compound.putString("MCAPlayerName", this.entityData.get(DATA_MCA_PLAYER_NAME));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("MCAEntityData")) {
            this.entityData.set(DATA_MCA_ENTITY_DATA, compound.getString("MCAEntityData"));
        }
        if (compound.contains("MCAPlayerName")) {
            this.entityData.set(DATA_MCA_PLAYER_NAME, compound.getString("MCAPlayerName"));
        }
        this.restoreMcaData();

        // 客户端立即重建皮肤数据
        if (this.level().isClientSide) {
            rebuildClientSkinData();
        }
    }

    public void setMcaVillagerLike(Object villagerLike) {
        this.mcaVillagerLike = villagerLike;
        this.saveMcaData();
    }

    public Object getMcaVillagerLike() {
        return this.mcaVillagerLike;
    }

    public String getMcaPlayerName() {
        return this.entityData.get(DATA_MCA_PLAYER_NAME);
    }

    private void saveMcaData() {
        if (this.mcaVillagerLike != null) {
            try {
                Class<?> villagerLikeClass = Class.forName("net.conczin.mca.entity.VillagerLike");

                CompoundTag tag = (CompoundTag) villagerLikeClass
                        .getMethod("toNbtForConversion")
                        .invoke(this.mcaVillagerLike);

                Object genetics = villagerLikeClass.getMethod("getGenetics").invoke(this.mcaVillagerLike);
                if (genetics != null) {
                    Class<?> geneticsClass = Class.forName("net.conczin.mca.entity.ai.Genetics");

                    try {
                        Float skinGene = (Float) geneticsClass.getMethod("getGene", String.class).invoke(genetics, "skin");
                        tag.putFloat("SkinGene", skinGene != null ? skinGene : 0.5f);
                    } catch (Exception e) {
                        tag.putFloat("SkinGene", 0.5f);
                    }

                    try {
                        Float melanin = (Float) geneticsClass.getMethod("getGene", String.class).invoke(genetics, "melanin");
                        Float hemoglobin = (Float) geneticsClass.getMethod("getGene", String.class).invoke(genetics, "hemoglobin");
                        tag.putFloat("Melanin", melanin != null ? melanin : 0.5f);
                        tag.putFloat("Hemoglobin", hemoglobin != null ? hemoglobin : 0.5f);
                    } catch (Exception e) {
                        tag.putFloat("Melanin", 0.5f);
                        tag.putFloat("Hemoglobin", 0.5f);
                    }

                    Object gender = geneticsClass.getMethod("getGender").invoke(genetics);
                    if (gender != null) {
                        this.entityData.set(DATA_MCA_PLAYER_NAME, gender.toString());
                        tag.putString("Gender", gender.toString());
                    }
                }

                this.entityData.set(DATA_MCA_ENTITY_DATA, tag.toString());

            } catch (Exception e) {
                CorpseOrigin.LOGGER.warn("保存MCA数据失败: {}", e.getMessage());
            }
        }
    }

    private void restoreMcaData() {
        String entityDataStr = this.entityData.get(DATA_MCA_ENTITY_DATA);
        if (entityDataStr != null && !entityDataStr.isEmpty()) {
            try {
                CompoundTag tag = TagParser.parseTag(entityDataStr);

                Class<?> villagerEntityMcaClass = Class.forName("net.conczin.mca.entity.VillagerEntityMCA");
                Class<?> genderClass = Class.forName("net.conczin.mca.entity.ai.relationship.Gender");
                Class<?> villagerLikeClass = Class.forName("net.conczin.mca.entity.VillagerLike");

                String savedGender = this.entityData.get(DATA_MCA_PLAYER_NAME);
                Object gender;
                if ("FEMALE".equals(savedGender)) {
                    gender = genderClass.getEnumConstants()[1];
                } else {
                    gender = genderClass.getEnumConstants()[0];
                }

                Object villager = villagerEntityMcaClass
                        .getConstructor(EntityType.class, Level.class, genderClass)
                        .newInstance(this.getType(), this.level(), gender);

                if (villager != null) {
                    villagerLikeClass.getMethod("readNbtForConversion", CompoundTag.class)
                            .invoke(villager, tag);

                    if (this.level() instanceof ServerLevel serverLevel) {
                        try {
                            villagerEntityMcaClass.getMethod("refreshBrain", ServerLevel.class)
                                    .invoke(villager, serverLevel);
                        } catch (NoSuchMethodException e) {
                            // 忽略
                        }
                    }

                    this.mcaVillagerLike = villager;

                    try {
                        Component name = (Component) villagerLikeClass.getMethod("getCustomName").invoke(villager);
                        if (name != null) {
                            this.setCustomName(name);
                        }
                    } catch (Exception e) {
                        // 忽略
                    }

                    CorpseOrigin.LOGGER.info("MCA数据恢复成功: {}", savedGender);
                }
            } catch (Exception e) {
                CorpseOrigin.LOGGER.warn("恢复MCA数据失败: {}", e.getMessage());
            }
        }
    }

    @Override
    public Component getDisplayName() {
        if (this.mcaVillagerLike != null) {
            try {
                Class<?> villagerLikeClass = Class.forName("net.conczin.mca.entity.VillagerLike");
                Component name = (Component) villagerLikeClass.getMethod("getCustomName").invoke(this.mcaVillagerLike);
                if (name != null) {
                    return name;
                }
            } catch (Exception e) {
                // 忽略
            }
        }
        return super.getDisplayName();
    }

    public static boolean isMcaAvailable() {
        try {
            Class.forName("net.conczin.mca.entity.VillagerEntityMCA");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static Optional<McaZombieEntity> createFromMcaVillager(net.minecraft.world.entity.Entity mcaVillager) {
        if (!isMcaAvailable()) {
            return Optional.empty();
        }

        try {
            EntityType<McaZombieEntity> entityType = com.phagens.corpseorigin.register.EntityRegistry.MCA_ZOMBIE.get();
            McaZombieEntity mcaZombie = entityType.create(mcaVillager.level());

            if (mcaZombie != null) {
                mcaZombie.setMcaVillagerLike(mcaVillager);
                mcaZombie.setPos(mcaVillager.getX(), mcaVillager.getY(), mcaVillager.getZ());
                mcaZombie.setYRot(mcaVillager.getYRot());
                mcaZombie.setXRot(mcaVillager.getXRot());

                Component name = mcaVillager.getCustomName();
                if (name != null) {
                    mcaZombie.setCustomName(name);
                }

                CorpseOrigin.LOGGER.info("MCA僵尸创建成功: {}", name != null ? name.getString() : "Unknown");

                return Optional.of(mcaZombie);
            }
        } catch (Exception e) {
            CorpseOrigin.LOGGER.warn("创建MCA僵尸失败: {}", e.getMessage());
        }

        return Optional.empty();
    }
}