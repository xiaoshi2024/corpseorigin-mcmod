package xiaoshi2022.corpseorigin.limb;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 截断来源规则注册表。
 * <p>
 * 匹配时按 {@link LimbSeverRule#priority()} 从小到大取第一条命中的规则，所以优先级高的
 * （更特殊的来源，比如某个专属技能的判定）要给它更小的数字。
 */
public final class LimbSeverRules {

    private static final List<LimbSeverRule> RULES = new ArrayList<>();
    private static boolean defaultsRegistered;

    private LimbSeverRules() {
    }

    public static void register(LimbSeverRule rule) {
        RULES.removeIf(existing -> existing.id().equals(rule.id()));
        RULES.add(rule);
        RULES.sort(Comparator.comparingInt(LimbSeverRule::priority));
    }

    public static void unregister(String id) {
        RULES.removeIf(rule -> rule.id().equals(id));
    }

    public static List<LimbSeverRule> all() {
        return Collections.unmodifiableList(RULES);
    }

    /** 找到第一条能吃下这次攻击的规则，没有就返回 null（= 不断肢） */
    public static @Nullable LimbSeverRule firstMatch(ServerPlayer victim, DamageSource source,
                                                     float hitPower, float damageTaken) {
        for (LimbSeverRule rule : RULES) {
            if (rule.matches(victim, source, hitPower, damageTaken)) {
                return rule;
            }
        }
        return null;
    }

    public static void registerDefaults() {
        if (defaultsRegistered) {
            return;
        }
        defaultsRegistered = true;

        register(new WeaponSeverRule());
        // 炸弹 / 爆炸断肢：默认关掉，拍摄要用就解掉下面这行
        // register(new ExplosionSeverRule());
    }
}
