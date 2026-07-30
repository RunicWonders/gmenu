package cn.ningmo.geysermenu;

import cn.ningmo.geysermenu.listeners.PlayerListener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.Bukkit;
import java.net.HttpURLConnection;
import java.net.URI;
import java.io.BufferedReader;
import org.json.JSONObject;

public class GeyserMenu extends JavaPlugin {
    private static GeyserMenu instance;
    private MenuManager menuManager;
    private YamlConfiguration messages;
    private YamlConfiguration defaultMessages;
    private BStatsManager bStatsManager;
    private PermissionManager permissionManager;

    // 添加更新检查相关字段
    private static final String UPDATE_URL = "https://api.github.com/repos/RunicWonders/gmenu/releases/latest";
    private volatile boolean updateAvailable = false;
    private volatile String latestVersion = null;

    @Override
    public void onEnable() {
        try {
            // 防止重复初始化（需在资源保存之前检查）
            if (instance != null) {
                getLogger().warning(getLogMessage("plugin.load.duplicate"));
                return;
            }
            instance = this;

            // 首先保存资源，因为 reloadMessages 需要它们
            saveResourceSafely("messages.yml");
            saveResourceSafely("messages_en.yml");

            // 立即加载消息，以便后续可以使用 getLogMessage
            reloadMessages();

            // 保存默认配置后再读取语言和迁移配置，确保 getConfig() 使用最新文件
            saveDefaultConfig();
            ConfigMigrator migrator = new ConfigMigrator(this);
            migrator.migrate();
            super.reloadConfig();

            saveResourceSafely("menus/menu.yml");
            saveResourceSafely("menus/shop.yml");
            saveResourceSafely("menus/teleport.yml");
            saveResourceSafely("menus/confirm.yml");
            saveResourceSafely("menus/settings.yml");

            createDirectories();
            reloadMessages();
            migrateMessages();

            // 检查更新
            if (getConfig().getBoolean("settings.check-updates", true)) {
                checkUpdate();
            }

            // 初始化权限管理器
            permissionManager = new PermissionManager(this);

            // 初始化菜单管理器（构造函数内部会加载菜单）
            menuManager = new MenuManager(this);

            // 注册命令
            if (getCommand("geysermenu") != null) {
                getCommand("geysermenu").setExecutor(new MenuCommand(this));
            } else {
                getLogger().severe(getLogMessage("plugin.command.register-failed"));
                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            // 注册事件监听器（进服更新通知）
            getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

            // 初始化 BStats 统计
            bStatsManager = new BStatsManager(this);
            bStatsManager.initialize();

            getLogger().info(getLogMessage("plugin.load.success", getPluginMeta().getVersion()));
        } catch (Exception e) {
            getLogger().severe(getLogMessage("plugin.load.error", e.getMessage()));
            e.printStackTrace();
            instance = null;
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {
            // 取消所有任务
            Bukkit.getScheduler().cancelTasks(this);

            // 关闭 BStats 统计
            if (bStatsManager != null) {
                bStatsManager.shutdown();
            }

            // 清除实例
            instance = null;

            getLogger().info(getLogMessage("plugin.disable.success"));
        } catch (Exception e) {
            getLogger().severe(getLogMessage("plugin.disable.error", e.getMessage()));
            e.printStackTrace();
        }
    }

    public void reloadMessages() {
        try {
            String language = getConfig().getString("settings.language", "zh_cn");
            String messagesFileName = "messages.yml";
            String resourceFileName = "messages.yml";

            if ("en".equalsIgnoreCase(language)) {
                messagesFileName = "messages_en.yml";
                resourceFileName = "messages_en.yml";
            }

            // 加载默认配置作为回退
            InputStream resourceStream = getResource(resourceFileName);
            if (resourceStream != null) {
                defaultMessages = YamlConfiguration.loadConfiguration(new InputStreamReader(resourceStream, StandardCharsets.UTF_8));
            }

            File messagesFile = new File(getDataFolder(), messagesFileName);
            if (!messagesFile.exists()) {
                saveResourceSafely(resourceFileName);
                if (!messagesFile.exists() && !messagesFileName.equals("messages.yml")) {
                    messagesFile = new File(getDataFolder(), "messages.yml");
                    if (!messagesFile.exists()) {
                        saveResourceSafely("messages.yml");
                    }
                }
            }
            messages = YamlConfiguration.loadConfiguration(messagesFile);
        } catch (Exception e) {
            getLogger().severe(getLogMessage("config.load-error", e.getMessage()));
            e.printStackTrace();
        }
    }

    private void saveResourceSafely(String resourcePath) {
        File file = new File(getDataFolder(), resourcePath);
        if (!file.exists()) {
            try {
                saveResource(resourcePath, false);
            } catch (Exception e) {
                getLogger().warning("保存资源文件失败: " + resourcePath + ", 错误: " + e.getMessage());
            }
        }
    }

    public void migrateMessages() {
        if (messages == null || defaultMessages == null) return;

        boolean changed = false;
        for (String key : defaultMessages.getKeys(true)) {
            if (!messages.contains(key)) {
                messages.set(key, defaultMessages.get(key));
                changed = true;
            }
        }

        if (changed) {
            try {
                String language = getConfig().getString("settings.language", "zh_cn");
                String messagesFileName = "en".equalsIgnoreCase(language) ? "messages_en.yml" : "messages.yml";
                File messagesFile = new File(getDataFolder(), messagesFileName);
                messages.save(messagesFile);
                getLogger().info("已自动迁移并补充缺失的消息配置。");
            } catch (Exception e) {
                getLogger().warning("保存迁移后的消息配置失败: " + e.getMessage());
            }
        }
    }

    public String getMessage(String path, String... args) {
        if (messages == null && defaultMessages == null) {
            return "§6[GeyserMenu] §f" + path;
        }
        try {
            String message = messages != null ? messages.getString(path) : null;
            if (message == null && defaultMessages != null) {
                message = defaultMessages.getString(path);
            }

            if (message == null) {
                getLogger().warning(getLogMessage("message.not-found", path));
                return getPrefix() + getLogMessage("message.not-configured", path);
            }

            StringBuilder result = new StringBuilder(getPrefix());
            String formattedMessage = message;

            for (int i = 0; i < args.length; i++) {
                formattedMessage = formattedMessage.replace("{" + i + "}", args[i] != null ? args[i] : "null");
            }

            result.append(formattedMessage);
            return result.toString();
        } catch (Exception e) {
            getLogger().warning("获取消息时发生错误: " + path + ", 错误: " + e.getMessage());
            return getLogMessage("message.process-error", path);
        }
    }

    public String getRawMessage(String path) {
        if (messages == null && defaultMessages == null) {
            return path;
        }
        try {
            String message = messages != null ? messages.getString(path) : null;
            if (message == null && defaultMessages != null) {
                message = defaultMessages.getString(path);
            }
            return message != null ? message : path;
        } catch (Exception e) {
            getLogger().warning(getLogMessage("message.raw-error", path) + ", 错误: " + e.getMessage());
            return path;
        }
    }

    public String getLogMessage(String path, String... args) {
        if (messages == null && defaultMessages == null) {
            return "消息系统未就绪: " + path;
        }
        try {
            String message = messages != null ? messages.getString(path) : null;
            if (message == null && defaultMessages != null) {
                message = defaultMessages.getString(path);
            }

            if (message == null) {
                getLogger().warning("找不到消息配置: " + path);
                return "消息未配置: " + path;
            }

            String formattedMessage = message;
            for (int i = 0; i < args.length; i++) {
                formattedMessage = formattedMessage.replace("{" + i + "}", args[i] != null ? args[i] : "null");
            }

            return formattedMessage;
        } catch (Exception e) {
            getLogger().warning("获取日志消息时发生错误: " + path + ", 错误: " + e.getMessage());
            return "日志消息处理错误: " + path;
        }
    }

    public String getPermissionDescription(String key) {
        if (messages == null && defaultMessages == null) {
            return key;
        }
        String message = messages != null ? messages.getString("permission." + key) : null;
        if (message == null && defaultMessages != null) {
            message = defaultMessages.getString("permission." + key);
        }
        return message != null ? message : key;
    }

    @Override
    public void reloadConfig() {
        // 先重载主配置
        super.reloadConfig();

        // 重新加载消息配置
        reloadMessages();

        // 按配置决定是否清理占位符缓存，再加载菜单
        if (menuManager != null) {
            menuManager.clearPlaceholderCache(getConfig().getBoolean(
                "settings.performance.clear-cache-on-reload", true));
            menuManager.loadMenus();
        }

        // 消息迁移必须在 reloadMessages 后执行，避免重复加载消息文件。
        migrateMessages();
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
            throw new IllegalStateException(getLogMessage("instance.manager-not-init", "菜单管理器"));
        }
        return menuManager;
    }

    public BStatsManager getBStatsManager() {
        return bStatsManager;
    }

    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    public String getPrefix() {
        if (messages == null && defaultMessages == null) {
            return "§6[GeyserMenu] §f";
        }
        try {
            String prefix = messages != null ? messages.getString("prefix") : null;
            if (prefix == null && defaultMessages != null) {
                prefix = defaultMessages.getString("prefix");
            }
            return prefix != null ? prefix : "§6[GeyserMenu] §f";
        } catch (Exception e) {
            getLogger().warning(getLogMessage("message.prefix-error") + ": " + e.getMessage());
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
            HttpURLConnection conn = null;
            try {
                URI uri = new URI(UPDATE_URL);
                conn = (HttpURLConnection) uri.toURL().openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() == 200) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }

                        // 解析JSON响应
                        JSONObject json = new JSONObject(response.toString());
                        String tagName = json.getString("tag_name");
                        if (!tagName.startsWith("v") && getConfig().getBoolean("settings.debug", false)) {
                            getLogger().info("远程版本标签不含 v 前缀: " + tagName);
                        }
                        // 去掉 v 前缀后按语义化版本比较
                        latestVersion = tagName.startsWith("v") ? tagName.substring(1) : tagName;
                        String currentVersion = getPluginMeta().getVersion();

                        if (isNewerVersion(currentVersion, latestVersion)) {
                            updateAvailable = true;
                            getLogger().info(getMessage("update.console.found", latestVersion));
                            getLogger().info(getMessage("update.console.download",
                                json.getString("html_url")));
                        } else if (getConfig().getBoolean("settings.debug", false)) {
                            getLogger().info(getMessage("update.console.up-to-date"));
                        }
                    }
                }
            } catch (Exception e) {
                if (getConfig().getBoolean("settings.debug", false)) {
                    getLogger().warning(getMessage("update.console.failed", e.getMessage()));
                }
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    // 语义化版本比较：仅当远程版本高于当前版本时返回 true
    private boolean isNewerVersion(String currentVersion, String remoteVersion) {
        return compareVersions(remoteVersion, currentVersion) > 0;
    }

    private int compareVersions(String left, String right) {
        String[] leftParts = left.split("\\+", 2)[0].split("-", 2);
        String[] rightParts = right.split("\\+", 2)[0].split("-", 2);
        String[] leftNumbers = leftParts[0].split("\\.");
        String[] rightNumbers = rightParts[0].split("\\.");
        int length = Math.max(leftNumbers.length, rightNumbers.length);
        for (int i = 0; i < length; i++) {
            int leftNumber = i < leftNumbers.length ? parseVersionPart(leftNumbers[i]) : 0;
            int rightNumber = i < rightNumbers.length ? parseVersionPart(rightNumbers[i]) : 0;
            if (leftNumber != rightNumber) {
                return Integer.compare(leftNumber, rightNumber);
            }
        }
        // 正式版高于同版本任意预发布版；预发布标识按数字和字典序比较。
        if (leftParts.length == 1 || rightParts.length == 1) {
            return Integer.compare(leftParts.length, rightParts.length);
        }
        return comparePreRelease(leftParts[1], rightParts[1]);
    }

    private int comparePreRelease(String left, String right) {
        String[] leftParts = left.split("\\.");
        String[] rightParts = right.split("\\.");
        int length = Math.max(leftParts.length, rightParts.length);
        for (int i = 0; i < length; i++) {
            if (i >= leftParts.length) return -1;
            if (i >= rightParts.length) return 1;
            String leftPart = leftParts[i];
            String rightPart = rightParts[i];
            boolean leftNumeric = leftPart.matches("\\d+");
            boolean rightNumeric = rightPart.matches("\\d+");
            if (leftNumeric && rightNumeric) {
                int comparison = Integer.compare(Integer.parseInt(leftPart), Integer.parseInt(rightPart));
                if (comparison != 0) return comparison;
            } else if (leftNumeric != rightNumeric) {
                return leftNumeric ? -1 : 1;
            } else {
                int comparison = leftPart.compareTo(rightPart);
                if (comparison != 0) return comparison;
            }
        }
        return 0;
    }

    // 解析版本号单个字段，取开头的数字部分（兼容异常版本字段）
    private int parseVersionPart(String part) {
        int end = 0;
        while (end < part.length() && Character.isDigit(part.charAt(end))) {
            end++;
        }
        if (end == 0) {
            return 0;
        }
        try {
            return Integer.parseInt(part.substring(0, end));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // 添加 getter 方法
    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }
}
