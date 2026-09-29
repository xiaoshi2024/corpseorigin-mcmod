package xiaoshi2022.corpseorigin.item;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.growth.BalanceRules;
import xiaoshi2022.corpseorigin.growth.FreeGrowth;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.skill.EvolutionManager;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One registered item per technique, with a shared book model. */
public final class SkillBookItem extends Item {
    private static final Map<String, SkillBookItem> BOOKS = new LinkedHashMap<>();
    private final ISkill skill;

    private SkillBookItem(ISkill skill, Properties properties) {
        super(properties);
        this.skill = skill;
    }

    public static void registerAll() {
        for (ISkill skill : FreeGrowth.skills()) {
            String path = skill.getId().getPath();
            if (BOOKS.containsKey(path)) continue;
            String id = "skill_book_" + path;
            var key = ResourceKey.create(BuiltInRegistries.ITEM.key(), CorpseOrigin.id(id));
            var properties = new Properties().stacksTo(1).rarity(weight(skill) <= 2 ? Rarity.EPIC : Rarity.RARE)
                    .setId(key);
            BOOKS.put(path, Registry.register(BuiltInRegistries.ITEM, CorpseOrigin.id(id),
                    new SkillBookItem(skill, properties)));
        }
    }

    public static Map<String, SkillBookItem> books() { return Map.copyOf(BOOKS); }
    public static List<SkillBookItem> allBooks() { return List.copyOf(BOOKS.values()); }

    public static int weight(ISkill skill) {
        int level = skill.getRequiredLevel();
        if (skill.getSkillType() == SkillType.ULTIMATE || level >= 10 || skill.getCost() >= 18) return 1;
        if (level >= 7 || skill.getCost() >= 10) return 2;
        if (level >= 5 || skill.getCost() >= 6) return 4;
        return 7;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.corpseorigin.skill_book.named", skill.getName());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        if (!"mortal".equals(xiaoshi2022.corpseorigin.character.CharacterManager.getInstance()
                .getPlayerCharacterId(serverPlayer))) {
            serverPlayer.sendOverlayMessage(Component.translatable("item.corpseorigin.skill_book.mortal_only"));
            return InteractionResult.FAIL;
        }
        var data = PlayerCharacterData.get(serverPlayer);
        String path = skill.getId().getPath();
        if (data.hasLearned(serverPlayer.getUUID(), path)) {
            serverPlayer.sendOverlayMessage(Component.translatable("item.corpseorigin.skill_book.known"));
            return InteractionResult.FAIL;
        }
        if (FreeGrowth.skillLimitReached(serverPlayer)) {
            serverPlayer.sendOverlayMessage(Component.translatable("message.corpseorigin.free_growth.skill_limit", FreeGrowth.SKILL_LIMIT));
            return InteractionResult.FAIL;
        }
        int required = BalanceRules.discoveryLevel(skill.getRequiredLevel(), skill.getSkillType() == SkillType.ULTIMATE);
        int current = EvolutionManager.getLevel(data.getEarnedPoints(serverPlayer.getUUID()));
        if (current < required) {
            serverPlayer.sendOverlayMessage(Component.translatable("item.corpseorigin.skill_book.level", required));
            return InteractionResult.FAIL;
        }
        data.learnSkill(serverPlayer.getUUID(), path);
        FreeGrowth.discover(serverPlayer, path);
        FreeGrowth.awaken(serverPlayer);
        CorpseNetwork.sendEvolutionSync(serverPlayer);
        serverPlayer.sendSystemMessage(Component.translatable("item.corpseorigin.skill_book.learned", skill.getName()));
        if (!serverPlayer.getAbilities().instabuild) player.getItemInHand(hand).shrink(1);
        return InteractionResult.SUCCESS;
    }
}
