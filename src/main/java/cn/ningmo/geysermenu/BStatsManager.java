package cn.ningmo.geysermenu;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BStats 统计管理器
 * 负责收集和发送插件使用统计数据
 * 
 * @author 柠枺
 * @version 1.1.10
 */
public class BStatsManager {
    
    private final GeyserMenu plugin;
    private Metrics metrics;
    
    // BStats 插件 ID
    private static final int PLUGIN_ID = 26736;

    // 图表统计值使用固定英文常量，避免不同语言服务器的统计值碎片化（文案与 messages_en.yml 保持一致）
    private static final String LABEL_PAPI_CACHE = "PAPI Cache";
    private static final String LABEL_COMMAND_SECURITY = "Command Security";
    private static final String LABEL_UPDATE_CHECK = "Update Check";
    private static final String LABEL_DEBUG_MODE = "Debug Mode";
    private static final String LABEL_PLACEHOLDERAPI = "PlaceholderAPI";
    private static final String LABEL_MAIN_MENU = "Main Menu";
    private static final String LABEL_TELEPORT_MENU = "Teleport Menu";
    private static final String LABEL_SHOP_MENU = "Shop Menu";
    private static final String LABEL_HIGH_PERFORMANCE = "High Performance";
    private static final String LABEL_MEDIUM_PERFORMANCE = "Medium Performance";
    private static final String LABEL_DEFAULT_PERFORMANCE = "Default Performance";

    // 服务器版本：提取 1.x 主版本号（如 1.21.4-R0.1-SNAPSHOT -> 1.21）
    private static final Pattern SERVER_MAJOR_PATTERN = Pattern.compile("1\\.\\d+");
    // Java 版本：提取主版本号，兼容 1.8 与 9+ 两种格式
    private static final Pattern JAVA_MAJOR_PATTERN = Pattern.compile("^(?:1\\.(\\d+)|(\\d+)).*$");
    
    public BStatsManager(GeyserMenu plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 初始化 BStats 统计
     */
    public void initialize() {
        try {
            // 检查是否启用统计
            if (!plugin.getConfig().getBoolean("settings.statistics.enable-bstats", true)) {
                plugin.getLogger().info(plugin.getRawMessage("statistics.console.disabled"));
                return;
            }
            
            // 初始化 Metrics
            metrics = new Metrics(plugin, PLUGIN_ID);
            
            // 添加自定义统计图表
            addCustomCharts();
            
            plugin.getLogger().info(plugin.getRawMessage("statistics.console.enabled"));
            
        } catch (Exception e) {
            plugin.getLogger().warning(plugin.getMessage("statistics.console.init-error", e.getMessage()));
            if (plugin.getConfig().getBoolean("settings.debug", false)) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 添加自定义统计图表
     */
    private void addCustomCharts() {
        if (!plugin.getConfig().getBoolean("settings.statistics.collect-custom-data", true)) {
            return;
        }
        
        // 服务器版本统计
        metrics.addCustomChart(new SimplePie("server_version", () -> {
            // 通用解析：提取 1.x 主版本号作为值，解析失败归入 Other
            Matcher matcher = SERVER_MAJOR_PATTERN.matcher(Bukkit.getVersion());
            return matcher.find() ? matcher.group() : "Other";
        }));
        
        // Java 版本统计
        metrics.addCustomChart(new SimplePie("java_version", () -> {
            // 通用解析：提取主版本号，解析失败归入 Other
            Matcher matcher = JAVA_MAJOR_PATTERN.matcher(System.getProperty("java.version", ""));
            if (matcher.matches()) {
                String major = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
                return "Java " + major;
            }
            return "Other";
        }));
        
        // 在线玩家数量统计
        metrics.addCustomChart(new SingleLineChart("players", () -> 
            Bukkit.getOnlinePlayers().size()
        ));
        
        // 菜单数量统计
        metrics.addCustomChart(new SimplePie("menu_count", () -> {
            int menuCount = plugin.getMenuManager().getMenuList().size();
            if (menuCount <= 5) return "1-5";
            if (menuCount <= 10) return "6-10";
            if (menuCount <= 20) return "11-20";
            return "20+";
        }));
        
        // 启用的功能统计
        metrics.addCustomChart(new AdvancedPie("enabled_features", () -> {
            Map<String, Integer> valueMap = new HashMap<>();
            
            if (plugin.getConfig().getBoolean("settings.performance.cache-placeholders", false)) {
                valueMap.put(LABEL_PAPI_CACHE, 1);
            }
            
            if (plugin.getConfig().getBoolean("settings.enable-command-security", true)) {
                valueMap.put(LABEL_COMMAND_SECURITY, 1);
            }
            
            if (plugin.getConfig().getBoolean("settings.check-updates", true)) {
                valueMap.put(LABEL_UPDATE_CHECK, 1);
            }
            
            if (plugin.getConfig().getBoolean("settings.debug", false)) {
                valueMap.put(LABEL_DEBUG_MODE, 1);
            }
            
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                valueMap.put(LABEL_PLACEHOLDERAPI, 1);
            }
            
            return valueMap;
        }));
        
        // 服务器软件统计
        metrics.addCustomChart(new SimplePie("server_software", () -> {
            String serverName = Bukkit.getName().toLowerCase();
            if (serverName.contains("paper")) return "Paper";
            if (serverName.contains("spigot")) return "Spigot";
            if (serverName.contains("bukkit")) return "Bukkit";
            if (serverName.contains("purpur")) return "Purpur";
            if (serverName.contains("tuinity")) return "Tuinity";
            return "Other";
        }));
        
        // 配置的菜单类型统计
        metrics.addCustomChart(new AdvancedPie("menu_types", () -> {
            Map<String, Integer> valueMap = new HashMap<>();
            
            if (plugin.getConfig().getBoolean("menus.main.enable", true)) {
                valueMap.put(LABEL_MAIN_MENU, 1);
            }
            
            if (plugin.getConfig().getBoolean("menus.teleport.enable", true)) {
                valueMap.put(LABEL_TELEPORT_MENU, 1);
            }
            
            if (plugin.getConfig().getBoolean("menus.shop.enable", true)) {
                valueMap.put(LABEL_SHOP_MENU, 1);
            }
            
            return valueMap;
        }));
        
        // 性能设置统计
        metrics.addCustomChart(new SimplePie("performance_settings", () -> {
            int commandDelay = plugin.getConfig().getInt("settings.performance.command-delay", 0);
            boolean cacheEnabled = plugin.getConfig().getBoolean("settings.performance.cache-placeholders", false);
            
            if (cacheEnabled && commandDelay > 0) return LABEL_HIGH_PERFORMANCE;
            if (cacheEnabled || commandDelay > 0) return LABEL_MEDIUM_PERFORMANCE;
            return LABEL_DEFAULT_PERFORMANCE;
        }));
    }
    
    /**
     * 关闭 BStats 统计
     */
    public void shutdown() {
        if (metrics != null) {
            try {
                // 先停止 bStats 的上报任务，再释放引用，避免插件关闭后任务泄漏
                metrics.shutdown();
                metrics = null;
                plugin.getLogger().info(plugin.getRawMessage("statistics.console.shutdown"));
            } catch (Exception e) {
                plugin.getLogger().warning(plugin.getMessage("statistics.console.shutdown-error", e.getMessage()));
            }
        }
    }
}