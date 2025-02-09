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