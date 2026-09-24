package xiaoshi2022.corpseorigin.growth;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import xiaoshi2022.corpseorigin.character.*;
import xiaoshi2022.corpseorigin.skill.*;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;

/** Personal opportunities shared by the two sandbox roles, never by selecting a named role. */
public final class FreeGrowth {
    private FreeGrowth() {}
    public static boolean isFree(String role){return "mortal".equals(role)||"corpse_brother".equals(role);}
    public static boolean isFree(ServerPlayer p){return isFree(CharacterManager.getInstance().getPlayerCharacterId(p));}
    public static List<ISkill> skills(){
        var result=new LinkedHashMap<String,ISkill>();
        for(var role:CharacterManager.getInstance().getRegisteredCharacters())
            if(!isFree(role.getId()))for(var skill:role.getSkills())result.putIfAbsent(skill.getId().getPath(),skill);
        return List.copyOf(result.values());
    }
    public static boolean discovered(ServerPlayer p,String skill){return p.getAttachedOrCreate(SurvivalGrowth.JOURNAL).getBooleanOr("skill:"+skill,false);}
    public static void discover(ServerPlayer p,String path){
        if(!isFree(p)||discovered(p,path))return;
        var skill=skills().stream().filter(s->s.getId().getPath().equals(path)).findFirst();
        if(skill.isEmpty())return;
        var journal=p.getAttachedOrCreate(SurvivalGrowth.JOURNAL).copy();journal.putBoolean("skill:"+path,true);p.setAttached(SurvivalGrowth.JOURNAL,journal);
        p.sendSystemMessage(Component.translatable("message.corpseorigin.opportunity.discovered", skill.get().getName()));
    }
    public static void awaken(ServerPlayer p){
        if(!isFree(p))return;
        var journal=p.getAttachedOrCreate(SurvivalGrowth.JOURNAL).copy();
        if(journal.getBooleanOr("inner_power",false))return;
        journal.putBoolean("inner_power",true);p.setAttached(SurvivalGrowth.JOURNAL,journal);
        InnerPowerManager.reset(p);
        p.sendSystemMessage(Component.translatable("message.corpseorigin.free_growth.text_02"));
    }
    public static int innerPower(ServerPlayer p){
        if(!isFree(p)
                ||!p.getAttachedOrCreate(SurvivalGrowth.JOURNAL).getBooleanOr("inner_power",false))return 0;
        return 100+20*(EvolutionManager.getLevel(PlayerCharacterData.get(p).getEarnedPoints(p.getUUID()))-1);
    }
    public static void opportunity(ServerPlayer p,String event){
        if(!isFree(p))return;
        var journal=p.getAttachedOrCreate(SurvivalGrowth.JOURNAL).copy();
        if(journal.getBooleanOr("free_event:"+event,false))return;
        journal.putBoolean("free_event:"+event,true);p.setAttached(SurvivalGrowth.JOURNAL,journal);
        var cfg=xiaoshi2022.corpseorigin.config.CorpseConfig.get().growth;
        boolean discovery=true;
        if(event.startsWith("village_training:")){
            int lessons=(int)Math.min(Integer.MAX_VALUE,(long)Math.max(0,journal.getIntOr("village_lessons",0))+1);
            journal.putInt("village_lessons",lessons);p.setAttached(SurvivalGrowth.JOURNAL,journal);
            discovery=cfg.villageOpportunitiesEnabled && OpportunityRules.lessonDue(lessons,cfg.villageLessonsPerOpportunity);
            if(!discovery)p.sendSystemMessage((cfg.villageOpportunitiesEnabled ? Component.translatable("message.corpseorigin.opportunity.lessons", lessons%Math.clamp(cfg.villageLessonsPerOpportunity,1,10000), Math.clamp(cfg.villageLessonsPerOpportunity,1,10000)) : Component.translatable("message.corpseorigin.opportunity.disabled")));
        }else if(event.startsWith("exploration:")){
            discovery=cfg.explorationOpportunitiesEnabled && OpportunityRules.roll(p.getRandom().nextDouble(),cfg.explorationOpportunityChance);
            if(!discovery)p.sendSystemMessage(Component.translatable("message.corpseorigin.free_growth.text_03"));
        }else if(event.startsWith("flesh_count:"))discovery=cfg.fleshOpportunitiesEnabled;
        if(!discovery){if(!event.startsWith("flesh_count:"))awaken(p);return;}
        var data=PlayerCharacterData.get(p);
        int level=EvolutionManager.getLevel(data.getEarnedPoints(p.getUUID()));
        var choices=skills().stream().filter(s->s.getUnlockSources().isEmpty()
                        || s.getUnlockSources().stream().allMatch(source->source.id().startsWith("character:")))
                .filter(s->level>=BalanceRules.discoveryLevel(s.getRequiredLevel(),s.getSkillType()==SkillType.ULTIMATE))
                .filter(s->!discovered(p,s.getId().getPath())&&!data.hasLearned(p.getUUID(),s.getId().getPath())).toList();
        if(!choices.isEmpty()) {
            ISkill choice=choices.get(p.getRandom().nextInt(choices.size()));
            // Discover the prerequisite chain too, so random opportunities cannot strand a branch.
            discoverChain(p,choice,new HashSet<>());
        } else p.sendOverlayMessage(Component.translatable("message.corpseorigin.free_growth.text_04"));
        if (!event.startsWith("flesh_count:")) awaken(p);
        xiaoshi2022.corpseorigin.skill.unlock.SkillUnlockManager.grantUnlocked(p,false);
        CorpseNetwork.sendEvolutionSync(p);
    }
    private static void discoverChain(ServerPlayer p,ISkill skill,Set<String> visited){
        if(!visited.add(skill.getId().getPath()))return;
        for(var prerequisite:skill.getPrerequisites())
            skills().stream().filter(s->s.getId().equals(prerequisite)).findFirst().ifPresent(s->discoverChain(p,s,visited));
        discover(p,skill.getId().getPath());
    }
    public static boolean learned(ServerPlayer player,String skill){
        return isFree(player)&&PlayerCharacterData.get(player).hasLearned(player.getUUID(),skill);
    }
    public static boolean learnedFrom(ServerPlayer player,String role){
        if(!isFree(player)||isFree(role))return false;
        return CharacterManager.getInstance().getCharacter(role).getSkills().stream()
                .anyMatch(s->PlayerCharacterData.get(player).hasLearned(player.getUUID(),s.getId().getPath()));
    }
}
