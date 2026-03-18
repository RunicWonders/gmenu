# Placeholder Support

GeyserMenu supports PlaceholderAPI variables to dynamically display information in menus.

## Using Placeholders

Placeholders can be used in the following locations:

- Menu title
- Menu subtitle
- Menu content
- Menu footer
- Button text
- Button description
- Commands

## Example Configuration

```yaml
menu:
  # Use placeholder in title
  title: "§6%player_name%'s Menu"
  
  # Use placeholder in content
  content: |-
    §fHello, %player_name%
    §7Balance: §e%vault_eco_balance%
  
  # Use placeholder in footer
  footer: "§8Online players: %server_online%"
  
  items:
    - text: "§eMy Balance: %vault_eco_balance%"
      description: "§7Click for details"
      icon: "diamond"
      icon_type: "java"
      command: "balance %player_name%"
```

## Performance Optimization

You can configure placeholder caching in config.yml to improve performance:

```yaml
performance:
  # Enable placeholder caching
  cache-placeholders: true
  
  # Cache refresh interval (seconds)
  cache-refresh: 30
  
  # Maximum cache size
  max-cache-size: 1000
  
  # Clear cache on reload
  clear-cache-on-reload: true
```

::: tip Note
- Enabling caching can improve performance, but placeholder updates will be delayed
- It is recommended to adjust the refresh interval based on your server situation
- For placeholders that need real-time updates, you can disable caching
:::

## Common Placeholders

Here are some commonly used PlaceholderAPI variables:

- `%player_name%` - Player name
- `%player_displayname%` - Player display name
- `%server_online%` - Online player count
- `%vault_eco_balance%` - Player balance (requires Vault)
- `%player_health%` - Player health
- `%player_food_level%` - Player hunger level

::: warning Note
Please make sure PlaceholderAPI and corresponding expansions are installed before using placeholders
::: 
