# 自定义器官包

[返回首页](README.md) · [器官玩法](organs.md)

## 安装完整包

1. ZIP 放入游戏的 `config/corpseorigin/organ/`，也支持解压目录。
2. 根目录必须直接包含 `pack.mcmeta`、`organ.json`、`assets/`，不要多套一层文件夹。
3. F8 点击“刷新并加载器官包”，等待资源重载和服务器目录回复。
4. 到进化天梯解锁，在装配页选模型并保存。

不会监听文件变化，刚复制 ZIP 不会立刻出现。联机时服务端也必须安装批准的定义；客户端本地包不能自行注册远程服务器器官。旧 `config/corpseorigin/organs/*.json` 目录仍兼容；包内同 ID 定义覆盖外部定义，多个包按文件名顺序后者覆盖前者。

F8 的“打开器官资源包目录”会打开单数 `organ` 目录并释放示例，不覆盖已有文件。现成包和源文件见 [examples/organ-pack](../../examples/organ-pack/README.md)。

## Blockbench 制作约定

导出 GeckoLib GEO、动画 JSON 和 PNG；器官根放在连接点，内部骨骼可以自定义。这里绑定的是玩家关节，不能只靠把模型骨骼改名来获得攻击或多视角。

```text
my-organs.zip
├─ pack.mcmeta
├─ organ.json
└─ assets/myorgans/
   ├─ geckolib/models/bottle.geo.json
   ├─ geckolib/animations/bottle.animation.json
   └─ textures/bottle.png
```

`pack.mcmeta` 可从当前示例包复制版本信息。`organ.json` 根是数组，例如：

```json
[
  {
    "id": "bottle_body",
    "name": "矿泉水尸兄",
    "trait": "cosmetic",
    "model": "myorgans:geckolib/models/bottle.geo.json",
    "texture": "myorgans:textures/bottle.png",
    "animation": "myorgans:geckolib/animations/bottle.animation.json",
    "clips": {"idle": "bottle.idle", "walk": "bottle.walk", "attack": "bottle.attack"},
    "anatomy": {"extraArms": 2, "damagePerArm": 1.0, "extraLegs": 0, "speedPerLeg": 0.02, "stomachBonus": 100, "nightVision": false},
    "waterJet": {"material": "water", "capacity": 100, "refill": 25, "cost": 10, "cooldownTicks": 20, "damage": 6, "range": 16}
  }
]
```

示例中的路径与动画名必须替换成自己真正导出的资源。`trait` 支持 `cosmetic/wings/gills/vampire`；动画状态支持 `idle/walk/attack/crouch/fly/glide/swim`，缺状态回退 idle，所有引用动画必须实际存在。每项最多 128 骨骼、1024 立方体，服务器目录最多 128 项。

## 能力参数

| 字段 | 范围和效果 |
|---|---|
| extraArms / extraLegs | 各 0–8 |
| damagePerArm | 0–2，装配总额外攻击最多 8 |
| speedPerLeg | 0–0.05，总额外移速最多 20% |
| stomachBonus | 0–500，取已装配最高值 |
| nightVision | 布尔值，启用夜视每秒耗 1 气血 |
| waterJet.capacity | 1–1000 |
| refill / cost | 1 到容量 |
| cooldownTicks | 5–1200 |
| damage | 大于 0，不超过 40 |
| range | 1–32 格 |
| material | water / mud / sand，缺省 water |

重复同 ID 不复制属性或储量。G 喷射，潜行 G 补充；水对准 4 格内水源，不消耗水源方块；泥消耗主手泥土/泥巴，沙消耗沙/红沙。多个吞吐器官按装配顺序取首个匹配项。创造免储量消耗，仍有冷却；泥沙目前为直线即时判定配粒子，不是飞行弹丸。

## 联机与边界

同步器官定义、装配和储量，**不实时上传下载玩家的 ZIP**。服主统一分发资源包，或各客户端安装相同资源后刷新。完整身体外观并不赋予独立头部视角；多头相机、任意技能脚本、可视化能力编辑器尚未实现。

依据：[器官定义](../../src/main/java/xiaoshi2022/corpseorigin/growth/OrganDefinition.java)、[能力实现](../../src/main/java/xiaoshi2022/corpseorigin/growth/OrganAbilities.java)、[渲染挂点](../../src/main/java/xiaoshi2022/corpseorigin/client/render/layer/CustomOrganLayer.java)。
