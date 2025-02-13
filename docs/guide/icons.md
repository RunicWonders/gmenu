# 图标系统

GeyserMenu 支持两种类型的图标：Java 版物品 ID 和基岩版材质路径。

## 基础用法

### 1. Java 版物品 ID

使用 Java 版的物品 ID，插件会自动转换为对应的基岭版材质路径：

```yaml
items:
  - text: "传送菜单"
    icon: "compass"
    icon_type: "java"      # 使用 Java 版物品 ID
```

### 2. 基岩版材质路径

直接使用基岭版的材质路径：

```yaml
items:
  - text: "商店菜单"
    icon: "textures/items/diamond"
    icon_type: "bedrock"   # 使用基岩版材质路径
```

## 使用资源包图标

你可以通过基岭版资源包添加自定义图标：

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

3. 将资源包应用到基岭版客户端：
   - 在基岭版客户端导入资源包
   - 或通过服务器自动分发资源包

## 图标映射

在 config.yml 中配置 Java 版到基岭版的材质映射：

```yaml
icons:
  # 默认图标
  default: "textures/items/paper"
  
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

3. 资源包注意事项：
   - 确保资源包版本与客户端兼容
   - 图标路径必须完整且正确
   - 建议使用服务器资源包自动分发

::: tip 提示
- 自定义图标必须通过基岭版资源包加载
- 资源包可以在服务器或客户端加载
- 图标路径区分大小写
:::

::: warning 注意
- 如果图标路径无效，将使用默认图标
- 资源包需要符合基岭版的格式要求
- 建议测试所有图标是否正常显示
::: 