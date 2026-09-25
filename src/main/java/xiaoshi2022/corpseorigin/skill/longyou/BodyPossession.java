package xiaoshi2022.corpseorigin.skill.longyou;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.chat.Component;
import xiaoshi2022.corpseorigin.character.JinGangZb;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.character.ShiChaoZhiZi;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.entity.CloneAvatarEntity;
import xiaoshi2022.corpseorigin.skill.chapter.QiEffects;

/** Shared server-side entry point. Entity abilities may call possess after approaching a shell. */
public final class BodyPossession {
    public static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<String> SKIN =
            net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.create(
                    xiaoshi2022.corpseorigin.CorpseOrigin.id("possessed_skin"), b -> b.initializer(() -> "")
                    .persistent(com.mojang.serialization.Codec.STRING).syncWith(
                            net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8,
                            net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate.all()));
    public static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<Boolean> HUMAN =
            net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.create(
                    xiaoshi2022.corpseorigin.CorpseOrigin.id("possessor_human"), b -> b.initializer(() -> false)
                    .persistent(com.mojang.serialization.Codec.BOOL).syncWith(
                            net.minecraft.network.codec.ByteBufCodecs.BOOL,
                            net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate.all()));
    public static final String ABANDONED = "corpseorigin:abandoned_body";
    public static final String CAPABLE = "corpseorigin:can_possess";
    private BodyPossession() {}

    /** Register synchronized attachments on both sides before joining a world. */
    public static void init() {}

    public static boolean canPossess(LivingEntity actor) {
        if (!(actor instanceof ServerPlayer player)) return false;
        String role = CharacterManager.getInstance().getPlayerCharacterId(player);
        // 金刚尸兄和尸巢之子（少教主）都属于可夺舍意识体。
        return JinGangZb.ID.equals(role) || ShiChaoZhiZi.ID.equals(role);
    }

    /** Keep the possessor's identity, appearance, inventory, health and skills intact. */
    public static boolean possess(LivingEntity actor, CloneAvatarEntity body) {
        if (actor == body || actor.level().isClientSide() || !actor.isAlive() || !body.isAlive()
                || actor.level() != body.level() || actor.distanceToSqr(body) > 16
                || !actor.hasLineOfSight(body) || actor.isPassenger() || actor.isVehicle()
                || !body.isAbandonedBody() || !canPossess(actor)) return false;
        body.setAbandonedBody(false);
        if (body.getSkinUuid() != null) actor.setAttached(SKIN, body.getSkinUuid().toString());
        actor.setAttached(HUMAN, true);
        actor.setAttached(UndeadBodyState.STATE, 1);   // ★ 补这一句
        actor.teleportTo(body.getX(), body.getY(), body.getZ());
        body.discard();

        if (actor instanceof ServerPlayer player) {
            PlayerCharacterData data = PlayerCharacterData.get(player);
            data.learnSkill(player.getUUID(), "xuanwu_body");
            data.learnSkill(player.getUUID(), "peel_shell");
            data.learnSkill(player.getUUID(), "jingang_infant_convergence");
            xiaoshi2022.corpseorigin.network.CorpseNetwork.sendEvolutionSync(player);
            player.sendOverlayMessage(Component.translatable("skill.corpseorigin.flesh_abandon.possessed"));
        }
        return true;
    }

    public static void peel(LivingEntity actor) {
        if (actor.level().isClientSide() || actor.getAttachedOrCreate(SKIN).isEmpty()) return;

        // ★ 放宽：穿不死髅体（STATE=1）或已被封印（STATE=2）都能剥，不再强制要求封印
        int state = actor.getAttachedOrCreate(UndeadBodyState.STATE);
        if (state != 1 && state != 2) return;

        actor.setAttached(UndeadBodyState.STATE, 0);
        actor.setAttached(SKIN, "");
        actor.setAttached(HUMAN, true);

        UndeadBodyState.removeXuanwuAttributes(actor);   // ★ 补这句

        // ★ 剥下外皮：收回躯体技能
        if (actor instanceof ServerPlayer player) {
            PlayerCharacterData data = PlayerCharacterData.get(player);
            data.getLearnedSkills(player.getUUID()).remove("xuanwu_body");
            data.getLearnedSkills(player.getUUID()).remove("peel_shell");
            data.getLearnedSkills(player.getUUID()).remove("jingang_infant_convergence");
            data.setDirty();
            xiaoshi2022.corpseorigin.network.CorpseNetwork.sendEvolutionSync(player);
            player.sendOverlayMessage(Component.translatable("skill.corpseorigin.peel_shell.done"));
        }

        if (actor.level() instanceof net.minecraft.server.level.ServerLevel level) {
            QiEffects.burst(level, actor.getX(), actor.getY() + 1, actor.getZ(), 0xc0182a, 20, .5);
        }
    }
}

