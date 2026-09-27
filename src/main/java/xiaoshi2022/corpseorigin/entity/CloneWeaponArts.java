package xiaoshi2022.corpseorigin.entity;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import xiaoshi2022.corpseorigin.item.sword.JuQue;
import xiaoshi2022.corpseorigin.registry.ModEntities;
import xiaoshi2022.corpseorigin.registry.ModItems;

/**
 * 克隆分身使用模组武器的"招式发射台"。
 * <p>
 * 玩家武器技能强绑 {@code ServerPlayer}（{@code SkillManager.activate}），
 * 而剑气类武器的弹道本身（{@link JuQueBeamEntity} / {@link BloodWingBeamEntity}）
 * 的主人就是 {@link LivingEntity}，所以分身可以直接复用：按手上物品发射对应剑气，
 * 伤害 / 耐久损耗 / 冷却都和玩家版保持一致。
 * <p>
 * 鬼棍系列武器的招式走 {@link xiaoshi2022.corpseorigin.skill.chapter.CloneCaster}，
 * 本类只管"远程剑气"。
 */
public final class CloneWeaponArts {

    private CloneWeaponArts() {}

    /** 巨阙剑气 */
    public static final int JUQUE_RANGE = 12;
    /** 血翼黑刃剑气（飞得更远更快） */
    public static final int BLOOD_WING_RANGE = 15;
    /** 近于这个距离就不射剑气，交给近战 goal */
    public static final double MELEE_LIMIT = 3.5;

    /**
     * 克隆体会捡起来当武器用的模组物品白名单。
     * <p>
     * 只收"战斗握持物"（剑 / 棍 / 灯 / 飞轮），药剂、方块、功能道具不收 ——
     * 免得克隆体把地上的杂物全捡走。
     */
    public static boolean isWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return stack.is(ModItems.JUQUE_TW)
                || stack.is(ModItems.BLOOD_WING_BLADE)
                || stack.is(ModItems.GUIGUN_WEAP)
                || stack.is(ModItems.GUIGUN_CLUB)
                || stack.is(ModItems.RED_METEOR_SWORD)
                || stack.is(ModItems.TIAN_GANG_KEY)
                || stack.is(ModItems.BLOOD_LOTUS_LAMP)
                || stack.is(ModItems.BEE_WHEEL)
                || stack.is(ModItems.PARCEL_BOMB) || stack.is(ModItems.BILLIARD_EIGHT) || stack.is(ModItems.DOG_CAGE);
    }

    /** 手上这把武器能不能打出远程剑气。 */
    public static boolean isRangedWeapon(ItemStack stack) {
        return isWeapon(stack) && !stack.is(ModItems.GUIGUN_WEAP) && !stack.is(ModItems.GUIGUN_CLUB);
    }

    /** 这种武器理想的交战距离平方（远了追、近了退）。 */
    public static int preferredRange(ItemStack stack) {
        if (stack.is(ModItems.TIAN_GANG_KEY)) return 24;
        if (stack.is(ModItems.BEE_WHEEL)) return 24;
        if (stack.is(ModItems.BLOOD_LOTUS_LAMP)) return 16;
        return stack.is(ModItems.BLOOD_WING_BLADE) ? BLOOD_WING_RANGE : JUQUE_RANGE;
    }

    /**
     * 尝试发射一发剑气。调用方负责距离 / 视线 / 冷却判定。
     *
     * @return 这一发打完后武器应进入的冷却 tick 数；0 表示没发出来
     */
    public static void face(CloneAvatarEntity caster, LivingEntity target) {
        var direction = target.getBoundingBox().getCenter().subtract(caster.getEyePosition());
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(direction.y, Math.sqrt(direction.x * direction.x + direction.z * direction.z)));
        caster.setYRot(yaw);
        caster.setXRot(pitch);
        caster.setYHeadRot(yaw);
        caster.yBodyRot = yaw;
    }

    public static void tick(CloneAvatarEntity caster) {
        var target = caster.getTarget();
        var stack = caster.getMainHandItem();
        if (target == null || !isWeapon(stack) || !xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat.canHit(caster, target)
                || !caster.hasLineOfSight(target) || caster.distanceToSqr(target) > Math.pow(attackRange(caster,stack), 2)) return;
        fire(caster, stack);
    }

    public static double attackRange(CloneAvatarEntity caster, ItemStack stack) {
        if(stack.is(ModItems.JUQUE_TW)||stack.is(ModItems.BLOOD_WING_BLADE))
            return JuQueBeamEntity.rangeFor(stack.is(ModItems.JUQUE_TW)?JuQue.BEAM_LEVEL:4,JuQueBeamEntity.powerFor(caster));
        return preferredRange(stack);
    }

    public static int fire(CloneAvatarEntity caster, ItemStack stack) {
        String key = "clone_weapon_cd:" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        var data = caster.getAttachedOrCreate(xiaoshi2022.corpseorigin.growth.SurvivalGrowth.BODY);
        if (data.getLongOr(key, 0) > caster.level().getGameTime()) return 0;
        int cooldown = fireTechnique(caster, stack);
        if (cooldown > 0) {
            data = caster.getAttachedOrCreate(xiaoshi2022.corpseorigin.growth.SurvivalGrowth.BODY).copy();
            data.putLong(key, caster.level().getGameTime() + cooldown);
            caster.setAttached(xiaoshi2022.corpseorigin.growth.SurvivalGrowth.BODY, data);
        }
        return cooldown;
    }

    private static int fireTechnique(CloneAvatarEntity caster, ItemStack stack) {
        if (!(caster.level() instanceof net.minecraft.server.level.ServerLevel level)) return 0;
        var target = caster.getTarget();
        if (target == null || !xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat.canHit(caster, target)
                || !caster.hasLineOfSight(target)) return 0;
        face(caster, target);
        if (stack.is(ModItems.GUIGUN_WEAP) || stack.is(ModItems.GUIGUN_CLUB)) {
            if (!xiaoshi2022.corpseorigin.skill.chapter.CloneCaster.castWeapon(caster)) return 0;
            stack.hurtAndBreak(1, caster, EquipmentSlot.MAINHAND);
            return 30;
        }
        if (stack.is(ModItems.BLOOD_LOTUS_LAMP)) {
            ((xiaoshi2022.corpseorigin.item.weapon.BloodLotusLamp) stack.getItem()).drainLife(caster, stack);
            return 10;
        }
        if (stack.is(ModItems.RED_METEOR_SWORD)) {
            if (caster.distanceToSqr(target) > 144) return 0;
            target.hurtServer(level, caster.damageSources().mobAttack(caster), 10);
            var start = caster.getEyePosition();
            xiaoshi2022.corpseorigin.skill.chapter.QiEffects.cloud(level, start.lerp(target.getEyePosition(), .5), 0xeb3349,
                    (float) Math.max(.5, start.distanceTo(target.getEyePosition()) * .5), 12);
            stack.hurtAndBreak(1, caster, EquipmentSlot.MAINHAND);
            caster.swing(InteractionHand.MAIN_HAND, true);
            return 600;
        }
        if (stack.is(ModItems.BEE_WHEEL)) {
            if (SkillConstructEntity.findOwned(caster, "bee_wheel") != null) return 0;
            var wheel = SkillConstructEntity.spawn(caster, ModEntities.BEE_WHEEL, null, 120);
            wheel.setDeltaMovement(caster.getLookAngle().scale(1.5));
            wheel.hurtMarked = true;
            stack.hurtAndBreak(1, caster, EquipmentSlot.MAINHAND);
            caster.swing(InteractionHand.MAIN_HAND, true);
            return 100;
        }
        if (stack.is(ModItems.PARCEL_BOMB) || stack.is(ModItems.BILLIARD_EIGHT)) {
            ChapterBombEntity.launch(caster, stack.getItem());
            stack.shrink(1);
            return 40;
        }
        if (stack.is(ModItems.DOG_CAGE)) {
            return xiaoshi2022.corpseorigin.item.DogCageItem.fireClone(caster, stack) ? 200 : 0;
        }
        if (stack.is(ModItems.TIAN_GANG_KEY)) {
            return xiaoshi2022.corpseorigin.skill.chapter.CloneRoleSkills.startKey(caster) ? 400 : 0;
        }
        if (stack.is(ModItems.JUQUE_TW)) {
            JuQueBeamEntity beam = new JuQueBeamEntity(caster.level(), caster);
            beam.setDamage((float) caster.getAttributeValue(Attributes.ATTACK_DAMAGE) * JuQue.BEAM_DAMAGE_MULT);
            beam.setLevel(JuQue.BEAM_LEVEL);
            beam.shootFromRotation(caster, caster.getXRot(), caster.getYRot(), 0F,
                    beam.getVelocity(), 1.0F);
            caster.level().addFreshEntity(beam);
            stack.hurtAndBreak(1, caster, EquipmentSlot.MAINHAND);
            caster.swing(InteractionHand.MAIN_HAND, true);
            caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.4F, 0.5F);
            return JuQue.COOLDOWN;
        }
        if (stack.is(ModItems.BLOOD_WING_BLADE)) {
            BloodWingBeamEntity beam = new BloodWingBeamEntity(ModEntities.BLOOD_WING_BEAM, caster.level());
            beam.setOwner(caster);
            beam.setPos(caster.getEyePosition().add(0, -0.1, 0));
            beam.setDamage((float) caster.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.5F);
            beam.setLevel(4);
            beam.shootFromRotation(caster, caster.getXRot(), caster.getYRot(), 0F,
                    beam.getVelocity(), 0F);
            caster.level().addFreshEntity(beam);
            stack.hurtAndBreak(1, caster, EquipmentSlot.MAINHAND);
            caster.swing(InteractionHand.MAIN_HAND, true);
            caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 0.8F, 0.9F);
            // 与玩家的血翼黑刃技能冷却一致（10 秒）
            return 200;
        }
        return 0;
    }
}
