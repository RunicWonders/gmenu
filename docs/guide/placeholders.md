# 变量支持

GeyserMenu 支持 PlaceholderAPI 变量，可以在菜单中动态显示信息。

## 使用变量

变量可以在以下位置使用：

- 菜单标题
- 菜单内容
- 按钮文本
- 按钮描述
- 命令

## 示例

```yaml
menu:
  title: "欢迎 %player_name%"
  subtitle: "你的等级: %player_level%"
  content: "你的余额: %vault_eco_balance%"
  footer: "在线人数: %server_online%"
  items:
    - text: "%player_name% 的背包"
      description: "点击打开背包"
      icon: "chest"
      command: "invsee %player_name%"
```

::: tip 提示
变量会在显示菜单时自动更新，除非启用了缓存。
:::

## 变量缓存

为提高性能，可以启用变量缓存：

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

::: warning 注意
启用缓存可能导致变量更新有延迟，请根据需要调整刷新间隔。
::: 