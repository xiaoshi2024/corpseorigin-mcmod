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

    /**
     * 自由路线（凡人 / 尸兄）能学会的技能上限。
     * <p>
     * 这两个沙盒角色的卖点是"招式全靠探索凑"，代价原本只有"没有角色专属形态"；
     * 但技能表攒到十几个之后玩法会退化成"我全都有"，所以给一条硬上限：
     * 学满之后不再接受新技能，已经学会的不回收。
     * <p>
     * 只卡<b>学习</b>这一环 —— 三条入口（技能树 / 获取式解锁 / 拜师）都查这里。
     */
    public static final int SKILL_LIMIT = 10;

    /**
     * 自由角色是不是已经学满技能了；<b>非自由角色恒为 false</b>（固定角色不受这条限制）。
     * <p>
     * 只数"已学会"的条数：{@code learnedSkills} 是玩家级扁平集合，而 {@code CharacterManager}
     * 换角色时会整份清空（只有 凡人 ↔ 尸兄 互切才保留），所以自由角色身上不会混进固定角色的旧技能。
     */
    public static boolean skillLimitReached(ServerPlayer p){
        return isFree(p)
                && PlayerCharacterData.get(p).getLearnedSkills(p.getUUID()).size()>=SKILL_LIMIT;
    }
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
        if(InnerPowerManager.getMaxInnerPower(p)>0)return;
        var journal=p.getAttachedOrCreate(SurvivalGrowth.JOURNAL).copy();
        if(journal.getBooleanOr("inner_power",false))return;
        journal.putBoolean("inner_power",true);p.setAttached(SurvivalGrowth.JOURNAL,journal);
        InnerPowerManager.reset(p);
        p.sendSystemMessage(Component.translatable("message.corpseorigin.free_growth.text_02"));
    }
    public static int innerPower(ServerPlayer p){
        if(!isFree(p)
                ||!p.getAttachedOrCreate(SurvivalGrowth.JOURNAL).getBooleanOr("inner_power",false))return 0;
        return InnerPowerRules.growthCapacity(EvolutionManager.getLevel(PlayerCharacterData.get(p).getEarnedPoints(p.getUUID())));
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
