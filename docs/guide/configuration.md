# 配置详解

## 主配置文件

`config.yml` 包含插件的核心设置：

### 性能设置

```yaml
performance:
  # 命令执行延迟（毫秒）
  command-delay: 0
  
  # PAPI变量缓存
  cache-placeholders: false
  cache-refresh: 30
  max-cache-size: 1000
```

### 统计设置

```yaml
statistics:
  # 是否启用 BStats 统计
  enable-bstats: true
  
  # 是否收集自定义统计数据
  collect-custom-data: true
```

::: tip 提示
关于统计功能的详细说明，请参阅 [统计功能](./statistics.md) 文档。
:::

### 安全设置

```yaml
security:
  # 禁止执行的命令
  blocked-commands:
    - "op"
    - "deop"
  
  # 特殊字符检查
  allow-special-chars: false
  
  # 文件路径安全检查
  check-file-path: true
```

### 图标设置

GeyserMenu 支持两种类型的图标：

### 1. Java 版物品 ID
使用 Java 版的物品 ID，会自动转换为对应的基岩版材质路径：

```yaml
items:
  - text: "传送菜单"
    icon: "compass"      # Java版物品ID
    icon_type: "java"    # 指定使用Java版图标
```

### 2. 基岩版材质路径
直接使用基岩版的材质路径：

```yaml
items:
  - text: "商店菜单"
    icon: "textures/items/diamond"    # 基岩版材质路径
    icon_type: "bedrock"             # 指定使用基岭版图标
```

::: warning 重要提示
从版本 1.1.0 开始，必须通过 `icon_type` 属性明确指定图标类型（"java" 或 "bedrock"）。这是为了避免图标显示错误和提高性能。
:::

### 图标映射配置

在 config.yml 中配置 Java 版到基岭版的材质映射：

```yaml
icons:
  # 默认图标
  default: "textures/items/paper"
  
  # 图标类型映射 (Java版 -> 基岭版)
  mappings:
    # 方块
    grass_block: "textures/blocks/grass_side"
    stone: "textures/blocks/stone"
    dirt: "textures/blocks/dirt"
    
    # 物品
    diamond: "textures/items/diamond"
    compass: "textures/items/compass_item"
    book: "textures/items/book_normal"
```

### 使用建议

1. 如果你熟悉 Java 版物品 ID，使用 `icon_type: "java"`
2. 如果你需要使用特定的基岭版材质，使用 `icon_type: "bedrock"`
3. 如果没有指定 `icon_type`，默认会尝试作为 Java 版物品 ID 处理

::: tip 提示
- Java 版物品 ID 不需要包含 `minecraft:` 前缀
- 基岭版材质路径必须是完整的材质路径
- 可以在配置文件中添加新的材质映射
- 图标类型必须通过 icon_type 指定 ("java" 或 "bedrock")
:::

::: warning 注意
如果 Java 版物品 ID 没有对应的映射，将使用默认图标
:::

## 消息配置

`messages.yml` 用于配置插件的所有文本消息：

```yaml
prefix: "§6[GeyserMenu] §f"  # 消息前缀

reload:
  success: "§a配置重载成功!"  # 重载成功提示
  start: "§e正在重载插件配置..."  # 开始重载提示

error:
  no-permission: "§c你没有权限执行此命令!"  # 权限不足提示
  # ... 其他错误消息
```

## 菜单配置

菜单文件结构说明：

### 基础结构

```yaml
menu:
  # 菜单标题
  title: "主菜单"
  
  # 副标题（可选）
  subtitle: "选择一个选项"
  
  # 主要内容（可选）
  content: "这是菜单内容"
  
  # 页脚（可选）
  footer: "在线人数: %server_online%"
  
  # 按钮列表
  items:
    - text: "传送菜单"
      description: "打开传送菜单"
      icon: "compass"
      icon_type: "java"      # 必须指定图标类型
      submenu: "teleport.yml"
    
    - text: "商店菜单"
      description: "打开商店菜单"
      icon: "textures/items/diamond"
      icon_type: "bedrock"   # 必须指定图标类型
      submenu: "shop.yml"
    
    - text: "返回出生点"
      description: "点击传送到出生点"
      icon: "nether_star"
      icon_type: "java"    # 添加图标类型
      command: "spawn"
```

::: tip 提示
- 所有文本支持颜色代码 (使用 & 符号)
- 支持 PlaceholderAPI 变量
- 图标类型必须通过 icon_type 指定 ("java" 或 "bedrock")
:::

### 菜单类型

GeyserMenu 支持多种菜单类型，通过不同的配置实现：

#### 1. 子菜单类型

通过 `submenu` 属性打开另一个菜单文件：

```yaml
items:
  - text: "传送菜单"
    description: "打开传送菜单"
    icon: "compass"
    icon_type: "java"
    submenu: "teleport.yml"  # 点击后打开teleport.yml菜单
```

#### 2. 命令执行类型

通过 `command` 属性执行指定命令：

```yaml
items:
  - text: "返回出生点"
    description: "点击传送到出生点"
    icon: "nether_star"
    icon_type: "java"
    command: "spawn"  # 点击后执行spawn命令
```

#### 3. 命令执行方式

通过 `execute_as` 属性指定命令的执行方式：

```yaml
items:
  - text: "获取钻石"
    description: "获得一个钻石"
    icon: "diamond"
    icon_type: "java"
    command: "give {player} diamond 1"
    execute_as: "console"  # 以控制台身份执行命令
```

可用的执行方式：
- `player`: 以玩家身份执行命令（默认）
- `console`: 以控制台身份执行命令
- `op`: 临时给予玩家OP权限执行命令

::: warning 安全提示
使用 `console` 或 `op` 执行方式时要特别小心，确保命令不会被滥用。建议在 config.yml 中配置 `blocked-commands` 列表，禁止执行危险命令。
:::

## 配置保存

配置文件的保存和重载机制：

1. 首次启动时会生成所有默认配置文件
2. 之后修改配置文件不会被覆盖
3. 使用 `/gmenu reload` 重载时会保留修改

::: warning 注意
不要在服务器运行时直接删除配置文件，这可能导致插件无法正常工作。
::: 

## 目录说明

### 菜单目录

`menus` 目录用于存放菜单配置文件：
- 使用 YAML 格式
- 文件名即为菜单名
- 支持子目录组织菜单