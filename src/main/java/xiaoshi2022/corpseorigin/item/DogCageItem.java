package xiaoshi2022.corpseorigin.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.XiaoYanZi;
import xiaoshi2022.corpseorigin.entity.HamEntity;
import xiaoshi2022.corpseorigin.registry.ModEntities;
import java.util.function.Consumer;

/** One serialized Ham per cage; capture/release mutate only on the server. */
public class DogCageItem extends Item {
    private static final String HAM = "CapturedHam";
    public static final int FIRE_COOLDOWN = 200;
    public DogCageItem(Properties properties) { super(properties); }

    public static boolean isLoaded(ItemStack stack) {
        return stack.getItem() instanceof DogCageItem
                && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(HAM).isPresent();
    }
    public static ItemStack heldCage(Player player) {
        for (InteractionHand hand : InteractionHand.values())
            if (isLoaded(player.getItemInHand(hand))) return player.getItemInHand(hand);
        return ItemStack.EMPTY;
    }
    public static boolean hasCapturedHam(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++)
            if (isLoaded(player.getInventory().getItem(i))) return true;
        return false;
    }
    private static InteractionResult fail(Player player, String key) {
        if (!player.level().isClientSide()) player.sendOverlayMessage(Component.translatable(key));
        return InteractionResult.FAIL;
    }
    public static InteractionResult capture(ItemStack stack, Player player, HamEntity ham) {
        if (isLoaded(stack)) return fail(player, "item.corpseorigin.dog_cage.full");
        if (!ham.isAlive() || ham.isPassenger() || ham.isVehicle() || ham.isLeashed())
            return fail(player, "item.corpseorigin.dog_cage.busy");
        if (ham.isTame() && !ham.isOwnedBy(player))
            return fail(player, "item.corpseorigin.dog_cage.not_owner");
        if (ham.isTemporarySummon()) return fail(player, "item.corpseorigin.dog_cage.temporary");
        if (player.level() instanceof ServerLevel level) {
            var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
            ham.saveWithoutId(output);
            CompoundTag saved = output.buildResult();
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put(HAM, saved));
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(java.util.List.of(),
                    java.util.List.of(true), java.util.List.of(), java.util.List.of()));
            ham.discard();
            level.playSound(null, player.blockPosition(), SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.PLAYERS, 1, 1);
        }
        return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        return entity instanceof HamEntity ham ? capture(stack, player, ham) : InteractionResult.PASS;
    }
    public static Component fireError(Player player, ItemStack stack) {
        if (!isLoaded(stack)) return Component.translatable("item.corpseorigin.dog_cage.empty");
        if (!XiaoYanZi.ID.equals(CharacterManager.getInstance().getPlayerCharacterId(player)))
            return Component.translatable("item.corpseorigin.dog_cage.xiaoyanzi_only");
        if (player.getCooldowns().isOnCooldown(stack))
            return Component.translatable("item.corpseorigin.dog_cage.cooldown");
        return null;
    }
    public static InteractionResult fire(Player player, ItemStack stack) {
        if (player.level().isClientSide()) return InteractionResult.SUCCESS;
        Component error = fireError(player, stack);
        if (error != null) { player.sendOverlayMessage(error); return InteractionResult.FAIL; }
        ServerLevel level = (ServerLevel) player.level();
        Vec3 direction = player.getLookAngle();
        // Start at the eyes so shooting next to a wall cannot skip through it.
        SmallFireball ball = new SmallFireball(level, player, direction);
        ball.setPos(player.getEyePosition());
        ball.setDeltaMovement(direction.scale(0.8));
        if (!level.addFreshEntity(ball)) return InteractionResult.FAIL;
        player.getCooldowns().addCooldown(stack, FIRE_COOLDOWN);
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
            xiaoshi2022.corpseorigin.network.CorpseNetwork.sendCooldownSync(serverPlayer, "ham_summon", FIRE_COOLDOWN);
        level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1, 1);
        level.sendParticles(ParticleTypes.SMALL_FLAME, player.getX(), player.getEyeY(), player.getZ(),
                8, .1, .1, .1, .02);
        return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        return fire(player, player.getItemInHand(hand));
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        ItemStack stack = context.getItemInHand();
        if (!player.isShiftKeyDown()) return fire(player, stack);
        if (!isLoaded(stack)) return fail(player, "item.corpseorigin.dog_cage.empty");
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        if (!level.mayInteract(player, context.getClickedPos())) return InteractionResult.FAIL;
        HamEntity ham = ModEntities.HAM.create(level, EntitySpawnReason.BUCKET);
        if (ham == null) return InteractionResult.FAIL;
        CompoundTag saved = stack.get(DataComponents.CUSTOM_DATA).copyTag().getCompound(HAM).orElseThrow();
        ham.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
        var pos = context.getClickedPos().relative(context.getClickedFace());
        ham.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        ham.setDeltaMovement(Vec3.ZERO);
        if (!level.getWorldBorder().isWithinBounds(ham.getBoundingBox())
                || !level.noCollision(ham, ham.getBoundingBox()) || level.containsAnyLiquid(ham.getBoundingBox()))
            return fail(player, "item.corpseorigin.dog_cage.no_space");
        if (!level.addFreshEntity(ham)) return fail(player, "item.corpseorigin.dog_cage.no_space");
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(HAM));
        stack.remove(DataComponents.CUSTOM_MODEL_DATA);
        ham.triggerAnim("action", "summon");
        level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.PLAYERS, 1, 1);
        return InteractionResult.SUCCESS;
    }
    @Override public Component getName(ItemStack stack) {
        return Component.translatable(isLoaded(stack) ? "item.corpseorigin.dog_cage.loaded" : "item.corpseorigin.dog_cage");
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                         Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.translatable("item.corpseorigin.dog_cage.instructions"));
    }
}
