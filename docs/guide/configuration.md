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

GeyserMenu 支持多种类型的图标：

#### 1. 基岩版材质

直接使用物品ID，不需要 minecraft: 前缀：

```yaml
items:
  - text: "传送菜单"
    icon: "compass"  # 使用指南针图标
  
  - text: "商店"
    icon: "diamond"  # 使用钻石图标
```

#### 2. 本地图片

使用 icons 目录下的自定义图片：

```yaml
items:
  - text: "自定义图标"
    icon_type: "path"
    icon_path: "custom.png"  # 相对于 plugins/GeyserMenu/icons 目录
```

::: warning 图片要求
- 支持格式：PNG、JPG
- 推荐尺寸：32x32 或 64x64 像素
- 最大尺寸：128x128 像素
- 建议使用正方形图片
- 图片大小不要超过 1MB
:::

#### 3. 网络图片

使用网络图片作为图标：

```yaml
items:
  - text: "网络图标"
    icon: "paper"  # 基础图标（必需）
    icon_type: "url"
    icon_path: "https://example.com/icon.png"
```

#### 4. 基岩版UI图标

使用基岩版内置的UI图标：

```yaml
items:
  - text: "UI图标"
    icon: "textures/ui/icon"  # 直接使用完整路径
```

#### 图标安全设置

在 config.yml 中配置图标相关安全选项：

```yaml
icons:
  # 是否允许使用网络图片
  allow_url: true
  
  # 默认图标
  default: "paper"
  
  # 网络图标设置
  url:
    # 允许的域名
    allowed-domains:
      - "i.imgur.com"
      - "cdn.example.com"
    
    # 是否只允许HTTPS
    https-only: true
    
    # URL最大长度
    max-length: 256
```

::: tip 提示
- 本地图片支持 PNG、JPG 格式
- 图片大小建议不超过 128x128
- 网络图片需要配置允许的域名
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
      submenu: "teleport.yml"
    
    - text: "商店菜单"
      description: "打开商店菜单"
      icon: "diamond"
      submenu: "shop.yml"
    
    - text: "返回出生点"
      description: "点击传送到出生点"
      icon: "nether_star"
      command: "spawn"
```

::: tip 提示
- 所有文本支持颜色代码 (使用 & 符号)
- 支持 PlaceholderAPI 变量
- 图标不需要 minecraft: 前缀
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

### 图标目录

`icons` 目录用于存放自定义图标：
- 支持 PNG、JPG 格式的图片
- 图片大小建议不超过 128x128
- 文件名不要包含特殊字符

### 菜单目录

`menus` 目录用于存放菜单配置文件：
- 使用 YAML 格式
- 文件名即为菜单名
- 支持子目录组织菜单 