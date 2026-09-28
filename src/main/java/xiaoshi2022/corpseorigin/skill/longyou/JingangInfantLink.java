package xiaoshi2022.corpseorigin.skill.longyou;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.*;
import xiaoshi2022.corpseorigin.skill.chapter.CreatureAbilities;
import xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat;

public final class JingangInfantLink {
    public static final AttachmentType<String> PARENT = AttachmentRegistry.create(CorpseOrigin.id("jingang_infant_parent"),
            b -> b.initializer(() -> "").persistent(Codec.STRING)
                    .syncWith(ByteBufCodecs.STRING_UTF8, AttachmentSyncPredicate.all()));
    public static final AttachmentType<String> CHILD = AttachmentRegistry.create(CorpseOrigin.id("jingang_infant_child"),
            b -> b.initializer(() -> "").persistent(Codec.STRING)
                    .syncWith(ByteBufCodecs.STRING_UTF8, AttachmentSyncPredicate.all()));
    private JingangInfantLink() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer infant : server.getPlayerList().getPlayers()) {
                String parentId = infant.getAttachedOrCreate(PARENT);
                if (parentId.isEmpty()) continue;
                ServerPlayer parent;
                try { parent = server.getPlayerList().getPlayer(java.util.UUID.fromString(parentId)); }
                catch (IllegalArgumentException ignored) { unlink(infant, null, false); continue; }
                // Body stage is independent of the shared evolution/skill-point level.
                boolean secondStage = !infant.getAttachedOrCreate(CreatureAbilities.INFANT);
                boolean jingang = JinGangZb.ID.equals(CharacterManager.getInstance().getPlayerCharacterId(infant));
                if (parent == null || !parent.isAlive() || !infant.isAlive()
                        || parent.isSpectator() || infant.isSpectator() || !jingang
                        || !infant.getUUID().toString().equals(parent.getAttachedOrCreate(CHILD))
                        || infant.level() != parent.level() || secondStage) {
                    unlink(infant, parent, jingang && secondStage && infant.isAlive());
                    continue;
                }
                if (infant.distanceToSqr(parent) > 24 * 24)
                    infant.teleportTo(parent.getX() + 1, parent.getY(), parent.getZ());
                if (infant.tickCount % 3 == 0) drawCord(infant, parent);
            }
        });
    }

    public static ServerPlayer findCandidate(ServerPlayer owner) {
        ServerPlayer best = null;
        double bestDistance = 64 * 64;
        for (ServerPlayer player : owner.level().getServer().getPlayerList().getPlayers()) {
            // Both forms can be recalled: selecting this role initially gives the adult form.
            if (player == owner || player.level() != owner.level() || !player.isAlive() || player.isSpectator()
                    || !JinGangZb.ID.equals(CharacterManager.getInstance().getPlayerCharacterId(player))) continue;
            double distance = player.distanceToSqr(owner);
            if (distance <= bestDistance) { bestDistance = distance; best = player; }
        }
        return best;
    }

    public static boolean converge(ServerPlayer owner) {
        ServerPlayer infant = findCandidate(owner);
        if (infant == null) return false;
        detachExisting(owner);
        String oldParent = infant.getAttachedOrCreate(PARENT);
        if (!oldParent.isEmpty()) {
            try {
                ServerPlayer old = owner.level().getServer().getPlayerList().getPlayer(java.util.UUID.fromString(oldParent));
                if (old != null && infant.getUUID().toString().equals(old.getAttachedOrCreate(CHILD)))
                    old.setAttached(CHILD, "");
            } catch (IllegalArgumentException ignored) {}
        }
        owner.setAttached(CHILD, infant.getUUID().toString());
        infant.setAttached(PARENT, owner.getUUID().toString());
        infant.setAttached(CreatureAbilities.INFANT, true);
        infant.setAttached(CreatureAbilities.RAGE_UNTIL, 0L);
        Vec3 side = owner.getLookAngle().cross(new Vec3(0, 1, 0)).normalize();
        infant.teleportTo(owner.getX() + side.x * 1.5, owner.getY(), owner.getZ() + side.z * 1.5);
        owner.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
                "skill.corpseorigin.jingang_infant_convergence.done", infant.getDisplayName()));
        infant.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
                "skill.corpseorigin.jingang_infant_convergence.linked", owner.getDisplayName()));
        return true;
    }

    private static void detachExisting(ServerPlayer owner) {
        String childId = owner.getAttachedOrCreate(CHILD);
        if (childId.isEmpty()) return;
        try {
            ServerPlayer child = owner.level().getServer().getPlayerList().getPlayer(java.util.UUID.fromString(childId));
            if (child != null && owner.getUUID().toString().equals(child.getAttachedOrCreate(PARENT)))
                child.setAttached(PARENT, "");
        } catch (IllegalArgumentException ignored) {}
        owner.setAttached(CHILD, "");
    }

    private static void unlink(ServerPlayer infant, ServerPlayer parent, boolean grown) {
        infant.setAttached(PARENT, "");
        // Disconnecting (logout, dimension change, etc.) is not a body-stage transition.
        if (parent != null && infant.getUUID().toString().equals(parent.getAttachedOrCreate(CHILD)))
            parent.setAttached(CHILD, "");
        if (grown) infant.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
                "skill.corpseorigin.jingang_infant_convergence.grown"));
    }

    private static void drawCord(ServerPlayer infant, ServerPlayer parent) {
        var level = (net.minecraft.server.level.ServerLevel) infant.level();
        Vec3 from = parent.position().add(0, .9, 0);
        Vec3 to = infant.position().add(0, .55, 0);
        Vec3 delta = to.subtract(from);

        // 每格约 8 个点，间距 ~0.125 格，视觉上接近连续细线
        int points = Math.max(8, (int)(delta.length() * 8));

        for (int i = 0; i <= points; i++) {
            double t = i / (double) points;
            Vec3 p = from.add(delta.scale(t)).add(0, -.12 * Math.sin(Math.PI * t), 0);

            // 更暗、更小：0x4a121a + 0.2f
            ChapterCombat.dust(level, p, 0x4a121a, 0.2f);
        }
    }
}
