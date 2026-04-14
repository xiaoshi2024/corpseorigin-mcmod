package com.phagens.corpseorigin.Item.Organic;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.player.PlayerCorpseData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.List;
import java.util.function.Consumer;

/**
 * 普通尸眼 - 尸兄掉落物
 * 食用后有概率获得夜视能力或长出额外的眼睛
 */
public class OrdinaryZbEyeItem extends ZbOrganItem {
    
    // 夜视进化概率
    private static final float NIGHT_VISION_CHANCE = 0.15f;
    // 额外眼睛进化概率
    private static final float EXTRA_EYE_CHANCE = 0.10f;
    // 需要食用的数量才能获得稳定夜视
    private static final int REQUIRED_FOR_NIGHT_VISION = 5;
    // 需要食用的数量才能长出额外眼睛
    private static final int REQUIRED_FOR_EXTRA_EYE = 8;
    
    public OrdinaryZbEyeItem() {
        super(OrganType.EYE, 0.25f, 3, 2, 0.1f);
    }
    
    @Override
    protected void onOrganConsumed(Player player, ItemStack stack) {
        CorpseOrigin.LOGGER.info("尸兄玩家 {} 食用了普通尸眼", player.getName().getString());
        
        // 增加食用计数
        incrementConsumedCount(player);
        int consumedCount = getConsumedCount(player);
        
        boolean evolved = false;
        
        // 检查是否满足夜视进化条件
        if (consumedCount >= REQUIRED_FOR_NIGHT_VISION) {
            if (tryEvolve(player)) {
                evolved = true;
                grantNightVision(player);
            }
        }
        
        // 检查是否满足额外眼睛进化条件
        if (consumedCount >= REQUIRED_FOR_EXTRA_EYE) {
            if (player.getRandom().nextFloat() < EXTRA_EYE_CHANCE) {
                evolved = true;
                grantExtraEye(player);
            }
        }
        
        // 临时夜视效果（每次食用都给予）
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 6000, 0));
        
        // 恢复饥饿值
        PlayerCorpseData.setHunger(player, Math.min(100, PlayerCorpseData.getHunger(player) + 10));
        
        if (evolved) {
            player.sendSystemMessage(Component.translatable("item.corpseorigin.ordinary_zb_eye.evolution_success")
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        } else {
            player.sendSystemMessage(Component.translatable("item.corpseorigin.ordinary_zb_eye.consumed", 
                    consumedCount, REQUIRED_FOR_EXTRA_EYE));
        }
    }
    
    @Override
    protected boolean applyEvolution(Player player) {
        // 夜视进化逻辑
        String key = "has_permanent_night_vision";
        net.minecraft.nbt.CompoundTag data = PlayerCorpseData.getCorpseData(player);
        
        if (!data.getBoolean(key)) {
            data.putBoolean(key, true);
            player.setData(com.phagens.corpseorigin.player.CorpsePlayerAttachment.CORPSE_DATA, data);
            CorpseOrigin.LOGGER.info("尸兄玩家 {} 进化出夜视能力", player.getName().getString());
            return true;
        }
        return false;
    }
    
    /**
     * 给予夜视能力
     */
    private void grantNightVision(Player player) {
        // 永久夜视标记
        String key = "has_permanent_night_vision";
        net.minecraft.nbt.CompoundTag data = PlayerCorpseData.getCorpseData(player);
        data.putBoolean(key, true);
        player.setData(com.phagens.corpseorigin.player.CorpsePlayerAttachment.CORPSE_DATA, data);
        
        player.sendSystemMessage(Component.translatable("item.corpseorigin.ordinary_zb_eye.night_vision_unlocked")
                .withStyle(ChatFormatting.GOLD));
    }
    
    /**
     * 给予额外眼睛（视觉增强）
     * 进化后一次性获得所有9个眼睛
     */
    private void grantExtraEye(Player player) {
        String key = "extra_eye_count";
        net.minecraft.nbt.CompoundTag data = PlayerCorpseData.getCorpseData(player);
        int currentEyes = data.getInt(key);

        // 如果还没有进化出多眼，一次性获得全部9个眼睛
        if (currentEyes == 0) {
            data.putInt(key, 9); // 直接给9个眼睛
            player.setData(com.phagens.corpseorigin.player.CorpsePlayerAttachment.CORPSE_DATA, data);

            CorpseOrigin.LOGGER.info("尸兄玩家 {} 进化出多眼形态！", player.getName().getString());

            player.sendSystemMessage(Component.translatable("item.corpseorigin.ordinary_zb_eye.multi_eye_evolved")
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));

            // 设置最大视野范围
            increaseVisionRange(player, 9);
        }
    }
    
    /**
     * 增加视野范围
     */
    private void increaseVisionRange(Player player, int extraEyeCount) {
        // 每只额外眼睛增加10%视野范围
        float visionMultiplier = 1.0f + (extraEyeCount * 0.1f);
        
        net.minecraft.nbt.CompoundTag data = PlayerCorpseData.getCorpseData(player);
        data.putFloat("vision_multiplier", visionMultiplier);
        player.setData(com.phagens.corpseorigin.player.CorpsePlayerAttachment.CORPSE_DATA, data);
    }
    
    @Override
    protected void addEffectDescription(List<Component> tooltip) {
        tooltip.add(Component.translatable("item.corpseorigin.ordinary_zb_eye.effect1")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.corpseorigin.ordinary_zb_eye.effect2", 
                (int)(NIGHT_VISION_CHANCE * 100), REQUIRED_FOR_NIGHT_VISION)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.corpseorigin.ordinary_zb_eye.effect3", 
                (int)(EXTRA_EYE_CHANCE * 100), REQUIRED_FOR_EXTRA_EYE)
                .withStyle(ChatFormatting.GRAY));
    }
    
    @OnlyIn(Dist.CLIENT)
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private com.phagens.corpseorigin.client.Renderer.item.OrdinaryZbEyeRenderer renderer;
            
            @Override
            public GeoItemRenderer<OrdinaryZbEyeItem> getGeoItemRenderer() {
                if (this.renderer == null) {
                    this.renderer = new com.phagens.corpseorigin.client.Renderer.item.OrdinaryZbEyeRenderer();
                }
                return this.renderer;
            }
        });
    }
    
    /**
     * 检查玩家是否有永久夜视
     */
    public static boolean hasPermanentNightVision(Player player) {
        return PlayerCorpseData.getCorpseData(player).getBoolean("has_permanent_night_vision");
    }
    
    /**
     * 获取玩家额外眼睛数量
     */
    public static int getExtraEyeCount(Player player) {
        return PlayerCorpseData.getCorpseData(player).getInt("extra_eye_count");
    }
    
    /**
     * 获取玩家视野倍率
     */
    public static float getVisionMultiplier(Player player) {
        return PlayerCorpseData.getCorpseData(player).getFloat("vision_multiplier");
    }
}
