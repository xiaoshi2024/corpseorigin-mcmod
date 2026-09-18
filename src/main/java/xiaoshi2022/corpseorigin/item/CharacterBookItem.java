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
import xiaoshi2022.corpseorigin.client.CharacterBookScreen;
import xiaoshi2022.corpseorigin.registry.ModDataComponents;
import xiaoshi2022.corpseorigin.registry.ModItems;
import xiaoshi2022.corpseorigin.skill.ISkill;

import java.util.function.Consumer;

/**
 * 角色选择书。两种用法，同一个物品：
 * <ul>
 *   <li><b>统一书</b>（不带 {@link ModDataComponents#CHARACTER_ID}）—— 生存开局那一本。
 *       右键打开 {@link CharacterBookScreen}，从全部已注册角色里挑一位，选完这本书就消耗掉；</li>
 *   <li><b>绑定书</b>（带角色 ID，{@link #createStack}）—— 右键直接切换成书里写的那个角色，
 *       方便成就奖励 / 指令 / 整合包定向发某一角色。</li>
 * </ul>
 * 两种书的"换人 + 提示 + 扣书"走同一套服务端逻辑（见 {@link #selectFromBook} 与 {@link #announceAcquired}）。
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

    /**
     * 统一的一本角色书（不带角色 ID）。
     * <p>
     * 右键打开 {@link xiaoshi2022.corpseorigin.client.CharacterBookScreen}，从全部已注册角色里挑一位，
     * 选完这本书就消耗掉 —— 也就是"生存开局选人"那一套。
     */
    public static ItemStack createUnboundStack() {
        return new ItemStack(ModItems.CHARACTER_BOOK);
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
            // 统一书：只有客户端要做事 —— 打开选人界面。服务端这一步什么都不做，
            // 真正的"换人 + 扣书"等界面里选完发回 CharacterBookSelectC2S 再在服务端处理
            // （见 #selectFromBook），所以这里两边都返回 SUCCESS，客户端不会把它当成一次挥空。
            if (level.isClientSide()) {
                CharacterBookScreen.open();
            }
            return InteractionResult.SUCCESS;
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

        announceAcquired(serverPlayer, character);
        consume(serverPlayer, hand);
        return InteractionResult.SUCCESS;
    }

    /**
     * 「统一角色书」的选择落地（服务端）—— 由 {@code CharacterBookSelectC2S} 触发。
     * <p>
     * 三重校验：手上确实拿着角色书、选的角色和当前不同、角色应用成功。全部通过才扣掉一本；
     * 选了当前角色时只提示、<b>不扣书</b>，免得玩家白丢一本。
     */
    public static boolean selectFromBook(ServerPlayer player, String characterId) {
        InteractionHand hand = findBookHand(player);
        if (hand == null) {
            return false;   // 手上没书：静默忽略（接口界面只是发包，判定全在这边）
        }

        CharacterManager manager = CharacterManager.getInstance();
        ICharacter character = manager.getCharacter(characterId);

        if (characterId.equals(manager.getPlayerCharacterId(player))) {
            player.sendOverlayMessage(Component.translatable(
                            "message.corpseorigin.character_book.already", character.getName())
                    .withStyle(ChatFormatting.RED));
            return false;   // 没换人 → 不扣书
        }

        if (!manager.setPlayerCharacter(player, characterId)) {
            return false;
        }

        announceAcquired(player, character);
        consume(player, hand);
        return true;
    }

    /** 主手 / 副手哪只手拿着角色书；都没拿返回 null */
    private static InteractionHand findBookHand(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).getItem() instanceof CharacterBookItem) {
                return hand;
            }
        }
        return null;
    }

    /** 换人成功的提示：成功语 + 该角色自带的技能列表（技能仍需在技能树里解锁） */
    private static void announceAcquired(ServerPlayer player, ICharacter character) {
        player.sendOverlayMessage(Component.translatable(
                        "message.corpseorigin.character_book.success", character.getName())
                .withStyle(ChatFormatting.GREEN));

        if (character.getSkills().isEmpty()) {
            return;
        }
        player.sendOverlayMessage(Component.translatable(
                        "message.corpseorigin.character_book.skills")
                .withStyle(ChatFormatting.GOLD));
        for (ISkill skill : character.getSkills()) {
            player.sendOverlayMessage(
                    Component.literal("• ").append(skill.getName()).withStyle(ChatFormatting.YELLOW));
        }
    }

    /** 消耗一本角色书（创造模式不扣） */
    private static void consume(ServerPlayer player, InteractionHand hand) {
        if (player.getAbilities().instabuild) {
            return;
        }
        player.getItemInHand(hand).shrink(1);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> builder, TooltipFlag flag) {
        String characterId = getCharacterId(stack);
        if (characterId == null) {
            builder.accept(Component.translatable("item.corpseorigin.character_book.tooltip.unbound")
                    .withStyle(ChatFormatting.DARK_PURPLE));
        } else {
            ICharacter character = CharacterManager.getInstance().getCharacter(characterId);
            builder.accept(character.getDescription().copy().withStyle(ChatFormatting.GRAY));
            builder.accept(Component.translatable("item.corpseorigin.character_book.tooltip")
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }
        super.appendHoverText(stack, context, display, builder, flag);
    }
}
