package xiaoshi2022.corpseorigin.growth;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.InnerPowerManager;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.registry.ModItems;

/** One authoritative gate for item attacks, item use, skill learning and activation. */
public final class WeaponEligibility {
    private WeaponEligibility() {}
    public static boolean vampire(ServerPlayer p){
        String role=CharacterManager.getInstance().getPlayerCharacterId(p);
        return VampireRules.isVampire(role,SurvivalGrowth.has(p,"vampire"));
    }
    public static boolean lineage(ServerPlayer p){
        String role=CharacterManager.getInstance().getPlayerCharacterId(p);
        return role.equals("longyou")||role.equals("zhaoritian")
                ||PlayerCharacterData.get(p).hasLearned(p.getUUID(),"tiangang_zhi");
    }
    public static boolean innerInheritance(ServerPlayer p){
        return InnerPowerManager.getMaxInnerPower(p)>0;
    }
    /** 当前是不是"尸兄身体" —— 尸棍这类尸兄专属兵器的持有资格，也是尸棍招式的资格。 */
    public static boolean corpseBody(ServerPlayer p){
        return xiaoshi2022.corpseorigin.component.PlayerCorpseComponent.isCorpse(p);
    }
    public static Component skillReason(ServerPlayer p,String skill){
        if((skill.equals("blood_wing_blade")||skill.equals("dark_siphon"))&&!vampire(p))
            return Component.translatable("message.corpseorigin.weapon_eligibility.text_01");
        if(skill.equals("tian_gang_blood_lotus")&&(!lineage(p)||!innerInheritance(p)))
            return Component.translatable("message.corpseorigin.weapon_eligibility.text_02");
        // 尸棍那两招是尸兄身体的招式：人形角色（含人类鬼棍）拿到尸棍也不该学会、更不该放出来
        if((skill.equals("guigun_resonance")||skill.equals("guigun_crush"))&&!corpseBody(p))
            return Component.translatable("message.corpseorigin.weapon_eligibility.text_04");
        return null;
    }
    public static Component itemReason(ServerPlayer p,ItemStack stack){
        // 只有二阶巨阙要求进化等级≥2；一阶巨阙人人可用
        if(stack.getItem() instanceof xiaoshi2022.corpseorigin.item.sword.JuQue juque && juque.evolutionGated()
                &&xiaoshi2022.corpseorigin.skill.EvolutionManager.getLevel(PlayerCharacterData.get(p).getEarnedPoints(p.getUUID()))<2)
            return Component.translatable("message.corpseorigin.weapon_eligibility.text_03");
        // 尸棍是尸兄身体的兵器：人类形态（含人类鬼棍）连挥都挥不动它（三节棍才是给人形用的）
        if(stack.is(ModItems.GUIGUN_CLUB)&&!corpseBody(p))
            return Component.translatable("message.corpseorigin.weapon_eligibility.text_04");
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
