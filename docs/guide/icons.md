# Icon System

GeyserMenu supports three types of icons: Java Edition item IDs, Bedrock Edition texture paths, and URL icons.

## Basic Usage

### 1. Java Edition Item ID

Use Java Edition item IDs, the plugin will automatically convert them to corresponding Bedrock Edition texture paths:

```yaml
items:
  - text: "Teleport Menu"
    icon: "compass"
    icon_type: "java"      # Use Java Edition item ID
```

### 2. Bedrock Edition Texture Path

Directly use Bedrock Edition texture paths:

```yaml
items:
  - text: "Shop Menu"
    icon: "textures/items/diamond"
    icon_type: "bedrock"   # Use Bedrock Edition texture path
```

### 3. URL Icon

Load icons from network URLs:

```yaml
items:
  - text: "Custom Button"
    icon: "https://example.com/icon.png"
    icon_type: "url"       # Use URL icon
```

:::warning Note
URL icons require `icons.allow_url: true` to be enabled in config.yml, and only HTTPS links are allowed by default.
:::

## Using Resource Pack Icons

You can add custom icons through Bedrock Edition resource packs:

1. Create resource pack directory structure:
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

2. Use custom icons in menus:
```yaml
items:
  - text: "Custom Button"
    icon: "textures/gui/icons/my_icon1"
    icon_type: "bedrock"
```

3. Apply the resource pack to Bedrock Edition clients:
   - Import the resource pack in Bedrock Edition client
   - Or distribute the resource pack automatically through the server

## Icon Mapping Configuration

Configure Java Edition to Bedrock Edition texture mappings in config.yml:

```yaml
icons:
  # Default icon
  default: "textures/items/paper"
  
  # Whether to allow URL icons
  allow_url: true
  
  # URL icon settings
  url:
    https-only: true
    max-length: 256
    allowed-domains: []
  
  # Icon type mapping
  mappings:
    grass_block: "textures/blocks/grass_side"
    diamond: "textures/items/diamond"
    compass: "textures/items/compass_item"
```

## Best Practices

1. When using resource packs:
   - Recommended image size is 32x32 or 64x64
   - Use PNG format with transparency support
   - Use lowercase letters and underscores for filenames
   - Use `textures/gui/icons/` prefix for paths

2. Choosing icon type:
   - If using Java Edition items, choose `icon_type: "java"`
   - If using custom icons, choose `icon_type: "bedrock"`
   - If using network icons, choose `icon_type: "url"`

3. URL icon considerations:
   - Ensure HTTPS protocol is used
   - Image size should not be too large
   - Consider using CDN for faster loading

:::tip Note
- Custom icons must be loaded through Bedrock Edition resource packs
- Resource packs can be loaded on server or client
- Icon paths are case-sensitive
- You can add custom mappings in config.yml
:::

:::warning Note
- If the icon path is invalid, the default icon will be used
- Resource packs must comply with Bedrock Edition format requirements
- It is recommended to test all icons for proper display
- URL icons require network connection to display
::: 
