package cn.ningmo.geysermenu;

import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.api.FloodgateApi;

public class GeyserMenu extends JavaPlugin {
    private static GeyserMenu instance;
    private MenuManager menuManager;
    
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
        getLogger().info("GeyserMenu 已卸载!");
    }
    
    public static GeyserMenu getInstance() {
        return instance;
    }
    
    public MenuManager getMenuManager() {
        return menuManager;
    }
    
    public String getMessage(String path) {
        return getConfig().getString("messages." + path, "§c消息未配置: " + path);
    }
    
    public String getPrefix() {
        return getConfig().getString("settings.prefix", "§6[GeyserMenu] §f");
    }
    
    private boolean checkDependencies() {
        return getServer().getPluginManager().getPlugin("Geyser-Spigot") != null 
            && getServer().getPluginManager().getPlugin("floodgate") != null;
    }
} 