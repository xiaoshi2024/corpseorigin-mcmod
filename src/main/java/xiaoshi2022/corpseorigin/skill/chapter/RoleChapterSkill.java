package xiaoshi2022.corpseorigin.skill.chapter;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 新章节经典角色的专属招式基类：除通用的冷却/消耗外，硬校验当前角色必须是招式主人，
 * 避免自由成长等途径把别人的招式错放到其他角色身上。角色不符时只提示、不吃冷却。
 * <p>
 * <b>绑定兵器的招式</b>（{@code weapon} 非空）走另一套判据：<b>主手握着那把兵器就是唯一条件</b>，
 * 谁拿着谁能放 —— 这是刻意的，见 {@code GuigunCombat} 的棍法：三节棍 / 尸棍是招式的钥匙，
 * 从任何途径拿到（例如博物馆开出来）都该能放出来，而鬼棍本人空手同样放不了。
 * <p>
 * "拿到兵器就学会"那一半不在这里，而是走 {@code ItemSkillSources}（见该技能各自的 path）。
 */
public abstract class RoleChapterSkill extends AbstractSkill {
    protected final String role;
    /** 绑定的兵器；{@code null} = 不绑定，走下面的角色校验 */
    @Nullable
    private final Item weapon;

    protected RoleChapterSkill(String path, SkillType type, int cooldownTicks, String role) {
        this(path, type, cooldownTicks, role, null);
    }

    protected RoleChapterSkill(String path, SkillType type, int cooldownTicks, String role,
                               @Nullable Item weapon) {
        super(path, type, cooldownTicks);
        this.role = role;
        this.weapon = weapon;
    }

    @Override
    public Component checkUsable(ServerPlayer p) {
        if (weapon != null) {
            // 绑定了兵器：手持它就是唯一门槛，任何角色都能放（角色校验在这里刻意让位）
            return p.getMainHandItem().is(weapon) ? null
                    : Component.translatable("message.corpseorigin.chapter_weapon.text_01",
                            weapon.getDefaultInstance().getHoverName());
        }
        if (!role.equals(CharacterManager.getInstance().getPlayerCharacterId(p)))
            return Component.translatable("message.corpseorigin.chapter_role.text_01",
                    Component.translatable("character.corpseorigin." + role));
        return null;
    }
}
