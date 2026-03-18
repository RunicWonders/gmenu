# Configuration Guide

## Main Configuration File

`config.yml` contains the core settings of the plugin:

### Performance Settings

```yaml
performance:
  # Command execution delay (milliseconds)
  command-delay: 0
  
  # PAPI variable caching
  cache-placeholders: false
  cache-refresh: 30
  max-cache-size: 1000
```

### Statistics Settings

```yaml
statistics:
  # Enable BStats statistics
  enable-bstats: true
  
  # Collect custom statistics data
  collect-custom-data: true
```

::: tip Note
For detailed information about statistics, please refer to the [Statistics](./statistics.md) documentation.
:::

### Security Settings

```yaml
security:
  # Blocked commands
  blocked-commands:
    - "op"
    - "deop"
  
  # Special character check
  allow-special-chars: false
  
  # File path security check
  check-file-path: true
```

### Icon Settings

GeyserMenu supports two types of icons:

### 1. Java Edition Item ID
Use Java Edition item IDs, which will be automatically converted to corresponding Bedrock Edition texture paths:

```yaml
items:
  - text: "Teleport Menu"
    icon: "compass"      # Java Edition item ID
    icon_type: "java"    # Specify using Java Edition icon
```

### 2. Bedrock Edition Texture Path
Directly use Bedrock Edition texture paths:

```yaml
items:
  - text: "Shop Menu"
    icon: "textures/items/diamond"    # Bedrock Edition texture path
    icon_type: "bedrock"             # Specify using Bedrock Edition icon
```

::: warning Important
Starting from version 1.1.0, you must explicitly specify the icon type ("java" or "bedrock") through the `icon_type` attribute. This is to avoid icon display errors and improve performance.
:::

### Icon Mapping Configuration

Configure Java Edition to Bedrock Edition texture mappings in config.yml:

```yaml
icons:
  # Default icon
  default: "textures/items/paper"
  
  # Icon type mapping (Java Edition -> Bedrock Edition)
  mappings:
    # Blocks
    grass_block: "textures/blocks/grass_side"
    stone: "textures/blocks/stone"
    dirt: "textures/blocks/dirt"
    
    # Items
    diamond: "textures/items/diamond"
    compass: "textures/items/compass_item"
    book: "textures/items/book_normal"
```

### Usage Recommendations

1. If you are familiar with Java Edition item IDs, use `icon_type: "java"`
2. If you need to use specific Bedrock Edition textures, use `icon_type: "bedrock"`
3. If `icon_type` is not specified, it will default to Java Edition item ID

::: tip Note
- Java Edition item IDs do not need the `minecraft:` prefix
- Bedrock Edition texture paths must be complete texture paths
- You can add new texture mappings in the configuration file
- Icon type must be specified via icon_type ("java" or "bedrock")
:::

::: warning Note
If a Java Edition item ID has no corresponding mapping, the default icon will be used
:::

## Message Configuration

`messages.yml` is used to configure all text messages of the plugin:

```yaml
prefix: "§6[GeyserMenu] §f"  # Message prefix

reload:
  success: "§aConfiguration reloaded successfully!"  # Reload success message
  start: "§eReloading plugin configuration..."  # Start reload message

error:
  no-permission: "§cYou don't have permission to execute this command!"  # No permission message
  # ... other error messages
```

## Menu Configuration

Menu file structure explanation:

### Basic Structure

```yaml
menu:
  # Menu title
  title: "Main Menu"
  
  # Subtitle (optional)
  subtitle: "Select an option"
  
  # Main content (optional)
  content: "This is menu content"
  
  # Footer (optional)
  footer: "Online players: %server_online%"
  
  # Button list
  items:
    - text: "Teleport Menu"
      description: "Open teleport menu"
      icon: "compass"
      icon_type: "java"      # Must specify icon type
      submenu: "teleport.yml"
    
    - text: "Shop Menu"
      description: "Open shop menu"
      icon: "textures/items/diamond"
      icon_type: "bedrock"   # Must specify icon type
      submenu: "shop.yml"
    
    - text: "Return to Spawn"
      description: "Click to teleport to spawn"
      icon: "nether_star"
      icon_type: "java"    # Add icon type
      command: "spawn"
```

::: tip Note
- All text supports color codes (using & symbol)
- Supports PlaceholderAPI variables
- Icon type must be specified via icon_type ("java" or "bedrock")
:::

### Menu Types

GeyserMenu supports multiple menu types through different configurations:

#### 1. Submenu Type

Open another menu file through the `submenu` attribute:

```yaml
items:
  - text: "Teleport Menu"
    description: "Open teleport menu"
    icon: "compass"
    icon_type: "java"
    submenu: "teleport.yml"  # Opens teleport.yml menu on click
```

#### 2. Command Execution Type

Execute a specified command through the `command` attribute:

```yaml
items:
  - text: "Return to Spawn"
    description: "Click to teleport to spawn"
    icon: "nether_star"
    icon_type: "java"
    command: "spawn"  # Executes spawn command on click
```

#### 3. Command Execution Mode

Specify the command execution mode through the `execute_as` attribute:

```yaml
items:
  - text: "Get Diamond"
    description: "Receive a diamond"
    icon: "diamond"
    icon_type: "java"
    command: "give {player} diamond 1"
    execute_as: "console"  # Execute command as console
```

Available execution modes:
- `player`: Execute command as player (default)
- `console`: Execute command as console
- `op`: Temporarily grant player OP permission to execute command

::: warning Security Note
Be extra careful when using `console` or `op` execution modes to ensure commands are not abused. It is recommended to configure the `blocked-commands` list in config.yml to block dangerous commands.
:::

## Configuration Saving

Configuration file saving and reloading mechanism:

1. All default configuration files are generated on first startup
2. Subsequent modifications to configuration files will not be overwritten
3. Modifications are preserved when using `/gmenu reload` to reload

::: warning Note
Do not delete configuration files while the server is running, as this may cause the plugin to malfunction.
::: 

## Directory Description

### Menu Directory

The `menus` directory is used to store menu configuration files:
- Uses YAML format
- The filename is the menu name
- Supports subdirectory organization for menus
