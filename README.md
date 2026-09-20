# LoliPickaxe（Minecraft 1.20.1 Forge 移植版）

> 本项目是 [LoliPickaxe](https://github.com/IslenautsGK/LoliPickaxe)（Minecraft 1.12.2 Forge）到
> **Minecraft 1.20.1 + Forge 47.2.0** 的移植版本。

## 一、作者与来源（署名）

| 角色 | 署名 | 链接 |
| --- | --- | --- |
| 原作者（1.12.2 版本） | **Is_GK** | <https://github.com/IslenautsGK/LoliPickaxe> |
| 1.20.1 Forge 移植 | **ZHCOOL520** | <https://github.com/ZHCOOL520/LoliPickaxe-1.20.1AI> |

- 原始项目：<https://github.com/IslenautsGK/LoliPickaxe>（Minecraft 1.12.2，mod 版本 `1.2.16f`）
- 移植仓库：<https://github.com/ZHCOOL520/LoliPickaxe-1.20.1AI>（Minecraft 1.20.1 Forge，mod 版本 `1.2.16f`）
- mod id：`lolipickaxe`；包名根：`com.anotherstar`

### 许可

原始项目使用 **GNU General Public License v3.0**（见根目录 [LICENSE](LICENSE)）。
本移植版为原项目的衍生作品，**同样以 GPL-3.0 发布**，请遵守 GPL 的传染性条款（分发时必须提供对应源码）。

`META-INF/mods.toml` 中的 `authors` / `credits` 字段已同步写入上述署名信息。

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

## 三、1.12.2 → 1.20.1 移植说明

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

### 已知的行为差异 / 降级

- **外部模组集成已移除**：IC2（EU 充能）、Baubles（饰品栏）、CoE/RedstoneFlux（RF 能量）、
  Touhou Little Maid（车万女仆模型）相关代码已全部删除，因为这些依赖没有对应的 1.20.1 版本或不适合直接移植。
- **OBJ 模型**：`ModelNevermore` 原本是 OBJ 模型，1.20.1 的实体模型体系改为数据驱动，
  现降级为等效的方块盒模型（`client/util/obj` 的解析器仍保留）。
- **蓝屏/崩溃/未响应打击**：原实现会释放并执行内嵌的 `BlueScreen.exe`，移植版不再执行外部程序，
  改为进程级终止（`Runtime.halt`），"未响应" 仅记录日志。
- **跨世界传送黑名单**：配置项仍为数字维度 id，1.20.1 维度标识为 `ResourceKey`，仅原版三个维度可命中。
- **萝莉镐展示框渲染**：不再替换原版 `ItemFrame` 渲染器，配置项 `loliCardRenderFrame` 不再生效。

## 五、Mixin 兼容性（面向 ATM9 / GTL 等大型整合包）

移植版刻意把 Mixin 的侵入面压到最小，原则是：**能用 Forge 标准事件实现的，绝不使用 Mixin**。

### 5.1 当前全部 Mixin 与冲突面

| Mixin | 目标 | 注入方式 | 风险与处理 |
| --- | --- | --- | --- |
| `LivingEntityMixin` | `LivingEntity` | 新增字段 + `getHealth` / `getMaxHealth` 的 `HEAD` 可取消注入 | 只在这两个方法上各有一个注入点，且**仅在实体确实持有本人萝莉镐时才取消**，其余情况立刻返回原版逻辑 |
| `LivingEntityHealthAccessor` | `LivingEntity` | `@Accessor("DATA_HEALTH_ID")` | 只读取原版同步数据，零行为改变 |
| `PlayerMixin` | `Player` | 新增字段（无注入） | 不产生注入点冲突 |
| `EntityMixin` | `Entity` | `isInvisibleTo` 的 `HEAD` 注入 | `require = 0`：匹配失败只告警、不崩溃 |
| `InventoryMixin` | `Inventory` | `dropAll` / `clearContent` 的 `HEAD` 注入 | `require = 0`：同上 |
| `ServerGamePacketListenerImplMixin` | `ServerGamePacketListenerImpl` | `disconnect` 的 `HEAD` 注入 | `require = 0`：同上 |

### 5.2 为兼容性做的关键取舍

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

### 5.3 与 AE2（Applied Energistics 2）的兼容性

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

### 5.4 与 JEI（Just Enough Items）的兼容性

原始 1.12.2 项目虽然在 `build.gradle` 中声明了 JEI，但**代码中没有任何 JEI API 调用**（已确认无 `mezz.jei` 引用），
因此移植版不含 JEI 依赖，也就不存在版本/API 冲突。为保证 JEI 正常工作，移植版确保：

- 所有 `MenuType` 均在 `MenuLoader.MENUS`（`DeferredRegister`）中注册，并在客户端通过
  `MenuScreens.register(...)` 绑定界面 —— 这是 JEI 识别容器、提供"配方转移"的前提；
- 创造模式物品栏使用 Forge 标准的 `BuildCreativeModeTabContentsEvent` 填充，
  不使用任何私有/反射注入，JEI 的物品列表能够正确抓取；
- 物品子类型通过 NBT（`getDamageValue()` 写入 `Damage`）表达，`ItemStack` 序列化保持原版语义，
  JEI 的 `IngredientManager` 可正常索引；
- 不拦截 `isInvisibleTo` 以外的渲染路径，JEI 的配方预览渲染不受影响；
- 新增配方走标准的 `RecipeSerializer` / `data/<modid>/recipes/*.json`，
  自定义配方（`isSpecial() == true`）不进配方书，避免 JEI 报出无法解析的配方。

### 5.5 refmap（重映射表）说明 —— 维护时务必注意

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

### 5.6 若仍然出现冲突

1. 检查日志中是否有 `Mixin apply failed` / `Mixin config ... failed`：
   - 若失败项来自 `require = 0` 的注入（`isInvisibleTo` / `dropAll` / `clearContent` / `disconnect`），
     游戏仍可正常启动，只是对应功能被跳过，日志中会有 `WARN`。
2. 如定位到某个模组与本模组在同一方法上冲突，可在 `src/main/resources/lolipickaxe.mixins.json`
   中调整 `priority`（数值越大越晚应用），把关键注入让给冲突模组。
3. 排查时建议先只加入本模组 + 冲突模组复现，再逐步加入其它模组定位。

## 六、目录结构

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
├── core/
│   ├── mixin/                 Mixin（替代原 ASM 改写）
│   └── util/EventUtil.java    Mixin 与事件的核心处理逻辑
├── network/                   网络包
└── util/                      通用工具
```

## 五、文档

- [docs/API.md](docs/API.md)：公开 API 文档（接口说明、参数列表、返回值、调用示例）

## 六、致谢

- 感谢 **Is_GK** 创作并开源原始 LoliPickaxe（1.12.2）。
- 感谢 Forge / Minecraft ForgeGradle / Mixin 社区提供的工具链。
