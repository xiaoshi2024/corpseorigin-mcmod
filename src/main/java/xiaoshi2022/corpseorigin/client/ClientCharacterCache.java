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
        List<ISkill> skills = new ArrayList<>(character.getSkills());
        if (!"longyou".equals(character.getId()))
            skills.removeIf(s -> isBodySkill(s.getId().getPath()));
        if (hasBorrowedBody()) {
            if (skills.stream().noneMatch(s -> s.getId().getPath().equals("xuanwu_body")))
                skills.add(new xiaoshi2022.corpseorigin.skill.longyou.XuanwuBodySkill());
            if (skills.stream().noneMatch(s -> s.getId().getPath().equals("peel_shell")))
                skills.add(new xiaoshi2022.corpseorigin.skill.jingang_zb.PeelShellSkill());
        }
        return skills;
    }

    /** 已学习且可主动释放的技能（轮盘用） */
    public static List<ISkill> getActivatableSkills() {
        List<ISkill> result = new ArrayList<>();
        for (ISkill skill : getCharacterSkills()) {
            if (skill.isActivatable() && (ClientState.hasLearned(skill.getId().getPath())
                    || (hasBorrowedBody() && isBodySkill(skill.getId().getPath())))) {
                result.add(skill);
            }
        }
        return result;
    }

    private static boolean hasBorrowedBody() {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        return player != null && !player.getAttachedOrCreate(
                xiaoshi2022.corpseorigin.skill.longyou.BodyPossession.SKIN).isEmpty();
    }

    private static boolean isBodySkill(String path) {
        return path.equals("xuanwu_body") || path.equals("peel_shell");
    }
}
