package xiaoshi2022.corpseorigin.growth;

import net.minecraft.resources.Identifier;

import java.util.Map;

/** Resource paths are supplied by a server-approved resource pack, never arbitrary files. */
public record OrganDefinition(String id, String name, String trait, String model, String texture,
                              String animation, Map<String, String> clips, WaterJet waterJet, Anatomy anatomy) {
    public OrganDefinition(String id,String name,String trait,String model,String texture,String animation,Map<String,String> clips){
        this(id,name,trait,model,texture,animation,clips,null,null);
    }
    public record Anatomy(int extraArms, float damagePerArm, int extraLegs, float speedPerLeg,
                          int stomachBonus, boolean nightVision) {
        public boolean valid(){return extraArms>=0&&extraArms<=8&&extraLegs>=0&&extraLegs<=8
            &&Float.isFinite(damagePerArm)&&damagePerArm>=0&&damagePerArm<=2
            &&Float.isFinite(speedPerLeg)&&speedPerLeg>=0&&speedPerLeg<=.05f
            &&stomachBonus>=0&&stomachBonus<=500;}
    }
    public record WaterJet(int capacity,int refill,int cost,int cooldownTicks,float damage,float range,String material) {
        public String materialType(){return material==null?"water":material;}
        public boolean valid(){return capacity>=1&&capacity<=1000&&refill>=1&&refill<=capacity&&cost>=1&&cost<=capacity
            &&cooldownTicks>=5&&cooldownTicks<=1200&&Float.isFinite(damage)&&damage>0&&damage<=40
            &&Float.isFinite(range)&&range>=1&&range<=32&&java.util.Set.of("water","mud","sand").contains(materialType());}
    }
    public net.minecraft.network.chat.Component displayName() {
        return name.startsWith("organ.") ? net.minecraft.network.chat.Component.translatable(name) : net.minecraft.network.chat.Component.literal(name);
    }
    public boolean valid() {
        return id != null && !id.isBlank() && id.length() <= 96 && name != null && !name.isBlank() && name.length() <= 80
                && java.util.Set.of("wings", "gills", "vampire", "cosmetic").contains(trait == null ? "" : trait)
                && resource(model, "geckolib/models/", ".geo.json")
                && resource(texture, "textures/", ".png")
                && resource(animation, "geckolib/animations/", ".animation.json")
                && (waterJet == null || waterJet.valid())
                && (anatomy == null || anatomy.valid())
                && clips != null && clips.size() <= 10 && clips.containsKey("idle")
                && clips.keySet().stream().allMatch(k -> java.util.Set.of("idle", "walk", "attack", "crouch", "fly", "glide", "swim").contains(k == null ? "" : k))
                && clips.values().stream().allMatch(v -> v != null && !v.isBlank() && v.length() <= 96);
    }
    private static boolean resource(String value, String prefix, String suffix) {
        if (value == null || value.length() > 192 || value.contains("..")) return false;
        Identifier id = Identifier.tryParse(value);
        return id != null && id.getPath().startsWith(prefix) && id.getPath().endsWith(suffix);
    }
}
