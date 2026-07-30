package cn.ningmo.geysermenu;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;

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
        if (!backupDir.exists() && !backupDir.mkdirs() && !backupDir.isDirectory()) {
            plugin.getLogger().warning(plugin.getLogMessage("migration.backup-failed", "无法创建备份目录: " + backupDir.getPath()));
            return false;
        }
        
        File backupFile = new File(backupDir, "config.yml.v" + version);
        try {
                // 备份文件可能已存在（如上次迁移失败重试），直接覆盖避免迁移卡死
                Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                plugin.getLogger().info(plugin.getLogMessage("migration.backup-success", backupFile.getPath()));
        } catch (IOException e) {
                plugin.getLogger().warning(plugin.getLogMessage("migration.backup-failed", e.getMessage()));
                return false;
            }
        
        try {
            YamlConfiguration newConfigFile = new YamlConfiguration();

            // 旧值全保留：遍历旧配置所有叶子路径原样复制，跳过 ConfigurationSection 中间节点
            for (Map.Entry<String, Object> entry : config.getValues(true).entrySet()) {
                if (!(entry.getValue() instanceof ConfigurationSection)) {
                    newConfigFile.set(entry.getKey(), entry.getValue());
                }
            }

            // 兼容旧版顶层 performance，迁移到 settings.performance；新路径已有值时保留新值。
            for (Map.Entry<String, Object> entry : config.getValues(true).entrySet()) {
                if (entry.getKey().startsWith("performance.")
                        && !newConfigFile.contains("settings." + entry.getKey())) {
                    newConfigFile.set("settings." + entry.getKey(), entry.getValue());
                }
            }

            // 用 jar 内默认 config.yml 补齐缺失的新键（旧值优先，不覆盖已有键）
            try (InputStream in = plugin.getResource("config.yml")) {
                if (in != null) {
                    YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(in, StandardCharsets.UTF_8));
                    for (Map.Entry<String, Object> entry : defaultConfig.getValues(true).entrySet()) {
                        if (!(entry.getValue() instanceof ConfigurationSection)
                                && !newConfigFile.contains(entry.getKey())) {
                            newConfigFile.set(entry.getKey(), entry.getValue());
                        }
                    }
                }
            }

            newConfigFile.set("config-version", CURRENT_CONFIG_VERSION);

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
    
    public int getCurrentVersion() {
        return CURRENT_CONFIG_VERSION;
    }
}
