package cn.ningmo.geysermenu;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

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
            String version = Bukkit.getVersion();
            if (version.contains("1.21")) return "1.21.x";
            if (version.contains("1.20")) return "1.20.x";
            if (version.contains("1.19")) return "1.19.x";
            if (version.contains("1.18")) return "1.18.x";
            return "Other";
        }));
        
        // Java 版本统计
        metrics.addCustomChart(new SimplePie("java_version", () -> {
            String javaVersion = System.getProperty("java.version");
            if (javaVersion.startsWith("21")) return "Java 21";
            if (javaVersion.startsWith("17")) return "Java 17";
            if (javaVersion.startsWith("11")) return "Java 11";
            if (javaVersion.startsWith("8")) return "Java 8";
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
                valueMap.put("PAPI缓存", 1);
            }
            
            if (plugin.getConfig().getBoolean("settings.enable-command-security", true)) {
                valueMap.put("命令安全检查", 1);
            }
            
            if (plugin.getConfig().getBoolean("settings.check-updates", true)) {
                valueMap.put("更新检查", 1);
            }
            
            if (plugin.getConfig().getBoolean("settings.debug", false)) {
                valueMap.put("调试模式", 1);
            }
            
            // 检查是否安装了 PlaceholderAPI
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                valueMap.put("PlaceholderAPI", 1);
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
            
            // 统计启用的菜单类型
            if (plugin.getConfig().getBoolean("menus.main.enable", true)) {
                valueMap.put("主菜单", 1);
            }
            
            if (plugin.getConfig().getBoolean("menus.teleport.enable", true)) {
                valueMap.put("传送菜单", 1);
            }
            
            if (plugin.getConfig().getBoolean("menus.shop.enable", true)) {
                valueMap.put("商店菜单", 1);
            }
            
            return valueMap;
        }));
        
        // 性能设置统计
        metrics.addCustomChart(new SimplePie("performance_settings", () -> {
            int commandDelay = plugin.getConfig().getInt("settings.performance.command-delay", 0);
            boolean cacheEnabled = plugin.getConfig().getBoolean("settings.performance.cache-placeholders", false);
            
            if (cacheEnabled && commandDelay > 0) return "高性能配置";
            if (cacheEnabled || commandDelay > 0) return "中等性能配置";
            return "默认配置";
        }));
    }
    
    /**
     * 获取 Metrics 实例
     * @return Metrics 实例，如果未初始化则返回 null
     */
    public Metrics getMetrics() {
        return metrics;
    }
    
    /**
     * 检查 BStats 是否已启用
     * @return 如果已启用返回 true
     */
    public boolean isEnabled() {
        return metrics != null;
    }
    
    /**
     * 关闭 BStats 统计
     */
    public void shutdown() {
        if (metrics != null) {
            try {
                // BStats 会自动处理关闭逻辑
                metrics = null;
                plugin.getLogger().info(plugin.getRawMessage("statistics.console.shutdown"));
            } catch (Exception e) {
                plugin.getLogger().warning(plugin.getMessage("statistics.console.shutdown-error", e.getMessage()));
            }
        }
    }
}