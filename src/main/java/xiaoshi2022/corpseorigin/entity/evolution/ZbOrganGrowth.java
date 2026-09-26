package xiaoshi2022.corpseorigin.entity.evolution;

import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;
import xiaoshi2022.corpseorigin.growth.OrganDefinition;
import xiaoshi2022.corpseorigin.growth.OrganLibrary;
import xiaoshi2022.corpseorigin.growth.OrganSlot;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 临界突破成功后的"器官突变"：从服务端已加载的器官池里随机长一个没长过的器官，
 * 关节位置与变换随机，每只进化尸兄都不一样。
 * <p>
 * 器官池 = 硬编内置（翅膀 / 尾巴）+ 玩家自定义目录 / 资源包，
 * <b>不含</b>随模组分发的示例内容（自动生成的 {@code examples.json} 与内置示例包）——
 * 那些只是给玩家参照格式的样板。
 */
public final class ZbOrganGrowth {

    /** 器官可能挂的关节；{@code full_body} 是全身替换，一只尸兄最多一个。 */
    private static final String[] JOINTS = {"body", "head", "left_arm", "right_arm", "left_leg", "right_leg", "full_body"};

    private ZbOrganGrowth() {}

    /** 尸兄突变可用的器官：排除随模组分发的示例内容。 */
    public static List<OrganDefinition> allowedDefinitions() {
        return OrganLibrary.definitions().stream()
                .filter(d -> !OrganLibrary.isExampleOrgan(d.id()))
                .toList();
    }

    /** @return 是否真的突变出了器官 */
    public static boolean tryMutateOrgan(LowerLevelZbEntity self) {
        List<OrganSlot> slots = parseLoadout(self);
        if (slots.size() >= OrganLibrary.MAX_SLOTS) return false;

        Set<String> owned = slots.stream().map(OrganSlot::organ).collect(Collectors.toSet());
        List<OrganDefinition> pool = allowedDefinitions().stream()
                .filter(d -> !owned.contains(d.id()))
                .toList();
        if (pool.isEmpty()) return false;

        OrganDefinition def = pool.get(self.getRandom().nextInt(pool.size()));
        String joint = randomJoint(self, slots.stream().noneMatch(OrganSlot::replacesBody));
        slots.add(randomSlot(self, def.id(), joint));
        self.setOrganLoadout(OrganLibrary.JSON.toJson(slots.toArray(new OrganSlot[0])));
        return true;
    }

    /**
     * 随机挑一个关节。
     *
     * @param allowFullBody 已经有一个全身替换器官时给 {@code false} ——
     *                      {@link OrganLibrary#parseSlots} 只允许一个，多给会让整份配置失效。
     */
    private static String randomJoint(LowerLevelZbEntity self, boolean allowFullBody) {
        String joint;
        do {
            joint = JOINTS[self.getRandom().nextInt(JOINTS.length)];
        } while (!allowFullBody && "full_body".equals(joint));
        return joint;
    }

    /** 读取尸兄当前的器官配置；脏数据一律当作"没长过"，不影响实体存活。 */
    public static List<OrganSlot> parseLoadout(LowerLevelZbEntity self) {
        try {
            return new ArrayList<>(OrganLibrary.parseSlots(self.getOrganLoadout()));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /** 已装备且可用器官池里存在的去重器官定义（服务端用）。 */
    public static List<OrganDefinition> activeDefinitions(LowerLevelZbEntity self) {
        Set<String> ids = parseLoadout(self).stream().map(OrganSlot::organ).collect(Collectors.toSet());
        return allowedDefinitions().stream().filter(d -> ids.contains(d.id())).toList();
    }

    private static OrganSlot randomSlot(LowerLevelZbEntity self, String organ, String joint) {
        var rng = self.getRandom();
        // 小范围随机偏移/旋转，让同一个器官长在不同尸兄身上也有区别
        float x = (rng.nextFloat() * 2 - 1) * 1.5F;
        float y = (rng.nextFloat() * 2 - 1) * 1.5F;
        float z = (rng.nextFloat() * 2 - 1) * 1.5F;
        float rx = (rng.nextFloat() * 2 - 1) * 8.0F;
        float ry = (rng.nextFloat() * 2 - 1) * 8.0F;
        float rz = (rng.nextFloat() * 2 - 1) * 12.0F;
        float scale = 0.9F + rng.nextFloat() * 0.2F;
        return new OrganSlot(organ, joint, x, y, z, rx, ry, rz, scale, false);
    }
}
