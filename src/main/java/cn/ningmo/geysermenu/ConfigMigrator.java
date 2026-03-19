package cn.ningmo.geysermenu;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ConfigMigrator {
    private final GeyserMenu plugin;
    private static final int CURRENT_CONFIG_VERSION = 3;
    
    public ConfigMigrator(GeyserMenu plugin) {
        this.plugin = plugin;
    }
    
    public boolean migrate() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            return false;
        }
        
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        int version = config.getInt("config-version", 1);
        
        if (version >= CURRENT_CONFIG_VERSION) {
            return false;
        }
        
        plugin.getLogger().info(plugin.getLogMessage("migration.start", String.valueOf(version)));
        
        File backupDir = new File(plugin.getDataFolder(), "backup");
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
        
        File backupFile = new File(backupDir, "config.yml.v" + version);
        try {
                Files.copy(configFile.toPath(), backupFile.toPath());
                plugin.getLogger().info(plugin.getLogMessage("migration.backup-success", backupFile.getPath()));
        } catch (IOException e) {
                plugin.getLogger().warning(plugin.getLogMessage("migration.backup-failed", e.getMessage()));
                return false;
            }
        
        try {
            Map<String, Object> oldConfig = config.getValues(true);
            Map<String, Object> newConfig = new HashMap<>();
            
            migrateSettings(oldConfig, newConfig);
            migratePerformance(oldConfig, newConfig);
            migrateMenus(oldConfig, newConfig);
            migrateIcons(oldConfig, newConfig);
            migrateSecurity(oldConfig, newConfig);
            
            newConfig.put("config-version", CURRENT_CONFIG_VERSION);
            
            YamlConfiguration newConfigFile = new YamlConfiguration();
            for (Map.Entry<String, Object> entry : newConfig.entrySet()) {
                newConfigFile.set(entry.getKey(), entry.getValue());
            }
            
            newConfigFile.save(configFile);
            plugin.getLogger().info(plugin.getLogMessage("migration.complete", 
                String.valueOf(version), String.valueOf(CURRENT_CONFIG_VERSION)));
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe(plugin.getLogMessage("migration.error", e.getMessage()));
            e.printStackTrace();
            return false;
        }
    }
    
    private void migrateSettings(Map<String, Object> oldConfig, Map<String, Object> newConfig) {
        if (oldConfig.containsKey("settings")) {
            Map<String, Object> settings = new HashMap<>();
            Object settingsObj = oldConfig.get("settings");
            if (settingsObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> oldSettings = (Map<String, Object>) settingsObj;
                
                settings.put("default-menu", oldSettings.getOrDefault("default-menu", "menu.yml"));
                settings.put("debug", oldSettings.getOrDefault("debug", false));
                settings.put("check-updates", oldSettings.getOrDefault("check-updates", true));
                settings.put("enable-command-security", oldSettings.getOrDefault("enable-command-security", true));
                
                Map<String, Object> performance = new HashMap<>();
                if (oldSettings.containsKey("performance")) {
                    Object perfObj = oldSettings.get("performance");
                    if (perfObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> oldPerf = (Map<String, Object>) perfObj;
                        performance.put("command-delay", oldPerf.getOrDefault("command-delay", 0));
                        performance.put("cache-placeholders", oldPerf.getOrDefault("cache-placeholders", false));
                        performance.put("cache-refresh", oldPerf.getOrDefault("cache-refresh", 30));
                        performance.put("max-cache-size", oldPerf.getOrDefault("max-cache-size", 1000));
                        performance.put("clear-cache-on-reload", oldPerf.getOrDefault("clear-cache-on-reload", true));
                        performance.put("form-cooldown", oldPerf.getOrDefault("form-cooldown", 500));
                        performance.put("log-form-cooldown", oldPerf.getOrDefault("log-form-cooldown", false));
                    } else {
                        performance.put("command-delay", 0);
                        performance.put("cache-placeholders", false);
                        performance.put("cache-refresh", 30);
                        performance.put("max-cache-size", 1000);
                        performance.put("clear-cache-on-reload", true);
                        performance.put("form-cooldown", 500);
                        performance.put("log-form-cooldown", false);
                    }
                } else {
                    performance.put("command-delay", 0);
                    performance.put("cache-placeholders", false);
                    performance.put("cache-refresh", 30);
                    performance.put("max-cache-size", 1000);
                    performance.put("clear-cache-on-reload", true);
                    performance.put("form-cooldown", 500);
                    performance.put("log-form-cooldown", false);
                }
                settings.put("performance", performance);
                
                Map<String, Object> update = new HashMap<>();
                if (oldSettings.containsKey("update")) {
                    Object updateObj = oldSettings.get("update");
                    if (updateObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> oldUpdate = (Map<String, Object>) updateObj;
                        update.put("notify-on-join", oldUpdate.getOrDefault("notify-on-join", true));
                        update.put("notify-ops-only", oldUpdate.getOrDefault("notify-ops-only", false));
                    } else {
                        update.put("notify-on-join", true);
                        update.put("notify-ops-only", false);
                    }
                } else {
                    update.put("notify-on-join", true);
                    update.put("notify-ops-only", false);
                }
                settings.put("update", update);
                
                Map<String, Object> statistics = new HashMap<>();
                if (oldSettings.containsKey("statistics")) {
                    Object statsObj = oldSettings.get("statistics");
                    if (statsObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> oldStats = (Map<String, Object>) statsObj;
                        statistics.put("enable-bstats", oldStats.getOrDefault("enable-bstats", true));
                        statistics.put("collect-custom-data", oldStats.getOrDefault("collect-custom-data", true));
                    } else {
                        statistics.put("enable-bstats", true);
                        statistics.put("collect-custom-data", true);
                    }
                } else {
                    statistics.put("enable-bstats", true);
                    statistics.put("collect-custom-data", true);
                }
                settings.put("statistics", statistics);
            }
            newConfig.put("settings", settings);
        } else {
            Map<String, Object> settings = new HashMap<>();
            settings.put("default-menu", "menu.yml");
            settings.put("debug", false);
            settings.put("check-updates", true);
            settings.put("enable-command-security", true);
            
            Map<String, Object> performance = new HashMap<>();
            performance.put("command-delay", 0);
            performance.put("cache-placeholders", false);
            performance.put("cache-refresh", 30);
            performance.put("max-cache-size", 1000);
            performance.put("clear-cache-on-reload", true);
            performance.put("form-cooldown", 500);
            performance.put("log-form-cooldown", false);
            settings.put("performance", performance);
            
            Map<String, Object> update = new HashMap<>();
            update.put("notify-on-join", true);
            update.put("notify-ops-only", false);
            settings.put("update", update);
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("enable-bstats", true);
            statistics.put("collect-custom-data", true);
            settings.put("statistics", statistics);
            
            newConfig.put("settings", settings);
        }
    }
    
    private void migratePerformance(Map<String, Object> oldConfig, Map<String, Object> newConfig) {
    if (oldConfig.containsKey("performance")) {
            newConfig.put("performance", oldConfig.get("performance"));
        }
    }
    
    private void migrateMenus(Map<String, Object> oldConfig, Map<String, Object> newConfig) {
        if (oldConfig.containsKey("menus")) {
            newConfig.put("menus", oldConfig.get("menus"));
        }
    }
    
    private void migrateIcons(Map<String, Object> oldConfig, Map<String, Object> newConfig) {
        if (oldConfig.containsKey("icons")) {
            newConfig.put("icons", oldConfig.get("icons"));
        }
    }
    
    private void migrateSecurity(Map<String, Object> oldConfig, Map<String, Object> newConfig) {
        if (oldConfig.containsKey("security")) {
            newConfig.put("security", oldConfig.get("security"));
        }
    }
    
    public int getCurrentVersion() {
        return CURRENT_CONFIG_VERSION;
    }
}
