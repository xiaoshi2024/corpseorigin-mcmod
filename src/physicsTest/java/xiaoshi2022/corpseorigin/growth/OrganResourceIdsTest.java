package xiaoshi2022.corpseorigin.growth;

public final class OrganResourceIdsTest {
    public static void main(String[] args) {
        equal("corpseorigin:entity/organ_parts",OrganResourceIds.animation("corpseorigin:geckolib/animations/entity/organ_parts.animation.json"));
        equal("corpseorigin:entity/organ_feather",OrganResourceIds.model("corpseorigin:geckolib/models/entity/organ_feather.geo.json"));
        equal("organ_demo:demo",OrganResourceIds.animation("organ_demo:geckolib/animations/demo.animation.json"));
        equal("organ_demo:body",OrganResourceIds.model("organ_demo:geckolib/models/body.geo.json"));
        equal("minecraft:demo",OrganResourceIds.animation("geckolib/animations/demo.animation.json"));
        System.out.println("Organ resource IDs: 5 regression checks passed.");
    }
    private static void equal(String expected,String actual){if(!expected.equals(actual))throw new AssertionError(expected+" != "+actual);}
}
