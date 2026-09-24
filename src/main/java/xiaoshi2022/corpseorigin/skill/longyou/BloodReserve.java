package xiaoshi2022.corpseorigin.skill.longyou;

import net.fabricmc.fabric.api.attachment.v1.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.codec.ByteBufCodecs;
import xiaoshi2022.corpseorigin.CorpseOrigin;

public final class BloodReserve {
    public static final int MAX = 600, RESTORE_COST = 200;
    private static final java.util.Map<java.util.UUID,xiaoshi2022.corpseorigin.growth.BalanceRules.Window> COMBAT=new java.util.HashMap<>();
    public static void addCombat(ServerPlayer p,float damage){
        if(!p.isAlive()||p.isSpectator()||!Float.isFinite(damage)||damage<=0)return;
        int limit=Math.clamp(xiaoshi2022.corpseorigin.config.CorpseConfig.get().growth.combatBloodPerSecond,0,600);
        int amount=COMBAT.computeIfAbsent(p.getUUID(),k->new xiaoshi2022.corpseorigin.growth.BalanceRules.Window())
                .take(p.level().getGameTime(),(int)Math.min(600,Math.ceil(damage*.25)),limit);
        if(amount>0)add(p,amount);
    }
    public static final AttachmentType<Integer> VALUE = AttachmentRegistry.create(CorpseOrigin.id("blood_reserve"),
            b -> b.initializer(() -> 0).persistent(com.mojang.serialization.Codec.INT)
                    .syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.all()));
    public static void init() {
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((h,s)->COMBAT.remove(h.player.getUUID()));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->COMBAT.clear());
        net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.register((player, level, hand) -> {
            var stack = player.getItemInHand(hand);
            if (!player.isShiftKeyDown() || !stack.is(xiaoshi2022.corpseorigin.registry.ModItems.ZBR_FLESH))
                return net.minecraft.world.InteractionResult.PASS;
            if (player instanceof ServerPlayer serverPlayer) {
                if (!isEligible(serverPlayer)) return net.minecraft.world.InteractionResult.PASS;
                if (serverPlayer.getAttachedOrCreate(VALUE) >= MAX
                        && !xiaoshi2022.corpseorigin.growth.FreeGrowth.isFree(serverPlayer))
                    return net.minecraft.world.InteractionResult.FAIL;
                stack.shrink(1);
                int gained = Math.min(20, MAX-get(serverPlayer));
                add(serverPlayer, 20);
                xiaoshi2022.corpseorigin.growth.SurvivalGrowth.fleshConsumed(serverPlayer);
                serverPlayer.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.corpseorigin.blood_reserve.text_01", gained));
            } else {
                return net.minecraft.world.InteractionResult.PASS;
            }
            return net.minecraft.world.InteractionResult.SUCCESS;
        });
    }
    public static boolean isEligible(ServerPlayer player) {
        if (xiaoshi2022.corpseorigin.character.PlayerCharacterData.get(player).getLearnedSkills(player.getUUID())
                .stream().anyMatch(path -> xiaoshi2022.corpseorigin.skill.SkillResourceRules.cost(path, 0).blood() > 0)
                || xiaoshi2022.corpseorigin.growth.WeaponEligibility.vampire(player)) return true;
        String role = xiaoshi2022.corpseorigin.character.CharacterManager.getInstance().getPlayerCharacterId(player);
        int level = xiaoshi2022.corpseorigin.skill.EvolutionManager.getLevel(
                xiaoshi2022.corpseorigin.character.PlayerCharacterData.get(player).getEarnedPoints(player.getUUID()));
        return BloodReserveRules.eligible(role,
                xiaoshi2022.corpseorigin.component.PlayerCorpseComponent.isCorpse(player), level)
                || xiaoshi2022.corpseorigin.component.PlayerCorpseComponent.isCorpse(player)
                && (xiaoshi2022.corpseorigin.growth.SurvivalGrowth.has(player,"wings")||xiaoshi2022.corpseorigin.growth.SurvivalGrowth.has(player,"gills"));
    }
    public static void add(ServerPlayer p, int amount) {
        p.setAttached(VALUE, (int)Math.clamp((long)p.getAttachedOrCreate(VALUE) + amount, 0, MAX));
    }
    public static boolean spend(ServerPlayer p) {
        if (p.getAttachedOrCreate(VALUE) < RESTORE_COST) {
            p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.corpseorigin.blood_reserve.text_02"));
            return false;
        }
        add(p, -RESTORE_COST);
        return true;
    }

    /** 当前气血余量 */
    public static int get(ServerPlayer p) {
        return p.getAttachedOrCreate(VALUE);
    }

    /**
     * 通用气血消耗（左护法「唤龙」复活青龙等用）。
     * 余量不足返回 false（调用方自己提示），不做任何改动。
     */
    public static boolean spend(ServerPlayer p, int amount) {
        if(amount<0)return false;
        if (p.getAttachedOrCreate(VALUE) < amount) {
            return false;
        }
        add(p, -amount);
        return true;
    }
}
