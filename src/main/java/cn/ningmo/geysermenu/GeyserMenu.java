package cn.ningmo.geysermenu;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import org.bukkit.Bukkit;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.json.JSONObject;

public class GeyserMenu extends JavaPlugin {
    private static GeyserMenu instance;
    private MenuManager menuManager;
    private YamlConfiguration messages;
    private BStatsManager bStatsManager;
    
    // 添加更新检查相关字段
    private static final String UPDATE_URL = "https://api.github.com/repos/ning-g-mo/gmenu/releases/latest";
    private boolean updateAvailable = false;
    private String latestVersion = null;
    
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
            
            // 保存默认配置
            saveDefaultConfig();
            
            // 保存其他配置文件
            saveResource("messages.yml", false);
            saveResource("menus/menu.yml", false);
            saveResource("menus/shop.yml", false);
            saveResource("menus/teleport.yml", false);
            
            // 创建必要的目录
            createDirectories();
            
            // 加载消息配置
            reloadMessages();
            
            // 检查更新
            if (getConfig().getBoolean("settings.check-updates", true)) {
                checkUpdate();
            }
            
            // 初始化菜单管理器
            menuManager = new MenuManager(this);
            
            // 加载菜单
            menuManager.loadMenus();
            
            // 注册命令
            if (getCommand("geysermenu") != null) {
                getCommand("geysermenu").setExecutor(new MenuCommand(this));
            } else {
                getLogger().severe("命令注册失败!");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            // 初始化 BStats 统计
            bStatsManager = new BStatsManager(this);
            bStatsManager.initialize();
            
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
            
            // 关闭 BStats 统计
            if (bStatsManager != null) {
                bStatsManager.shutdown();
            }
            
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
            return getServer().getPluginManager().getPlugin("floodgate") != null;
        } catch (Exception e) {
            getLogger().severe("检查依赖时发生错误: " + e.getMessage());
            return false;
        }
    }
    
    private boolean checkConfig() {
        try {
            // 检查配置文件版本
            if (!getConfig().isString("settings.default-menu")) {
                getLogger().warning("配置文件缺少必要设置");
                return false;
            }
            
            // 检查消息文件
            File messagesFile = new File(getDataFolder(), "messages.yml");
            if (!messagesFile.exists()) {
                getLogger().warning("消息配置文件不存在");
                return false;
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
        // 先重载主配置
        super.reloadConfig();
        
        // 重新加载消息配置
        reloadMessages();
        
        // 如果菜单管理器已初始化，则重新加载菜单
        if (menuManager != null) {
            menuManager.loadMenus();
        }
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
    
    public BStatsManager getBStatsManager() {
        return bStatsManager;
    }
    
    public String getPrefix() {
        try {
            return messages.getString("prefix", "§6[GeyserMenu] §f");
        } catch (Exception e) {
            getLogger().warning("获取前缀时出错");
            return "§6[GeyserMenu] §f";
        }
    }
    
    private void createDirectories() {
        // 创建菜单目录
        File menuDir = new File(getDataFolder(), "menus");
        if (!menuDir.exists()) {
            menuDir.mkdirs();
        }
        
        // 创建图标目录
        File iconDir = new File(getDataFolder(), "icons");
        if (!iconDir.exists()) {
            iconDir.mkdirs();
        }
    }
    
    private void checkUpdate() {
        if (getConfig().getBoolean("settings.debug", false)) {
            getLogger().info(getMessage("update.console.checking"));
        }
        
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                URL url = new URL(UPDATE_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() == 200) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()))) {
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }

                        // 解析JSON响应
                        JSONObject json = new JSONObject(response.toString());
                        String tagName = json.getString("tag_name");
                        if (tagName.startsWith("v")) {
                            latestVersion = tagName.substring(1);
                            String currentVersion = getDescription().getVersion();
                            
                            if (!currentVersion.equals(latestVersion)) {
                                updateAvailable = true;
                                getLogger().info(getMessage("update.console.found", latestVersion));
                                getLogger().info(getMessage("update.console.download", 
                                    json.getString("html_url")));
                            } else if (getConfig().getBoolean("settings.debug", false)) {
                                getLogger().info(getMessage("update.console.up-to-date"));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                if (getConfig().getBoolean("settings.debug", false)) {
                    getLogger().warning(getMessage("update.console.failed", e.getMessage()));
                }
            }
        });
    }
    
    // 添加 getter 方法
    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }
}
