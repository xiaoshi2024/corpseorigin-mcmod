package xiaoshi2022.corpseorigin.character;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.Holder;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.shichaozhizi.BloodLotusArmorSkill;
import xiaoshi2022.corpseorigin.skill.shichaozhizi.GroundBurrowSkill;
import xiaoshi2022.corpseorigin.skill.shichaozhizi.SonOfCorpseNestSkill;

import java.util.List;

/**
 * 尸巢之子 - 少教主，《尸巢之战篇》的最终隐藏 BOSS。
 */
public class ShiChaoZhiZi implements ICharacter {

    public static final String ID = "shichaozhizi";
    public static final float SECOND_FORM_SCALE = 4.0F;
    public static final double SECOND_FORM_HEALTH = 500.0;
    public static final double SECOND_FORM_ARMOR = 20.0;

    private static final Identifier HEALTH_MODIFIER = CorpseOrigin.id("shichao_second_form_health");
    private static final Identifier ARMOR_MODIFIER = CorpseOrigin.id("shichao_second_form_armor");
    private static final Identifier KNOCKBACK_MODIFIER = CorpseOrigin.id("shichao_second_form_knockback");
    private static final Identifier SCALE_MODIFIER = CorpseOrigin.id("shichao_second_form_scale");

    private static final List<ISkill> SKILLS = List.of(
            new SonOfCorpseNestSkill(),
            new BloodLotusArmorSkill(),
            new GroundBurrowSkill()
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
    public int getMaxInnerPower() {
        return 90;
    }

    @Override
    public void onAcquire(Player player) {
        if (!PlayerCorpseComponent.isCorpse(player)) {
            PlayerCorpseComponent.setPlayerAsCorpse(player, PlayerCorpseComponent.TYPE_ELITE,
                    PlayerCorpseComponent.VARIANT_NO_EXOSKELETON);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            CorpseNetwork.broadcastPlayerCorpseSync(serverPlayer);
        }
    }

    @Override
    public void onLose(Player player) {
        removeSecondFormAttributes(player);
        PlayerCorpseComponent.removeCorpseState(player);
    }

    public static void applySecondFormAttributes(LivingEntity entity) {
        setTarget(entity, Attributes.MAX_HEALTH, HEALTH_MODIFIER, SECOND_FORM_HEALTH);
        setTarget(entity, Attributes.ARMOR, ARMOR_MODIFIER, SECOND_FORM_ARMOR);
        setTarget(entity, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_MODIFIER, 1.0);
        setTarget(entity, Attributes.SCALE, SCALE_MODIFIER, SECOND_FORM_SCALE);
        entity.refreshDimensions();
    }

    public static void removeSecondFormAttributes(LivingEntity entity) {
        remove(entity, Attributes.MAX_HEALTH, HEALTH_MODIFIER);
        remove(entity, Attributes.ARMOR, ARMOR_MODIFIER);
        remove(entity, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_MODIFIER);
        remove(entity, Attributes.SCALE, SCALE_MODIFIER);
        entity.refreshDimensions();
        if (entity.getHealth() > entity.getMaxHealth()) entity.setHealth(entity.getMaxHealth());
    }

    public static void reconcileSecondForm(LivingEntity entity) {
        if (entity instanceof Player player
                && PlayerCorpseComponent.get(player).getVariant()
                == PlayerCorpseComponent.VARIANT_SHICHAOZHIZI) {
            applySecondFormAttributes(entity);
        } else {
            removeSecondFormAttributes(entity);
        }
    }

    private static void setTarget(LivingEntity entity, Holder<Attribute> attribute,
                                  Identifier id, double target) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) return;
        instance.addOrReplacePermanentModifier(new AttributeModifier(
                id, target - instance.getBaseValue(), AttributeModifier.Operation.ADD_VALUE));
    }

    private static void remove(LivingEntity entity, Holder<Attribute> attribute, Identifier id) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) instance.removeModifier(id);
    }
}
