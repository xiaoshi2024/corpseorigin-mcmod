package xiaoshi2022.corpseorigin.mixin;

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.KaiWeiNai;

/**
 * 开胃奶体质：吃生肉即可直接恢复气血。
 * <p>
 * 钩子挂在 {@link Item#finishUsingItem} 的 TAIL —— 26.2 里所有"吃完"的效果都从
 * {@code Consumable.onConsume} 结算，这一步之后进食动作已完成。
 * 生肉判定走 Fabric 约定标签（整合包/其他模组的生肉同样生效）。
 */
@Mixin(Item.class)
public class ItemConsumeMixin {

    /** 吃生肉恢复的血量（2 颗心）。 */
    private static final float RAW_MEAT_HEAL = 4.0F;
    /** 吃生鱼恢复的血量（1 颗心）。 */
    private static final float RAW_FISH_HEAL = 2.0F;

    @Inject(method = "finishUsingItem", at = @At("TAIL"))
    private void corpseorigin$onFinishUsing(ItemStack stack, Level level, LivingEntity entity,
                                            CallbackInfoReturnable<ItemStack> cir) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) return;
        if (!KaiWeiNai.ID.equals(CharacterManager.getInstance().getPlayerCharacterId(player))) return;

        boolean meat = stack.is(ConventionalItemTags.RAW_MEAT_FOODS);
        boolean fish = !meat && stack.is(ConventionalItemTags.RAW_FISH_FOODS);
        if (!meat && !fish) return;
        // 满血就不再播治疗反馈（heal 本身是 no-op，但粒子不能误导）
        if (player.getHealth() >= player.getMaxHealth()) return;

        float amount = meat ? RAW_MEAT_HEAL : RAW_FISH_HEAL;
        player.heal(amount);

        // 红心粒子：吃了多少就飘几颗
        ((ServerLevel) level).sendParticles(ParticleTypes.HEART,
                player.getX(), player.getY() + 1.1, player.getZ(),
                meat ? 4 : 2, 0.3, 0.4, 0.3, 0.0);
    }
}
