package xiaoshi2022.corpseorigin.item;

import net.minecraft.nbt.CompoundTag;
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
import xiaoshi2022.corpseorigin.character.MortalCharacter;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.registry.ModDataComponents;
import xiaoshi2022.corpseorigin.registry.ModItems;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * 角色记忆书 —— "死一次就把角色废掉"的保险。
 * <p>
 * 死亡自动夺舍时，旧身体要是<b>没有空克隆仓可放</b>，它会被直接丢弃；而那具身体带着的角色数据
 * （身份 + 已学技能 + 进化点，见 {@code CharacterShellStateComponent}）也会跟着消失 ——
 * 换过去的新身体只是一具按完成度裁剪过的克隆体，技能被随机砍、点数还缩水。
 * 这条通路现在会把那份角色数据封成这本书，掉在<b>死亡点</b>，玩家回来捡起来右键即可恢复。
 * <p>
 * 数据格式就是 {@code CharacterShellStateComponent#writeNbt} 的原样输出：{@code {Uuid, Data}}。
 * 其中 {@code Uuid} 是<b>原主人</b>，用来做「只有本人能用」的锁 —— 否则这书就成了
 * "把角色打包送人/交易"的道具。
 * <p>
 * 恢复先走正规换角色流程 {@code CharacterManager#setPlayerCharacter}（客户端同步、形态自洽、
 * 属性重套、内力重置都在里面），之后再把记忆里的技能与进化点盖回去 —— 因为换角色会清空已学技能。
 */
public class CharacterMemoryItem extends Item {

    public CharacterMemoryItem(Properties properties) {
        super(properties);
    }

    /** 把一份角色数据 NBT（{@code CharacterShellStateComponent#writeNbt} 的输出）封成记忆书 */
    public static ItemStack create(CompoundTag memory) {
        ItemStack stack = new ItemStack(ModItems.CHARACTER_MEMORY);
        stack.set(ModDataComponents.CHARACTER_MEMORY, memory.copy());
        return stack;
    }

    /** 书里那份记忆；空书返回 null */
    public static CompoundTag memoryOf(ItemStack stack) {
        return stack.get(ModDataComponents.CHARACTER_MEMORY);
    }

    /** 记忆里记的是谁的角色（原主人 UUID）；读不出来返回 null */
    public static UUID ownerOf(ItemStack stack) {
        CompoundTag memory = memoryOf(stack);
        if (memory == null) return null;
        return memory.getString("Uuid").map(raw -> {
            try {
                return UUID.fromString(raw);
            } catch (IllegalArgumentException invalid) {
                return null;
            }
        }).orElse(null);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer server)) return InteractionResult.PASS;

        ItemStack stack = player.getItemInHand(hand);
        CompoundTag memory = memoryOf(stack);
        CompoundTag data = CharacterMemoryData.characterData(memory);
        UUID owner = ownerOf(stack);
        if (data == null || owner == null) {
            server.sendOverlayMessage(Component.translatable("item.corpseorigin.character_memory.broken"));
            return InteractionResult.FAIL;
        }
        // ★ 只有原主人能读 —— 否则这书就成了"把角色打包送人"的道具
        if (!owner.equals(server.getUUID())) {
            server.sendOverlayMessage(Component.translatable("item.corpseorigin.character_memory.not_owner"));
            return InteractionResult.FAIL;
        }

        String characterId = data.getStringOr("CharacterId", MortalCharacter.ID);
        CharacterManager manager = CharacterManager.getInstance();
        if (manager.getRegisteredCharacters().stream().noneMatch(c -> c.getId().equals(characterId))) {
            server.sendOverlayMessage(Component.translatable("item.corpseorigin.character_memory.broken"));
            return InteractionResult.FAIL;
        }
        // ① 正规换角色流程：客户端同步 / 形态自洽 / 属性重套 / 内力重置都在这条路上
        if (!manager.setPlayerCharacter(server, characterId)) return InteractionResult.FAIL;
        // ② 再把记忆里的已学技能与进化点盖回去 —— 上一步刚把已学技能清空过
        PlayerCharacterData.get(server).readNbt(server.getUUID(), data);
        xiaoshi2022.corpseorigin.skill.EvolutionStats.reconcile(server);
        manager.syncToClient(server);

        ICharacter character = CharacterManager.getInstance().getCharacter(characterId);
        server.sendOverlayMessage(Component.translatable("item.corpseorigin.character_memory.restored",
                character == null ? Component.literal(characterId) : character.getName()));

        if (!server.isCreative()) stack.shrink(1);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        CompoundTag memory = CharacterMemoryData.characterData(memoryOf(stack));
        if (memory == null) {
            tooltip.accept(Component.translatable("item.corpseorigin.character_memory.tooltip.empty"));
            return;
        }
        String characterId = memory.getStringOr("CharacterId", MortalCharacter.ID);
        ICharacter character = CharacterManager.getInstance().getCharacter(characterId);
        tooltip.accept(Component.translatable("item.corpseorigin.character_memory.tooltip",
                character == null ? Component.literal(characterId) : character.getName()));
        tooltip.accept(Component.translatable("item.corpseorigin.character_memory.tooltip.stats",
                memory.getList("LearnedSkills").map(list -> list.size()).orElse(0),
                memory.getIntOr("Earned", 0),
                memory.getIntOr("Available", 0)));
    }
}
