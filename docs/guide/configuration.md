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

```yaml
icons:
  # 自定义图标目录
  custom_path: "plugins/GeyserMenu/icons"
  
  # URL图标设置
  allow_url: true
  url:
    allowed-domains:
      - "i.imgur.com"
    https-only: true
    max-length: 256
```

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
    icon: "minecraft:diamond"
    command: "tp spawn"
    execute_as: "player"

  - text: "子菜单"
    icon: "book"
    submenu: "other_menu.yml"
```

### 图标类型

支持三种图标来源：

1. Minecraft 材质
```yaml
icon: "minecraft:diamond"
```

2. 本地图片
```yaml
icon: "stone"
icon_type: "path"
icon_path: "plugins/GeyserMenu/icons/custom.png"
```

3. 网络图片
```yaml
icon: "stone"
icon_type: "url"
icon_url: "https://example.com/icon.png"
``` 