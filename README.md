# GeyserMenu

一个轻量化且简单的基岩版自定义表单插件。

## 最新版本：v1.2.0

### 更新内容
- 完善菜单类型和权限配置文档
- 优化菜单配置说明
- 修正文档中的术语（"基岭版"统一修正为"基岩版"）

## 功能特性

### 基础功能
- 仅支持基岩版玩家使用
- 支持多菜单配置
- 支持无限层级子菜单
- 支持PlaceholderAPI变量
- 支持两种图标类型：
  - Java版物品ID (使用 `icon_type: "java"`)
  - 基岭版材质路径 (使用 `icon_type: "bedrock"`)
- 集成 BStats 统计功能（可配置）

### 命令系统
- 支持三种命令执行方式：
  - 玩家执行 (`execute_as: "player"`)
  - 控制台执行 (`execute_as: "console"`)
  - OP权限执行 (`execute_as: "op"`)

### 命令列表
- `/gmenu` - 打开默认菜单
- `/gmenu help` - 显示帮助信息
- `/gmenu reload` - 重载配置文件 (需要权限: geysermenu.reload)
- `/gmenu open <玩家名> <菜单名>` - 为指定玩家打开菜单 (需要权限: geysermenu.open)

### 权限节点
- `geysermenu.use` - 允许使用菜单命令 (默认: true)
- `geysermenu.reload` - 允许重载配置 (默认: op)
- `geysermenu.open` - 允许为其他玩家打开菜单 (默认: op)
- `geysermenu.*` - 允许使用所有功能 (默认: op)

### 菜单配置
- 支持通过config.yml启用/禁用菜单
- 支持自定义菜单标题和按钮
- 支持自定义按钮图标和命令
-  - 支持物品ID图标 (例如: `minecraft:diamond`)
-  - 支持URL图标 (使用 `icon_type: "url"`)
-  - 支持自定义路径图标 (使用 `icon_type: "path"` 和 `icon_path`)
- 支持菜单间的相互跳转
- 支持菜单副标题和简介
- 支持按钮描述文本
- 支持表单页脚文本
- 支持颜色代码 (使用§或&)
- 支持多行文本 (使用 |- 语法)

## 图标支持

GeyserMenu 支持两种类型的图标：

1. Java 版物品 ID
   ```yaml
   icon: "diamond_sword"
   icon_type: "java"
   ```

2. 基岩版材质路径
   ```yaml
   icon: "textures/items/diamond_sword"
   icon_type: "bedrock"
   ```

所有支持的 Java 版物品 ID 都会自动映射到对应的基岩版材质路径。你可以在 config.yml 中添加或修改这些映射。

## 统计功能

GeyserMenu 集成了 BStats 统计功能，用于收集匿名的插件使用数据，帮助开发者了解插件使用情况并改进插件质量。

### 配置选项
```yaml
settings:
  statistics:
    # 是否启用 BStats 统计
    enable-bstats: true
    # 是否收集自定义统计数据
    collect-custom-data: true
```

### 收集的数据
- 服务器版本和软件类型
- Java 版本信息
- 在线玩家数量
- 插件功能使用情况
- 菜单配置统计

### 隐私保护
- 所有数据都是匿名的
- 不收集服务器IP、玩家信息等敏感数据
- 可以随时在配置中禁用

详细信息请查看 [BSTATS.md](BSTATS.md) 文件。
