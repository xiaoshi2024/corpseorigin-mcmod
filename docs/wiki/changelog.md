# 更新日志

[返回首页](README.md)

按日期记录已经落地的玩法与规则变更。条目只写**行为**和**往哪调**，实现细节以源码为准；与旧页面冲突时以本页 + 源码为准。

## 2026-09-26

### 象棋尸兄：从"一个方块"变成"方块形态的尸兄实体"

**战斗**

- 方块本体改为不可挖掘（`strength(-1, 0)`），击杀只能靠血量：40 HP、10 tick 无敌帧。
- 三个伤害入口全部接通：

| 来源 | 实现 |
|---|---|
| 近战左键 | [`ServerPlayerGameModeMixin`](../../src/main/java/xiaoshi2022/corpseorigin/mixin/ServerPlayerGameModeMixin.java)，攻击力 × 攻击冷却 |
| 箭矢 / 三叉戟 | [`ProjectileOnHitBlockMixin`](../../src/main/java/xiaoshi2022/corpseorigin/mixin/ProjectileOnHitBlockMixin.java) + [`AbstractArrowAccessor`](../../src/main/java/xiaoshi2022/corpseorigin/mixin/AbstractArrowAccessor.java)，baseDamage × 箭速 |
| 爆炸 | [`CNChessZbrsBlock#wasExploded`](../../src/main/java/xiaoshi2022/corpseorigin/block/CNChessZbrsBlock.java)，威力 × 6 点 |

- HP 归零：掉落自身方块物品 → 播 `die` → 方块消失。
- 处决 `/chesszb execute`：100% 最大生命值的致命伤；尸王与地2及以上玩家豁免，只受 8 点普通伤害。

**修复**

- 覆盖了 `BlockEntity#getUpdatePacket`（26.2 默认返回 `null`）。在此之前 `sendBlockUpdated` 只发方块状态、不发 tag，血量 / 动画 / 走子状态其实同步不到客户端。

**行动：走子**

- [`movePiece`](../../src/main/java/xiaoshi2022/corpseorigin/block/entity/CNChessZbrsBlockEntity.java)：校验目标格 → 整体搬迁（HP 等状态保留）→ 渲染偏移平滑滑动。
- 指令 `/chesszb move <up|down|north|south|east|west> [格数]`，命令方块可用。

**背上生物随行**

- 站在它背上（脚底与方块同格）的生物跟着一起走；判定与处决夹人同一套。
- 与模型**同一条缓动曲线**同步滑动：客户端画模型、服务端搬乘客共用 `remainingFactor()`。
- 新增净空校验：目标格上方放不下乘客时，整次走子被拒绝。
- 每 tick 由 `registerCarryTick()` 推进（挂服务端全局 tick，不是方块自己的 4 tick 调度）。

### 低阶尸兄：吸食血肉进化

- 击杀活体得血肉能量：动物 1 / 村民 3 / 同类 4 / 玩家 5；攒满"等级 × 10"进化。
- 1–5 级正常进化；5 级后是**超脱临界**：每次 15% 成功率，每失败一次下次 +10%；上限 10 级。
- 每次突破成功随机突变一个器官（关节、偏移、旋转、缩放都随机）。
- 器官实际生效：腮 = 水下呼吸，翅膀 = 免摔落 + 缓落，吸血 = 命中回血，解剖 = 加攻 / 加速 / 夜视。
- 新增：[`ZbEvolution`](../../src/main/java/xiaoshi2022/corpseorigin/entity/evolution/ZbEvolution.java)、[`ZbOrganGrowth`](../../src/main/java/xiaoshi2022/corpseorigin/entity/evolution/ZbOrganGrowth.java)、[`ZbOrganEffects`](../../src/main/java/xiaoshi2022/corpseorigin/entity/evolution/ZbOrganEffects.java)、[`ZbOrganLayer`](../../src/main/java/xiaoshi2022/corpseorigin/client/render/layer/ZbOrganLayer.java)。

### 自然生成：按游戏日"越后期越强"

- 自然生成的尸兄会掷进化等级，**权重峰值跟着当天的等级上限走**，不再堆在最低级。
- 默认台阶：每 8 天抬一档；第 0–7 天全人1，约 56 天到地4；地2（6 级）以上顺带带突变器官。
- 只对自然生成生效，刷怪蛋 / `/summon` 不受影响。
- 凹凸曼排除在外（固定强度 + 渲染器无器官层），走 `rollsSpawnEvolution()` 钩子。

### 器官来源分级

- [`OrganLibrary`](../../src/main/java/xiaoshi2022/corpseorigin/growth/OrganLibrary.java) 新增来源标记：内置（硬编翅膀 / 尾巴）、玩家目录、玩家资源包、示例。
- 硬编的翅膀与尾巴现在是**真·硬编源**，`examples.json` 不再能覆盖它。
- 自动生成的 `examples.json` 与模组分发的示例包标记为"示例"，**不参与尸兄突变**；玩家器官编辑器照旧可见。
- 支持全身替换（`full_body`）：开放该关节（一只一个）、映射到 `Body` 骨骼、生效时跳过本体几何。

### 开胃奶：吃生肉直接补气血

- 生肉 **+20 气血**、生鱼 **+10**（血肉储备 `BloodReserve`，0–600，就是 HUD 那条电池条），带红心粒子与「气血 +N」提示。
- 判定走 Fabric 约定标签，整合包 / 其他模组的生肉同样生效；气血条满了不再重复提示。
- 新增 [`ItemConsumeMixin`](../../src/main/java/xiaoshi2022/corpseorigin/mixin/ItemConsumeMixin.java)。

### 技能解锁条件修正

| 技能 | 改动 |
|---|---|
| 杀戮觉醒 | → 地3（绝对 7 级）+ 3 点 |
| 水异能 | → 天（9 级） |
| 空间异能 | → 天（9 级） |
| 小鹿·锇金化 / 锇冰梭 | 不再能花点解锁，只能吃**美杜莎之眼**（获取式，同黑金心脏模式） |

### 技能解锁难度中心表重调

[`SkillLearningRules`](../../src/main/java/xiaoshi2022/corpseorigin/skill/unlock/SkillLearningRules.java) 改为**保守梯度**并按冷却分档：

| 档位 | 判定 | 等级 | 点数 |
|---|---|---|---|
| 普通招式 | 冷却 < 200 | 人2 | 4 |
| 中坚招式 | 冷却 200–599 | 人3 | 6 |
| 强力招式 | 冷却 ≥ 600 | 人4 | 10 |
| 辅助 | UTILITY | 人2 | 4 |
| 被动 | 不可主动释放 | 人2 | 5 |
| 终极 | ULTIMATE | 地1 | 18 |

- "人1 就能学"的技能从 90 个降到 7 个，剩下的都是刻意保留的例外。
- `level()` 签名补上 `cooldown` / `active`（`AbstractSkill` 已同步，新增技能无需关心）。
- 例外技能（左护法 6 式、杀势、天罡 12 式、赤血枪、尸兄集结、获取式那批）**保持原值**，留待单独调整。

### 鬼棍：形态不串 + 握兵器右键出招

- [`RoleChapterSkill#checkUsable`](../../src/main/java/xiaoshi2022/corpseorigin/skill/chapter/RoleChapterSkill.java) 默认是**角色硬门槛**；招式可以按需声明"**只认兵器**"（六参构造器 `weaponOnly`），此时不看角色：

| 招式 | 绑定兵器 | 门槛 |
|---|---|---|
| 棍术横扫 `guigun_sweep` | 三节棍 | **只认兵器 —— 任何角色拿着都能使** |
| 尸棍共振 / 尸棍重击 | 尸棍 | **只认兵器 —— 尸兄身体（含尸兄鬼棍）拿着就能使**；人形角色拿不动尸棍 |

- 新增 `RoleChapterSkill#castWithWeapon`：握着对应兵器右键**直接出招** —— 不必先在技能轮盘里选中，也**不要求已学会**（`SkillManager.activate(..., requireLearned=false)`）；同一把兵器绑多招时放当前能放的那一招。
- 棍术横扫**不再消耗内力**（原 10 点）：尸兄鬼棍的内力上限是 0，收内力会让它反而挥不动三节棍。
- 天生的招式改成**按形态给**（`SkillLearningRules.innate`）：人类形态只有棍法与金针刺穴，尸兄形态只有尸棍那两招，不再按 `guigun_` 前缀一刀切。
- 尸棍本体的资格门槛（`WeaponEligibility.itemReason`）与招式门槛（`skillReason`）都按**"是不是尸兄身体"**判（`PlayerCorpseComponent.isCorpse`）：尸兄玩家（含尸兄鬼棍）可用；人形角色（含人类鬼棍）连近战都挥不出它。
- 生效于三节棍（`GuigunWeapItem`）与尸棍（`GuigunClubItem`）。

### 黑金心脏：尸兄也能承装

- 植入门槛从"只能是黑小飞"放宽到**黑小飞或已经是尸兄的身体**（`HeartImplant.canBear`，六处旧判定统一收进这一个方法）。
- 连带放开的：假死 / 唤醒流程、感染度收尾、`SkillRework` 里的黑金心脏强化（+60 生命 / +8 攻击 / +8 护甲）与每 20 tick 回血。
- **Boss 形态除外**：龙右 / 左护法 / 食巢之子的属性被刻意置零（它们自带远高的本体数值），不叠加黑金心脏那一份。
- 背包里的「心脏预览」页也对尸兄开放（`InventoryHeartPreviewMixin`）：原来写死 `role == heixiaofei`，现在与服务端同口径 —— 客户端读 `corpseDataCache`，和感染条 HUD 同源。
- 想收回口子：只改 `HeartImplant.canBear`。

### 象棋尸兄棋子：可被尸兄吸收成气血

- 潜行 + 右键「象棋尸兄」物品 → 消耗 1 个，气血 **+60**，与「尸兄肉块」共用同一条右键通道（`BloodReserve.init`）和同一句提示。
- 数值不是拍脑袋：走既有的"生物血量 → 血肉"公式（`GourdBalance.flesh`，象棋尸兄 40 血 → 60），并直接引它的血量常量，改血量会自动跟着变。
- **只有尸兄身体能吸**（`WeaponEligibility.corpseBody`）；尸兄肉块那条保持原样（凡是够格的尸族都能吃，+20）。

### 顺带修掉的两个隐藏缺陷

1. `BlockEntity#getUpdatePacket()` 在 26.2 默认返回 `null` —— 方块实体的自定义同步字段一直没真正发出去。
2. `RoleChapterSkill` 里"绑了兵器就跳过角色校验"是刻意的旧设计，但它让人类鬼棍能放尸兄鬼棍的招式。

### 调参速查

| 想改什么 | 去哪 |
|---|---|
| 走子速度 | `CNChessZbrsBlockEntity.MOVE_TICKS_PER_BLOCK` |
| 尸兄血量 / 无敌帧 / 处决豁免等级 | 同上文件顶部常量 |
| 自然生成强度曲线 | `config/corpseorigin.json` → `spawn`：`evolutionLevelRamp`、`daysPerEvolutionLevel`、`maxEvolutionLevel`、`levelDecay` |
| 进化能量与突破率 | `ZbEvolution` 顶部常量 |
| 技能难度中心表 | `SkillLearningRules.cost()` / `level()` |
| 各类生成权重、湖边聚集 | `config/corpseorigin.json` → `spawn` |

### 本次未同步的页面

下面这些页面的内容在本日改动后已经过时，尚未修改：`skills.md`（技能点数 / 等级表）、`growth.md`（进化与器官）、`organs.md`（器官来源）、`world.md`（自然生成）。

依据：[象棋尸兄方块实体](../../src/main/java/xiaoshi2022/corpseorigin/block/entity/CNChessZbrsBlockEntity.java)、[走子指令](../../src/main/java/xiaoshi2022/corpseorigin/command/ChessZbCommand.java)、[进化规则](../../src/main/java/xiaoshi2022/corpseorigin/entity/evolution/ZbEvolution.java)、[器官来源](../../src/main/java/xiaoshi2022/corpseorigin/growth/OrganLibrary.java)、[技能难度中心表](../../src/main/java/xiaoshi2022/corpseorigin/skill/unlock/SkillLearningRules.java)、[章节招式基类](../../src/main/java/xiaoshi2022/corpseorigin/skill/chapter/RoleChapterSkill.java)、[配置](../../src/main/java/xiaoshi2022/corpseorigin/config/CorpseConfig.java)。
