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

    /**
     * 角色 ID → 阵营 的集中映射表。
     * <p>
     * 后期想调整阵营只改这一张表即可，不用动任何角色类。
     * <p>
     * ⚠️ 注册了但<b>不在这张表里</b>的角色一律归 {@link CharacterFaction#OTHER}。
     */
    private final Map<String, CharacterFaction> factionMap = new LinkedHashMap<>();

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
        registerCharacter(new NewChapterCharacter("xiaojingang",true));
        registerCharacter(new NewChapterCharacter("guigun_human",false));
        registerCharacter(new NewChapterCharacter("guigun_corpse",true));
        registerCharacter(new NewChapterCharacter("hei_wuchou",true));
        registerCharacter(new NewChapterCharacter("bai_wusheng",true));

        // ==== 阵营映射 —— 后期想调整阵营只改这里 ====
        populateFactionMap();
    }

    /**
     * 角色 ID → 阵营 的集中映射表。后期想把某角色从"尸王"挪去"其他"、
     * 或者新增阵营，都只需要改这一张表。
     * <p>
     * 没在这里列出的注册角色会自动归到 {@link CharacterFaction#OTHER}，
     * 这样新加角色时也不会丢。
     */
    private void populateFactionMap() {
        factionMap.put("guigun_human",CharacterFaction.HUMAN);
        factionMap.put("xiaojingang",CharacterFaction.OTHER);
        for(String role:List.of("guigun_corpse","hei_wuchou","bai_wusheng"))factionMap.put(role,CharacterFaction.CORPSE_KING);
        // ========== 人类阵营 ==========
        // 凡人 / 被感染但仍有理智的主角团 / 炎黄特能队 / 收复部队
        factionMap.put(MortalCharacter.ID, CharacterFaction.HUMAN);
        factionMap.put("baixiaofei", CharacterFaction.HUMAN);
        factionMap.put("heixiaofei", CharacterFaction.HUMAN);
        factionMap.put("xiaolu", CharacterFaction.HUMAN);
        factionMap.put("xiaoyanzi", CharacterFaction.HUMAN);
        factionMap.put("kaiweinai", CharacterFaction.HUMAN);
        factionMap.put("xiaohui", CharacterFaction.HUMAN);
        factionMap.put("tushu", CharacterFaction.HUMAN);
        factionMap.put("muxi", CharacterFaction.HUMAN);
        factionMap.put("formation_metal", CharacterFaction.HUMAN);
        factionMap.put("formation_water", CharacterFaction.HUMAN);
        factionMap.put("formation_earth", CharacterFaction.HUMAN);
        factionMap.put("yanyan", CharacterFaction.HUMAN);
        factionMap.put("yanhuang_budui", CharacterFaction.HUMAN);
        factionMap.put("chuangshang_xingcunzhe", CharacterFaction.HUMAN);
        factionMap.put("bianyi_guiyu", CharacterFaction.HUMAN);

        // ========== 尸王阵营 ==========
        // 龙右本人 + 效忠龙右的尸兄
        factionMap.put("longyou", CharacterFaction.CORPSE_KING);
        factionMap.put("shichaozhizi", CharacterFaction.CORPSE_KING);
        factionMap.put("tianxianbaobao_zb", CharacterFaction.CORPSE_KING);
        factionMap.put("jingang_zb", CharacterFaction.CORPSE_KING);
        factionMap.put("bianselong_zb", CharacterFaction.CORPSE_KING);
        factionMap.put("chongmu", CharacterFaction.CORPSE_KING);
        factionMap.put("qingwa_zb", CharacterFaction.CORPSE_KING);
        factionMap.put("zuohufa", CharacterFaction.CORPSE_KING);
        factionMap.put("hujie", CharacterFaction.CORPSE_KING);
        factionMap.put("xiongxing_zb", CharacterFaction.CORPSE_KING);
        factionMap.put("chongqun", CharacterFaction.CORPSE_KING);
        factionMap.put("siyangyuan_zb", CharacterFaction.CORPSE_KING);
        factionMap.put("kuaidiyuan_zb", CharacterFaction.CORPSE_KING);

        // ========== 东瀛 ==========
        factionMap.put("fengmohuitailang", CharacterFaction.TOYO);

        // ========== 米国欧盟 ==========
        // 黑暗议会（K + 使者 Jack/Laura + 随从）
        factionMap.put("k", CharacterFaction.WESTERN);
        factionMap.put("heianhui_suicong", CharacterFaction.WESTERN);
        factionMap.put("jack", CharacterFaction.WESTERN);
        factionMap.put("laura", CharacterFaction.WESTERN);

        // ========== 其他 ==========
        // 血莲教唯欣、赵日天这类不属于四大阵营的中立/独立势力
        factionMap.put("zhaoritian", CharacterFaction.OTHER);
        factionMap.put("weixin", CharacterFaction.OTHER);
        factionMap.put(CorpseBrother.ID, CharacterFaction.OTHER);

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

    /** 查询指定角色的阵营；没在映射表里的一律归 OTHER */
    public CharacterFaction getFaction(String characterId) {
        return factionMap.getOrDefault(characterId, CharacterFaction.OTHER);
    }

    /** 查询指定阵营下的全部角色（保持注册顺序） */
    public List<ICharacter> getCharactersByFaction(CharacterFaction faction) {
        List<ICharacter> result = new ArrayList<>();
        for (ICharacter c : registeredCharacters.values()) {
            CharacterFaction f = factionMap.getOrDefault(c.getId(), CharacterFaction.OTHER);
            if (f == faction) {
                result.add(c);
            }
        }
        return result;
    }

    /** 全部已定义的阵营（按枚举顺序） */
    public CharacterFaction[] getAllFactions() {
        return CharacterFaction.values();
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

        // 进化属性成长跟着新角色的成长路线（GrowthProfile）重算；
        // evo_* 修饰符 id 全局固定，这里原地替换，旧角色的成长加成不会残留
        xiaoshi2022.corpseorigin.skill.EvolutionStats.reconcile(serverPlayer);

        // ✅ 角色切换后重置内力（上线即满 / 切换角色满内力）
        InnerPowerManager.reset(serverPlayer);

        // ✅ 自动学习该角色的全部技能（技能树未启用，故直接授予）
        if (!(xiaoshi2022.corpseorigin.growth.FreeGrowth.isFree(oldId)
                && xiaoshi2022.corpseorigin.growth.FreeGrowth.isFree(character.getId())))
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
        CharacterBookPolicy.sync(player);
        xiaoshi2022.corpseorigin.skill.unlock.SkillUnlockManager.grantUnlocked(player, true);
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
