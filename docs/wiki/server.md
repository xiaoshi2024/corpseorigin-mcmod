# 服务器配置与指令

[返回首页](README.md)

主配置为 `config/corpseorigin.json`。第一次启动生成默认值；服务端玩法读取服务端文件，客户端 HUD/皮肤读取客户端文件。改后重启对应端。无效 JSON 可能回退默认配置，不能靠写坏字段禁用功能。

## 角色书禁用

在主 JSON 中添加或修改以下段，不要用它覆盖整个文件：

```json
"characterBooks": {
  "disabledCharacters": ["longyou", "zuohufa", "xiaojingang"]
}
```

禁用后通用书和绑定书都不能选择对应角色，创造和管理员使用书也受限制；已有角色不撤销，书不删除。管理员显式 `/character select` 不受书的限制。ID 允许带 `corpseorigin:` 前缀，使用[角色图鉴](characters.md)里的实际 ID。

## growth 默认值

以下均位于 `growth` 对象；秒数按 20 tick/s 换算。

| 字段 | 默认 | 含义 |
|---|---:|---|
| enabled | true | 成长及相关生理能力总开关，不等于整个模组的总开关 |
| flightBloodPerSecond | 6 | 飞行气血/秒 |
| aquaticBloodPerSecond | 2 | 水生气血/秒 |
| combatBloodPerSecond | 4 | 战斗补给每秒气血上限 |
| fleshPerGrowthPoint | 5 | 自由尸兄每多少次吸食得 1 点 |
| fleshPerOpportunity | 20 | 血肉机遇基础间隔 |
| fleshOpportunityMultiplier | 3 | 实际间隔乘数，默认 60 次 |
| fleshOpportunityCooldownTicks | 12000 | 血肉机遇最短间隔，10 分钟 |
| fleshOpportunitiesEnabled | true | 血肉技能机遇开关 |
| villageTrainingPoints | 3 | 每次有效村民训练点数 |
| villageLessonsPerOpportunity | 3 | 每多少次课程产生一次技能机遇 |
| villageOpportunitiesEnabled | true | 课程技能机遇开关，不关闭普通训练奖励 |
| explorationOpportunityChance | 0.5 | 自由角色探索发现技能概率 |
| explorationOpportunitiesEnabled | true | 探索技能机遇开关，不等于关闭结构点数 |
| teachingEnabled | true | 配置 NPC 传承开关 |
| teachingCooldownTicks | 12000 | 首次传承之间的冷却 |
| juqueExecuteThreshold | 0.20 | 巨阙处决阈值比例 |
| juqueExecuteDamageCap | 20 | 处决额外伤害上限 |
| sustainedAreaDamageMultiplier | 0.35 | 已接入持续范围伤害的倍率，非全技能通用倍率 |
| preyRequired | 5 | 旧猎物进度兼容字段；不再用来解锁翼/尾 |
| inheritanceChance | 0.15 | 旧随机继承字段；器官现走天梯 |
| exploration | 见下文 | 一次性结构奖励 |
| teachings | [] | 自定义 NPC 传承事件 |

想降低机遇频率，可提高 `fleshOpportunityMultiplier`、`fleshOpportunityCooldownTicks`、`villageLessonsPerOpportunity`，降低 `explorationOpportunityChance`。降低机遇不等于降低所有击杀点数或血肉掉落；后者需要分别调整实际实现或掉落数据包。

默认探索数组：

```json
[
  {"structure": "corpseorigin:h_city_ruins", "points": 12},
  {"structure": "corpseorigin:h_city_library", "points": 25}
]
```

`teachings` 示例条目：

```json
{"id":"master_water_orb","npcTag":"corpseorigin_teacher_water","role":"baixiaofei","skill":"water_orb","points":10}
```

管理员给 NPC 执行 `/tag <NPC选择器> add corpseorigin_teacher_water`。展示名称不参与匹配。技能必须确实在适用角色目录中；事件 ID 要稳定且唯一，防止改 ID 意外重复领奖。

## 其他配置分组

| 分组 | 配置内容与默认要点 |
|---|---|
| spawn | lowerLevelZbWeight=2、aotumanZbWeight=2、mikuZbWeight=2、cocoZombieWeight=2、cocoZombieXWeight=1、cocoPenguinWeight=8、uncleWeight=3；nearBywaterChance=0.2 |
| infectedWater | oceanDilutionTicks=24000，尸水接触海水后的稀释时间；0 关闭 |
| names | 尸兄皮肤名字来源；ids 默认空，consentedChance=8；名单只填符合格式并获同意的账号 |
| skin | tintUnresolvedSkins=true，tintStrength=1.0 |
| mutantBody | 左护法模型开关、scale=0.5、yOffset=0、动作名映射、多段碰撞箱 |
| compat | mouthSnakeUsesSnakesAlive=true，mouthSnakeSpecies=king_snake；未安装时使用内置效果 |
| hud | x/y=-1 自动定位，batteryWidth=116、batteryHeight=13、rowGap=3、scale=1 |
| skillHud | x/y=-1，spacing=4、scale=1 |

多段碰撞箱以服务端固定偏移表达，不能自动随客户端每根动画骨骼移动。现场调节后要写回配置才能跨重启保留。

## 指令速查

`<>` 必填，`[]` 可选。管理指令通常需要游戏管理员权限；以下同时列出普通玩家功能。目标选择器按 Minecraft 标准填写。

| 指令 | 用途 / 权限 |
|---|---|
| `/character` 或 `/character current` | 查看自己角色 |
| `/character list` | 列角色 ID |
| `/character clear` | 清除自己角色，当前入口未要求管理员权限 |
| `/character select <id>` | 管理员切换自己的角色 |
| `/character unlock <skill> <targets>` | 管理员授予指定技能 |
| `/character unlockall [targets]` | 管理员解锁；省略目标会作用所有在线玩家 |
| `/character hitbox` | 列碰撞箱配置 |
| `/character hitbox <segment> <field> <value>` | 现场调整，重启不保留；当前实现没有管理员权限检查，公开服需留意 |
| `/corpsepoints get [player]` | 管理员查点数 |
| `/corpsepoints add <amount> [player]` | 管理员加点 |
| `/corpsepoints set <amount> [player]` | 管理员设置点数，amount 非负 |
| `/corpseskill list [player]` | 管理员查看技能状态 |
| `/corpseskill all` | 管理员列全部角色技能 |
| `/corpseskill unlock <player> <skill>` | 管理员强制学习，不代表施法可绕过资源/角色条件 |
| `/corpseskill relic <player> list` | 管理员查获取物记录 |
| `/corpseskill relic <player> add/remove <relic>` | 管理员增删记录，不等于安装器官 ZIP |
| `/corpselimb <targets> sever/regen <slot>` | 管理员断肢/再生，slot 用 Tab 补全 |
| `/corpselimb <targets> clear/info` | 管理员清除/查看断肢 |
| `/summonzb [playerName]` | 管理员生成尸兄 |
| `/shichaoform` | 少教主形态切换，仍检查角色和技能 |
| `/heartwake` | 植入心脏假死后的主动苏醒 |
| `/tengu_laser <target>` | 天狗相关锁定指令，检查所属召唤物、角色与目标 |
| `/corpse_scene <cue> <players>` | 管理员剧情姿态；cue 包含 injured、parasitized、drain、poisoned、knockback、entrance、threat、bound、fear、black_general、infant、adult、clear |

配置来源：[CorpseConfig](../../src/main/java/xiaoshi2022/corpseorigin/config/CorpseConfig.java)、[GrowthConfig](../../src/main/java/xiaoshi2022/corpseorigin/growth/GrowthConfig.java)。指令来源：[command 目录](../../src/main/java/xiaoshi2022/corpseorigin/command)、[剧情动作](../../src/main/java/xiaoshi2022/corpseorigin/skill/chapter/ChapterScenes.java)。
