package com.phagens.corpseorigin.client.mca;

import com.phagens.corpseorigin.CorpseOrigin;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public class McaSkinHelper {

    public static Optional<ResourceLocation> getMcaSkinReflective(Object villagerLike) {
        if (!isMcaAvailable()) {
            CorpseOrigin.LOGGER.debug("MCA not available");
            return Optional.empty();
        }

        try {
            Class<?> villagerLikeClass = Class.forName("net.conczin.mca.entity.VillagerLike");

            if (!villagerLikeClass.isInstance(villagerLike)) {
                CorpseOrigin.LOGGER.debug("Not an instance of VillagerLike: {}", villagerLike.getClass().getName());
                return Optional.empty();
            }

            // 获取遗传数据
            Object genetics = villagerLikeClass.getMethod("getGenetics").invoke(villagerLike);
            if (genetics == null) {
                CorpseOrigin.LOGGER.debug("Genetics is null for MCA villager");
                return Optional.empty();
            }

            Class<?> geneticsClass = Class.forName("net.conczin.mca.entity.ai.Genetics");

            // 获取性别
            Object gender = geneticsClass.getMethod("getGender").invoke(genetics);
            if (gender == null) {
                CorpseOrigin.LOGGER.debug("Gender is null");
                return Optional.empty();
            }

            String genderName = gender.toString().toLowerCase();
            CorpseOrigin.LOGGER.info("MCA Gender: {}", genderName);

            // 获取皮肤基因值 - 注意：基因名称可能是 "skin" 或 "SKIN"
            Float skinGene = null;
            try {
                skinGene = (Float) geneticsClass.getMethod("getGene", String.class).invoke(genetics, "skin");
            } catch (Exception e) {
                try {
                    skinGene = (Float) geneticsClass.getMethod("getGene", String.class).invoke(genetics, "SKIN");
                } catch (Exception e2) {
                    CorpseOrigin.LOGGER.warn("无法获取皮肤基因，使用默认值");
                    skinGene = 0.5f;
                }
            }

            int skinIndex = (int) Math.min(4, Math.max(0, skinGene * 5));
            CorpseOrigin.LOGGER.info("MCA Skin gene: {}, index: {}", skinGene, skinIndex);

            // 尝试多种可能的皮肤路径
            String[] possiblePaths = {
                    "skins/skin/" + genderName + "/" + skinIndex + ".png",
                    "textures/skins/skin/" + genderName + "/" + skinIndex + ".png",
                    "skins/" + genderName + "/" + skinIndex + ".png",
                    "skins/skin/" + genderName + "/default.png"
            };

            Class<?> mcaClass = Class.forName("net.conczin.mca.MCA");
            ResourceLocation skinTexture = null;

            for (String path : possiblePaths) {
                try {
                    skinTexture = (ResourceLocation) mcaClass.getMethod("locate", String.class).invoke(null, path);
                    // 检查纹理是否存在
                    if (net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(skinTexture).isPresent()) {
                        CorpseOrigin.LOGGER.info("找到MCA皮肤纹理: {}", skinTexture);
                        return Optional.of(skinTexture);
                    }
                } catch (Exception e) {
                    // 继续尝试下一个路径
                }
            }

            // 如果都找不到，返回一个默认的腐肉纹理作为占位符
            CorpseOrigin.LOGGER.warn("未找到MCA皮肤纹理，使用默认纹理");
            return Optional.of(ResourceLocation.withDefaultNamespace("textures/item/rotten_flesh.png"));

        } catch (Exception e) {
            CorpseOrigin.LOGGER.warn("获取 MCA 皮肤时出错: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public static Optional<int[]> getMcaSkinColorReflective(Object villagerLike) {
        if (!isMcaAvailable()) {
            return Optional.empty();
        }

        try {
            Class<?> villagerLikeClass = Class.forName("net.conczin.mca.entity.VillagerLike");

            if (!villagerLikeClass.isInstance(villagerLike)) {
                return Optional.empty();
            }

            Object genetics = villagerLikeClass.getMethod("getGenetics").invoke(villagerLike);
            Object traits = villagerLikeClass.getMethod("getTraits").invoke(villagerLike);

            if (genetics == null || traits == null) {
                return Optional.empty();
            }

            Class<?> geneticsClass = Class.forName("net.conczin.mca.entity.ai.Genetics");
            Class<?> traitsClass = Class.forName("net.conczin.mca.entity.ai.Traits");

            // 尝试获取肤色基因
            Float melanin = 0.5f;
            Float hemoglobin = 0.5f;

            try {
                melanin = (Float) geneticsClass.getMethod("getGene", String.class).invoke(genetics, "melanin");
                hemoglobin = (Float) geneticsClass.getMethod("getGene", String.class).invoke(genetics, "hemoglobin");
            } catch (Exception e) {
                CorpseOrigin.LOGGER.debug("使用默认肤色值");
            }

            if (melanin == null) melanin = 0.5f;
            if (hemoglobin == null) hemoglobin = 0.5f;

            // 检查是否有白化病
            boolean hasAlbinism = false;
            try {
                Object albinismTrait = traitsClass.getField("ALBINISM").get(null);
                hasAlbinism = (Boolean) traitsClass.getMethod("hasTrait", Object.class)
                        .invoke(traits, albinismTrait);
            } catch (Exception e) {
                // 忽略
            }

            float albinism = hasAlbinism ? 0.1f : 1.0f;

            // 计算颜色
            int red = (int) (255 * Math.min(1.0f, melanin * albinism));
            int green = (int) (255 * Math.min(1.0f, hemoglobin * albinism));
            int blue = (int) (255 * Math.min(1.0f, 0.8f * albinism));

            CorpseOrigin.LOGGER.debug("MCA skin color: R={}, G={}, B={}", red, green, blue);

            return Optional.of(new int[]{red, green, blue});
        } catch (Exception e) {
            CorpseOrigin.LOGGER.warn("获取 MCA 皮肤颜色时出错: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public static boolean isMcaAvailable() {
        try {
            Class.forName("net.conczin.mca.entity.VillagerLike");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}