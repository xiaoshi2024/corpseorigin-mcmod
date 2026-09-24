# 技能索引与资源消耗

[返回首页](README.md)

此表按技能实现类、动态天罡招式和新角色目录汇总。数值为统一入口的单次启动消耗，**0 不代表无代价**：持续耗能、道具、饥饿、重塑及复活费用另算。被动技能不会出现在技能环。20 tick 约为 1 秒。描述栏引用当前游戏中文提示，仅用于辨识招式，旧提示中的数值不取代服务端规则。

| 技能 / ID | 所属目录 | 内力 / 气血 | 启动冷却 tick | 游戏内说明 | 实现状态 / 来源 |
|---|---|---|---|---|---|
| 诗仙剑·早发白帝城 · `ancient_poetry_sword` | 白小飞 | 10 / 0 | 0 | 需要觉醒气感。启动消耗10内力，左键四段连招依次消耗10/15/20/25内力；剑域每秒消耗2内力并暂停自然回气。内力不足自动回收，再按技能键可免费关闭。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/baixiaofei/AncientPoetrySwordSkill.java) |
| 天线格挡 · `antenna_block` | 天线宝宝尸兄 | 0 / 10 | 400 | 被动：受到斧头与箭矢/投掷物伤害时有 15% 概率整个挡下。主动：立刻张开天线，%s 秒内这类伤害必定挡下。冷却 20 秒。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/tianxianbaobao_zb/AntennaBlockSkill.java) |
| 袋中捕获 · `bag_capture` | 虫母 | 0 / 15 | 400 | 以布袋捕获目标，禁锢并施加毒伤。冷却 20 秒。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/chongmu/BagCaptureSkill.java) |
| 蝙蝠披风 · `bat_cloak` | K | 0 / 15 | 300 | 召唤5只吸血蝙蝠追击附近敌人，持续8秒；可被击杀。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/k/BatCloakSkill.java) |
| 巨力冲撞 · `bear_charge` | 熊型尸兄 | 0 / 8 | 200 | 向前冲撞，撞飞路径上的敌人。冷却 10 秒。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/xiongxing_zb/BearChargeSkill.java) |
| 黑色星期八 · `black_friday_eight` | 杰克 | 0 / 0 | 160 | 一次发射8枚台球，命中后随机延迟0.5至3秒爆炸，单次爆炸24点伤害。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/chapter/BlackFridaySkill.java) |
| 黑金心脏 · `black_gold_heart` | 黑小飞 | 0 / 0 | 1200 | 被动：额外60点最大生命、8点攻击、8点护甲及再生。致命伤保留1点生命并震退敌人（冷却60秒）。 | 被动/事件处理；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/heixiaofei/BlackGoldHeartSkill.java) |
| 血云 · `blood_cloud` | 唯欣 | 10 / 15 | 400 | 血红雾气随身持续8秒，冲向对手；雾中伤害敌人并强化自身与同队目标的力量、速度和抗性。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/weixin/BloodCloudSkill.java) |
| 血莲金瓣 · `blood_lotus` | 唯欣 | 25 / 30 | 1200 | 释放红气缠绕的血莲金瓣实体，命中造成40点伤害和击退。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/weixin/BloodLotusSkill.java) |
| 血莲护身甲 · `blood_lotus_armor` | 尸巢之子 | 10 / 15 | 600 | 主动展开血莲气甲，持续15秒，获得抗性IV和伤害吸收IV。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/shichaozhizi/BloodLotusArmorSkill.java) |
| 血翼黑刃 · `blood_wing_blade` | K | 0 / 15 | 200 | 手持血翼黑刃，以当前攻击力的150%重击目标，将实际伤害的40%转为生命，最多回复4点。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/k/BloodWingBladeSkill.java) |
| 伪装 · `chameleon_disguise` | 变色龙尸兄 | 0 / 15 | 600 | 选择小惠（默认）或输入玩家ID/UUID伪装30秒，隐藏头顶本体；偷袭会解除伪装。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/bianselong_zb/ChameleonDisguiseSkill.java) |
| 菊花盾·开 · `chrysanthemum_shield` | 开胃奶 | 0 / 15 | 400 | 背后菊花盾张开 5 秒：正面来的伤害整下挡掉，迎面箭矢会被弹回给射手。冷却 20 秒。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/kaiweinai/ChrysanthemumShieldSkill.java) |
| 尸兄召集 · `corpse_brother_rally` | 龙右 | 0 / 20 | 1200 | 龙右自带能力，无需进化点解锁。召集附近受尸王威压的尸兄互相吞噬，经历初始、成长、成熟三阶段，形成10×10×10完整空心尸巢（488块肉块）。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/longyou/CorpseBrotherRallySkill.java) |
| 尸兄鱼卵 · `corpse_fish_eggs` | 变异鲑鱼 | 0 / 25 | 200 | 水中产下3枚鱼卵，附着吸血3秒。生物身上的鱼卵随后消失；玩家可用Shift+右键逐个拔除。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/bianyi_guiyu/FishEggSkill.java) |
| 尸王次声波 · `corpse_king_infrasound` | 龙右 | 40 / 0 | 240 | 展开 12 格声场持续 3 秒：频率与飞行中的箭矢/弩矢共振时将其震散（不损坏手持武器）；无内力者直接晕厥受伤，有内力者内力足够则免疫、不足则被抽内力并削弱；范围内的尸兄会被操控，跟随你并扑向你攻击的目标。冷却 12 秒，消耗 40 内力。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/longyou/CorpseKingInfrasoundSkill.java) |
| 雷鳗 · `corpse_king_thunder` | 龙右 | 25 / 0 | 200 | 向视线上的敌人召唤一道巨大的紫色天雷：范围雷伤、击飞、麻痹，并把落点地形轰出一个坑。冷却 10 秒，消耗 25 内力。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/longyou/CorpseKingThunderSkill.java) |
| 血魔大法·赤血矛 · `crimson_blood_spear` | 赵日天 | 30 / 15 | 360 | 四团血气汇聚，蓄力1.2秒凝成赤血长矛，沿瞄准方向射出。射程40格，基础伤害160，消耗30内力，冷却18秒。命中首个敌人或实心障碍后消散。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/zhaoritian/CrimsonBloodSpearSkill.java) |
| 黑暗虹吸 · `dark_siphon` | 黑小飞 | 0 / 0 | 20 | 主手持血翼黑刃贴近敌人持续虹吸。潜行、换手、远离或再次施放拔刀。累计吸收超过自身最大生命的两倍时受到反噬。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/heixiaofei/DarkSiphonSkill.java) |
| 防御姿态 · `defense_stance` | 屠叔 | 0 / 0 | 300 | 凝聚内力护体，获得抗性IV（80%减伤），持续10秒。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/tushu/DefenseStanceSkill.java) |
| 脱离 · `detach_guardian` | 左护法 | 0 / 0 | 200 | 蜕下身上的蛟龙：人恢复人形（仍是尸族），蛟龙落地成为听命于你的宠物 BOSS，可用「合体」收回。冷却 10 秒。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/zuohufa/DetachGuardianSkill.java) |
| 狗眼炮 · `dog_eye_cannon` | 开胃奶 | 0 / 0 | 240 | 将 6 格内最近且视线可达的一只哈姆狗狗朝瞄准方向丢出去。冷却 12 秒。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/kaiweinai/DogEyeCannonSkill.java) |
| 跑路被动 · `escape_passive` | 小言子 | 0 / 0 | 0（被动） | 被动：附近有队友时自身速度 +1。 | 被动/事件处理；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/xiaoyanzi/EscapePassiveSkill.java) |
| 五行阵 · `five_elements_formation` | 木犀、五行阵·金位（占位）、五行阵·水位（占位）、五行阵·土位（占位） | 20 / 0 | 600 | 展开五行阵，为队友提供增益并压制阵内敌人。冷却 30 秒。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/muxi/FiveElementsFormationSkill.java) |
| 炎燕击 · `flame_strike` | 炎燕 | 12 / 0 | 100 | 攻击前方目标；命中自己的燕窝标记时提高伤害并引燃6秒。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/yanyan/FlameStrikeSkill.java) |
| 血肉抛弃 · `flesh_abandon` | 龙右 | 0 / 0 | 200 | 抛弃当前外皮，生成可供少教主夺舍的不死髅体。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/longyou/FleshAbandonSkill.java) |
| 血肉重塑 · `flesh_reshape` | 龙右 | 0 / 0 | 0 | 血肉重构成一具崭新的正常身体，代价是一半饱食度；旧身体同样蜕成分身，行囊留在那具壳上。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/longyou/FleshReshapeSkill.java) |
| 金蝉脱壳 · `golden_cicada_shell` | 龙右 | 0 / 40 | 200 | 把当前身体蜕成一个分身（背包、装备都留在壳上），意识直接转移进藏在体内的「原体」——拇指大小的真身。冷却 10 秒。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/longyou/GoldenCicadaShellSkill.java) |
| 腐蚀喷吐 · `gourd_acid` | 葫芦小金刚 | 0 / 20 | 160 | 16格酸液连续喷吐4次，每次7伤害，附带8秒中毒II和4秒缓慢II；动画5秒，消耗20气血，冷却8秒。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/chapter/NewChapterSkill.java) |
| 巨臂 · `gourd_arms` | 葫芦小金刚 | 0 / 15 | 120 | 前方5格范围巨臂横扫，26伤害并击退；动画4.5秒，消耗15气血，冷却6秒。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/chapter/NewChapterSkill.java) |
| 葫芦吞噬 · `gourd_devour` | 葫芦小金刚 | 0 / 0 | 200 | 蛇形出现，捕获前方8格内的生物并缩小吸入。普通村民和流浪商人（最大生命≤20）可满血吞噬；其他目标只需剩余生命低于最大生命的50%，无固定血量上限。成功击杀转化8–60气血；免费，冷却10秒。排除玩家、队友、驯养宠物及首领。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/chapter/NewChapterSkill.java) |
| 多眼警戒·适配 · `gourd_eyes` | 葫芦小金刚 | 0 / 10 | 240 | 20秒夜视，显露20格内可见敌人10秒；消耗10气血，冷却12秒。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/chapter/NewChapterSkill.java) |
| 烈焰火海 · `gourd_fire` | 葫芦小金刚 | 0 / 30 | 240 | 需手持打火石。16格范围持续喷火10次，每次5伤害并点燃8秒，喷到的地面可燃火；动画7秒，消耗30气血，冷却12秒。地火遵循原版燃烧规则。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/chapter/NewChapterSkill.java) |
| 葫芦·归体／离体 · `gourd_link` | 葫芦小金刚 | 0 / 0 | 20 | 归体为附加骨骼；离体成为独立生命。死亡后再生消耗100气血，创造免费。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/chapter/NewChapterSkill.java) |
| 葫芦护体·适配 · `gourd_power` | 葫芦小金刚 | 0 / 20 | 300 | 获得12秒抗性II和伤害吸收II，恢复离体葫芦8生命；消耗20气血，冷却15秒。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/chapter/NewChapterSkill.java) |
| 地遁突袭 · `ground_burrow` | — | 0 / 15 | 400 | 潜入地下后从玩家脚下破土冲出，造成高额伤害。冷却 20 秒。 | 未挂入当前角色目录；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/shichaozhizi/GroundBurrowSkill.java) |
| 尸棍重击〔占位〕 · `guigun_crush` | 鬼棍·尸兄 | 0 / 0 | 0 | 基于角色武器与战斗方向的占位，招式命名为模组适配。 | 占位，拒绝施放；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/chapter/NewChapterSkill.java) |
| 架棍格挡〔占位〕 · `guigun_guard` | 鬼棍·人类 | 0 / 0 | 0 | 基于角色武器与战斗方向的占位，招式命名为模组适配。 | 占位，拒绝施放；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/chapter/NewChapterSkill.java) |
| 次声波尸棍〔占位〕 · `guigun_resonance` | 鬼棍·尸兄 | 0 / 0 | 0 | 基于角色武器与战斗方向的占位，招式命名为模组适配。 | 占位，拒绝施放；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/chapter/NewChapterSkill.java) |
| 棍术横扫〔占位〕 · `guigun_sweep` | 鬼棍·人类 | 0 / 0 | 0 | 基于角色武器与战斗方向的占位，招式命名为模组适配。 | 占位，拒绝施放；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/chapter/NewChapterSkill.java) |
| 哈姆火球 · `ham_summon` | 小言子 | 0 / 0 | 200 | 用狗笼子捕获哈姆，手持狗笼右键或使用技能发射火球，共享10秒冷却。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/xiaoyanzi/HamSummonSkill.java) |
| 偷袭·捏心 · `heart_grab_ambush` | 变色龙尸兄 | 0 / 15 | 1200 | 从背后近距离重击目标，解除伪装。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/bianselong_zb/HeartGrabAmbushSkill.java) |
| 恶犬出笼 · `hound_unleashed` | 黑小飞 | 0 / 0 | 300 | 将6格内最近且视线可达的自家哈姆朝视线方向发射；无需瞄准敌人，不会生成新哈姆。冷却15秒。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/heixiaofei/HoundUnleashedSkill.java) |
| 刀枪不入 · `iron_body` | 金刚尸兄 | 0 / 0 | 0（被动） | 被动：正面免疫普通攻击，攻击头部弱点才能造成伤害。 | 被动/事件处理；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/jingang_zb/IronBodySkill.java) |
| 金刚尸婴凝聚 · `jingang_infant_convergence` | 龙右 | 0 / 10 | 200 | 召回64格内最近的一阶或二阶金刚玩家，凝聚为一阶尸婴并以脐带连接；使用肌肉狂暴进入二阶后自动断脐，与进化等级无关。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/longyou/JingangInfantConvergenceSkill.java) |
| 饲养员扑击 · `keeper_melee` | 饲养员尸兄 | 0 / 0 | 60 | 近距离攻击目标。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/chapter/KeeperMeleeSkill.java) |
| 杀势刺激气体 · `killing_gas` | 青蛙尸兄 | 0 / 15 | 300 | 吞吐刺激气体，范围增幅杀势并触发狂暴。冷却 15 秒。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/qingwa_zb/KillingGasSkill.java) |
| 杀戮化形·身外化身 · `killing_incarnation` | 黑小飞 | 40 / 40 | 2400 | 召唤持续20秒的杀戮化身，每秒冲击8格内的可见敌人。出场后再次按技能键释放蓄力冲击：1.6秒后攻击前方12格锥形范围，造成64点伤害，特殊攻击独立冷却6秒。蓄力和收招期间暂停普通冲击。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/heixiaofei/KillingIncarnationSkill.java) |
| 吸食 · `life_drain_suck` | 天线宝宝尸兄 | 0 / 0 | 60 | 抓住身边 5 格内最近的目标持续吸血 5 秒（触手会自动转向目标），可被打断技解救。冷却 3 秒。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/tianxianbaobao_zb/LifeDrainSuckSkill.java) |
| 合体 · `merge_guardian` | 左护法 | 0 / 10 | 200 | 把身边的蛟龙收回身上，重新变成蛟龙形态（多段碰撞箱一并回来）。冷却 10 秒。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/zuohufa/MergeGuardianSkill.java) |
| 红陨石之剑 · `meteor_sword` | 风魔灰太郎 | 25 / 0 | 600 | 手持红陨石剑释放红色剑气，攻击12格内目标。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/fengmohuitailang/MeteorSwordSkill.java) |
| 嘴里吐蛇 · `mouth_snake` | 左护法 | 0 / 15 | 200 | 吐出嘴里的金蛇（金旒龙），沿直线贯穿最多 5 个敌人；装了 Snakes Alive 时会真吐出一条蛇，并认你为主。冷却 10 秒，消耗内力 5。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/zuohufa/MouthSnakeSkill.java) |
| 进阶狂暴 · `muscle_rage` | 金刚尸兄 | 0 / 25 | 400 | 血肉狂暴8秒，力量与速度提升，暂时失去刀枪防护。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/jingang_zb/MuscleRageSkill.java) |
| 自然审判·真·球状闪电 · `natural_judgment` | 龙右 | 80 / 0 | 1200 | 凝出球状闪电飞出，沿途持续放电并把敌人拽向球心；撞到东西或到期后炸开，抹平一大片地形。冷却 60 秒，消耗 80 内力。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/longyou/BallLightningSkill.java) |
| 尸巢感知 · `nest_sense` | 龙右、尸巢之子 | 0 / 0 | 0 | 感知128格内肉块附近的入侵者。雷达选择目标，召唤尸巢巨手吞噬或诛杀。肉块抵御爆炸并拦截投射物。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/longyou/NestSenseSkill.java) |
| 锇金化 · `osmium_gold` | 小鹿 | 15 / 0 | 1200 | 全身覆盖金黄色锇金层，获得抗性IV（80%减伤），持续20秒。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/xiaolu/OsmiumGoldSkill.java) |
| 锇冰梭 · `osmium_ice_spike` | 小鹿 | 15 / 0 | 600 | 射出锇冰梭，对视线命中的目标造成 8 点伤害。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/xiaolu/OsmiumIceSpikeSkill.java) |
| 包裹爆炸 · `parcel_bomb` | 快递员尸兄 | 0 / 0 | 200 | 投掷包裹造成简易爆炸伤害。冷却 10 秒。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/kuaidiyuan_zb/ParcelBombSkill.java) |
| 剥下外皮 · `peel_shell` | 尸巢之子 | 0 / 0 | 20 | 从被封印的不死髅体上剥下外皮，恢复人身 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/jingang_zb/PeelShellSkill.java) |
| 扑咬连击 · `pounce_combo` | 金刚尸兄 | 0 / 8 | 160 | 三连扑击，命中附带流血。冷却 8 秒。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/jingang_zb/PounceComboSkill.java) |
| 强力一击 · `power_strike` | 赵日天 | 15 / 0 | 400 | 近身80点重击，目标存活时沿击飞方向破坏普通建筑；不破坏不可破坏方块和容器，遵守区域保护。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/zhaoritian/PowerStrikeSkill.java) |
| 锁气 · `qi_lock` | 左护法 | 20 / 0 | 400 | 锁定周围敌人身上的气：24 格内非尸族活体发光 10 秒，隔着墙也能看到轮廓。冷却 20 秒。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/zuohufa/QiLockSkill.java) |
| 五行逆阵·火球 · `reverse_formation_fireball` | 炎燕 | 20 / 0 | 600 | 吸收同队阵员全部内力释放增强火球。转入的内力每秒消耗4点维持增幅，最多20秒；不足时从原阵员补充。阵员离开则结束。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/yanyan/ReverseFormationFireballSkill.java) |
| 唤龙 · `revive_guardian` | 左护法 | 0 / 0 | 600 | 以 %1$s 点气血为引，将一条被击杀的青龙尸兄重新凝聚于身前。打普通尸兄或 Shift 右键食用尸肉可积攒气血。冷却 30 秒。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/zuohufa/ReviveGuardianSkill.java) |
| 圆舞 · `round_dance` | 黑小飞 | 15 / 0 | 400 | 跃起翻滚旋转2秒，持续攻击6格内可见敌人。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/heixiaofei/RoundDanceSkill.java) |
| 断臂攻击 · `severed_arm_strike` | 黑小飞 | 0 / 15 | 200 | 将右前臂末段甩出，造成26点伤害；4秒后恢复。完整右臂已断时无法使用。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/heixiaofei/SeveredArmStrikeSkill.java) |
| 杀戮觉醒 · `slaughter_awakening` | 白小飞 | 0 / 0 | 1200 | 短暂觉醒杀戮血脉，获得力量 II，持续 10 秒。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/baixiaofei/SlaughterAwakeningSkill.java) |
| 杀势 · `slaughter_momentum` | 黑小飞 | 0 / 0 | 1200 | 杀势持续20秒：力量III、速度III、跳跃III、抗性II与再生，并呈现红眼；持续消耗饥饿。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/heixiaofei/SlaughterMomentumSkill.java) |
| 尸巢之子 · `son_of_corpse_nest` | 尸巢之子 | 0 / 40 | 0 | 生存模式需吸收一千位尸兄。启用后进入少教主的巨大第二形态，获得500点生命、20点护甲与完全击退抗性；再次启用恢复普通形态。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/shichaozhizi/SonOfCorpseNestSkill.java) |
| 空间异能 · `spatial_blink` | 白小飞 | 15 / 0 | 300 | 沿视线进行最长16格空间移动，不穿过实体墙壁，落点检查碰撞和世界边界。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/baixiaofei/SpatialBlinkSkill.java) |
| 八极拳·五禽硬功 · `special_forces_combat` | 屠叔 | 0 / 0 | 60 | 以传统武术硬功近身重击，造成36点伤害并大幅击退。冷却3秒。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/tushu/SpecialForcesCombatSkill.java) |
| 召唤虫群 · `summon_swarm` | 虫母 | 0 / 25 | 600 | 召唤红火蚁与巨大子弹蚁虫群。冷却 30 秒。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/chongmu/SummonSwarmSkill.java) |
| 燕窝 · `swallow_nest` | 炎燕 | 8 / 0 | 120 | 吐出红色唾液，标记目标10秒；用炎燕击命中可引燃。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/yanyan/SwallowNestSkill.java) |
| 群体撕咬 · `swarm_bite` | 虫群 | 0 / 8 | 40 | 群体近战撕咬，附带毒素或爆炸伤害。冷却 2 秒。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/chongqun/SwarmBiteSkill.java) |
| SWORD 剑潮花舞 · `sword_flower` | 劳拉 | 15 / 0 | 300 | 剑潮花舞持续8秒，每半秒对6格内可见敌人造成18点伤害。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/chapter/SwordFlowerSkill.java) |
| 忍术·大天狗神御 · `tengu_divine_array` | 风魔灰太郎 | 40 / 0 | 1800 | 召出大天狗神御，在聊天栏输入 /tengu_laser <目标> 呼叫激光；射程50格，48点伤害，激光冷却2秒。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/fengmohuitailang/TenguDivineArraySkill.java) |
| 千眼万目 · `thousand_eyes` | 尸巢之子 | 0 / 25 | 600 | 第二形态专属：展开万千魔瞳的凝视。正看着你的生物与玩家会被麻木定身，站在原地动不了（转头或躲到墙后就解除）。冷却 30 秒。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/shichaozhizi/ThousandEyesSkill.java) |
| 雷电之力 · `thunder_power` | 龙右 | 5 / 0 | 0 | 主动开关：开启后近战命中附带雷电，造成额外雷伤并让目标短暂麻痹；再按一次关闭。无冷却、不消耗内力。雷电系的基础，也是雷鳗与球状闪电的前置。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/longyou/ThunderPowerSkill.java) |
| 血莲大法·天罡匙 · `tian_gang_blood_lotus` | 龙右、赵日天 | 10 / 0 | 400 | 主副手均可持有，右键或技能键开关，双持时优先主手。启动消耗10内力，蓄力1.2秒后延长刀身50格，左键挥动开启的持刀手并按轨迹判定伤害。开启时消耗1.2内力/秒并暂停自然回气；再次使用、移走武器或内力不足时停止。每目标每0.5秒最多30点基础伤害，方块阻挡伤害。启动冷却20秒，关闭不受冷却限制。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/zhaoritian/TianGangKeySkill.java) |
| 天罡气六重·毁 · `tiangang_hui` | 龙右 | 30 / 0 | 800 | 跃起后向下猛击，落地释放8格震地冲击。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/longyou/TianGangSkill.java) |
| 天罡气二重·疾 · `tiangang_ji` | 龙右 | 15 / 0 | 600 | 30秒内速度大幅提升。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/longyou/TianGangSkill.java) |
| 天罡气三重·力 · `tiangang_li` | 龙右 | 20 / 0 | 600 | 30秒内身体缩小至75%，力量大幅提升。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/longyou/TianGangSkill.java) |
| 天罡气七重·灭 · `tiangang_mie` | 龙右 | 25 / 0 | 300 | 右拳打出红色冲击波，沿途冲击敌人。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/longyou/TianGangSkill.java) |
| 逆破拳 · `tiangang_nipo` | 龙右 | 15 / 0 | 200 | 九重·神期间连续五拳打击前方近身目标。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/longyou/TianGangSkill.java) |
| 破罡 · `tiangang_pogang` | 龙右 | 20 / 0 | 300 | 九重·神期间击破近身目标的内力防御。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/longyou/TianGangSkill.java) |
| 天罡气五重·气 · `tiangang_qi` | 龙右 | 0 / 0 | 0 | 合一基础：不单独施放，参与九重·神的八重合一。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/longyou/TianGangSkill.java) |
| 天罡气九重·神 · `tiangang_shen` | 龙右 | 50 / 0 | 1200 | 习得前八重后八重合一，30秒强化攻防速度，完全化解天罡匙激光，开放高级招式。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/longyou/TianGangSkill.java) |
| 天罡破 · `tiangang_tiangangpo` | 龙右 | 40 / 0 | 1000 | 九重·神期间跃起下砸，释放12格强冲击和地面破坏。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/longyou/TianGangSkill.java) |
| 天罡气八重·无 · `tiangang_wu` | 龙右 | 25 / 0 | 400 | 左拳破除目标的护盾、抗性、吸收与血莲气甲。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/longyou/TianGangSkill.java) |
| 天罡气四重·御 · `tiangang_yu` | 龙右 | 20 / 0 | 600 | 内力护体15秒，物理和内力攻击减伤80%，完全化解天罡匙激光的伤害与击退。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/longyou/TianGangSkill.java) |
| 天罡气一重·智 · `tiangang_zhi` | 龙右 | 0 / 0 | 0 | 合一基础：不单独施放，参与九重·神的八重合一。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/longyou/TianGangSkill.java) |
| 虎爪·飞蜂轮 · `tiger_claw_bee_wheel` | 黑小飞 | 0 / 0 | 100 | 主手持飞蜂轮，右键或技能键发射32格钩爪，命中敌人造成28点伤害并抓住。钩住方块可悬挂摆荡，按空格或再次使用起跳牵引，并尝试登上锚点上方的平台；钩住敌人则拉回敌人。潜行松钩回收，长按或重复操作不会取消牵引。发射冷却5秒，牵引与回收不受冷却限制。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/heixiaofei/TigerClawBeeWheelSkill.java) |
| 暴龙一击 · `tyrant_strike` | 左护法 | 10 / 15 | 300 | 武神阁学来的贯注重击：对正前方单体重击并大幅击飞。冷却 15 秒，消耗内力 8。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/zuohufa/TyrantStrikeSkill.java) |
| 不死髅体 · `undying_chest` | 龙右 | 0 / 0 | 0（被动） | 被动：持续再生，空血后进入「爆头复活」阶段一次。 | 被动/事件处理；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/longyou/UndyingChestSkill.java) |
| 水战撕咬 · `water_bite` | 变异鲑鱼 | 0 / 8 | 120 | 水下突袭撕咬目标。冷却 6 秒。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/bianyi_guiyu/WaterBiteSkill.java) |
| 水异能 · `water_orb` | 白小飞 | 10 / 0 | 160 | 向20格内视线目标释放水异能，造成24点伤害、击退与4秒减速。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/baixiaofei/WaterOrbSkill.java) |
| 尸水之源 · `water_pollution` | 龙右 | 0 / 0 | 0 | 主动开关：开启后走过的水源会被污染成尸水；近战命中血量低于一半的目标时，有 35% 概率把尸水灌进去感染对方（村民/玩家会变异成尸兄）。代价是持续消耗饱食度（约 8 秒 1 点饥饿），再按一次关闭；饿到见底会自动中断。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/longyou/WaterPollutionSkill.java) |
| 木系束缚 · `wood_bind` | 木犀 | 12 / 0 | 200 | 藤蔓实体缠绕8秒，限制水平移动与跳跃，每秒10点伤害。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/muxi/WoodBindSkill.java) |
| 武丑刀法〔占位〕 · `wuchou_blade` | 黑·武丑 | 0 / 0 | 0 | 基于角色武器与战斗方向的占位，招式命名为模组适配。 | 占位，拒绝施放；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/chapter/NewChapterSkill.java) |
| 武丑身法〔占位〕 · `wuchou_step` | 黑·武丑 | 0 / 0 | 0 | 基于角色武器与战斗方向的占位，招式命名为模组适配。 | 占位，拒绝施放；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/chapter/NewChapterSkill.java) |
| 双刀交斩〔占位〕 · `wusheng_cross` | 白·武生 | 0 / 0 | 0 | 基于角色武器与战斗方向的占位，招式命名为模组适配。 | 占位，拒绝施放；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/chapter/NewChapterSkill.java) |
| 武生双刀〔占位〕 · `wusheng_twin` | 白·武生 | 0 / 0 | 0 | 基于角色武器与战斗方向的占位，招式命名为模组适配。 | 占位，拒绝施放；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/chapter/NewChapterSkill.java) |
| 玄武甲 · `xuanwu_body` | 龙右、尸巢之子 | 0 / 25 | 20 | 展开全身龟甲，获得玄武鳞甲防护。再次使用可关闭。 | 已有施放实现；[源码](../../src/main/java/xiaoshi2022/corpseorigin/skill/longyou/XuanwuBodySkill.java) |

## 学习和使用

B 打开技能树；V 打开分页技能环，每页最多 10 项；1、2、3 对应快捷技能槽。学习检查等级、可用进化点、前置技能以及机遇/道具条件。携带红陨石剑、血翼黑刃、飞蜂轮、天罡匙可为匹配角色或自由角色解锁对应技能，但武器资格仍要满足。

内力与气血分别检查，不能互相代付。没有气感时不能释放诗仙剑等内力技能。诗仙剑四段另需 10/15/20/25 内力，领域维持另耗 2 内力/秒；天罡匙、雷电、复活等也有专用持续或内部费用。

移除了统一的“必须瞄准攻击目标”门槛，但抓取、吞噬和实际伤害仍需要有效命中。开胃奶狗眼炮投掷 6 格内可见哈姆；黑小飞恶犬出笼还要求这只哈姆属于自己，均不会凭空生成哈姆。
