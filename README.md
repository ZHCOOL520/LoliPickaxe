# LoliPickaxe（Minecraft 1.20.1 Forge 移植版）

> 本项目是 [LoliPickaxe](https://github.com/IslenautsGK/LoliPickaxe)（Minecraft 1.12.2 Forge）到
> **Minecraft 1.20.1 + Forge 47+** 的移植版本。

| 项目 | 值 |
| --- | --- |
| 当前版本 | `1.0` |
| 支持版本 | Minecraft **1.20.1** + Forge **47+** |
| mod id | `lolipickaxe` |
| 包名根 | `com.anotherstar` |
| 可选前置 | [JEI](https://www.curseforge.com/minecraft/mc-mods/jei)（**仅在需要查看合成表时**，非必需） |
| 许可 | GPL-3.0-only |

---

## 一、作者与来源（署名）

| 角色 | 署名 | 链接 |
| --- | --- | --- |
| 原作者（1.12.2 版本） | **Is_GK** | <https://github.com/IslenautsGK/LoliPickaxe> |
| 1.20.1 Forge 移植 | **ZHCOOL520** | <https://github.com/ZHCOOL520/LoliPickaxe> |

- 原始项目：<https://github.com/IslenautsGK/LoliPickaxe>（Minecraft 1.12.2，mod 版本 `1.2.16f`）
- 移植仓库：<https://github.com/ZHCOOL520/LoliPickaxe>（Minecraft 1.20.1 Forge，mod 版本 `1.0`）

> **版本号说明**：移植版使用本仓库自己的编号 **`1.0`**。
> 原 1.12.2 版本的 `1.2.16f` 属于上游编号，两套编号不必一一对应。

### 许可

原始项目使用 **GNU General Public License v3.0**（见根目录 [LICENSE](LICENSE)）。
本移植版为原项目的衍生作品，**同样以 GPL-3.0 发布**，请遵守 GPL 的传染性条款（分发时必须提供对应源码）。

`META-INF/mods.toml` 中的 `authors` / `credits` 字段已同步写入上述署名信息，
原作者署名与出处**保持保留**。

---

## 二、构建

环境要求：

- JDK 17（Forge 1.20.1 要求）
- Gradle 8.1.1（已通过 `gradle/wrapper` 锁定）

```bash
# 编译
./gradlew compileJava

# 构建 mod jar（输出到 build/libs/LoliPickaxe-<version>.jar）
./gradlew build

# 启动开发环境客户端 / 服务端
./gradlew runClient
./gradlew runServer
```

> 本机 `JAVA_HOME` 未配置时，可在 `gradle.properties` 中通过 `org.gradle.java.home` 指定 JDK 17，
> 或在执行前设置环境变量 `JAVA_HOME`。

---

## 三、内容一览

### 3.1 物品

#### 工具

| 注册名 | 显示名 | 说明 |
| --- | --- | --- |
| `loli_pickaxe` | 氪金萝莉 | 核心工具，拥有无敌、范围挖掘、范围攻击、储藏室等全套能力 |
| `small_loli_pickaxe` | 普通萝莉 | 氪金萝莉的前置，可逐项升级属性 |
| `loli_dispersal` | 萝莉退散! | 用于解除萝莉效果 |
| `bug_entity_clear` | 清除只在客户端存在的生物（擦屁股） | 清理客户端残留实体 |

#### 升级材料（14 种）

每种材料用 **damage 值承载等级**（0 基），等级越高效果越强。

| 注册名 | 显示名 | 等级数 |
| --- | --- | --- |
| `loli_coal_addon` | 回到过去升级 | 10 |
| `loli_iron_addon` | 采集速度升级 | 10 |
| `loli_gold_addon` | 攻击伤害升级 | 7 |
| `loli_redstone_addon` | 攻击速度升级 | 4 |
| `loli_lapis_addon` | 时运掠夺升级 | 6 |
| `loli_diamond_addon` | 采集等级升级 | 6 |
| `loli_emerald_addon` | 采集范围升级 | 5 |
| `loli_obsidian_addon` | 勇气之霎升级 | 10 |
| `loli_glow_addon` | 状态效果升级 | 3 |
| `loli_quartz_addon` | 攻击范围升级 | 3 |
| `loli_nether_star_addon` | 储存容量升级 | 5 |
| `loli_auto_furnace_addon` | 自动熔炼升级 | 1 |
| `loli_fly_addon` | 飞行升级 | 1 |
| `loli_entity_soul_addon` | 生物灵魂 | 7 |

> 升级规则：**9 个同级材料 → 1 个高一级材料**，也可反向拆分。
> 材料等级名称统一为「一级 ~ 十级」，最高级显示为「终极」。

#### 卡片与唱片

| 注册名 | 显示名 | 获取方式 |
| --- | --- | --- |
| `loli_card` | 萝莉卡片 | 击杀生物概率掉落（默认 10%） |
| `loli_card_album` | 萝莉卡片册 | 击杀生物概率掉落（默认 1%） |
| `loli_card_online` | 萝莉网络卡片 | 合成（8 皮革 + 1 钻石） |
| `loli_record` | 萝莉唱片 | **仅苦力怕**概率掉落（默认 0.1%），可由配置追加 |

### 3.2 方块

| 注册名 | 显示名 | 说明 |
| --- | --- | --- |
| `loli_altar` | 萝莉祭坛 | 功能性方块 |
| `password_work_bench` | 密码工作台 | 带密码的合成台 |
| `loli_blue_screen_tnt` | 蓝屏炸弹 | 9 个 TNT 合成 |
| `loli_exit_tnt` | 蹦溃炸弹 | 5 个 TNT 合成 |
| `loli_fail_respond_tnt` | 未响应炸弹 | 8 个 TNT 合成 |

### 3.3 实体

| 注册名 | 显示名 | 说明 |
| --- | --- | --- |
| `loli` | 萝莉 | 可配置行为、模型与瞬移的强战力生物 |
| `loli_buff_attack_tnt` | 特效炸弹 | 三色炸弹点燃后的实体形态 |

> **说明**：本模组**没有**传统意义上的 Boss 实体。源码中注册的实体只有上述两个。

### 3.4 合成表

共 **23** 个配方。其中 20 个是标准工作台配方，**JEI 可原生显示**；
3 个是自定义类型配方（氪金萝莉、小萝莉升级、材料叠加/拆分），由 JEI 插件展示。

| 产物 | 图案 | 材料 |
| --- | --- | --- |
| 普通萝莉 | `ABC` / ` # ` / ` # ` | 铁斧 + 铁镐 + 铁锹 + 2 铁锭 |
| 蓝屏炸弹 | 3×3 满 | 9 个 TNT |
| 蹦溃炸弹 | X 形 | 5 个 TNT |
| 未响应炸弹 | 中空 3×3 | 8 个 TNT |
| 萝莉祭坛 | 无序 | 煤 + 铁锭 + 金锭 + 红石 + 青金石 + 钻石 + 绿宝石 + 石英 + 荧石粉 |
| 密码工作台 | `III` / `I#I` / `III` | 8 铁锭 + 工作台 |
| 萝莉网络卡片 | `XXX` / `XYX` / `XXX` | 8 皮革 + 钻石 |
| 各材料附加物 | 见 JEI | 对应材料 + 对应压缩块 |

### 3.5 按键绑定

| 按键 | 功能 |
| --- | --- |
| `配置氪金萝莉` | 打开物品配置界面（可逐项开关各项能力） |
| `附魔氪金萝莉` | 打开附魔界面 |
| `药水氪金萝莉` | 打开药水界面 |
| `跨世界氪金萝莉` | 打开跨世界传送界面 |
| `打开氪金萝莉储藏室` | 打开储藏室；按住 `Shift` 则全部倒出 |
| `打开氪金萝莉黑名单` | 打开黑名单界面 |

### 3.6 命令

| 命令 | 权限 | 说明 |
| --- | --- | --- |
| `/loli <flag> [value]` | OP | 读取/修改配置项 |
| `/loli reload` | OP | 重新加载配置文件 |
| `/loli listFlag` / `/loli listValue` | OP | 列出配置项与当前值 |
| `/loliattack <玩家> <攻击特效>` | 权限等级 4 | 对玩家施加蓝屏/蹦溃/未响应特效，需 `loliEnableBuffAttackTNT` 为 true |

---

## 四、1.12.2 → 1.20.1 移植说明

1.12.2 版本的 LoliPickaxe 依赖 **Coremod（IFMLLoadingPlugin）+ ASM 字节码改写** 原版类来实现核心玩法；
该机制在 1.20.1 的 Forge 中已完全移除。移植时做了如下等价替换：

| 1.12.2 实现 | 1.20.1 移植实现 |
| --- | --- |
| `LoliPickaxeCore` + `LoliPickaxeTransformer`（ASM 改写原版类） | Mixin（`com.anotherstar.core.mixin`） |
| 向 `EntityLivingBase` 注入 `loliDead` / `loliCool` / `loliDeathTime` 字段 | `LivingEntityMixin` 注入字段 + `ILoliDataHolder` 接口 |
| 向 `EntityPlayer` 注入 `hodeLoli` 字段 | `PlayerMixin` + `ILoliPlayerData` 接口 |
| 重写 `getHealth` / `getMaxHealth` | `LivingEntityMixin` 在 `HEAD` 处 `@Inject`，转发到 `EventUtil` |
| 替换 `ForgeHooks.onLivingDeath` | `EventUtil#onLivingDeath` 监听 `LivingDeathEvent` |
| 替换 `InventoryPlayer#dropAllItems` / `clear` | `InventoryMixin` 拦截 `Inventory#dropAll` / `clearContent` |
| 替换 `NetHandlerPlayServer#disconnect` | `ServerGamePacketListenerImplMixin` 拦截 `disconnect` |
| 替换 `Entity#rayTrace`（挖掘距离） | 使用 Forge 的 `ForgeMod.BLOCK_REACH` 属性 |
| `RenderManager` 替换展示框渲染器 | 已移除该 ASM 替换，改为独立的展示框渲染器（默认不注册） |
| `IGuiHandler` GUI 系统 | `MenuType` + `AbstractContainerMenu` + `AbstractContainerScreen` / `Screen` |
| `SimpleNetworkWrapper` + `IMessage` | Forge `SimpleChannel`（`NetworkHandler`） |
| `Configuration`（Forge 1.12 配置） | `ForgeConfigSpec`（动态构建，见 `ConfigLoader`） |
| `mcmod.info` / `*.lang` | `META-INF/mods.toml` / `lang/en_us.json`、`lang/zh_cn.json` |
| IForgeRegistry 指令式注册 | `DeferredRegister` |
| `assets/lolipickaxe/recipes/*.json`（旧格式） | `data/lolipickaxe/recipes/*.json`（1.13+ 数据驱动格式） |

### 已知的行为差异 / 降级

- **外部模组集成已移除**：IC2（EU 充能）、Baubles（饰品栏）、CoE/RedstoneFlux（RF 能量）、
  Touhou Little Maid（车万女仆模型）相关代码已全部删除，因为这些依赖没有对应的 1.20.1 版本或不适合直接移植。
- **OBJ 模型**：`ModelNevermore` 原本是 OBJ 模型，1.20.1 的实体模型体系改为数据驱动，
  现降级为等效的方块盒模型（`client/util/obj` 的解析器仍保留）。
- **蓝屏/崩溃/未响应打击**：原实现会释放并执行内嵌的 `BlueScreen.exe`，移植版不再执行外部程序，
  改为进程级终止（`Runtime.halt`），"未响应" 仅记录日志。
- **跨世界传送黑名单**：配置项仍为数字维度 id，1.20.1 维度标识为 `ResourceKey`，仅原版三个维度可命中。
- **萝莉镐展示框渲染**：不再替换原版 `ItemFrame` 渲染器，配置项 `loliCardRenderFrame` 不再生效。
- **无刷怪蛋**：1.12.2 的 `registerEgg` 未保留，萝莉实体需通过其他方式生成。

---

## 五、JEI 联动

JEI 是**可选**依赖（`compileOnly`，不打包进本模组）。未安装 JEI 时模组照常运行。

### 5.1 为什么自定义配方需要专门的插件

本模组的三个核心配方继承自原版 `CustomRecipe`，配方 JSON 只有一行：

```json
{ "type": "lolipickaxe:loli_pickaxe" }
```

**没有 `ingredients` 与 `result` 字段** —— 匹配与合成逻辑完全写在 Java 的
`matches()` / `assemble()` 中。JEI 的显示机制依赖配方提供的结构化「原料/产物」数据，
而 `CustomRecipe` 不暴露这类数据，因此 **JEI 无法自动识别并展示这些配方**。

此外三个配方都依赖 `damage`（等级）与 NBT：

| 配方 | 真实判定条件 |
| --- | --- |
| `LoliPickaxeRecipe` | 小萝莉**所有属性满级** + 生物灵魂满级 |
| `SmallLoliPickaxeRecipe` | 镐子某属性等级 == **材料等级 - 1** |
| `SuperpositionRecipe` | 按等级 9 合 1 / 1 拆 9 |

JEI 默认按物品展示、不区分 NBT，必须由插件显式构造带等级的 `ItemStack`。

> 另外 20 个标准工作台配方是普通的 `minecraft:crafting_shaped` / `crafting_shapeless`，
> **JEI 原生即可显示**，无需插件介入。

### 5.2 提供的类别

`com.anotherstar.compat.jei` 包提供 **5 个** JEI 类别：

| 类别 | 内容 |
| --- | --- |
| 氪金萝莉合成 | 满级小萝莉 + 满级生物灵魂 → 氪金萝莉 |
| 小萝莉属性升级 | 小萝莉 + 高 1 级材料 → 该属性 +1 |
| 材料叠加 | 9 个 N 级材料 → 1 个 N+1 级材料 |
| 材料拆分 | 1 个 N+1 级材料 → 9 个 N 级材料 |
| 生物掉落 | 击杀生物的概率掉落（卡片 / 卡片册 / 唱片 / 生物灵魂） |

并为以下物品提供 **信息页**（鼠标悬停显示获取途径与当前配置下的实际概率）：
氪金萝莉、普通萝莉、生物灵魂、萝莉卡片、卡片册、唱片、三色炸弹。

### 5.3 设计要点

1. **只读展示层**：插件不参与任何合成判定，三个 `CustomRecipe` 子类**一行未改**。
2. **展示与真实配方严格一致**：所有展示堆叠的等级都对照 `matches()` 构造，
   避免玩家"照着 JEI 做却合不出来"。
3. **概率从配置实时读取**，不硬编码；管理员改配置并重启后 JEI 会显示新数值。
4. **生物灵魂的 7 个等级逐级登记信息页**，避免玩家点到非 0 级物品时看不到说明。
5. **无 JEI 时零影响**：JEI 依赖为 `compileOnly`，插件由 `@JeiPlugin` 被 JEI 主动发现。
6. **条目数量受控**：升级配方每种材料只展示起步/中档/满级 3 个代表档位。

### 5.4 依赖版本

JEI API 版本由 `gradle.properties` 的 `jei_version` 控制（当前 `15.20.0.106`，1.20.1 Forge）。

---

## 六、Mixin 兼容性（面向 ATM9 / GTL 等大型整合包）

移植版刻意把 Mixin 的侵入面压到最小，原则是：**能用 Forge 标准事件实现的，绝不使用 Mixin**。

### 6.1 当前全部 Mixin 与冲突面

| Mixin | 目标 | 注入方式 | 风险与处理 |
| --- | --- | --- | --- |
| `LivingEntityMixin` | `LivingEntity` | 新增字段 + `getHealth` / `getMaxHealth` 的 `HEAD` 可取消注入 | 只在这两个方法上各有一个注入点，且**仅在实体确实持有本人萝莉镐时才取消**，其余情况立刻返回原版逻辑 |
| `LivingEntityHealthAccessor` | `LivingEntity` | `@Accessor("DATA_HEALTH_ID")` | 只读取原版同步数据，零行为改变 |
| `PlayerMixin` | `Player` | 新增字段（无注入） | 不产生注入点冲突 |
| `EntityMixin` | `Entity` | `isInvisibleTo` 的 `HEAD` 注入 | `require = 0`：匹配失败只告警、不崩溃 |
| `InventoryMixin` | `Inventory` | `dropAll` / `clearContent` 的 `HEAD` 注入 | `require = 0`：同上 |
| `ServerGamePacketListenerImplMixin` | `ServerGamePacketListenerImpl` | `disconnect` 的 `HEAD` 注入 | `require = 0`：同上 |

### 6.2 为兼容性做的关键取舍

1. **不再注入 `LivingEntity#tick()`**
   原 1.12.2 版把每 tick 的维护逻辑插在 `onUpdate()` 末尾。`tick()` 是**全生态被注入最多的方法**，
   AE2、属性类/性能优化类模组都会注入它，甚至可能被 `@Overwrite`；一旦目标方法被覆盖，
   本模组的注入会应用失败并**直接导致游戏启动崩溃**。
   移植版改由 Forge 标准事件 `LivingEvent.LivingTickEvent` 驱动，注入点冲突数降为 0。

2. **不再替换 `Entity#rayTrace`，改用 `ForgeMod.BLOCK_REACH` 属性**
   避免与任何改动射线追踪的模组（如各类"距离显示/长臂"模组）抢占同一方法。

3. **不再替换原版 `RenderItemFrame` 渲染器**
   渲染器替换是渲染类模组冲突的高发区，移植版改为独立渲染器且默认不注册。

4. **只读路径零副作用**
   `getHealth()` / `getMaxHealth()` 会被 AI、HUD、GUI、整合包统计等**极高频**调用。
   原实现会在这些只读方法里扫描背包并丢弃物品，移植版改为：
   - 每 tick 只在 `LivingTickEvent` 中计算一次「是否受保护」并缓存；
   - 只读方法仅读缓存，不做背包扫描、不写属性；
   - 非玩家实体走快路径，避免热路径上的 `UUID → String` 分配
     （ATM9 这类整合包实体数量以千计，热路径分配会明显增加 GC 压力）。

5. **只在服务端丢弃物品**
   `player.drop()` 在客户端执行会产生"幽灵掉落物"，干扰客户端实体同步；现已限制为仅服务端执行。

6. **容器 `quickMoveStack` 增加越界保护**
   整理类模组（Inventory Profiles 等）可能以异常索引调用该方法，原版写法会抛
   `IndexOutOfBoundsException` 导致**服务端崩溃**。现已对 `ContainerLoliPickaxe`、
   `ContainerPasswordWorkbench` 增加边界判断。

### 6.3 与 AE2（Applied Energistics 2）的兼容性

- **不混入 AE2 的目标类**：本模组的 Mixin 只作用于 `LivingEntity` / `Player` / `Entity` / `Inventory` /
  `ServerGamePacketListenerImpl`，不触碰 AE2 的任何类，也不与其抢占 `Grid`、`IStorageGrid` 等逻辑。
- **新增成员命名隔离**：所有注入字段统一使用 `loli$` 前缀，接口仅 `ILoliDataHolder` / `ILoliPlayerData`，
  不会与其它模组的字段/接口重名（Mixin 合并接口时不会冲突）。
- **背包操作不越界**：对 `Inventory#dropAll` / `clearContent` 的拦截只在**该背包所属玩家持有本人萝莉镐**时生效，
  不会影响 AE2 便携终端、ME 存储总线等对其它背包的操作。
- **数值安全**：萝莉镐储藏室允许超大堆叠（配置 `loliPickaxeCancelStackLimit`），
  但该上限只作用于萝莉镐自己的 `AbstractContainerMenu`，AE2 的存储/自动化无法直接写入该容器，
  因此不会向 AE2 的物流系统泄漏异常堆叠数。
- **维度/重生处理**：`PlayerEvent.Clone` 会清空萝莉状态，因此 AE2 空间塔（Spatial IO）跨维度搬运实体、
  以及常规死亡重生，都不会让「已死亡」标记残留导致永久假死。

### 6.4 refmap（重映射表）说明 —— 维护时务必注意

Forge 1.20.1 **开发环境**使用 Mojang 官方名称（`getHealth`、`deathTime`），
而**生产环境**（PCL / 正式客户端）运行的是 **SRG 名称**（`m_21223_`、`f_20918_`）。
Mixin 的 `@Inject(method = "getHealth")` 是**字符串**，`reobfJar` 无法改写它，
因此必须依靠 **refmap** 把名称翻译成 SRG；缺少 refmap 时会在启动阶段直接崩溃：

```
InvalidMixinException: ... was not located in the target class ... No refMap loaded.
```

本项目的 refmap 由 `src/main/resources/lolipickaxe.refmap.json` **随源码维护**（不用插件自动生成，
原因见 `build.gradle` 中的注释：MixinGradle 0.7.38 在 Gradle 8.1.1 下生成的 refmap 会被清理掉、进不了 jar）。

> **新增 / 改名任何 Mixin 注入目标时，必须同步更新该文件**，否则生产环境会崩溃。
> 格式为 `"mixin 类全限定名(斜杠分隔)": {"Mojang 成员名": "L所有者;SRG名(参数描述符)返回值"}`，
> 其中 `mappings` 与 `data.searge` 两段内容相同。
> 可先用 `.\gradlew compileJava --rerun-tasks --info` 观察注解处理器输出的
> `Note: Writing refmap to ...` 内容，再据此更新（开发环境不受影响，所以只会在生产环境暴露）。

不需要 refmap 的场景：`@Mixin(Xxx.class)` 的**类**名、`@Inject` 的 `at` 位置（如 `HEAD`）、
`@Unique` 字段、以及 `implements` 的接口都不会被重映射。
本项目已**刻意移除所有 `@Shadow` 字段**：注解处理器不会为影子字段生成 refmap 映射，
生产环境必然失败，因此改为「按需覆盖」设计（详见 `LivingEntityMixin` 的类注释）。

### 6.5 若仍然出现冲突

1. 检查日志中是否有 `Mixin apply failed` / `Mixin config ... failed`：
   - 若失败项来自 `require = 0` 的注入（`isInvisibleTo` / `dropAll` / `clearContent` / `disconnect`），
     游戏仍可正常启动，只是对应功能被跳过，日志中会有 `WARN`。
2. 如定位到某个模组与本模组在同一方法上冲突，可在 `src/main/resources/lolipickaxe.mixins.json`
   中调整 `priority`（数值越大越晚应用），把关键注入让给冲突模组。
3. 排查时建议先只加入本模组 + 冲突模组复现，再逐步加入其它模组定位。

---

## 七、配置

配置文件为 `config/lolipickaxe-common.toml`，也可用 `/loli` 命令在线修改。
部分选项可通过游戏内「配置氪金萝莉」界面**逐件物品**覆盖（覆盖值存在物品 NBT 的 `LoliConfig` 节）。

以下为主要选项及**出厂默认值**：

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `loliPickaxeMaxRange` | 5 | 最大采掘范围 |
| `loliPickaxeMandatoryDrop` | false | 强制掉落方块（无掉落时补掉方块本身） |
| `loliPickaxeSlotStackLimit` | 2000000000 | 储藏室单格堆叠上限 |
| `loliPickaxeCancelStackLimit` | true | 储藏室取消物品堆叠限制 |
| `loliPickaxeAutoAccept` | true | 自动收纳进储藏室（**会让掉落物不进世界**） |
| `loliPickaxeThorns` | true | 反伤 |
| `loliPickaxeKillRangeEntity` | true | 潜行右键杀死周围实体 |
| `loliPickaxeCompulsoryRemove` | true | 强制清除生物 |
| `loliPickaxeKillFacing` | true | 左键范围攻击 |
| `loliPickaxeKillFacingRange` | 50 | 范围攻击范围 |
| `loliPickaxeInfiniteBattery` | true | 超级电池 |
| `loliEnableBuffAttackTNT` | **true** | 启用特效攻击炸弹（本版改为默认开启） |
| `loliCardDropProbability` | 0.1 | 萝莉卡片掉落概率 |
| `loliCardAlbumDropProbability` | 0.01 | 萝莉卡片册掉落概率 |
| `loliRecordDropProbability` | 0.001 | 萝莉唱片掉落概率（仅苦力怕） |
| `entitySoulDropProbability` | 0.01 | 生物灵魂掉落概率 |
| `loliPickaxeGuiChangeList` | （见文件） | 允许在游戏内界面逐件修改的配置项白名单 |

> **注意**：若你的存档里 `config/lolipickaxe-common.toml` **已经存在**，
> Forge **不会**用新的默认值覆盖它。例如要让炸弹生效，需手动把
> `loliEnableBuffAttackTNT` 改为 `true`，或删除该文件让其重新生成。

---

## 八、目录结构

```
src/main/java/com/anotherstar/
├── client/                    客户端专用代码
│   ├── creative/              创造模式物品栏
│   ├── event/                 客户端事件
│   ├── gui/                   界面（Screen / AbstractContainerScreen）
│   ├── key/                   按键绑定
│   ├── model/                 实体模型
│   ├── render/                实体/物品渲染器
│   └── util/                  客户端工具（卡片贴图、OBJ 解析）
├── common/                    双端通用代码
│   ├── block/                 方块
│   ├── command/               命令
│   ├── config/                配置系统（注解驱动 + ForgeConfigSpec）
│   ├── enchantment/           附魔
│   ├── entity/                实体与 AI
│   ├── event/                 玩法事件
│   ├── gui/                   容器 / 菜单 / 物品栏
│   ├── item/                  物品
│   ├── recipe/                配方
│   └── registry/              自定义注册表
├── compat/jei/                JEI 插件（可选依赖，未装 JEI 时不加载）
├── core/
│   ├── mixin/                 Mixin（替代原 ASM 改写）
│   └── util/EventUtil.java    Mixin 与事件的核心处理逻辑
├── network/                   网络包
└── util/                      通用工具
```

---

## 九、文档

| 文档 | 说明 |
| --- | --- |
| [MC百科页面.md](MC百科页面.md) | 面向 MC 百科的词条文案（玩家向介绍） |
| [适配执行报告.md](适配执行报告.md) | 1.12.2 → 1.20.1 移植的执行情况与验证记录 |
| [整合包适配任务规划.md](整合包适配任务规划.md) | 面向大型整合包的兼容性适配规划 |
| [崩溃修复任务计划.md](崩溃修复任务计划.md) | 启动期注册表冻结崩溃的排查与修复记录 |
| [贴图与地址修正任务计划.md](贴图与地址修正任务计划.md) | 贴图显示异常与开源地址修正记录 |
| [JEI联动与版本号任务计划.md](JEI联动与版本号任务计划.md) | JEI 联动实现与版本号调整记录 |
| [配方补齐与JEI联动任务计划.md](配方补齐与JEI联动任务计划.md) | 缺失配方补齐与 JEI 联动记录 |
| [JEI布局与配置开关修复任务计划.md](JEI布局与配置开关修复任务计划.md) | JEI 布局重叠与配置同步问题的修复记录 |

> 上述「任务计划」类文档同时记录了**问题根因**与**验证证据**，
> 便于后续维护时追溯每一项改动的理由。

---

## 十、更新日志

### 1.0（当前版本）

1.20.1 Forge 首个发布版本。相对 1.12.2 原版的移植与修复要点：

**移植**

- Coremod + ASM 改写 → Mixin（注入面压缩至 6 个目标，详见第六节）
- 配置系统 → `ForgeConfigSpec`（注解驱动）
- GUI 系统 → `MenuType` + `AbstractContainerMenu`
- 网络层 → Forge `SimpleChannel`
- 配方 → 1.13+ 数据驱动格式（`data/lolipickaxe/recipes/`）

**修复**

- 修复启动时 `Registry is already frozen` 崩溃（注册对象改为惰性构造）
- 修复 `small_loli_pickaxe` 缺少模型导致的紫黑贴图
- 修复 `lolipickaxe:end` 模型属性从未注册，导致生物灵魂终极形态贴图不显示
- **补齐 20 个缺失的工作台配方**（1.12.2 → 1.20.1 迁移时整体遗漏，
  包括三色炸弹、普通萝莉、萝莉祭坛、密码工作台及全部材料附加物）
- 修复 JEI 自定义类别**槽位坐标重叠**导致的「合成表错乱摆放」
- 修复物品配置开关**服务端写入后未同步回客户端**，
  导致点击 true/false 后重开界面又变回旧值
- 修复「强制掉落」判定过严（列表非空但元素为空时不补掉落）与丢失方块状态

**新增**

- JEI 联动：5 个配方类别 + 7 项物品信息页（含概率掉落展示）
- 模组信息（`mods.toml`）与进服提示指向本移植仓库

**默认值调整**

- `loliEnableBuffAttackTNT` 默认值 `false` → **`true`**

---

## 十一、致谢

- 感谢 **Is_GK** 创作并开源原始 LoliPickaxe（1.12.2）。
- 感谢 Forge / Minecraft ForgeGradle / Mixin 社区提供的工具链。
