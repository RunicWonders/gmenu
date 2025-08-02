# BStats 统计功能

## 概述

GeyserMenu 现已集成 BStats 统计功能，用于收集匿名的插件使用统计数据。这些数据帮助开发者了解插件的使用情况，从而更好地改进插件。

## 收集的数据

### 基础统计
- 服务器版本分布
- Java 版本分布
- 在线玩家数量
- 服务器软件类型（Paper、Spigot、Bukkit 等）

### 插件特定统计
- 配置的菜单数量
- 启用的功能（PAPI缓存、命令安全检查、更新检查等）
- 菜单类型使用情况（主菜单、传送菜单、商店菜单）
- 性能配置设置
- PlaceholderAPI 使用情况

## 配置选项

在 `config.yml` 中，您可以找到以下统计相关配置：

```yaml
settings:
  statistics:
    # 是否启用 BStats 统计 - 帮助开发者了解插件使用情况
    enable-bstats: true
    
    # 是否收集自定义统计数据
    collect-custom-data: true
```

### 配置说明

- `enable-bstats`: 控制是否启用 BStats 统计功能
  - `true`: 启用统计（默认）
  - `false`: 禁用统计

- `collect-custom-data`: 控制是否收集自定义统计数据
  - `true`: 收集详细的插件使用统计（默认）
  - `false`: 仅收集基础统计

## 隐私说明

### 收集的信息
- **匿名数据**: 所有统计数据都是匿名的，不包含任何可识别服务器或玩家的信息
- **服务器信息**: 服务器版本、Java版本、插件版本等技术信息
- **使用统计**: 功能使用情况、配置选项等

### 不收集的信息
- 服务器IP地址或域名
- 玩家用户名或UUID
- 聊天内容或命令内容
- 服务器配置的敏感信息

## 如何禁用

如果您不希望发送统计数据，可以通过以下方式禁用：

### 方法1：插件配置
在 `config.yml` 中设置：
```yaml
settings:
  statistics:
    enable-bstats: false
```

### 方法2：全局禁用
在服务器的 `plugins/bStats/config.yml` 中设置：
```yaml
enabled: false
```

## 查看统计数据

您可以在 [bStats 官网](https://bstats.org/plugin/bukkit/GeyserMenu/26736) 查看 GeyserMenu 的公开统计数据。

## 技术实现

### 依赖
- `org.bstats:bstats-bukkit:3.0.2`

### 重定位
为避免与其他插件的 BStats 版本冲突，我们将 BStats 重定位到：
```
cn.ningmo.geysermenu.bstats
```

### 插件ID
BStats 插件ID: `26736`

## 开发者信息

如果您是开发者并想了解更多关于 BStats 的信息：

1. 访问 [BStats 官网](https://bstats.org/)
2. 注册并创建插件页面
3. 获取插件ID并替换代码中的 `PLUGIN_ID`
4. 查看统计图表和数据

## 支持

如果您对统计功能有任何疑问或建议，请：

1. 查看 [BStats FAQ](https://bstats.org/help/faq)
2. 在 GitHub 上提交 Issue
3. 联系插件作者

---

**注意**: 统计数据的收集完全符合 Minecraft 服务器社区的最佳实践，并且是完全透明的。我们鼓励保持统计功能启用，以帮助改进插件质量。