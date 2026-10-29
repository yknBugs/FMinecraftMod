# F Minecraft Mod

[English](https://github.com/yknBugs/FMinecraftMod) 中文

包含一些显示信息和管理服务器的工具。

## 注意

这是一个**服务端模组**，即使客户端不安装也能正常工作。

所有功能默认禁用，你需要手动通过命令或配置文件来启用本 Mod 的功能。

## 功能介绍

### 通知系统

追踪对管理员或你的朋友有用的事件。

- 实体数量超过阈值时发出警告
- 自动实体密度分析
- 显示玩家死亡坐标
- Boss、命名实体和复仇的死亡信息
- 玩家挂机检测
- 弹射物命中实体消息（包含距离和生命值）
- Boss 战斗信息
- 玩家跑图行为检测
- 玩家传送通知
- 玩家生物群系变化通知
- 睡觉时间提醒

所有种类的通知消息均可通过配置文件进行设置。

### 获取玩家信息

获取并分享玩家的相关信息：

- 坐标和距离
- 生命值、饥饿值、饱和度、经验值
- 物品栏内容（鼠标悬停可查看详细信息）
- 当前手持物品
- 跑图统计数据

### 音符盒歌曲播放器

在游戏中向玩家播放Note Block Studio（`.nbs`）的歌曲。

- 为特定玩家或所有在线玩家播放歌曲
- 控制播放速度和位置
- 在动作栏显示/隐藏播放进度
- 暂停、恢复和停止播放

### GPT 对话

在游戏中直接与大型语言模型聊天，支持 Markdown 语法高亮。

- 兼容 OpenAI API
- 对话历史管理
- 系统提示和温度参数控制
- 简易的代码块语法高亮

### 流程图系统

使用游戏内基于节点的流程图功能实现简单的自定义功能。无需重启服务器即可创建、编辑和执行。

- 事件触发器（玩家行为、实体事件、定时任务等）
- 算术、条件和数据操作节点
- 将流程图保存为 `.flow` 文件并可随时读取
- 允许服务器启动时自动加载流程图

### 规则系统

用事件、条件和动作创建自定义规则，规则会自动判定并执行命令、发送消息或调用流程图/规则。

- 使用 `/f rule` 创建、编辑、启用和删除规则
- 事件 -> 条件 -> 动作流水线，条件满足和不满足时可执行不同的动作
- 将规则保存为 `.rule` 文件并可随时读取
- 参数化规则组件，支持类型化参数和 Tab 补全
- 公式条件可用于组合多个条件
- 提供规则触发历史、执行日志以及手动触发规则的命令
- 允许服务器启动时自动加载规则

### 可视化编辑器

在客户端编辑流程图和规则（需要安装 Mod Menu，仅单人游戏内可编辑）。

- 规则编辑器：事件选择、条件/动作分区、公式输入框和自动生成的参数控件
- 流程图编辑器：节点拖拽、连线、撤销/重做
- 两个编辑器均支持客户端撤销/重做

## 使用方法

所有命令以 `/f` 开头，除非另有说明，否则需要管理员权限。

### 基础命令

- `/f` - 显示模组版本信息
- `/f reload` - 重新加载配置文件
- `/f options` - 查看或更改模组设置

### Say 命令

向所有玩家广播消息，支持动态占位符（无需管理员权限）：

```text
/f say <消息>
```

**样式码：**

使用 `&` 来作为 Minecraft 颜色代码：

- `&0-&f` - 颜色
- `&l` - 粗体, `&o` - 斜体, `&n` - 下划线, `&m` - 删除线, `&k` - 混淆
- `&r` - 重置格式

**自定义样式：**

- `${color:RRGGBB}`     - 应用十六进制颜色（例如：`${color:FF5733}`）
- `${link:url}`         - 创建可点击的 URL 链接
- `${copy:text}`        - 创建点击复制文本
- `${hint:text}`        - 添加悬停提示
- `${suggest:command}`  - 点击时建议命令
- `${markdown}`         - 将内容解析为 Markdown

`${hint:}` 的悬停文本可以继续嵌套占位符和样式（例如 `${hint:&c${player}}`、`${hint:血量 ${health}}`）。`${markdown}` 之后的全部内容会作为一个 Markdown 文档整体解析。

**玩家占位符：**

- `${player}`                   - 玩家显示名称
- `${health}`, `${hp}`          - 当前生命值
- `${maxhealth}`, `${maxhp}`    - 最大生命值
- `${level}`                    - 经验等级
- `${hunger}`                   - 饥饿值
- `${saturation}`               - 饱和度
- `${x}`, `${y}`, `${z}`        - 玩家坐标
- `${pitch}`, `${yaw}`          - 玩家视线朝向
- `${biome}`                    - 当前生物群系
- `${coord}`                    - 玩家的坐标
- `${mainhand}`, `${offhand}`   - 玩家的手持物品

**示例：**

- `/f say &a你好 &b${player}&a！你的生命值是 ${hp}/${maxhp}`
- `/f say ${color:FF5733}警告！${player} 在 ${coord}`
- `/f say 看看这个：${link:https://example.com}${hint:点击打开}点击这里！`
- `/f say ${suggest:/tp @s yourName}${hint:点击传送}点击传送到我的位置`
- `/f say ${hint:&c${player}} 把鼠标放上来看看是谁`

### GPT 命令

首先在配置中设置 GPT 服务器 URL 和 Access Token。

- `/f gpt new <消息>`           - 开始新对话
- `/f gpt reply <消息>`         - 在当前对话中回复
- `/f gpt regenerate`           - 重新生成上一条回复
- `/f gpt edit <索引> <文本>`   - 编辑之前的消息
- `/f gpt history [页码]`       - 查看对话历史记录

### 信息命令

获取其他玩家的信息：

- `/f get coord <玩家>`             - 获取玩家坐标
- `/f get distance <玩家>`          - 获取到玩家的距离
- `/f get health <玩家>`            - 获取玩家生命值
- `/f get status <玩家>`            - 获取综合状态
- `/f get inventory <玩家>`         - 查看玩家物品栏
- `/f get item <玩家>`              - 查看手持物品
- `/f get crowd [数量] [半径]`      - 查找实体最密集的区域

分享你自己的信息（无需管理员权限）：

- `/f share coord`              - 分享你的坐标
- `/f share distance`           - 分享到其他玩家的距离
- `/f share health`             - 分享你的生命值
- `/f share status`             - 分享综合状态
- `/f share inventory`          - 分享你的物品栏
- `/f share item`               - 分享手持物品

### 音符盒歌曲命令

将 `.nbs` 文件放入 `config/fminecraftmod/` 文件夹，然后：

- `/f song play <玩家> <歌曲>`              - 播放歌曲
- `/f song get <玩家>`                      - 检查播放状态
- `/f song cancel <玩家>`                   - 停止播放
- `/f song seek <玩家> <时间>`              - 跳转到特定时间（秒）
- `/f song speed <玩家> <倍数>`             - 设置播放速度（例如：1.5）

### 流程图命令

创建和管理自定义流程图：

- `/f flow create <名称> <事件类型> <第一个节点名称>`  - 创建新流程图
- `/f flow list`                                       - 列出所有流程图
- `/f flow enable <名称> [true|false]`                 - 启用/禁用流程图
- `/f flow rename <旧名称> <新名称>`                   - 重命名流程图
- `/f flow copy <源> <目标>`                           - 复制流程图
- `/f flow delete <名称>`                              - 删除流程图

编辑流程图：

- `/f flow edit <流程图> new <节点类型> <节点名称>`                    - 添加节点
- `/f flow edit <流程图> remove <节点名称>`                            - 删除节点
- `/f flow edit <流程图> rename <旧名称> <新名称>`                     - 重命名节点
- `/f flow edit <流程图> const <节点> <输入索引> <值>`                 - 设置常量输入
- `/f flow edit <流程图> reference <节点> <输入> <源> <输出>`          - 连接节点
- `/f flow edit <流程图> disconnect <节点> <输入>`                     - 断开输入连接
- `/f flow edit <流程图> next <节点> <分支> <目标>`                    - 设置下一个节点
- `/f flow edit <流程图> final <节点> <分支>`                          - 标记为最终节点
- `/f flow edit <流程图> undo`                                         - 撤销上一次编辑
- `/f flow edit <流程图> redo`                                         - 重做上一次撤销

保存和加载流程图：

- `/f flow save <名称>`               - 保存到文件（config/fminecraftmod/）
- `/f flow save *`                    - 保存所有流程图
- `/f flow load <文件名>`             - 从文件加载
- `/f flow load *`                    - 加载所有 .flow 文件

执行与调试：

- `/f flow history [页码]`              - 查看执行历史
- `/f flow log <索引>`                  - 查看详细执行日志
- `/f trigger <触发器名称> [参数]`      - 执行触发节点（无需管理员权限）

### 规则命令

创建和管理自定义规则：

- `/f rule create <名称> <事件>`               - 创建规则
- `/f rule copy <规则> <名称>`                 - 复制规则
- `/f rule list`                               - 列出所有规则
- `/f rule enable <名称> [true|false]`         - 启用/禁用或查询规则
- `/f rule rename <旧名称> <新名称>`           - 重命名规则
- `/f rule delete <名称>`                      - 删除规则

编辑规则：

- `/f rule edit <规则> event <事件>`                          - 修改事件类型
- `/f rule edit <规则> condition add <类型> ...`               - 添加条件
- `/f rule edit <规则> condition set <公式>`                   - 设置主条件公式
- `/f rule edit <规则> condition remove <条件>`                - 删除条件
- `/f rule edit <规则> condition rename <旧名称> <新名称>`     - 重命名条件
- `/f rule edit <规则> action add <类型> ...`                  - 添加条件满足时执行的动作
- `/f rule edit <规则> action remove <动作>`                   - 删除动作
- `/f rule edit <规则> action rename <旧名称> <新名称>`        - 重命名动作
- `/f rule edit <规则> punish add <类型> ...`                  - 添加条件不满足时执行的动作
- `/f rule edit <规则> punish remove <动作>`                   - 删除动作
- `/f rule edit <规则> punish rename <旧名称> <新名称>`        - 重命名动作

保存、加载与调试：

- `/f rule save <名称>`        - 保存规则到 `config/fminecraftmod/`
- `/f rule save *`             - 保存所有规则
- `/f rule load <文件名>`      - 加载规则文件，`*` 表示全部
- `/f rule view <名称>`        - 查看规则定义
- `/f rule history [页码]`     - 查看规则触发历史
- `/f rule log <索引>`         - 查看详细触发日志
- `/f rule test <名称>`        - 只判定条件，不执行动作
- `/f rule trigger <名称>`     - 触发规则并执行动作

### 配置示例

使用 `/f options` 启用功能：

- `/f options serverTranslation [true|false]`           - 服务端翻译
- `/f options entityDeathMessage [位置]`                - 实体死亡消息
- `/f options passiveDeathMessage [位置]`               - 被动实体死亡消息
- `/f options hostileDeathMessage [位置]`               - 敌对实体死亡消息
- `/f options playerDeathCoordLocation [位置]`          - 玩家死亡坐标
- `/f options gptUrl [url]`                             - GPT 服务器地址
- `/f options autoLoadRuleFiles [true|false]`            - 启动时自动加载 `.rule` 文件
- `/f options autoLoadFlowFiles [true|false]`            - 启动时自动加载 `.flow` 文件
- 等等

提示：合理使用 Tab 键自动补全命令来获得提示。

## 安装

### 玩家

1. 从 Releases 下载与你的模组加载器和 Minecraft 版本匹配的 `.jar` 文件。
2. 将其放入你的 Minecraft `mods` 文件夹。
3. 启动游戏

### 开发者

#### 步骤 1：克隆此仓库

```bash
git clone https://github.com/yknBugs/FMinecraftMod
cd FMinecraftMod
```

#### 步骤 2：构建项目

```bash
./gradlew build
```

如果遇到错误，请在 `gradle.properties` 中添加以下内容：

```text
org.gradle.java.home=<你的_JDK21_路径>
```

构建需要 JDK 21；1.20.1 的 jar 仍以 Java 17 为目标，1.21.1 的 jar 以 Java 21 为目标。

#### 步骤 3：安装

编译后的模组文件将位于 `./build/libs/` 中。将其复制到你的 Minecraft mods 文件夹并启动游戏。

## 配置

首次启动后，将在 `config/fminecraftmod/server.json` 创建配置文件。

你可以直接编辑此文件或在游戏中使用 `/f options` 命令。

你可以使用 `/f reload` 命令从文件重新加载配置。

**注意：**将 `.nbs` 歌曲文件、`.flow` 流程图文件和 `.rule` 规则文件放入 `config/fminecraftmod/` 文件夹。

## 支持的版本

- Minecraft 1.20.1 Fabric
- Minecraft 1.20.1 Forge（测试版）
- Minecraft 1.21.1 Fabric（测试版）
- Minecraft 1.21.1 NeoForge（测试版）

## 已知问题

- Markdown 语法高亮在复杂代码块中存在一些限制

## 更新日志

### 版本 0.4（当前版本）

**主要功能：**

- 游戏内自定义规则系统
  - 使用 `/f rule` 创建、编辑和管理规则
  - 事件 -> 条件 -> 动作流水线，条件满足和不满足时执行不同的动作
  - 将规则保存到 `.rule` 文件并读取，支持服务器启动时自动加载
  - 参数化规则组件，支持类型化参数、参数提示和 Tab 补全
  - 公式条件，可组合多个条件
  - 计算和变量操作类组件，可将临时运算结果保存到变量
- 可视化编辑器（客户端安装 Mod Menu，仅单人游戏）
  - 规则编辑器：事件、条件和动作编辑界面与自动生成的参数控件
  - 流程图编辑器：节点拖拽、连线、撤销/重做
- 流程图系统
  - 允许服务器启动时自动加载 `.flow` 文件
  - `/f flow load *` 和 `/f rule load *` 改为异步执行
  - 配置文件中缺失的字段会在加载时用默认值修复
- 文本占位符
  - `${hint:...}` 支持嵌套样式（例如 `${hint:&c${player}}`）
  - `${markdown}` 将后面的全部内容作为一个 Markdown 文档解析

**改进：**

- 使用注解重构配置系统，所有消息都可以配置显示位置和接收者
- 对于不是目标接收者的玩家，消息可以改为去掉部分信息的简化版本
- 从 Fabric 映射迁移到 Mojang 官方映射
- 为规则和流程图增加更多有效性检查

**Bug 修复：**

- 修复 TEAM 消息类型下广播消息错误落入 SELF 分支的问题
- 修复二元运算节点在比较前将输入转为小写、导致无法进入字符串分支的问题
- 修复获取方块节点坐标四舍五入、与原版行为不一致的问题
- 修复同一首 `.nbs` 歌曲播放给多个玩家时，调整播放速度会同步影响所有玩家的问题
- 修复生物群系变化检查中，其它模组添加未注册生物群系时抛出 NullPointerException 的问题
- 修复命令 Tab 补全时网络线程与主线程的竞争问题
- 修复从文件或网络读取数据时资源未正确关闭的问题
- 修复允许创建全空格流程图的问题
- 修复延迟执行的流程图后续添加变量被丢弃的问题
- 修复用户自身导致的流程图异常被记录为错误日志的问题
- 修复 `/f get distance` 视角朝向计算错误
- 修复服务器控制台执行 `/f say` 使用玩家占位符时抛出 NullPointerException 的问题
- 修复 say 命令彩色文本无法正常渲染的问题
- 修复 `AddVariableToScheduledFlowNode` 在变量表只读时抛出 `UnsupportedOperationException` 的问题

**兼容性：**

- 配置文件格式在 0.3 之后发生了变化。旧文件不再兼容，需要重新配置。
- 0.3 保存的 `.flow` 文件会触发版本兼容性警告，升级前请备份并测试。
- `.rule` 文件与 `.nbs`、`.flow` 文件一样放在 `config/fminecraftmod/` 目录下。

### 版本 0.3

**主要功能：**

- 扩展了流程图系统，新增多种节点类型：
  - RunCommandNode 用于执行 Minecraft 命令
  - UnaryArithmeticNode 用于单操作数运算
  - GetNbtValueNode、GetEntityDataNode 用于数据检索
  - GetWorldListNode、GatherEntityNode、GetBlockNode 用于世界查询
- 改善了 BinaryArithmeticNode 和 BroadcastMessageNode 节点
- 流程图中现在支持处理列表数据类型
- 向流程图计划任务中添加变量
- 玩家跑图和传送检测功能
- 玩家跑图信息统计
- 实体密度计算和最密集的区域检测
- 现在允许替换流程图中的事件节点
- 非管理员可调用的事件节点

**改进：**

- 跑图检查不再清空历史坐标记录
- 新增流程图的无限递归保护
- 敌对和被动实体的单独死亡消息配置
- 额外的配置选项
- 挂机和跑图信息指令

**Bug 修复：**

- 修复了 `/f get distance` 视角朝向计算错误
- 修复了在控制台中执行 `/f say` 时会抛出 NullPointerException
- 修复了 say 命令彩色文本无法正常渲染的问题

### 版本 0.2

**主要功能：**

- 游戏内基于节点的流程图系统
  - 使用 `/f flow` 命令创建、编辑和管理流程图
  - 将流程图保存到 `.flow` 文件
  - 基于事件的执行系统
- 为玩家播放 `.nbs` 文件
  - 播放控制（播放、暂停、停止、跳转）
  - 可调节播放速度
  - 在动作栏显示播放进度

### 版本 0.1

**初始功能：**

- 在游戏中与 AI 聊天
  - 兼容 OpenAI API
  - 对话历史和管理
  - 系统提示和温度参数控制
  - Markdown 语法高亮
- 玩家信息系统：
  - `/f get` 命令获取玩家数据（坐标、生命值、物品栏等）
  - `/f share` 命令分享信息
  - 距离和方向计算
- 实体监控：
  - 实体数量追踪和警告
  - 可配置阈值
- 战斗功能：
  - 弹射物命中消息（包含距离和生命值）
  - Boss 战斗通知
  - 怪物围攻警告
  - 显示生物死亡消息
- 挂机检测：
  - 自动检测挂机玩家
  - 可配置阈值
  - 播报挂机玩家
- 其他功能：
  - 生物群系变化通知
  - 玩家死亡坐标广播
  - 使用 `/f options` 修改配置文件
  - 可自定义消息显示的位置和接收者
