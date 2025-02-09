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

支持三种类型的图标：

1. Minecraft 材质
```yaml
icon: "diamond"  # 不需要 minecraft: 前缀
```

2. 本地图片
```yaml
icon: "stone"  # 基础图标（必需）
icon_type: "path"
icon_path: "plugins/GeyserMenu/icons/custom.png"
```

3. 网络图片
```yaml
icon: "stone"  # 基础图标（必需）
icon_type: "url"
icon_url: "https://example.com/icon.png"
```

#### 基岩版材质路径

插件内置了常用的材质路径映射：

- 方块：
  - grass_block -> textures/blocks/grass_side
  - stone -> textures/blocks/stone
  - dirt -> textures/blocks/dirt
  - diamond_block -> textures/blocks/diamond_block
  - oak_log -> textures/blocks/log_oak
  - oak_planks -> textures/blocks/planks_oak

- 物品：
  - diamond -> textures/items/diamond
  - diamond_sword -> textures/items/diamond_sword
  - diamond_pickaxe -> textures/items/diamond_pickaxe
  - compass -> textures/items/compass_item
  - clock -> textures/items/clock_item
  - paper -> textures/items/paper
  - book -> textures/items/book_normal
  - writable_book -> textures/items/book_writable
  - written_book -> textures/items/book_written
  - arrow -> textures/items/arrow
  - chest -> textures/blocks/chest_front
  - wheat -> textures/items/wheat

其他物品默认使用 `textures/items/物品名` 路径。

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
  title: "菜单标题"
  subtitle: "副标题"
  content: "菜单内容"
  footer: "页脚文本"
```

### 按钮配置

```yaml
items:
  - text: "按钮文本"
    description: "按钮描述"
    icon: "diamond"  # 不需要 minecraft: 前缀
    command: "tp spawn"
    execute_as: "player"

  - text: "子菜单"
    icon: "book"
    submenu: "other_menu.yml"
```

### 图标设置

支持三种类型的图标：

1. Minecraft 材质
```yaml
icon: "diamond"  # 不需要 minecraft: 前缀
```

2. 本地图片
```yaml
icon: "stone"  # 基础图标（必需）
icon_type: "path"
icon_path: "plugins/GeyserMenu/icons/custom.png"
```

3. 网络图片
```yaml
icon: "stone"  # 基础图标（必需）
icon_type: "url"
icon_url: "https://example.com/icon.png"
```

## 配置保存

配置文件的保存和重载机制：

1. 首次启动时会生成所有默认配置文件
2. 之后修改配置文件不会被覆盖
3. 使用 `/gmenu reload` 重载时会保留修改

::: warning 注意
不要在服务器运行时直接删除配置文件，这可能导致插件无法正常工作。
::: 