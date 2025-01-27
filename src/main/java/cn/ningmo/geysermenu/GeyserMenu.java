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
            instance = this;
            
            // 检查前置插件
            if (!checkDependencies()) {
                getLogger().severe("缺少必要的前置插件，插件将被禁用!");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            // 保存默认配置
            saveDefaultConfig();
            saveResource("menus/menu.yml", false);
            saveResource("menus/teleport.yml", false);  // 添加默认子菜单
            saveResource("menus/shop.yml", false);      // 添加默认子菜单
            
            // 加载消息配置
            saveResource("messages.yml", false);
            reloadMessages();
            
            // 初始化菜单管理器
            menuManager = new MenuManager(this);
            
            // 注册命令
            getCommand("geysermenu").setExecutor(new MenuCommand(this));
            
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
            
            getLogger().info("GeyserMenu 已卸载!");
        } catch (Exception e) {
            getLogger().severe("插件卸载时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static GeyserMenu getInstance() {
        return instance;
    }
    
    public MenuManager getMenuManager() {
        return menuManager;
    }
    
    public void reloadMessages() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }
    
    public String getMessage(String path) {
        return getMessage(path, new String[0]);
    }
    
    public String getMessage(String path, String... args) {
        String message = messages.getString("messages." + path);
        if (message == null) {
            return "§c消息未配置: " + path;
        }
        return getPrefix() + String.format(message, (Object[]) args);
    }
    
    public String getPrefix() {
        return messages.getString("prefix", "§6[GeyserMenu] §f");
    }
    
    public String getRawMessage(String path) {
        return messages.getString("messages." + path);
    }
    
    private boolean checkDependencies() {
        return getServer().getPluginManager().getPlugin("Geyser-Spigot") != null 
            && getServer().getPluginManager().getPlugin("floodgate") != null;
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
} 