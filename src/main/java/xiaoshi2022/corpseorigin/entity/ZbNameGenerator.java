package xiaoshi2022.corpseorigin.entity;

import net.minecraft.util.RandomSource;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.config.CorpseConfig;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 尸兄的"玩家 ID"生成器。
 * <p>
 * 尸兄的设定是"被尸水感染后变成的玩家"，所以野外刷出来的也应该有个像玩家的 ID，
 * 客户端会拿这个名字去查同名玩家的皮肤（见 {@code ZbSkinLoader} / {@code ZbSkinIntegration}）。
 * <p>
 * 取名字有三条路，按优先级（前两条来自 {@code config/corpseorigin.json} 的 {@code names} 段）：
 * <ol>
 *   <li>{@code consentedIds}：作者本人与已同意的朋友，原样使用（查得到真皮肤、不染色）；</li>
 *   <li>{@code ids}：你自己填的大名单（真实账号 ID）。配了它就只用它 ——
 *       这是"想用一大堆真实账号皮肤"的正路，名字从哪来由你决定，模组不替你抓站；</li>
 *   <li>兜底：内置<b>音节重组</b>（{@code xiao + tian + 520} 这类），有玩家味但不指向任何真人，
 *       代价是查不到皮肤、只能靠本地色彩变体互相区分。</li>
 * </ol>
 * ⚠️ 名字必须合法 MC 用户名（英文/数字/下划线，3~16 位），中文查不到皮肤。
 */
public final class ZbNameGenerator {

    /**
     * 起手音节。
     * <p>
     * ⚠️ 只放"放在谁身上都说得通"的通用词根。像 {@code ben}（小本）、{@code kouki}（阿神）这种
     * 一眼能认出人的碎片不要放进来 —— 拼出来正好撞脸就失去"不针对具体的人"的意义了。
     */
    private static final String[] HEAD = {
            "xiao", "da", "hei", "bai", "zi", "lan", "hong", "moon", "ice", "pink",
            "snow", "yao", "wen", "tian", "dige", "zhao", "miao", "quan", "cheng", "hehe",
            "steven", "ash", "wan", "kl", "q3", "ufo", "uncle", "se", "the", "north"};

    /** 收尾音节 */
    private static final String[] TAIL = {
            "tian", "bao", "han", "gx", "qi", "tao", "jie", "da", "mie", "ge",
            "zi", "bet", "lor", "mao", "gg", "red", "fish", "zai", "yo", "yu",
            "lin", "lei", "dou", "tang", "xia", "zhu", "nian", "cloud", "block", "yy"};

    /** 偶尔缀一串数字，更像玩家自己起的 ID（空串占多数） */
    private static final String[] NUMBER = {
            "", "", "", "", "", "520", "233", "666", "11", "158", "998", "007", "101"};

    /** MC 用户名长度范围 */
    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 16;

    /** 配置里的大名单，筛过一遍后的结果（null = 还没筛） */
    private static List<String> customNames;

    private ZbNameGenerator() {
    }

    /** 随机挑一个 ID：优先本人/朋友，其次配置里的大名单，最后音节重组 */
    public static String random(RandomSource random) {
        CorpseConfig.Names names = CorpseConfig.get().names;

        // 偶尔直接给一个"本人/朋友"的 ID：查得到真皮肤，而且不染色
        List<String> consented = names.consentedIds;
        if (!consented.isEmpty() && random.nextInt(names.consentedChance) == 0) {
            return consented.get(random.nextInt(consented.size()));
        }

        List<String> pool = customNames();
        if (!pool.isEmpty()) {
            return pool.get(random.nextInt(pool.size()));
        }

        // 四分之一几率加下划线，让名字看起来更像玩家自己起的
        String separator = random.nextInt(4) == 0 ? "_" : "";
        String name = HEAD[random.nextInt(HEAD.length)]
                + separator
                + TAIL[random.nextInt(TAIL.length)]
                + NUMBER[random.nextInt(NUMBER.length)];
        return name.length() > MAX_LENGTH ? name.substring(0, MAX_LENGTH) : name;
    }

    /**
     * 把配置里的大名单筛一遍（去掉非法/中文条目并去重），只做一次。
     * <p>
     * 只在服务端刷怪时才会被调到，客户端不会走这条路。
     */
    private static synchronized List<String> customNames() {
        if (customNames != null) {
            return customNames;
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String raw : CorpseConfig.get().names.ids) {
            if (raw == null) {
                continue;
            }
            String name = raw.trim();
            if (isValidUsername(name)) {
                unique.add(name);
            }
        }
        customNames = List.copyOf(unique);
        if (!customNames.isEmpty()) {
            CorpseOrigin.LOGGER.info("尸兄 ID 名单：{} 个（来自配置文件 names.ids）", customNames.size());
        }
        return customNames;
    }

    /** 合法 MC 用户名：3~16 位英文 / 数字 / 下划线（中文查不到皮肤，直接排除） */
    private static boolean isValidUsername(String name) {
        if (name.length() < MIN_LENGTH || name.length() > MAX_LENGTH) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean valid = (c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || c == '_';
            if (!valid) {
                return false;
            }
        }
        return true;
    }
}
