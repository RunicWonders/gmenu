# GeyserMenu

一个轻量化且简单的基岩版自定义表单插件。

## 当前版本：v1.3.0-beta5

### 近期更新
- 支持 SimpleForm、ModalForm 和 CustomForm 三种表单类型
- 支持标签、输入框、下拉框、滑块和开关组件
- 修复自定义表单响应解析，支持下拉框文本和 `{player}` 占位符
- 增加菜单级权限、命令安全检查和 URL 图标域名限制
- 支持配置和消息文件自动迁移，重载时按配置清理运行时缓存
- 增加更新检查和进服更新提示
- 更新依赖到 Paper API 1.21.4+、bStats 3.2.1 和 org.json 20250107

## 环境要求

- Minecraft 服务器: Paper 1.21.4 或更高版本（仅支持 Paper，不支持 Spigot）
- Java: 21 或更高版本
- 前置插件: Floodgate

## 功能特性

### 基础功能
- 仅支持基岩版玩家使用
- 支持多菜单配置
- 支持无限层级子菜单
- 支持PlaceholderAPI变量
- 支持三种图标类型：
  - Java版物品 ID（使用 `icon_type: "java"`）
  - 基岩版材质路径（使用 `icon_type: "bedrock"`）
  - HTTPS URL 图标（使用 `icon_type: "url"`）
- 集成 BStats 统计功能（可配置）

### 命令系统
- 支持三种命令执行方式：
  - 玩家执行 (`execute_as: "player"`)
  - 控制台执行 (`execute_as: "console"`)
  - OP权限执行 (`execute_as: "op"`)

### 命令列表
- `/gmenu` - 打开默认菜单
- `/gmenu help` - 显示帮助信息
- `/gmenu reload` - 重载配置、消息和菜单（需要权限：`geysermenu.reload`）
- `/gmenu open <玩家名> <菜单名>` - 为指定玩家打开菜单（需要权限：`geysermenu.open`）

### 权限节点
- `geysermenu.use` - 使用默认菜单（默认：true）
- `geysermenu.reload` - 重载配置（默认：op）
- `geysermenu.open` - 为其他玩家打开菜单（默认：op）
- `geysermenu.admin` - 管理员权限，包含全部功能（默认：op）
- `geysermenu.menu.*` - 使用所有菜单（默认：op）
- `geysermenu.menu.<菜单键>` - 使用指定菜单，权限由 `config.yml` 中的菜单配置决定
- `geysermenu.*` - 使用所有功能（默认：op）

### 菜单配置
- 支持通过config.yml启用/禁用菜单
- 支持自定义菜单标题和按钮
- 支持自定义按钮图标和命令
  - 支持物品 ID 图标（使用 `icon_type: "java"`）
  - 支持基岩版材质路径（使用 `icon_type: "bedrock"`）
  - 支持 HTTPS URL 图标（使用 `icon_type: "url"`）
- 支持菜单间的相互跳转
- 支持菜单副标题和简介
- 支持按钮描述文本
- 支持表单页脚文本
- 支持颜色代码（使用 `§` 或 `&`）
- 支持多行文本（使用 `|-` 语法）

### 表单类型
- `simple`：多按钮导航菜单
- `modal`：确认/取消双按钮表单
- `custom`：包含 `label`、`input`、`dropdown`、`slider` 和 `toggle` 组件的自定义表单

完整表单配置示例请查看 [表单类型文档](docs/guide/form-types.md)。

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

## 安全设置

默认启用命令安全检查，会拦截 `op`、`deop`、`stop` 和 `reload` 等命令，并拒绝命令中的 `;`、`|`、`&` 和反引号。不要在不可信配置中关闭此功能。

URL 图标默认仅允许 HTTPS；可在 `icons.url.allowed-domains` 中限制允许的域名。

## 配置与文档

- 主配置：`src/main/resources/config.yml`（服务器运行后位于插件数据目录）
- 表单类型：[docs/guide/form-types.md](docs/guide/form-types.md)
- 统计说明：[BSTATS.md](BSTATS.md)
- 贡献指南：[CONTRIBUTING.md](CONTRIBUTING.md)

详细统计说明请查看 [BSTATS.md](BSTATS.md) 文件。

## 许可证

本项目使用 [MIT License](LICENSE)。
