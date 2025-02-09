# 快速上手

## 安装

1. 确保你的服务器已经安装了必需的前置插件：
   - Geyser-Spigot
   - Floodgate

2. 下载 GeyserMenu 最新版本

3. 将插件放入服务器的 `plugins` 文件夹

4. 重启服务器

## 目录结构

插件首次运行会生成以下目录和文件：

```text
plugins/GeyserMenu/
├── config.yml      # 主配置文件
├── messages.yml    # 消息配置文件
├── icons/          # 自定义图标目录
└── menus/          # 菜单文件夹
    ├── menu.yml    # 主菜单
    ├── shop.yml    # 商店菜单
    └── teleport.yml # 传送菜单
```

::: tip 提示
- 配置文件只会在首次启动时生成，之后的修改不会被覆盖
- 自定义图标请放在 icons 目录下
- 菜单文件请放在 menus 目录下
:::

## 配置

插件首次运行会生成以下文件：

```text
plugins/GeyserMenu/
├── config.yml      # 主配置文件
├── messages.yml    # 消息配置文件
├── icons/          # 自定义图标目录
└── menus/          # 菜单文件夹
    ├── menu.yml    # 主菜单
    ├── shop.yml    # 商店菜单
    └── teleport.yml # 传送菜单
```

::: tip 提示
配置文件只会在首次启动时生成，之后的修改不会被覆盖。
:::

### 基础配置

编辑 `config.yml` 进行基础设置：

```yaml
settings:
  default-menu: "menu.yml"  # 默认菜单
  debug: false              # 调试模式
```

### 创建菜单

在 `menus` 文件夹中创建新的 YAML 文件：

```yaml
menu:
  title: "我的菜单"
  subtitle: "选择一个选项"
  content: "这是菜单内容"
  items:
    - text: "测试按钮"
      description: "点击执行命令"
      icon: "diamond"  # 不需要 minecraft: 前缀
      command: "say 你好"
```

## 使用方法

1. 基岩版玩家输入 `/gmenu` 打开默认菜单

2. 使用 `/gmenu help` 查看所有可用命令

3. 管理员可以使用 `/gmenu reload` 重载配置