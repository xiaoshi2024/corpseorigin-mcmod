package xiaoshi2022.corpseorigin.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.ICharacter;
import xiaoshi2022.corpseorigin.registry.ModDataComponents;
import xiaoshi2022.corpseorigin.registry.ModItems;
import xiaoshi2022.corpseorigin.skill.ISkill;

import java.util.function.Consumer;

/**
 * 角色选择书 —— 右键使用后切换为书里记录的角色。
 * <p>
 * 所有角色的书共用同一个物品，靠 {@link ModDataComponents#CHARACTER_ID} 数据组件区分，
 * 因此显示名称里会带上角色名，方便在创造物品栏里搜索。
 */
public class CharacterBookItem extends Item {

    public CharacterBookItem(Properties properties) {
        super(properties);
    }

    /** 生成指定角色的「角色选择书」 */
    public static ItemStack createStack(String characterId) {
        ItemStack stack = new ItemStack(ModItems.CHARACTER_BOOK);
        stack.set(ModDataComponents.CHARACTER_ID, characterId);
        return stack;
    }

    /** 读取书里记录的角色 ID（没有则为 null） */
    public static String getCharacterId(ItemStack stack) {
        return stack.get(ModDataComponents.CHARACTER_ID);
    }

    @Override
    public Component getName(ItemStack stack) {
        String characterId = getCharacterId(stack);
        if (characterId == null) {
            return super.getName(stack);
        }
        ICharacter character = CharacterManager.getInstance().getCharacter(characterId);
        return Component.translatable("item.corpseorigin.character_book.named", character.getName());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        String characterId = getCharacterId(stack);
        if (characterId == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        CharacterManager manager = CharacterManager.getInstance();
        ICharacter character = manager.getCharacter(characterId);

        if (characterId.equals(manager.getPlayerCharacterId(player))) {
            serverPlayer.sendOverlayMessage(Component.translatable(
                            "message.corpseorigin.character_book.already", character.getName())
                    .withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        if (!manager.setPlayerCharacter(player, characterId)) {
            return InteractionResult.FAIL;
        }

        serverPlayer.sendOverlayMessage(Component.translatable(
                        "message.corpseorigin.character_book.success", character.getName())
                .withStyle(ChatFormatting.GREEN));

        // 角色自带的技能列表（技能仍需在技能树里解锁）
        if (!character.getSkills().isEmpty()) {
            serverPlayer.sendOverlayMessage(Component.translatable(
                            "message.corpseorigin.character_book.skills")
                    .withStyle(ChatFormatting.GOLD));
            for (ISkill skill : character.getSkills()) {
                serverPlayer.sendOverlayMessage(
                        Component.literal("• ").append(skill.getName()).withStyle(ChatFormatting.YELLOW));
            }
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> builder, TooltipFlag flag) {
        String characterId = getCharacterId(stack);
        if (characterId != null) {
            ICharacter character = CharacterManager.getInstance().getCharacter(characterId);
            builder.accept(character.getDescription().copy().withStyle(ChatFormatting.GRAY));
            builder.accept(Component.translatable("item.corpseorigin.character_book.tooltip")
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }
        super.appendHoverText(stack, context, display, builder, flag);
    }
}
