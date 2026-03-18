# Statistics

## BStats Statistics

GeyserMenu integrates BStats statistics functionality to collect anonymous plugin usage data, helping developers understand usage patterns and improve plugin quality.

### Configuration Options

In `config.yml`, you can find the following statistics-related configuration:

```yaml
settings:
  statistics:
    # Enable BStats statistics - helps developers understand plugin usage
    enable-bstats: true
    
    # Collect custom statistics data
    collect-custom-data: true
```

### Configuration Description

- `enable-bstats`: Controls whether BStats statistics functionality is enabled
  - `true`: Enable statistics (default)
  - `false`: Disable statistics

- `collect-custom-data`: Controls whether custom statistics data is collected
  - `true`: Collect detailed plugin usage statistics (default)
  - `false`: Collect only basic statistics

## Data Collected

### Basic Statistics
- Server version distribution
- Java version distribution
- Online player count
- Server software type (Paper, Spigot, Bukkit, etc.)

### Plugin-Specific Statistics
- Number of configured menus
- Enabled features (PAPI caching, command security check, update check, etc.)
- Menu type usage (main menu, teleport menu, shop menu)
- Performance configuration settings
- PlaceholderAPI usage

## Privacy Information

### Information Collected
- **Anonymous Data**: All statistics data is anonymous and does not contain any information that can identify servers or players
- **Server Information**: Technical information such as server version, Java version, plugin version
- **Usage Statistics**: Feature usage, configuration options, etc.

### Information Not Collected
- Server IP address or domain
- Player usernames or UUIDs
- Chat content or command content
- Sensitive server configuration information

## How to Disable

If you do not wish to send statistics data, you can disable it through the following methods:

### Method 1: Plugin Configuration
In `config.yml`, set:
```yaml
settings:
  statistics:
    enable-bstats: false
```

### Method 2: Global Disable
In the server's `plugins/bStats/config.yml`, set:
```yaml
enabled: false
```

## View Statistics

You can view GeyserMenu's public statistics data at [bStats Official Website](https://bstats.org/plugin/bukkit/GeyserMenu/26736).

::: tip Note
Keeping statistics enabled helps developers understand plugin usage, thereby better improving the plugin. All data is anonymous and no sensitive information is collected.
:::

::: warning Note
If your server has special privacy requirements, you can disable statistics functionality in the configuration at any time.
:::
