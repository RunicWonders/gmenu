package cn.ningmo.geysermenu;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import org.bukkit.Bukkit;

public class GeyserMenu extends JavaPlugin {
    private static GeyserMenu instance;
    private MenuManager menuManager;
    private YamlConfiguration messages;
    
    @Override
    public void onEnable() {
        try {
            // 防止重复初始化
            if (instance != null) {
                getLogger().warning("检测到插件重复加载!");
                return;
            }
            instance = this;
            
            // 检查前置插件
            if (!checkDependencies()) {
                getLogger().severe("缺少必要的前置插件，插件将被禁用!");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            // 添加配置检查
            if (!checkConfig()) {
                getLogger().severe("配置文件检查失败，插件将被禁用!");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            // 保存默认配置
            saveDefaultConfig();
            
            // 保存其他配置文件
            saveResource("messages.yml", false);
            saveResource("menus/menu.yml", false);
            saveResource("menus/shop.yml", false);
            saveResource("menus/teleport.yml", false);
            
            // 添加新的配置项到配置文件中
            getConfig().addDefault("settings.enable-command-security", true);
            getConfig().options().copyDefaults(true);
            saveConfig();
            
            // 创建菜单目录
            File menuDir = new File(getDataFolder(), "menus");
            if (!menuDir.exists()) {
                menuDir.mkdirs();
            }
            
            // 保存默认菜单
            saveResource("menus/menu.yml", false);
            saveResource("menus/teleport.yml", false);
            saveResource("menus/shop.yml", false);
            
            // 创建图标目录
            File iconDir = new File(getDataFolder(), "icons");
            if (!iconDir.exists()) {
                iconDir.mkdirs();
            }
            
            // 加载消息配置
            reloadMessages();
            
            // 初始化菜单管理器
            menuManager = new MenuManager(this);
            
            // 注册命令
            if (getCommand("geysermenu") != null) {
                getCommand("geysermenu").setExecutor(new MenuCommand(this));
            } else {
                getLogger().severe("命令注册失败!");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            getLogger().info("GeyserMenu v" + getDescription().getVersion() + " 已成功加载!");
        } catch (Exception e) {
            getLogger().severe("插件加载时发生错误: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        try {
            // 清理资源
            if (menuManager != null) {
                menuManager.getMenuList().clear();
            }
            
            // 取消所有任务
            Bukkit.getScheduler().cancelTasks(this);
            
            // 清除实例
            instance = null;
            
            getLogger().info("GeyserMenu 已卸载!");
        } catch (Exception e) {
            getLogger().severe("插件卸载时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void reloadMessages() {
        try {
            File messagesFile = new File(getDataFolder(), "messages.yml");
            if (!messagesFile.exists()) {
                saveResource("messages.yml", false);
            }
            messages = YamlConfiguration.loadConfiguration(messagesFile);
        } catch (Exception e) {
            getLogger().severe("加载消息配置时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public String getMessage(String path, String... args) {
        try {
            String message = messages.getString(path);
            if (message == null) {
                getLogger().warning("找不到消息配置: " + path);
                return "§c消息未配置: " + path;
            }
            
            StringBuilder result = new StringBuilder(getPrefix());
            String formattedMessage = message;
            
            // 替换参数
            for (int i = 0; i < args.length; i++) {
                formattedMessage = formattedMessage.replace("{" + i + "}", args[i] != null ? args[i] : "null");
            }
            
            result.append(formattedMessage);
            return result.toString();
        } catch (Exception e) {
            getLogger().warning("获取消息时出错: " + path + ", 错误: " + e.getMessage());
            return "§c消息处理错误: " + path;
        }
    }
    
    public String getRawMessage(String path) {
        try {
            return messages.getString(path, "§c消息未配置: " + path);
        } catch (Exception e) {
            getLogger().warning("获取原始消息时出错: " + path);
            return "§c消息处理错误: " + path;
        }
    }
    
    private boolean checkDependencies() {
        try {
            return getServer().getPluginManager().getPlugin("Geyser-Spigot") != null 
                && getServer().getPluginManager().getPlugin("floodgate") != null;
        } catch (Exception e) {
            getLogger().severe("检查依赖时发生错误: " + e.getMessage());
            return false;
        }
    }
    
    private boolean checkConfig() {
        try {
            // 检查配置文件版本
            if (!getConfig().isSet("settings.default-menu")) {
                getLogger().warning("配置文件缺少必要设置，将重新生成");
                saveResource("config.yml", true);
                reloadConfig();
            }
            
            // 检查消息文件
            File messagesFile = new File(getDataFolder(), "messages.yml");
            if (!messagesFile.exists()) {
                saveResource("messages.yml", false);
            }
            
            // 检查菜单目录
            File menuFolder = new File(getDataFolder(), "menus");
            if (!menuFolder.exists()) {
                menuFolder.mkdirs();
            }
            
            return true;
        } catch (Exception e) {
            getLogger().severe("检查配置文件时发生错误: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public void reloadConfig() {
        super.reloadConfig();
        
        // 重新加载消息配置
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (messagesFile.exists()) {
            messages = YamlConfiguration.loadConfiguration(messagesFile);
        }
        
        // 重新加载菜单
        menuManager.loadMenus();
    }
    
    // 静态方法应该检查实例是否存在
    public static GeyserMenu getInstance() {
        if (instance == null) {
            throw new IllegalStateException("插件实例未初始化!");
        }
        return instance;
    }
    
    public MenuManager getMenuManager() {
        if (menuManager == null) {
            throw new IllegalStateException("菜单管理器未初始化!");
        }
        return menuManager;
    }
    
    public String getPrefix() {
        try {
            return messages.getString("prefix", "§6[GeyserMenu] §f");
        } catch (Exception e) {
            getLogger().warning("获取前缀时出错");
            return "§6[GeyserMenu] §f";
        }
    }
}
