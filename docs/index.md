# GeyserMenu v1.3.0-beta1

A lightweight and simple custom form plugin for Bedrock Edition.

## Features

- Designed specifically for Bedrock Edition players
- Fully customizable menu configurations
- Built-in security check mechanism
- High performance and lightweight
- PlaceholderAPI variable support
- Integrated BStats statistics (configurable)
- Two icon types supported:
  - Java Edition item IDs (automatically mapped to Bedrock Edition)
  - Bedrock Edition texture paths (direct use)

## Requirements

- Java 21 or higher
- Paper 1.21.4 or higher
- [Geyser-Spigot](https://geysermc.org/) and [Floodgate](https://wiki.geysermc.org/floodgate/)

## Quick Start

1. Download the latest version of GeyserMenu
2. Place the plugin in your server's plugins folder
3. Start the server, the plugin will automatically generate configuration files
4. Edit `plugins/GeyserMenu/config.yml` for basic configuration
5. Edit or add menus in `plugins/GeyserMenu/menus/`

## Basic Commands

- `/gmenu` - Open default menu
- `/gmenu help` - Show help information
- `/gmenu reload` - Reload configuration files
- `/gmenu open <player> <menu>` - Open menu for specified player

## Permissions

- `geysermenu.use` - Allow using menu commands
- `geysermenu.reload` - Allow reloading plugin configuration
- `geysermenu.open` - Allow opening menus for other players
- `geysermenu.*` - Allow all features
