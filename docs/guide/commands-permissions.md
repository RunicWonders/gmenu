# 命令与权限

## 命令列表

### 基础命令

| 命令 | 描述 | 权限 |
|------|------|------|
| `/gmenu` | 打开默认菜单 | `geysermenu.use` |
| `/gmenu help` | 显示帮助信息 | `geysermenu.use` |
| `/gmenu reload` | 重载配置文件 | `geysermenu.reload` |
| `/gmenu open <玩家> <菜单>` | 为指定玩家打开菜单 | `geysermenu.open` |

### 命令参数

`/gmenu open` 命令参数说明：
- `<玩家>`: 目标玩家名称
- `<菜单>`: 要打开的菜单文件名（如 menu.yml）

### 重载命令

使用 `/gmenu reload` 重载插件时：
1. 重新加载主配置文件
2. 重新加载消息配置
3. 重新加载所有菜单
4. 如果启用了缓存，会清除变量缓存

::: tip 提示
重载不会影响正在使用菜单的玩家。
:::

## 权限节点

### 基础权限

| 权限节点 | 描述 | 默认值 |
|---------|------|--------|
| `geysermenu.use` | 允许使用菜单命令 | true |
| `geysermenu.reload` | 允许重载插件配置 | op |
| `geysermenu.open` | 允许为其他玩家打开菜单 | op |
| `geysermenu.*` | 允许使用所有功能 | op |

### 菜单权限

菜单权限在 config.yml 中配置：

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

所有菜单权限会自动成为 `geysermenu.menu.*` 的子权限。拥有 `geysermenu.menu.*` 权限的玩家可以使用所有已启用的菜单。

## 菜单类型与执行权限

### 菜单类型

GeyserMenu 支持多种菜单类型，每种类型都有不同的用途：

| 菜单类型 | 描述 | 示例 |
|---------|------|------|
| 主菜单 | 作为入口菜单，通常包含其他子菜单的入口 | `menu.yml` |
| 子菜单 | 从主菜单打开的二级菜单 | `shop.yml`, `teleport.yml` |
| 命令菜单 | 执行特定命令的菜单 | 包含 `command` 属性的菜单项 |

### 命令执行方式

菜单中的命令可以以不同的方式执行，通过 `execute_as` 属性设置：

| 执行方式 | 描述 | 权限要求 |
|---------|------|----------|
| `player` | 以玩家身份执行命令（默认） | 玩家需要有执行该命令的权限 |
| `console` | 以控制台身份执行命令 | 不需要玩家权限，但需要在安全设置中允许 |
| `op` | 临时给予玩家OP权限执行命令 | 需要在安全设置中允许 |

示例配置：

```yaml
items:
  - text: "传送到矿区"
    icon: "diamond_pickaxe"
    icon_type: "java"
    command: "warp mine"
    execute_as: "player"  # 以玩家身份执行
    
  - text: "获取特殊物品"
    icon: "nether_star"
    icon_type: "java"
    command: "give {player} diamond 1"
    execute_as: "console"  # 以控制台身份执行
```

### 命令安全设置

为了保护服务器安全，可以在 config.yml 中配置命令安全设置：

```yaml
security:
  # 禁止执行的命令列表
  blocked-commands:
    - "op"
    - "deop"
    - "stop"
    - "reload"
    
  # 是否允许命令中包含特殊字符(如 & | ; 等)
  allow-special-chars: false
```

::: warning 注意
使用 `execute_as: "console"` 或 `execute_as: "op"` 时要特别小心，确保命令不会被滥用。
:::