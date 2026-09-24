package xiaoshi2022.corpseorigin.growth;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import java.util.List;

public final class OrganEvolution {
    private OrganEvolution() {}
    public static List<OrganDefinition> entries(List<OrganDefinition> catalog) {
        var list = new java.util.ArrayList<>(catalog);
        list.add(new OrganDefinition("trait_vampire", "吸血鬼体质", "vampire", "", "", "", java.util.Map.of()));
        return list;
    }
    public static int stage(Player p, String id) { return Math.clamp(p.getAttachedOrCreate(SurvivalGrowth.BODY).getIntOr("organ_stage:"+id,0),0,3); }
    public static int stat(Player p, String id, String stat) { return Math.clamp(p.getAttachedOrCreate(SurvivalGrowth.BODY).getIntOr("organ_"+stat+":"+id,0),0,1000000); }
    public static String special(Player p, String id) { return p.getAttachedOrCreate(SurvivalGrowth.BODY).getStringOr("organ_special:"+id,""); }
    public static String act(ServerPlayer p, String id, String action) {
        if (!p.isAlive() || p.isSpectator()) return "当前状态不能进化";
        tick(p);
        if (!p.isCreative() && (!PlayerCorpseComponent.isCorpse(p) || !xiaoshi2022.corpseorigin.config.CorpseConfig.get().growth.enabled)) return "生存进化需要尸兄身体且服务器启用成长";
        var def = entries(OrganLibrary.definitions()).stream().filter(d->d.id().equals(id)).findFirst().orElse(null);
        if (def == null) return "服务器目录中不存在该器官";
        var data = PlayerCharacterData.get(p); int stage = stage(p,id);
        int level = xiaoshi2022.corpseorigin.skill.EvolutionManager.getLevel(data.getEarnedPoints(p.getUUID()));
        int points = data.getAvailablePoints(p.getUUID()), cost;
        var body = p.getAttachedOrCreate(SurvivalGrowth.BODY).copy();
        if (action.equals("advance")) {
            if (!OrganEvolutionRules.mayAdvance(p.isCreative(),stage,level,points)) return stage>=3 ? "天梯已完成，可继续属性加点或选择特化" : "需要等级"+OrganEvolutionRules.level(stage+1)+"和"+OrganEvolutionRules.cost(stage+1)+"可用进化点";
            cost = OrganEvolutionRules.cost(stage+1);
            body.putInt("organ_stage:"+id,stage+1);
            if (!def.trait().equals("cosmetic")) body.putBoolean(def.trait(),true);
            EvolutionAppearance.initialize(body,p.getRandom());
        } else if (action.equals("vitality") || action.equals("efficiency")) {
            int value=stat(p,id,action);
            if (!OrganEvolutionRules.mayAllocate(p.isCreative(),stage,value,points)) return "需II阶；每项生存最多5点，下一点消耗"+OrganEvolutionRules.attributeCost(value)+"进化点";
            cost=OrganEvolutionRules.attributeCost(value);body.putInt("organ_"+action+":"+id,value+1);
        } else if (action.equals("power") || action.equals("sustain")) {
            if (!p.isCreative() && stage<3) return "需先完成III阶特化进化";
            if (special(p,id).equals(action)) return "当前已经选择此特化";
            if (!p.isCreative() && !special(p,id).isEmpty()) return "生存特化二选一，确认后不能反复切换";
            cost=0;body.putString("organ_special:"+id,action);
        } else return "未知进化操作";
        if (p.isCreative() && stage==0 && !action.equals("advance")) {
            body.putInt("organ_stage:"+id,3);
            if (!def.trait().equals("cosmetic")) body.putBoolean(def.trait(),true);
        }
        if (!p.isCreative() && !data.spendPoints(p.getUUID(),cost)) return "可用进化点不足";
        p.setAttached(SurvivalGrowth.BODY,body);CorpseNetwork.sendEvolutionSync(p);
        return "已进化："+def.name()+"（"+(p.isCreative()?"创造免费":cost+"点")+"）";
    }
    // Duplicating a model/slot never duplicates its bonuses. Survival bonuses remain bounded.
    public static int efficiency(ServerPlayer p,String trait) {
        return entries(OrganLibrary.definitions()).stream().filter(d->d.trait().equals(trait) && stage(p,d.id())>0)
                .mapToInt(d->Math.min(5,stat(p,d.id(),"efficiency"))+(special(p,d.id()).equals("sustain")?2:0)).max().orElse(0);
    }
    public static boolean power(ServerPlayer p,String trait) {
        return entries(OrganLibrary.definitions()).stream().anyMatch(d->d.trait().equals(trait) && stage(p,d.id())>0 && special(p,d.id()).equals("power"));
    }
    public static void tick(ServerPlayer p) {
        OrganAnatomy.tick(p);
        var body=p.getAttachedOrCreate(SurvivalGrowth.BODY);
        if (!body.getBooleanOr("organ_ladder_migrated",false)) {
            var migrated=body.copy();
            for(var d:entries(OrganLibrary.definitions())) if(!d.trait().equals("cosmetic") && body.getBooleanOr(d.trait(),false)) migrated.putInt("organ_stage:"+d.id(),Math.max(1,stage(p,d.id())));
            migrated.putBoolean("organ_ladder_migrated",true);p.setAttached(SurvivalGrowth.BODY,migrated);
        }
        double health=0;
        if (p.isAlive() && (p.isCreative() || PlayerCorpseComponent.isCorpse(p)) && xiaoshi2022.corpseorigin.config.CorpseConfig.get().growth.enabled)
            health=entries(OrganLibrary.definitions()).stream().filter(d->stage(p,d.id())>0)
                    .mapToInt(d->(p.isCreative()?stat(p,d.id(),"vitality"):Math.min(5,stat(p,d.id(),"vitality")))
                        +(special(p,d.id()).equals("sustain")?2:0)).max().orElse(0)*2.0;
        var attr=p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        var id=xiaoshi2022.corpseorigin.CorpseOrigin.id("organ_vitality");
        if(attr!=null){var old=attr.getModifier(id);
            if(health==0){if(old!=null)attr.removeModifier(id);}
            else if(old==null||old.amount()!=health)attr.addOrReplacePermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(id,health,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
            if(p.getHealth()>p.getMaxHealth())p.setHealth(p.getMaxHealth());
        }
        var attack=p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        var attackId=xiaoshi2022.corpseorigin.CorpseOrigin.id("organ_special_power");
        boolean power=p.isAlive() && (p.isCreative()||PlayerCorpseComponent.isCorpse(p))
                && xiaoshi2022.corpseorigin.config.CorpseConfig.get().growth.enabled
                && entries(OrganLibrary.definitions()).stream().anyMatch(d->stage(p,d.id())>0 && special(p,d.id()).equals("power"));
        if(attack!=null){
            if(!power)attack.removeModifier(attackId);
            else if(attack.getModifier(attackId)==null)attack.addOrReplacePermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(attackId,2,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
        }
        var armor=p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
        var armorId=xiaoshi2022.corpseorigin.CorpseOrigin.id("organ_efficiency");
        double armorBonus=0;
        if(p.isAlive() && (p.isCreative()||PlayerCorpseComponent.isCorpse(p)) && xiaoshi2022.corpseorigin.config.CorpseConfig.get().growth.enabled)
            armorBonus=entries(OrganLibrary.definitions()).stream().filter(d->stage(p,d.id())>0)
                .mapToInt(d->p.isCreative()?stat(p,d.id(),"efficiency"):Math.min(5,stat(p,d.id(),"efficiency"))).max().orElse(0)*.4;
        if(armor!=null){var old=armor.getModifier(armorId);
            if(armorBonus==0){if(old!=null)armor.removeModifier(armorId);}
            else if(old==null||old.amount()!=armorBonus)armor.addOrReplacePermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(armorId,armorBonus,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
        }
    }
}
