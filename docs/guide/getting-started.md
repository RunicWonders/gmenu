# Quick Start

## Installation

1. Make sure your server has the required dependencies installed:
   - Geyser-Spigot
   - Floodgate

2. Download GeyserMenu v1.3.0-beta1

3. Place the plugin in your server's `plugins` folder

4. Restart the server

## Directory Structure

The plugin will generate the following directories and files on first run:

```text
plugins/GeyserMenu/
├── config.yml      # Main configuration file
├── messages.yml    # Message configuration file
└── menus/          # Menu folder
    ├── menu.yml    # Main menu
    ├── shop.yml    # Shop menu
    └── teleport.yml # Teleport menu
```

::: tip Note
- Configuration files are only generated on first startup, subsequent modifications will not be overwritten
- Menu files should be placed in the menus directory
:::

## Configuration

### Basic Configuration

Edit `config.yml` for basic settings:

```yaml
settings:
  default-menu: "menu.yml"  # Default menu
  debug: false              # Debug mode
```

### Creating Menus

Create a new YAML file in the `menus` folder:

```yaml
menu:
  title: "My Menu"
  subtitle: "Select an option"
  content: "This is menu content"
  items:
    - text: "Teleport Menu"
      description: "Open teleport menu"
      icon: "compass"
      icon_type: "java"     # Use Java Edition item ID
      submenu: "teleport.yml"
    
    - text: "Execute Command"
      description: "Click to execute command"
      icon: "textures/items/diamond"
      icon_type: "bedrock"  # Use Bedrock Edition texture path
      command: "say Hello"
```

::: tip Note
- Each button must have text and icon
- Choose either command or submenu
- description is optional
- Icon type must be specified (java or bedrock)
:::

## Usage

1. Bedrock Edition players can use `/gmenu` to open the default menu

2. Use `/gmenu help` to view all available commands

3. Administrators can use `/gmenu reload` to reload configuration
