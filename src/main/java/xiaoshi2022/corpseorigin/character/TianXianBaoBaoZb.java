package xiaoshi2022.corpseorigin.character;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import xiaoshi2022.corpseorigin.item.armor.AntennaZBRitem;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.registry.ModItems;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.tianxianbaobao_zb.AntennaBlockSkill;
import xiaoshi2022.corpseorigin.skill.tianxianbaobao_zb.LifeDrainSuckSkill;

import java.util.List;

/**
 * 天线宝宝尸兄 - 尸巢篇开篇小 BOSS，靠吸食与耳目虫传讯压迫玩家。
 * <p>
 * 角色效果：获得角色即变成尸兄，但走「无外骨骼通用变种」——不长那根尸眼骨骼，
 * 外观完全交给自动穿戴的天线宝宝尸兄盔甲。
 */
public class TianXianBaoBaoZb implements ICharacter {

    public static final String ID = "tianxianbaobao_zb";

    /** 自动穿戴的三件套（头盔 / 胸甲 / 护腿，可自由卸下） */
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS
    };

    private static final List<ISkill> SKILLS = List.of(
            new LifeDrainSuckSkill(),
            new AntennaBlockSkill()
    );

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Component getName() {
        return Component.translatable("character.corpseorigin." + ID);
    }

    @Override
    public Component getDescription() {
        return Component.translatable("character.corpseorigin." + ID + ".desc");
    }

    @Override
    public Identifier getIcon() {
        return ICharacter.iconId(ID);
    }

    @Override
    public boolean isPassive() {
        return false;
    }

    @Override
    public List<Component> getTraits() {
        return List.of(
                Component.translatable("character.corpseorigin." + ID + ".trait1"),
                Component.translatable("character.corpseorigin." + ID + ".trait2"),
                Component.translatable("character.corpseorigin." + ID + ".trait3")
        );
    }

    @Override
    public List<ISkill> getSkills() {
        return SKILLS;
    }

    @Override
    public float getInfectionMultiplier() {
        return 0.0f;
    }

    @Override
    public void onAcquire(Player player) {
        // 直接变成尸兄，但用「无外骨骼通用变种」：算尸兄、不长尸眼骨骼
        PlayerCorpseComponent.setPlayerAsCorpse(player, PlayerCorpseComponent.TYPE_ELITE,
                PlayerCorpseComponent.VARIANT_NO_EXOSKELETON);

        // 玩家角色要保留意识，否则一换角色就变成只会本能行动的怪物
        PlayerCorpseComponent comp = PlayerCorpseComponent.get(player);

        equipArmor(player);

        // restoreConsciousness 不会触发同步，补一次；
        // 必须广播：尸兄外观是别的玩家看你时才渲染的。
        if (player instanceof ServerPlayer serverPlayer) {
            CorpseNetwork.broadcastPlayerCorpseSync(serverPlayer);
        }
    }

    @Override
    public void onLose(Player player) {
        removeArmor(player);
        PlayerCorpseComponent.removeCorpseState(player);
    }

    /** 把三件套穿到盔甲槽上；原本穿着的东西退回背包，不吞玩家装备 */
    private static void equipArmor(Player player) {
        ItemStack[] pieces = {
                new ItemStack(ModItems.ANTENNA_ZBR_ARMOR_HELMET.get()),
                new ItemStack(ModItems.ANTENNA_ZBR_ARMOR_CHESTPLATE.get()),
                new ItemStack(ModItems.ANTENNA_ZBR_ARMOR_LEGGINGS.get())
        };

        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            ItemStack worn = player.getItemBySlot(ARMOR_SLOTS[i]);
            if (!worn.isEmpty()) {
                player.getInventory().placeItemBackInInventory(worn.copy());
            }
            player.setItemSlot(ARMOR_SLOTS[i], pieces[i]);
        }
    }

    /** 只收回本角色发出去的天线宝宝盔甲，玩家自己换上的其他装备不动 */
    private static void removeArmor(Player player) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (player.getItemBySlot(slot).getItem() instanceof AntennaZBRitem) {
                player.setItemSlot(slot, ItemStack.EMPTY);
            }
        }
    }
}
