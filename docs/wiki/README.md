# CorpseOrigin 尸兄模组 Wiki

适用：**1.0.3-alpha-26.2** · Minecraft 26.2 / Fabric · 核对日期：2026-09-24。

这份 Wiki 以工作区当前源码、注册表及数据资源为依据。它介绍已经接入的玩法，并明确区分占位、资源外观和真正的能力。旧开发记录属于历史资料，规则冲突时不要照旧记录操作。本文不是所有玩法已经实机验收的声明。

## 阅读导航

| 想了解什么 | 页面 |
|---|---|
| 安装、选角色、按键、第一步怎么玩 | [入门指南](quickstart.md) |
| 42 个角色及各自技能目录 | [角色图鉴](characters.md) |
| 103 个技能条目、资源费、冷却与实现入口 | [技能索引](skills.md) |
| 等级、点数、内力、血肉和机遇 | [生存与成长](growth.md) |
| 器官天梯、加点、飞行、水肺、装配 | [器官玩法](organs.md) |
| Blockbench、ZIP、动画、多臂和水枪 | [器官包制作](organ-packs.md) |
| 葫芦附着/离体、技能和吞噬 | [小金刚与葫芦](gourd.md) |
| 武器资格、克隆、感染、生物和世界 | [装备与世界](world.md) |
| 全部物品、实体、方块、效果 ID | [注册索引](registry.md) |
| 工作台配方与掉落表 | [合成与掉落](recipes.md) |
| 服务器限制、平衡配置、管理指令 | [服务器管理](server.md) |
| 不能保存、包不刷新、无法施法等 | [常见问题与限制](faq.md) |

## 数据口径

- 20 tick 在正常运行时约为 1 秒；生命 2 点对应原版 1 颗心。
- 成长等级、尸兄生理形态、角色身份和阵营是不同概念。
- 可用进化点用于消费，累计进化点用于计算等级。
- 技能表的启动费不包含全部持续费、物品费和内部复活费。
- 角色有技能入口不等于原著全部能力完成，也不等于凡人能无条件施放。
- 注册实体包含投射物、分身、技能构造体；不能把它们全部写成自然刷怪。

## 维护与核对

主要依据：[角色注册](../../src/main/java/xiaoshi2022/corpseorigin/character/CharacterManager.java)、[统一资源费用](../../src/main/java/xiaoshi2022/corpseorigin/skill/SkillResourceRules.java)、[成长逻辑](../../src/main/java/xiaoshi2022/corpseorigin/growth/SurvivalGrowth.java)、[器官天梯](../../src/main/java/xiaoshi2022/corpseorigin/growth/OrganEvolutionRules.java)、[服务器配置](../../src/main/java/xiaoshi2022/corpseorigin/config/CorpseConfig.java)。

[机器可读目录](catalogue.json)保存角色与技能 ID 的本次快照。以后修改注册表、技能消耗、配置默认值时，应同时更新 Wiki。游戏中文技能描述保留在技能表用于辨认；涉及准确伤害或专用判定时，以对应实现及专题页为准。
