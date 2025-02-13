# 变量支持

GeyserMenu 支持 PlaceholderAPI 变量，可以在菜单中动态显示信息。

## 使用变量

变量可以在以下位置使用：

- 菜单标题
- 菜单副标题
- 菜单内容
- 菜单页脚
- 按钮文本
- 按钮描述
- 命令

## 示例配置

```yaml
menu:
  # 在标题中使用变量
  title: "§6%player_name% 的菜单"
  
  # 在内容中使用变量
  content: |-
    §f你好, %player_name%
    §7余额: §e%vault_eco_balance%
  
  # 在页脚中使用变量
  footer: "§8在线人数: %server_online%"
  
  items:
    - text: "§e我的余额: %vault_eco_balance%"
      description: "§7点击查看详细信息"
      icon: "diamond"
      icon_type: "java"
      command: "balance %player_name%"
```

## 性能优化

在 config.yml 中可以配置变量缓存以提高性能：

```yaml
performance:
  # 启用变量缓存
  cache-placeholders: true
  
  # 缓存刷新间隔（秒）
  cache-refresh: 30
  
  # 最大缓存数量
  max-cache-size: 1000
  
  # 重载时清除缓存
  clear-cache-on-reload: true
```

::: tip 提示
- 启用缓存可以提高性能，但变量更新会有延迟
- 建议根据服务器情况调整刷新间隔
- 对于需要实时更新的变量，可以禁用缓存
:::

## 常用变量

以下是一些常用的 PlaceholderAPI 变量：

- `%player_name%` - 玩家名称
- `%player_displayname%` - 玩家显示名称
- `%server_online%` - 在线玩家数量
- `%vault_eco_balance%` - 玩家余额（需要 Vault）
- `%player_health%` - 玩家生命值
- `%player_food_level%` - 玩家饥饿值

::: warning 注意
使用变量前请确保已安装 PlaceholderAPI 和相应的扩展
::: 