package xiaoshi2022.corpseorigin.skill.chapter;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 新章节经典角色的专属招式基类：除通用的冷却/消耗外，硬校验当前角色必须是招式主人，
 * 避免自由成长等途径把别人的招式错放到其他角色身上。角色不符时只提示、不吃冷却。
 */
public abstract class RoleChapterSkill extends AbstractSkill {
    protected final String role;
    protected RoleChapterSkill(String path, SkillType type, int cooldownTicks, String role) {
        super(path, type, cooldownTicks);
        this.role = role;
    }
    @Override
    public Component checkUsable(ServerPlayer p) {
        if (!role.equals(CharacterManager.getInstance().getPlayerCharacterId(p)))
            return Component.translatable("message.corpseorigin.chapter_role.text_01",
                    Component.translatable("character.corpseorigin." + role));
        return null;
    }
}
