# 图标系统

GeyserMenu 支持三种类型的图标：Java 版物品 ID、基岩版材质路径和 URL 图标。

## 基础用法

### 1. Java 版物品 ID

使用 Java 版的物品 ID，插件会自动转换为对应的基岩版材质路径：

```yaml
items:
  - text: "传送菜单"
    icon: "compass"
    icon_type: "java"      # 使用 Java 版物品 ID
```

### 2. 基岩版材质路径

直接使用基岩版的材质路径：

```yaml
items:
  - text: "商店菜单"
    icon: "textures/items/diamond"
    icon_type: "bedrock"   # 使用基岩版材质路径
```

### 3. URL 图标

从网络 URL 加载图标：

```yaml
items:
  - text: "自定义按钮"
    icon: "https://example.com/icon.png"
    icon_type: "url"       # 使用 URL 图标
```

:::warning 注意
URL 图标需要在 config.yml 中启用 `icons.allow_url: true`，并且默认只允许 HTTPS 链接。
:::

## 使用资源包图标

你可以通过基岩版资源包添加自定义图标：

1. 创建资源包目录结构：
```
my_resource_pack/
├── manifest.json
├── pack_icon.png
└── textures/
    └── gui/
        └── icons/
            ├── my_icon1.png
            └── my_icon2.png
```

2. 在菜单中使用自定义图标：
```yaml
items:
  - text: "自定义按钮"
    icon: "textures/gui/icons/my_icon1"
    icon_type: "bedrock"
```

3. 将资源包应用到基岩版客户端：
   - 在基岩版客户端导入资源包
   - 或通过服务器自动分发资源包

## 图标映射配置

在 config.yml 中配置 Java 版到基岩版的材质映射：

```yaml
icons:
  # 默认图标
  default: "textures/items/paper"
  
  # 是否允许URL图标
  allow_url: true
  
  # URL图标设置
  url:
    https-only: true
    max-length: 256
    allowed-domains: []
  
  # 图标类型映射
  mappings:
    grass_block: "textures/blocks/grass_side"
    diamond: "textures/items/diamond"
    compass: "textures/items/compass_item"
```

## 最佳实践

1. 使用资源包时：
   - 图片尺寸建议为 32x32 或 64x64
   - 使用 PNG 格式，支持透明度
   - 文件名使用小写字母和下划线
   - 路径使用 `textures/gui/icons/` 前缀

2. 选择图标类型：
   - 如果使用 Java 版物品，选择 `icon_type: "java"`
   - 如果使用自定义图标，选择 `icon_type: "bedrock"`
   - 如果使用网络图标，选择 `icon_type: "url"`

3. URL 图标注意事项：
   - 确保使用 HTTPS 协议
   - 图片大小不宜过大
   - 考虑使用 CDN 加速加载

:::tip 提示
- 自定义图标必须通过基岩版资源包加载
- 资源包可以在服务器或客户端加载
- 图标路径区分大小写
- 可以在 config.yml 中添加自定义映射
:::

:::warning 注意
- 如果图标路径无效，将使用默认图标
- 资源包需要符合基岩版的格式要求
- 建议测试所有图标是否正常显示
- URL 图标需要网络连接才能显示
::: 
