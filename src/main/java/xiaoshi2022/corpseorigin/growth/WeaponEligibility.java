package xiaoshi2022.corpseorigin.growth;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import xiaoshi2022.corpseorigin.character.*;
import xiaoshi2022.corpseorigin.registry.ModItems;

/** One authoritative gate for item attacks, item use, skill learning and activation. */
public final class WeaponEligibility {
    private WeaponEligibility() {}
    public static boolean vampire(ServerPlayer p){
        String role=CharacterManager.getInstance().getPlayerCharacterId(p);
        return role.equals("k")||role.equals("heixiaofei")||SurvivalGrowth.has(p,"vampire");
    }
    public static boolean lineage(ServerPlayer p){
        String role=CharacterManager.getInstance().getPlayerCharacterId(p);
        return role.equals("longyou")||role.equals("zhaoritian")
                ||PlayerCharacterData.get(p).hasLearned(p.getUUID(),"tiangang_zhi");
    }
    public static boolean innerInheritance(ServerPlayer p){
        return CharacterManager.getInstance().getPlayerCharacter(p).getMaxInnerPower()>0
                ||p.getAttachedOrCreate(SurvivalGrowth.JOURNAL).getBooleanOr("inner_power",false);
    }
    public static Component skillReason(ServerPlayer p,String skill){
        if((skill.equals("blood_wing_blade")||skill.equals("dark_siphon"))&&!vampire(p))
            return Component.literal("血翼黑刃需要吸血鬼体质：K、黑小飞或已继承吸血体质");
        if(skill.equals("tian_gang_blood_lotus")&&(!lineage(p)||!innerInheritance(p)))
            return Component.literal("天罡匙需要修习天罡一脉（天罡·智）并获得内力传承");
        return null;
    }
    public static Component itemReason(ServerPlayer p,ItemStack stack){
        if(stack.getItem() instanceof xiaoshi2022.corpseorigin.item.sword.JuQue
                &&xiaoshi2022.corpseorigin.skill.EvolutionManager.getLevel(PlayerCharacterData.get(p).getEarnedPoints(p.getUUID()))<2)
            return Component.literal("巨阙需要达到人2（进化等级2）及以上");
        if(stack.is(ModItems.BLOOD_WING_BLADE))return skillReason(p,"blood_wing_blade");
        if(stack.is(ModItems.TIAN_GANG_KEY))return skillReason(p,"tian_gang_blood_lotus");
        return null;
    }
    public static boolean allow(ServerPlayer p,ItemStack stack){
        var reason=itemReason(p,stack);if(reason==null)return true;p.sendOverlayMessage(reason);return false;
    }
    public static void register(){
        net.fabricmc.fabric.api.event.player.AttackEntityCallback.EVENT.register((p,l,h,e,hit)->
            p instanceof ServerPlayer s&&!allow(s,p.getMainHandItem())?net.minecraft.world.InteractionResult.FAIL:net.minecraft.world.InteractionResult.PASS);
        net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.register((p,l,h)->
            p instanceof ServerPlayer s&&!allow(s,p.getItemInHand(h))?net.minecraft.world.InteractionResult.FAIL:net.minecraft.world.InteractionResult.PASS);
    }
}
