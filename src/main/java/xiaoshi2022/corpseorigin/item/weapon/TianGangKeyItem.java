package xiaoshi2022.corpseorigin.item.weapon;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.skill.SkillManager;
import xiaoshi2022.corpseorigin.skill.zhaoritian.TianGangKeySkill;
import java.util.function.Consumer;

/** Blood-lotus treasure: only Zhao Ritian can activate its learned technique. */
public final class TianGangKeyItem extends Item implements GeoItem {
    public static InteractionHand weaponHand(LivingEntity entity) {
        if (entity.getMainHandItem().getItem() instanceof TianGangKeyItem) return InteractionHand.MAIN_HAND;
        if (entity.getOffhandItem().getItem() instanceof TianGangKeyItem) return InteractionHand.OFF_HAND;
        return null;
    }
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");

    public TianGangKeyItem(Properties properties) { super(properties); GeoItem.registerSyncedAnimatable(this); }

    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (hand != weaponHand(player)) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer server)) {
            return InteractionResult.SUCCESS;
        }
        if (TianGangKeySkill.isChanneling(server)) {
            TianGangKeySkill.cancel(server);
            return InteractionResult.SUCCESS;
        }
        String roleId = CharacterManager.getInstance().getPlayerCharacterId(server);
        if (!"zhaoritian".equals(roleId) && !"longyou".equals(roleId)) {
            server.sendOverlayMessage(Component.translatable("item.corpseorigin.tian_gang_key.wrong_role"));
            return InteractionResult.FAIL;
        }
        if (!PlayerCharacterData.get(server).hasLearned(server.getUUID(), TianGangKeySkill.PATH)) {
            server.sendOverlayMessage(Component.translatable("item.corpseorigin.tian_gang_key.unlearned"));
            return InteractionResult.FAIL;
        }
        if (!SkillManager.activate(server, TianGangKeySkill.PATH)) {
            server.sendOverlayMessage(Component.translatable("item.corpseorigin.tian_gang_key.unavailable"));
            return InteractionResult.FAIL;
        }
        return InteractionResult.SUCCESS;
    }

    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return Integer.MAX_VALUE; }
    @Override public ItemUseAnimation getUseAnimation(ItemStack stack) { return ItemUseAnimation.NONE; }
    @Override public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTicks) {
        return false;
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.corpseorigin.tian_gang_key.desc"));
        tooltip.accept(Component.translatable("item.corpseorigin.tian_gang_key.usage"));
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<TianGangKeyItem>("main", 3, test -> test.setAndContinue(IDLE))
                .triggerableAnim("fire", RawAnimation.begin().thenPlay("charge").thenLoop("sustain"))
                .triggerableAnim("rest", RawAnimation.begin().thenPlay("idle")));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private xiaoshi2022.corpseorigin.client.renderer.item.TianGangKeyRenderer renderer;
            @Override public com.geckolib.renderer.GeoItemRenderer<?> getGeoItemRenderer() {
                if (renderer == null) renderer = new xiaoshi2022.corpseorigin.client.renderer.item.TianGangKeyRenderer();
                return renderer;
            }
        });
    }
}
