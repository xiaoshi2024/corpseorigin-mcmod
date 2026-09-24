package xiaoshi2022.corpseorigin.character;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 角色管理器 - 注册角色、分配玩家角色
 */
public class CharacterManager {

    private static CharacterManager instance;

    /** 按注册顺序保存（保证列表与创造物品栏里的顺序稳定） */
    private final Map<String, ICharacter> registeredCharacters = new LinkedHashMap<>();

    /** 客户端缓存的当前角色ID */
    private String clientCachedCharacterId = MortalCharacter.ID;

    private CharacterManager() {
    }

    public static CharacterManager getInstance() {
        if (instance == null) {
            instance = new CharacterManager();
        }
        return instance;
    }

    /** 在模组初始化时调用，注册所有内置角色 */
    public void registerDefaults() {
        registerCharacter(MortalCharacter.getInstance());
        // 凡人被尸水感染后自动转入的角色（尸兄的进化效果挂在它身上）
        registerCharacter(new CorpseBrother());

        // ==================== 一级优先级（核心战斗 / 主线剧情角色） ====================
        registerCharacter(new BaiXiaoFei());
        registerCharacter(new HeiXiaoFei());
        registerCharacter(new LongYou());
        registerCharacter(new ShiChaoZhiZi());
        registerCharacter(new KaiWeiNai());
        registerCharacter(new XiaoLu());
        registerCharacter(new XiaoYanZi());
        registerCharacter(new TianXianBaoBaoZb());
        registerCharacter(new JinGangZb());
        registerCharacter(new XiaoHui());

        // ==================== 二级优先级（关键剧情推动角色） ====================
        registerCharacter(new TuShu());
        registerCharacter(new MuXi());
        registerCharacter(new FiveElementsMember("metal"));
        registerCharacter(new FiveElementsMember("water"));
        registerCharacter(new FiveElementsMember("earth"));
        registerCharacter(new YanYan());
        registerCharacter(new FengMoHuiTaiLang());
        registerCharacter(new DarkCouncilK());
        registerCharacter(new BianSeLongZb());
        registerCharacter(new ChongMu());
        registerCharacter(new QingWaZb());
        registerCharacter(new HuJie());
        registerCharacter(new ZuoHuFa());

        // ==================== 三级优先级（合并简化的辅助 / 杂兵角色） ====================
        registerCharacter(new HeiAnHuiSuiCong());
        registerCharacter(new CouncilRetainer(false));
        registerCharacter(new CouncilRetainer(true));
        registerCharacter(new XiongXingZb());
        registerCharacter(new ChongQun());
        registerCharacter(new SiYangYuanZb());
        registerCharacter(new KuaiDiYuanZb());
        registerCharacter(new ZhaoRiTian());
        registerCharacter(new ChuanShangXingCunZhe());
        registerCharacter(new BianYiGuiYu());
        registerCharacter(new YanHuangBuDui());

        // 其他既有角色
        registerCharacter(new WeiXin());
    }

    public void registerCharacter(ICharacter character) {
        registeredCharacters.put(character.getId(), character);
        CorpseOrigin.LOGGER.debug("Registered character: {}", character.getId());
    }

    public ICharacter getCharacter(String id) {
        if (id == null || id.isEmpty()) {
            return MortalCharacter.getInstance();
        }
        return registeredCharacters.getOrDefault(id, MortalCharacter.getInstance());
    }

    public List<ICharacter> getRegisteredCharacters() {
        return new ArrayList<>(registeredCharacters.values());
    }

    // ==================== 玩家角色分配 ====================

    /**
     * 设置玩家角色（服务端）
     */
    public boolean setPlayerCharacter(Player player, String characterId) {
        if (player.level().isClientSide()) {
            return false;
        }
        ICharacter character = getCharacter(characterId);
        if (character == null || !registeredCharacters.containsKey(character.getId())) {
            return false;
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;
        PlayerCharacterData data = PlayerCharacterData.get(player);
        String oldId = data.getCharacterId(player.getUUID());

        if (oldId.equals(character.getId())) {
            // ★ 重复选择同一个角色：不发包也不清技能（下面那条 clearLearnedSkills 不会走到），
            //   但客户端可能因为换身 / 重登而把角色缓存丢了 —— 表现为"技能树空空如也、
            //   再选一次角色也没用"。这里补一次同步，让"重新选一次角色"真的能修好客户端。
            syncToClient(serverPlayer);
            return true;
        }

        // 清理旧角色
        ICharacter old = getCharacter(oldId);
        old.onLose(player);
        // Some roles have no onLose override. Never carry the previous body's
        // infection/identity into a newly selected human role.
        if (!CorpseBrother.ID.equals(character.getId()))
            xiaoshi2022.corpseorigin.component.PlayerCorpseComponent.removeCorpseState(player);

        // 应用新角色
        data.setCharacterId(player.getUUID(), character.getId());
        player.setAttached(xiaoshi2022.corpseorigin.skill.chapter.ChapterActorState.ROLE,character.getId());
        player.setAttached(xiaoshi2022.corpseorigin.skill.chapter.ChapterScenes.CONDITION,"");
        player.setAttached(xiaoshi2022.corpseorigin.skill.chapter.ChapterScenes.ACTION,"");
        player.setAttached(xiaoshi2022.corpseorigin.skill.chapter.ChapterScenes.UNTIL,0L);
        character.onAcquire(player);

        // ✅ 角色切换后重置内力（上线即满 / 切换角色满内力）
        InnerPowerManager.reset(serverPlayer);

        // ✅ 自动学习该角色的全部技能（技能树未启用，故直接授予）
        data.clearLearnedSkills(player.getUUID());
        //注释一下就不自动学习
//        for (ISkill skill : character.getSkills()) {
//            data.learnSkill(player.getUUID(), skill.getId().getPath());
//        }

        CorpseOrigin.LOGGER.info("Player '{}' selected character: {}",
                player.getName().getString(), character.getId());
        syncToClient(serverPlayer);
        return true;
    }

    /** 回到凡人（清除角色） */
    public boolean clearPlayerCharacter(Player player) {
        return setPlayerCharacter(player, MortalCharacter.ID);
    }

    public ICharacter getPlayerCharacter(Player player) {
        if (player.level().isClientSide()) {
            return getCharacter(clientCachedCharacterId);
        }
        PlayerCharacterData data = PlayerCharacterData.get(player);
        return getCharacter(data.getCharacterId(player.getUUID()));
    }

    public String getPlayerCharacterId(Player player) {
        if (player.level().isClientSide()) {
            return clientCachedCharacterId;
        }
        return PlayerCharacterData.get(player).getCharacterId(player.getUUID());
    }

    public boolean isMortal(Player player) {
        return MortalCharacter.ID.equals(getPlayerCharacterId(player));
    }

    // ==================== 客户端缓存 ====================

    public void setClientCachedCharacter(String characterId) {
        this.clientCachedCharacterId = characterId != null ? characterId : MortalCharacter.ID;
    }

    public String getClientCachedCharacterId() {
        return clientCachedCharacterId;
    }

    public ICharacter getClientCachedCharacter() {
        return getCharacter(clientCachedCharacterId);
    }

    // ==================== 同步 ====================

    public void syncToClient(ServerPlayer player) {
        xiaoshi2022.corpseorigin.skill.chapter.ChapterActorState.reconcileCorpseState(player);
        String characterId = getPlayerCharacterId(player);
        CorpseNetwork.sendCharacterSync(player, characterId);
        CorpseNetwork.sendInfectionSync(player);
        CorpseNetwork.broadcastPlayerCorpseSync(player);
        // ✅ 同步进化状态 + 已学技能
        CorpseNetwork.sendEvolutionSync(player);
        // ✅ 同步内力：HUD 只认这个包，而内力表在退出时会被清掉，
        //    登录/重生后不补发的话内力条要等到玩家用一次技能才会出现
        InnerPowerManager.syncTo(player);
    }
}
