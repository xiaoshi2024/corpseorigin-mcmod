package xiaoshi2022.corpseorigin.skill.chapter;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.growth.FreeGrowth;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.SkillManager;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 新章节经典角色的专属招式基类：除通用的冷却/消耗外，硬校验当前角色必须是招式主人，
 * 避免自由成长等途径把别人的招式错放到其他角色身上。角色不符时只提示、不吃冷却。
 * <p>
 * <b>绑定兵器的招式</b>（{@code weapon} 非空）在角色校验之外<b>再加一道</b>：主手必须握着那把兵器。
 * 这两道的关系由 {@code weaponOnly} 决定，见六参构造器：
 * <ul>
 *   <li>默认"角色 + 兵器都要满足"—— 尸兄鬼棍的尸棍招式，人类鬼棍握着尸棍也放不出来；</li>
 *   <li>"只认兵器"—— 三节棍的棍法是普通武艺，<b>谁拿着都能使</b>，不看角色。</li>
 * </ul>
 * <p>
 * "拿到兵器就学会"那一半不在这里，而是走 {@code ItemSkillSources}（见该技能各自的 path）；
 * "握着兵器右键直接出招"走 {@link #castWithWeapon}。
 */
public abstract class RoleChapterSkill extends AbstractSkill {
    protected final String role;
    /** 绑定的兵器；{@code null} = 不绑定，走下面的角色校验 */
    @Nullable
    private final Item weapon;
    /** 只认兵器：谁拿着谁能放，不校验角色。见六参构造器 */
    private final boolean weaponOnly;

    protected RoleChapterSkill(String path, SkillType type, int cooldownTicks, String role) {
        this(path, type, cooldownTicks, role, null);
    }

    protected RoleChapterSkill(String path, SkillType type, int cooldownTicks, String role,
                               @Nullable Item weapon) {
        this(path, type, cooldownTicks, role, weapon, false);
    }

    /**
     * @param weaponOnly {@code true} = <b>只认兵器</b>，谁拿着谁能放（"兵器本身就是钥匙"），
     *                   例如鬼棍的三节棍：棍法是普通武艺，不看是谁在使；
     *                   {@code false} = 角色与兵器都要满足，例如尸兄鬼棍的尸棍招式。
     */
    protected RoleChapterSkill(String path, SkillType type, int cooldownTicks, String role,
                               @Nullable Item weapon, boolean weaponOnly) {
        super(path, type, cooldownTicks);
        this.role = role;
        this.weapon = weapon;
        this.weaponOnly = weaponOnly;
    }

    /** 绑定的兵器；{@code null} = 不绑定（只校验角色）。 */
    @Nullable
    public Item weapon() {
        return weapon;
    }

    @Override
    public Component checkUsable(ServerPlayer p) {
        // 角色专属招式：换了角色、或换了形态（人类鬼棍 ↔ 尸兄鬼棍）一律放不出来。
        // 声明成 weaponOnly 的（三节棍的棍法）跳过这一关 —— 谁拿着谁能使。
        if (!weaponOnly && !role.equals(CharacterManager.getInstance().getPlayerCharacterId(p)))
            return Component.translatable("message.corpseorigin.chapter_role.text_01",
                    Component.translatable("character.corpseorigin." + role));
        // 绑定了兵器的招式还要主手握着它
        if (weapon != null && !p.getMainHandItem().is(weapon))
            return Component.translatable("message.corpseorigin.chapter_weapon.text_01",
                    weapon.getDefaultInstance().getHoverName());
        return null;
    }

    /**
     * 握着对应兵器右键 → 直接出招，不必先在技能轮盘里选中。
     * <p>
     * 在已注册角色的招式池里筛出「兵器 + 角色」都匹配的那些，逐个尝试激活；
     * 同一把兵器绑多招时（尸棍：尸棍共振 / 尸棍重击），先放当前能放的那一招。
     * 硬前置不满足的会被静默跳过（不刷提示），全都不行就返回 {@code false}，
     * 右键回到原本的行为。
     *
     * @return 是否真的放出了招式
     */
    public static boolean castWithWeapon(ServerPlayer player, ItemStack held) {
        if (held.isEmpty()) return false;
        for (ISkill skill : FreeGrowth.skills()) {
            if (!(skill instanceof RoleChapterSkill chapter)) continue;
            if (chapter.weapon == null || chapter.weapon != held.getItem()) continue;
            // 能不能放只认 checkUsable 一处（角色、兵器、weaponOnly 都在那里判定）
            if (chapter.checkUsable(player) != null) continue;
            // 免"已学会"：兵器在手就是钥匙，不该还要求先去技能树点亮
            if (SkillManager.activate(player, skill.getId().getPath(), false)) return true;
        }
        return false;
    }
}
