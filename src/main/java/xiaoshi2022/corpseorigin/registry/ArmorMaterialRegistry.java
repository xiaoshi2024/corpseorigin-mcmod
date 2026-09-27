package xiaoshi2022.corpseorigin.registry;

import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import java.util.Map;

public class ArmorMaterialRegistry {
    public static void init() {}

    public static final ArmorMaterial ZBR_ARMOR_MATERIAL = ArmorMaterials.DIAMOND;

    /** 小鹿：铁甲级轻装；头/胸/腿合计 13 护甲，无韧性及抗击退加成。 */
    public static final ArmorMaterial XIAOLU_ARMOR_MATERIAL = new ArmorMaterial(
            15, ArmorMaterials.IRON.defense(), 15, ArmorMaterials.LEATHER.equipSound(),
            0.0F, 0.0F, ArmorMaterials.IRON.repairIngredient(), ArmorMaterials.LEATHER.assetId());

    /** 尸王：三件合计 20 护甲、12 韧性、30% 抗击退，耐久系数 45。 */
    public static final ArmorMaterial LONGYOU_ARMOR_MATERIAL = new ArmorMaterial(
            45, Map.of(ArmorType.HELMET, 4, ArmorType.CHESTPLATE, 9,
                    ArmorType.LEGGINGS, 7, ArmorType.BOOTS, 4, ArmorType.BODY, 12),
            18, ArmorMaterials.NETHERITE.equipSound(), 4.0F, 0.1F,
            ArmorMaterials.NETHERITE.repairIngredient(), ArmorMaterials.NETHERITE.assetId());

}
