package xiaoshi2022.corpseorigin.limb;

/**
 * 肢体位定义与位掩码工具。
 * <p>
 * 四个部位各占 limb_mask 的一个 bit；模型里要操作的骨骼名也在这里统一登记，
 * 保证服务端判定与客户端骨骼操作不会写飘。
 * <p>
 * ⚠️ <b>在 Blockbench 里新增/重命名骨骼后，务必回来核对 {@link #LIMB_BONES}
 * 与 {@link #VEIN_BONES}</b> —— 命名对不上时不会有任何报错，只是"隐藏不生效"或"血管关不掉"。
 */
public final class LimbSlots {

    public static final int RIGHT_ARM = 0;
    public static final int LEFT_ARM = 1;
    public static final int RIGHT_LEG = 2;
    public static final int LEFT_LEG = 3;
    public static final int COUNT = 4;

    /** 四肢完好 */
    public static final byte NONE = 0;
    /** 四肢全断 */
    public static final byte ALL = (byte) 0b1111;

    /** regrow_ticks 为该值时表示"断掉但不能自愈" */
    public static final int REGROW_PERMANENT = -1;

    /**
     * 每肢<b>真正画出方块</b>的骨骼名 —— 断肢时被整体隐藏的就是它们。
     * <p>
     * ⚠️ 注意别写成 {@code right_arm}：模型里那只是挂在 root 上的<b>空挂点</b>（供动画/挂子骨用），
     * 真正画出袖子的是它的子骨 {@code right_arm2}。对空骨 {@code skipRender} 是没有任何效果的。
     * <p>
     * 用的是二维数组：一只肢体可以有多根显示骨。以后在 Blockbench 里再拆一层，
     * 只要往这里补名字即可，不用改渲染代码。
     */
    public static final String[][] LIMB_BONES = {
            {"right_arm2"},
            {"left_arm2"},
            {"right_leg2"},
            {"left_leg2"},
    };

    /** geo.json 里的残桩骨名（挂点的子骨骼） */
    public static final String[] STUMP_BONES =
            {"right_arm_stump", "left_arm_stump", "right_leg_stump", "left_leg_stump"};

    /**
     * 再生时爬出来的藤蔓状血管的<b>根骨</b>（挂点的子骨骼，自身没有方块）。
     * <p>
     * 血管是"根骨 → vein_1 → vein_2 → vein_3"的链，所以只要对根骨
     * {@code skipChildrenRender} 就能整条藏掉，不用逐段处理。
     * 每只肢体可以挂<b>好几组</b>血管（对应 {@code right_arm_vein} / {@code right_arm_vein2} / {@code right_arm_vein3}
     * 这样的独立根骨），所以这里也是二维数组。
     * 它们的形状与生长节奏全在 {@code regrow_*} 动画里。
     */
    public static final String[][] VEIN_BONES = {
            {"right_arm_vein", "right_arm_vein2", "right_arm_vein3"},
            {"left_arm_vein", "left_arm_vein2", "left_arm_vein3"},
            {"right_leg_vein", "right_leg_vein2", "right_leg_vein3"},
            {"left_leg_vein", "left_leg_vein2", "left_leg_vein3"},
    };

    /** 提示文字用 */
    public static final String[] DISPLAY_NAMES = {"右臂", "左臂", "右腿", "左腿"};
    /** 指令参数用 */
    public static final String[] KEYS = {"arm_r", "arm_l", "leg_r", "leg_l"};

    private LimbSlots() {
    }

    public static byte bit(int slot) {
        return (byte) (1 << slot);
    }

    public static boolean isSevered(byte mask, int slot) {
        return (mask & bit(slot)) != 0;
    }

    public static byte withSevered(byte mask, int slot) {
        return (byte) (mask | bit(slot));
    }

    public static byte withoutSevered(byte mask, int slot) {
        return (byte) (mask & ~bit(slot));
    }

    /** 该部位是否正在长回来（已断 + 有剩余 tick） */
    public static boolean isRegrowing(byte mask, int[] regrowTicks, int slot) {
        return isSevered(mask, slot) && regrowTicks[slot] > 0;
    }

    /** 按指令参数（arm_r / leg_l …）找部位下标，找不到返回 -1 */
    public static int slotFromKey(String key) {
        for (int i = 0; i < KEYS.length; i++) {
            if (KEYS[i].equalsIgnoreCase(key)) {
                return i;
            }
        }
        return -1;
    }

    /** 随机挑一个还完好的部位；四肢全断返回 -1 */
    public static int randomIntactSlot(byte mask, net.minecraft.util.RandomSource random) {
        int[] intact = new int[COUNT];
        int size = 0;
        for (int slot = 0; slot < COUNT; slot++) {
            if (!isSevered(mask, slot)) {
                intact[size++] = slot;
            }
        }
        return size == 0 ? -1 : intact[random.nextInt(size)];
    }
}
