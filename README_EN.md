# GeyserMenu

A lightweight and simple custom form plugin for Bedrock Edition.

## Current Version: v1.3.0-beta5

### What's New
- Supports SimpleForm, ModalForm, and CustomForm
- Supports label, input, dropdown, slider, and toggle components
- Improved custom form response parsing, dropdown text values, and the `{player}` placeholder
- Added per-menu permissions, command safety checks, and allowed domains for URL icons
- Added automatic config/message migration and configurable runtime cache clearing on reload
- Added update checking and join notifications
- Updated dependencies to Paper API 1.21.4+, bStats 3.2.1, and org.json 20250107

## Requirements

- Minecraft Server: Paper 1.21.4 or higher (Paper only, Spigot is not supported)
- Java: 21 or higher
- Required Plugin: Floodgate

## Features

### Basic Features
- Bedrock Edition players only
- Multiple menu configurations supported
- Unlimited sub-menu levels
- PlaceholderAPI variable support
- Three icon types supported:
  - Java Edition item IDs (use `icon_type: "java"`)
  - Bedrock Edition texture paths (use `icon_type: "bedrock"`)
  - HTTPS URL icons (use `icon_type: "url"`)
- Integrated BStats statistics (configurable)

### Command System
- Three command execution modes:
  - Player execution (`execute_as: "player"`)
  - Console execution (`execute_as: "console"`)
  - OP permission execution (`execute_as: "op"`)

### Commands
- `/gmenu` - Open the default menu
- `/gmenu help` - Show help information
- `/gmenu reload` - Reload configuration, messages, and menus (requires `geysermenu.reload`)
- `/gmenu open <player> <menu>` - Open a menu for another player (requires `geysermenu.open`)

### Permissions
- `geysermenu.use` - Use the default menu (default: true)
- `geysermenu.reload` - Reload configuration (default: op)
- `geysermenu.open` - Open menus for other players (default: op)
- `geysermenu.admin` - Administrator access to all features (default: op)
- `geysermenu.menu.*` - Use all menus (default: op)
- `geysermenu.menu.<menu-key>` - Use a specific menu; configured in `config.yml`
- `geysermenu.*` - Use all features (default: op)

### Menu Configuration
- Enable/disable menus via config.yml
- Custom menu titles and buttons
- Custom button icons and commands
  - Java item IDs (use `icon_type: "java"`)
  - Bedrock texture paths (use `icon_type: "bedrock"`)
  - HTTPS URL icons (use `icon_type: "url"`)
- Menu navigation between menus
- Menu subtitles and descriptions
- Button description text
- Form footer text
- Color codes support (use `§` or `&`)
- Multi-line text support (use `|-` syntax)

### Form Types
- `simple`: Multi-button navigation menu
- `modal`: Two-button confirmation form
- `custom`: Custom form with `label`, `input`, `dropdown`, `slider`, and `toggle` components

See the [form types guide](docs/guide/form-types.md) for complete examples.

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

## Security

Command security is enabled by default. It blocks commands such as `op`, `deop`, `stop`, and `reload`, and rejects `;`, `|`, `&`, and backticks in commands. Do not disable it for untrusted configurations.

URL icons require HTTPS by default. You can restrict hosts with `icons.url.allowed-domains`.

## Configuration and Documentation

- Main configuration: `src/main/resources/config.yml` (copied to the plugin data folder at runtime)
- Form types: [docs/guide/form-types.md](docs/guide/form-types.md)
- Statistics: [BSTATS.md](BSTATS.md)
- Contributing: [CONTRIBUTING.md](CONTRIBUTING.md)

## License

This project is licensed under the [MIT License](LICENSE).
