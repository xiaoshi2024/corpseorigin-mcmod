package xiaoshi2022.corpseorigin.client;

import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.skill.ISkill;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端角色数据访问（基于 ClientState 缓存）
 */
public final class ClientCharacterCache {

    private ClientCharacterCache() {
    }

    /** 当前角色的所有进化树技能 */
    public static List<ISkill> getCharacterSkills() {
        var character = CharacterManager.getInstance().getClientCachedCharacter();
        if (character == null || character.isPassive()) {
            return new ArrayList<>();
        }
        return character.getSkills();
    }

    /** 已学习且可主动释放的技能（轮盘用） */
    public static List<ISkill> getActivatableSkills() {
        List<ISkill> result = new ArrayList<>();
        for (ISkill skill : getCharacterSkills()) {
            if (skill.isActivatable() && ClientState.hasLearned(skill.getId().getPath())) {
                result.add(skill);
            }
        }
        return result;
    }
}
