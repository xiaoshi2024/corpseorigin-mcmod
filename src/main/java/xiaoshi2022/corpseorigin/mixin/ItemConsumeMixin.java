package xiaoshi2022.corpseorigin.mixin;

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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
import xiaoshi2022.corpseorigin.skill.longyou.BloodReserve;

/**
 * 开胃奶体质：吃生肉即可直接补<b>气血</b>。
 * <p>
 * 这里的"气血"是 {@link BloodReserve} 那条血肉储备（0~{@link BloodReserve#MAX}，HUD 上那条电池条），
 * <b>不是原版血量</b> —— 本模组的感染条、内力条、气血条是三份不同资源。
 * <p>
 * 钩子挂在 {@link Item#finishUsingItem} 的 TAIL —— 26.2 里所有"吃完"的效果都从
 * {@code Consumable.onConsume} 结算，这一步之后进食动作已完成。
 * 生肉判定走 Fabric 约定标签（整合包 / 其他模组的生肉同样生效）。
 */
@Mixin(Item.class)
public class ItemConsumeMixin {

    /** 吃生肉补的气血（与「尸兄肉块」zbr_flesh 同档）。 */
    private static final int RAW_MEAT_BLOOD = 20;
    /** 吃生鱼补的气血（小份）。 */
    private static final int RAW_FISH_BLOOD = 10;

    @Inject(method = "finishUsingItem", at = @At("TAIL"))
    private void corpseorigin$onFinishUsing(ItemStack stack, Level level, LivingEntity entity,
                                            CallbackInfoReturnable<ItemStack> cir) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) return;
        if (!KaiWeiNai.ID.equals(CharacterManager.getInstance().getPlayerCharacterId(player))) return;

        boolean meat = stack.is(ConventionalItemTags.RAW_MEAT_FOODS);
        boolean fish = !meat && stack.is(ConventionalItemTags.RAW_FISH_FOODS);
        if (!meat && !fish) return;
        // 气血条本来就是满的（或者这个人根本不显示气血条）就不动，省掉一次无意义的同步
        if (!BloodReserve.isEligible(player) || BloodReserve.get(player) >= BloodReserve.MAX) return;

        int before = BloodReserve.get(player);
        BloodReserve.add(player, meat ? RAW_MEAT_BLOOD : RAW_FISH_BLOOD);
        int gained = BloodReserve.get(player) - before;
        player.sendOverlayMessage(Component.translatable("message.corpseorigin.blood_reserve.text_01", gained));

        // 红心粒子：吃了多少就飘几颗
        ((ServerLevel) level).sendParticles(ParticleTypes.HEART,
                player.getX(), player.getY() + 1.1, player.getZ(),
                meat ? 4 : 2, 0.3, 0.4, 0.3, 0.0);
    }
}
