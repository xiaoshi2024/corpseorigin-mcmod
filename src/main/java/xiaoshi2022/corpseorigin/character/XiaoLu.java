package xiaoshi2022.corpseorigin.character;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import xiaoshi2022.corpseorigin.registry.ModItems;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.xiaolu.OsmiumGoldSkill;
import xiaoshi2022.corpseorigin.skill.xiaolu.OsmiumIceSpikeSkill;

import java.util.List;

/**
 * 小鹿 - 灵鹿化身的辅助/净化角色
 */
public class XiaoLu implements ICharacter {

    public static final String ID = "xiaolu";

    @Override
    public void onAcquire(Player player) {
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS};
        ItemStack[] pieces = {
                new ItemStack(ModItems.XIAOLU_ARMOR_HELMET.get()),
                new ItemStack(ModItems.XIAOLU_ARMOR_CHESTPLATE.get()),
                new ItemStack(ModItems.XIAOLU_ARMOR_LEGGINGS.get())
        };
        for (int i = 0; i < slots.length; i++) {
            ItemStack worn = player.getItemBySlot(slots[i]);
            if (worn.is(pieces[i].getItem())) continue;
            player.setItemSlot(slots[i], pieces[i]);
            if (!worn.isEmpty()) player.getInventory().placeItemBackInInventory(worn);
        }
    }

    private static final List<ISkill> SKILLS = List.of(
            new OsmiumGoldSkill(),
            new OsmiumIceSpikeSkill()
    );

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Component getName() {
        return Component.translatable("character.corpseorigin.xiaolu");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("character.corpseorigin.xiaolu.desc");
    }

    @Override
    public Identifier getIcon() {
        return ICharacter.iconId("xiaolu");
    }

    @Override
    public boolean isPassive() {
        return false;
    }

    @Override
    public List<Component> getTraits() {
        return List.of(
                Component.translatable("character.corpseorigin.xiaolu.trait1"),
                Component.translatable("character.corpseorigin.xiaolu.trait2")
        );
    }

    @Override
    public List<ISkill> getSkills() {
        return SKILLS;
    }

    @Override
    public float getInfectionMultiplier() {
        return 0.7f;
    }

    @Override
    public int getMaxInnerPower() {
        return 80;
    }
}
