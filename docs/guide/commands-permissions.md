# Commands and Permissions

## Command List

### Basic Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/gmenu` | Open default menu | `geysermenu.use` |
| `/gmenu help` | Show help information | `geysermenu.use` |
| `/gmenu reload` | Reload configuration files | `geysermenu.reload` |
| `/gmenu open <player> <menu>` | Open menu for specified player | `geysermenu.open` |

### Command Arguments

`/gmenu open` command arguments:
- `<player>`: Target player name
- `<menu>`: Menu filename to open (e.g., menu.yml)

### Reload Command

When using `/gmenu reload` to reload the plugin:
1. Reloads main configuration file
2. Reloads message configuration
3. Reloads all menus
4. Clears variable cache if caching is enabled

::: tip Note
Reloading does not affect players currently using menus.
:::

## Permission Nodes

### Basic Permissions

| Permission Node | Description | Default |
|-----------------|-------------|---------|
| `geysermenu.use` | Allow using menu commands | true |
| `geysermenu.reload` | Allow reloading plugin configuration | op |
| `geysermenu.open` | Allow opening menus for other players | op |
| `geysermenu.*` | Allow all features | op |

### Menu Permissions

Menu permissions are configured in config.yml:

```yaml
menus:
  main:
    file: "menu.yml"
    enable: true
    permission: "geysermenu.menu.main"
  
  shop:
    file: "shop.yml"
    enable: true
    permission: "geysermenu.menu.shop"
```

All menu permissions automatically become child permissions of `geysermenu.menu.*`. Players with `geysermenu.menu.*` permission can use all enabled menus.

## Menu Types and Execution Permissions

### Menu Types

GeyserMenu supports multiple menu types, each with different purposes:

| Menu Type | Description | Example |
|-----------|-------------|---------|
| Main Menu | Entry menu, usually contains entries to other submenus | `menu.yml` |
| Submenu | Secondary menu opened from main menu | `shop.yml`, `teleport.yml` |
| Command Menu | Menu that executes specific commands | Menu items with `command` attribute |

### Command Execution Modes

Commands in menus can be executed in different ways, set through the `execute_as` attribute:

| Execution Mode | Description | Permission Requirement |
|----------------|-------------|------------------------|
| `player` | Execute command as player (default) | Player needs permission to execute the command |
| `console` | Execute command as console | No player permission required, but must be allowed in security settings |
| `op` | Temporarily grant player OP permission to execute command | Must be allowed in security settings |

Example configuration:

```yaml
items:
  - text: "Teleport to Mine"
    icon: "diamond_pickaxe"
    icon_type: "java"
    command: "warp mine"
    execute_as: "player"  # Execute as player
    
  - text: "Get Special Item"
    icon: "nether_star"
    icon_type: "java"
    command: "give {player} diamond 1"
    execute_as: "console"  # Execute as console
```

### Command Security Settings

To protect server security, you can configure command security settings in config.yml:

```yaml
security:
  # List of blocked commands
  blocked-commands:
    - "op"
    - "deop"
    - "stop"
    - "reload"
    
  # Whether to allow special characters in commands (like & | ; etc.)
  allow-special-chars: false
```

::: warning Note
Be extra careful when using `execute_as: "console"` or `execute_as: "op"` to ensure commands are not abused.
:::
