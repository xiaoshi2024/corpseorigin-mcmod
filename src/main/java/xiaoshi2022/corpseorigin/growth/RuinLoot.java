package xiaoshi2022.corpseorigin.growth;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import xiaoshi2022.corpseorigin.config.CorpseConfig;
import xiaoshi2022.corpseorigin.item.SkillBookItem;

/** Adds one sparse pool; never replaces the structure's original loot. */
public final class RuinLoot {
    private RuinLoot(){}
    public static final class Config {
        public boolean enabled=true,moddedStructures=true;
        public double chance=.15;
        public boolean skillBooksEnabled=true;
        public double skillBookChance=.025;
        public boolean lostCitiesEnabled=true;
        public double lostCitiesSupplyChance=.08;
        public int lostCitiesDiscoveryPoints=30;
        public java.util.List<String> additionalTables=new java.util.ArrayList<>();
        public java.util.List<String> excludedTables=new java.util.ArrayList<>();
        public java.util.Map<String,Integer> weights=new java.util.LinkedHashMap<>(java.util.Map.ofEntries(
                java.util.Map.entry("corpseorigin:s_agent",25),java.util.Map.entry("corpseorigin:blue_s_agent",30),
                java.util.Map.entry("corpseorigin:kw89",25),java.util.Map.entry("corpseorigin:magician_rabbit",8),
                java.util.Map.entry("corpseorigin:black_gold_heart",3),java.util.Map.entry("corpseorigin:medusa_eye",2),
                java.util.Map.entry("corpseorigin:bee_wheel",2),java.util.Map.entry("corpseorigin:guigun_weap",2),
                java.util.Map.entry("corpseorigin:guigun_club",1),java.util.Map.entry("corpseorigin:blood_wing_blade",1),
                java.util.Map.entry("corpseorigin:tian_gang_key",1)));
        public void sanitize(){
            chance=Double.isFinite(chance)?Math.clamp(chance,0,1):.15;
            skillBookChance=Double.isFinite(skillBookChance)?Math.clamp(skillBookChance,0,1):.025;
            lostCitiesSupplyChance=Double.isFinite(lostCitiesSupplyChance)?Math.clamp(lostCitiesSupplyChance,0,1):.08;
            lostCitiesDiscoveryPoints=Math.clamp(lostCitiesDiscoveryPoints,0,10000);
            if(additionalTables==null)additionalTables=new java.util.ArrayList<>();
            if(excludedTables==null)excludedTables=new java.util.ArrayList<>();
            if(weights==null)weights=new Config().weights;
            weights.replaceAll((k,v)->v==null?0:Math.clamp(v,0,10000));
        }
    }
    public static boolean accepts(Identifier id,boolean chest,Config cfg){
        if(!cfg.enabled||cfg.excludedTables.contains(id.toString()))return false;
        if(cfg.additionalTables.contains(id.toString()))return true;
        if(!chest||!cfg.moddedStructures&&!id.getNamespace().equals("minecraft"))return false;
        String path=id.getPath();
        return (path.startsWith("chests/")||path.startsWith("chest/"))
                &&!path.startsWith("chests/village/")&&!path.contains("/supplies")&&!path.contains("/reward");
    }
    public static void initialize(){
        LootTableEvents.MODIFY.register((key,builder,source,registries)->{
            var cfg=CorpseConfig.get().ruinLoot;
            if(cfg.enabled && cfg.lostCitiesEnabled && builder.build().getParamSet()==LootContextParamSets.CHEST)
                addLostCitiesSupply(key.identifier(),builder,cfg);
            if(!accepts(key.identifier(),builder.build().getParamSet()==LootContextParamSets.CHEST,cfg))return;
            var pool=LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                    .when(LootItemRandomChanceCondition.randomChance((float)cfg.chance));
            int added=0;
            for(var entry:cfg.weights.entrySet()){
                var id=Identifier.tryParse(entry.getKey());
                if(id==null||entry.getValue()<=0||!BuiltInRegistries.ITEM.containsKey(id))continue;
                var item=BuiltInRegistries.ITEM.getValue(id);
                if(item==Items.AIR||item instanceof net.minecraft.world.item.SpawnEggItem)continue;
                pool.add(LootItem.lootTableItem(item).setWeight(entry.getValue()));added++;
            }
            if(added>0)builder.withPool(pool);
            if(cfg.skillBooksEnabled && cfg.skillBookChance>0) {
                var books=LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .when(LootItemRandomChanceCondition.randomChance((float)cfg.skillBookChance));
                int count=0;
                for(var skill:FreeGrowth.skills()) {
                    var book=SkillBookItem.books().get(skill.getId().getPath());
                    if(book==null)continue;
                    books.add(LootItem.lootTableItem(book).setWeight(SkillBookItem.weight(skill)));
                    count++;
                }
                if(count>0)builder.withPool(books);
            }
        });
    }

    private static void addLostCitiesSupply(Identifier id, net.minecraft.world.level.storage.loot.LootTable.Builder builder, Config cfg) {
        if (!id.getNamespace().equals("lostcities") || cfg.excludedTables.contains(id.toString())) return;
        String path=id.getPath();
        boolean rail=path.equals("chests/raildungeonchest");
        if (!rail && !path.equals("chests/lostcitychest")) return;
        var pool=LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                .when(LootItemRandomChanceCondition.randomChance((float)cfg.lostCitiesSupplyChance));
        // Rail tunnels favor emergency supplies; city buildings hold rarer research stock.
        add(pool,"corpseorigin:s_agent",rail?18:12);
        add(pool,"corpseorigin:blue_s_agent",rail?8:16);
        add(pool,"corpseorigin:kw89",rail?14:8);
        add(pool,"corpseorigin:black_gold_heart",rail?0:1);
        builder.withPool(pool);
    }

    private static void add(LootPool.Builder pool,String name,int weight) {
        if(weight<=0)return;
        Identifier id=Identifier.parse(name);
        if(BuiltInRegistries.ITEM.containsKey(id))
            pool.add(LootItem.lootTableItem(BuiltInRegistries.ITEM.getValue(id)).setWeight(weight));
    }
}
