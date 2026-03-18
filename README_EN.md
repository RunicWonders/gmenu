# GeyserMenu

A lightweight and simple custom form plugin for Bedrock Edition.

## Latest Version: v1.3.0-beta1

### What's New
- Updated dependencies to Paper API 1.21.4+
- Migrated Chat API to Adventure (Kyori)
- Updated bStats to 3.2.1
- Updated org.json to 20250107
- Improved menu types and permission configuration documentation
- Optimized menu configuration instructions

## Requirements

- Minecraft Server: Paper 1.21.4 or higher
- Java: 21 or higher
- Required Plugin: Floodgate

## Features

### Basic Features
- Bedrock Edition players only
- Multiple menu configurations supported
- Unlimited sub-menu levels
- PlaceholderAPI variable support
- Two icon types supported:
  - Java Edition item IDs (use `icon_type: "java"`)
  - Bedrock Edition texture paths (use `icon_type: "bedrock"`)
- Integrated BStats statistics (configurable)

### Command System
- Three command execution modes:
  - Player execution (`execute_as: "player"`)
  - Console execution (`execute_as: "console"`)
  - OP permission execution (`execute_as: "op"`)

### Commands
- `/gmenu` - Open default menu
- `/gmenu help` - Show help information
- `/gmenu reload` - Reload configuration (requires permission: geysermenu.reload)
- `/gmenu open <player> <menu>` - Open menu for specified player (requires permission: geysermenu.open)

### Permissions
- `geysermenu.use` - Allow using menu commands (default: true)
- `geysermenu.reload` - Allow reloading configuration (default: op)
- `geysermenu.open` - Allow opening menus for other players (default: op)
- `geysermenu.*` - Allow all features (default: op)

### Menu Configuration
- Enable/disable menus via config.yml
- Custom menu titles and buttons
- Custom button icons and commands
  - Item ID icons (e.g., `minecraft:diamond`)
  - URL icons (use `icon_type: "url"`)
  - Custom path icons (use `icon_type: "path"` and `icon_path`)
- Menu navigation between menus
- Menu subtitles and descriptions
- Button description text
- Form footer text
- Color codes support (use section symbol or &)
- Multi-line text support (use |- syntax)

## Icon Support

GeyserMenu supports two types of icons:

1. Java Edition Item ID
   ```yaml
   icon: "diamond_sword"
   icon_type: "java"
   ```

2. Bedrock Edition Texture Path
   ```yaml
   icon: "textures/items/diamond_sword"
   icon_type: "bedrock"
   ```

All supported Java Edition item IDs are automatically mapped to corresponding Bedrock Edition texture paths. You can add or modify these mappings in config.yml.

## Statistics

GeyserMenu integrates BStats statistics to collect anonymous plugin usage data, helping developers understand usage patterns and improve plugin quality.

### Configuration Options
```yaml
settings:
  statistics:
    # Enable BStats statistics
    enable-bstats: true
    # Collect custom statistics data
    collect-custom-data: true
```

### Data Collected
- Server version and software type
- Java version information
- Online player count
- Plugin feature usage
- Menu configuration statistics

### Privacy Protection
- All data is anonymous
- No sensitive data like server IP or player information is collected
- Can be disabled in configuration at any time

For more details, see [BSTATS.md](BSTATS.md).
