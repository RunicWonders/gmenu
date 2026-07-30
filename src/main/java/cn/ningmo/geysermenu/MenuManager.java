package cn.ningmo.geysermenu;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.SimpleForm;
import org.geysermc.cumulus.ModalForm;
import org.geysermc.cumulus.CustomForm;
import org.geysermc.floodgate.api.FloodgateApi;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.bukkit.configuration.ConfigurationSection;
import java.util.List;
import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.geysermc.cumulus.util.FormImage;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

public class MenuManager {
    private final GeyserMenu plugin;
    private final Map<String, YamlConfiguration> menus;
    // 菜单文件名 -> config.yml 中的菜单键，用于权限校验
    private final Map<String, String> menuKeyByFile;
    private final Map<String, String> placeholderCache;
    private volatile long lastCacheRefresh;
    private final Map<UUID, Long> formCooldowns = new ConcurrentHashMap<>();

    public MenuManager(GeyserMenu plugin) {
        this.plugin = plugin;
        this.menus = new ConcurrentHashMap<>();
        this.menuKeyByFile = new ConcurrentHashMap<>();
        this.placeholderCache = new ConcurrentHashMap<>();
        this.lastCacheRefresh = System.currentTimeMillis();
        loadMenus();
    }

    public void loadMenus() {
        try {
            File menuFolder = new File(plugin.getDataFolder(), "menus");
            if (!menuFolder.exists()) {
                menuFolder.mkdirs();
            }

            menus.clear();
            menuKeyByFile.clear();

            ConfigurationSection section = plugin.getConfig().getConfigurationSection("menus");
            if (section != null) {
                section.getKeys(false).forEach(menuKey -> {
                    try {
                        ConfigurationSection menu = section.getConfigurationSection(menuKey);
                        if (menu == null || !menu.getBoolean("enable", true)) return;

                        String fileName = menu.getString("file");
                        if (fileName == null) {
                            plugin.getLogger().warning(plugin.getLogMessage("menu.load.missing-config", menuKey));
                            return;
                        }
                        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                            plugin.getLogger().warning(plugin.getLogMessage("menu.load.unsafe-filename", fileName));
                        } else {
                            File menuFile = new File(menuFolder, fileName);
                            if (menuFile.exists()) {
                                menus.put(fileName, YamlConfiguration.loadConfiguration(menuFile));
                                menuKeyByFile.put(fileName, menuKey);
                                if (plugin.getConfig().getBoolean("settings.debug")) {
                                    plugin.getLogger().info(plugin.getLogMessage("menu.load.success", fileName));
                                }
                            } else {
                                plugin.getLogger().warning(plugin.getLogMessage("menu.load.missing-file", fileName));
                            }
                        }
                    } catch (Exception e) {
                        // 单个菜单加载失败只记警告，继续加载其余菜单
                        plugin.getLogger().warning(plugin.getLogMessage("menu.load.read-error", menuKey));
                    }
                });
            }
        } catch (Exception e) {
            plugin.getLogger().severe(plugin.getLogMessage("menu.load.error", e.getMessage()));
            e.printStackTrace();
        }
    }

    /**
     * 为玩家打开指定菜单
     * @return 表单真正发送成功返回 true；被权限/冷却/非基岩玩家/菜单不存在等拦截返回 false
     */
    public boolean openMenu(Player player, String menuName) {
        // player 判空必须在任何 player 方法调用之前
        if (player == null || menuName == null) {
            plugin.getLogger().warning(plugin.getLogMessage("menu.open.invalid-params",
                player != null ? player.getName() : "null", menuName != null ? menuName : "null"));
            return false;
        }

        long now = System.currentTimeMillis();
        long formCooldown = Math.max(0L, plugin.getConfig().getLong(
            "settings.performance.form-cooldown", 500L));

        try {
            Long lastOpen = formCooldowns.get(player.getUniqueId());
            if (lastOpen != null && now - lastOpen < formCooldown) {
                if (plugin.getConfig().getBoolean("settings.debug")) {
                    plugin.getLogger().info(plugin.getLogMessage("menu.open.cooldown", player.getName()));
                }
                player.sendMessage(plugin.getMessage("error.form-cooldown"));
                return false;
            }

            FloodgateApi floodgateApi = FloodgateApi.getInstance();
            if (floodgateApi == null) {
                plugin.getLogger().severe(plugin.getLogMessage("menu.open.floodgate-unavailable"));
                return false;
            }

            if (!floodgateApi.isFloodgatePlayer(player.getUniqueId())) {
                player.sendMessage(plugin.getMessage("error.bedrock-only"));
                return false;
            }

            // openMenu 收到的是菜单文件名，先解析出 config 菜单键再校验权限
            String menuKey = menuKeyByFile.get(menuName);
            if (menuKey == null) {
                menuKey = stripYmlSuffix(menuName);
            }
            if (!plugin.getPermissionManager().hasMenuPermission(player, menuKey)) {
                player.sendMessage(plugin.getMessage("error.no-menu-permission"));
                return false;
            }

            YamlConfiguration menuConfig = menus.get(menuName);
            if (menuConfig == null) {
                player.sendMessage(plugin.getMessage("error.menu-not-found"));
                return false;
            }

            ConfigurationSection menuSection = menuConfig.getConfigurationSection("menu");
            if (menuSection == null) {
                player.sendMessage(plugin.getMessage("error.menu-format-error"));
                return false;
            }

            FloodgatePlayer floodgatePlayer = floodgateApi.getPlayer(player.getUniqueId());
            if (floodgatePlayer == null) {
                plugin.getLogger().warning(plugin.getLogMessage("menu.open.player-instance-error", player.getName()));
                return false;
            }

            FormType formType = FormType.fromString(menuSection.getString("type", "simple"));

            boolean sent = switch (formType) {
                case MODAL -> openModalForm(player, floodgatePlayer, menuSection);
                case CUSTOM -> openCustomForm(player, floodgatePlayer, menuSection);
                default -> openSimpleForm(player, floodgatePlayer, menuSection);
            };
            if (sent) {
                formCooldowns.put(player.getUniqueId(), now);
            }
            return sent;

        } catch (Exception e) {
            plugin.getLogger().severe(plugin.getLogMessage("menu.open.error", e.getMessage()));
            e.printStackTrace();
            if (player != null) {
                player.sendMessage(plugin.getMessage("error.form-error"));
            }
            return false;
        } finally {
            formCooldowns.entrySet().removeIf(entry -> 
                now - entry.getValue() > formCooldown * 2);
        }
    }

    private boolean openSimpleForm(Player player, FloodgatePlayer floodgatePlayer, ConfigurationSection menuSection) {
        SimpleForm.Builder form = SimpleForm.builder()
            .title(parsePlaceholders(player, menuSection.getString("title", "菜单")));

        StringBuilder content = new StringBuilder();

        String subtitle = parsePlaceholders(player, menuSection.getString("subtitle", ""));
        if (!subtitle.isEmpty()) {
            content.append(subtitle).append("\n\n");
        }

        String mainContent = parsePlaceholders(player, menuSection.getString("content", ""));
        if (!mainContent.isEmpty()) {
            content.append(mainContent);
            if (!mainContent.endsWith("\n")) {
                content.append("\n");
            }
        }

        String footer = parsePlaceholders(player, menuSection.getString("footer", ""));
        if (!footer.isEmpty()) {
            if (content.length() > 0) {
                content.append("\n");
            }
            content.append(footer);
        }

        if (content.length() > 0) {
            form.content(content.toString());
        }

        List<MenuAction> actions = new ArrayList<>();
        List<Map<?, ?>> itemList = menuSection.getMapList("items");
        if (itemList != null && !itemList.isEmpty()) {
            for (Map<?, ?> item : itemList) {
                String text = parsePlaceholders(player, getString(item, "text", "未命名"));
                String description = parsePlaceholders(player, getString(item, "description", ""));

                String icon = getString(item, "icon", plugin.getConfig().getString("icons.default", "paper"));
                String iconType = getString(item, "icon_type", null);
                FormImage formImage = processIcon(player, icon, iconType);

                if (description != null && !description.isEmpty()) {
                    form.button(text + "\n" + description, formImage);
                } else {
                    form.button(text, formImage);
                }

                String command = getString(item, "command", null);
                String executeAs = getString(item, "execute_as", "player");
                String submenu = getString(item, "submenu", null);
                actions.add(new MenuAction(command, executeAs, submenu));
            }
        }

        form.responseHandler((form1, response) -> {
            if (!player.isOnline()) return; // 玩家已下线，直接忽略响应
            if (response == null || response.isEmpty()) return;

            try {
                String cleanResponse = response.trim();
                int clickedButton = Integer.parseInt(cleanResponse);
                if (clickedButton >= 0 && clickedButton < actions.size()) {
                    MenuAction action = actions.get(clickedButton);
                    // command 和 submenu 互不排斥，与 custom 表单行为一致
                    if (action.command() != null) {
                        executeCommand(player, action.command(), action.executeAs());
                    }
                    if (action.submenu() != null) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            openMenu(player, action.submenu());
                        });
                    }
                }
            } catch (NumberFormatException e) {
                if (plugin.getConfig().getBoolean("settings.debug", false)) {
                    plugin.getLogger().warning(plugin.getLogMessage("form.response-error", e.getMessage()));
                }
            }
        });

        return floodgatePlayer.sendForm(form);
    }

    private boolean openModalForm(Player player, FloodgatePlayer floodgatePlayer, ConfigurationSection menuSection) {
        String title = parsePlaceholders(player, menuSection.getString("title", "确认"));
        String content = parsePlaceholders(player, menuSection.getString("content", "确定要执行此操作吗？"));
        String button1 = parsePlaceholders(player, menuSection.getString("button1", "确认"));
        String button2 = parsePlaceholders(player, menuSection.getString("button2", "取消"));

        ModalForm.Builder form = ModalForm.builder()
            .title(title)
            .content(content)
            .button1(button1)
            .button2(button2);

        ConfigurationSection onButton1 = menuSection.getConfigurationSection("on_button1");
        ConfigurationSection onButton2 = menuSection.getConfigurationSection("on_button2");

        form.responseHandler((modalForm, response) -> {
            if (!player.isOnline()) return; // 玩家已下线，直接忽略响应
            if (response == null || response.isEmpty()) return;

            try {
                boolean confirmed = Boolean.parseBoolean(response.trim());
                ConfigurationSection actionSection = confirmed ? onButton1 : onButton2;

                if (actionSection != null) {
                    String command = actionSection.getString("command");
                    String executeAs = actionSection.getString("execute_as", "player");
                    String submenu = actionSection.getString("submenu");

                    // command 和 submenu 互不排斥，与 custom 表单行为一致
                    if (command != null) {
                        executeCommand(player, command, executeAs);
                    }
                    if (submenu != null) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            openMenu(player, submenu);
                        });
                    }
                }
            } catch (Exception e) {
                if (plugin.getConfig().getBoolean("settings.debug", false)) {
                    plugin.getLogger().warning(plugin.getLogMessage("form.modal-response-error", e.getMessage()));
                }
            }
        });

        return floodgatePlayer.sendForm(form);
    }

    private boolean openCustomForm(Player player, FloodgatePlayer floodgatePlayer, ConfigurationSection menuSection) {
        String title = parsePlaceholders(player, menuSection.getString("title", "自定义表单"));

        CustomForm.Builder form = CustomForm.builder().title(title);

        List<Map<?, ?>> components = menuSection.getMapList("components");
        List<String> componentTypes = new ArrayList<>();
        // 与 componentTypes 平行，仅 dropdown 组件存放选项列表，用于把索引值映射回选项文本
        List<List<String>> dropdownOptions = new ArrayList<>();

        if (components != null && !components.isEmpty()) {
            for (Map<?, ?> component : components) {
                String type = getString(component, "type", "label");
                String text = parsePlaceholders(player, getString(component, "text", ""));
                componentTypes.add(type);
                dropdownOptions.add(null);

                switch (type.toLowerCase()) {
                    case "label" -> form.label(text);

                    case "input" -> {
                        String placeholder = getString(component, "placeholder", "");
                        String defaultVal = parsePlaceholders(player, getString(component, "default", ""));
                        form.input(text, placeholder, defaultVal);
                    }

                    case "dropdown" -> {
                        List<String> options = new ArrayList<>();
                        List<?> optionsList = (List<?>) component.get("options");
                        if (optionsList != null) {
                            for (Object opt : optionsList) {
                                options.add(parsePlaceholders(player, opt.toString()));
                            }
                        }
                        if (options.isEmpty()) {
                            options.add("无选项");
                        }
                        // 默认索引 clamp 到合法范围
                        int defaultIndex = Math.max(0, Math.min(getInt(component, "default", 0), options.size() - 1));
                        dropdownOptions.set(dropdownOptions.size() - 1, options);
                        form.dropdown(text, defaultIndex, options.toArray(new String[0]));
                    }

                    case "slider" -> {
                        float min = getFloat(component, "min", 0);
                        float max = getFloat(component, "max", 100);
                        // 旧版 Cumulus API 的 slider 签名 step 为 int（全 float 重载只在新 API 中存在）
                        int step = getInt(component, "step", 1);
                        float defaultVal = getFloat(component, "default", min);
                        form.slider(text, min, max, step, defaultVal);
                    }

                    case "toggle" -> {
                        boolean defaultVal = getBoolean(component, "default", false);
                        form.toggle(text, defaultVal);
                    }

                    default -> {
                        form.label(text);
                        componentTypes.set(componentTypes.size() - 1, "label");
                    }
                }
            }
        }

        ConfigurationSection onSubmit = menuSection.getConfigurationSection("on_submit");

        form.responseHandler((customForm, response) -> {
            if (!player.isOnline()) return; // 玩家已下线，直接忽略响应
            if (response == null || response.isEmpty()) return;

            try {
                List<String> values = parseCustomFormResponse(response, componentTypes, dropdownOptions);

                if (onSubmit != null) {
                    String command = onSubmit.getString("command");
                    String executeAs = onSubmit.getString("execute_as", "player");
                    String submenu = onSubmit.getString("submenu");

                    if (command != null) {
                        String processedCommand = command;
                        for (int i = 0; i < values.size(); i++) {
                            processedCommand = processedCommand.replace("{" + i + "}", values.get(i));
                        }
                        executeCommand(player, processedCommand, executeAs);
                    }

                    if (submenu != null) {
                        String processedSubmenu = submenu;
                        for (int i = 0; i < values.size(); i++) {
                            processedSubmenu = processedSubmenu.replace("{" + i + "}", values.get(i));
                        }
                        final String finalSubmenu = processedSubmenu;
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            openMenu(player, finalSubmenu);
                        });
                    }
                }
            } catch (Exception e) {
                if (plugin.getConfig().getBoolean("settings.debug", false)) {
                    plugin.getLogger().warning(plugin.getLogMessage("form.custom-response-error", e.getMessage()));
                    e.printStackTrace();
                }
            }
        });

        return floodgatePlayer.sendForm(form);
    }

    /**
     * 用 org.json 解析自定义表单响应数组。
     * 响应数组中每个组件（含 label）都占一个槽位，label 的值为 null；
     * label 不产生输入值、不占 {n} 索引；dropdown 的索引值会映射回选项文本。
     */
    private List<String> parseCustomFormResponse(String response, List<String> componentTypes, List<List<String>> dropdownOptions) {
        List<String> values = new ArrayList<>();
        JSONArray array = new JSONArray(response.trim());

        int index = 0;
        for (int i = 0; i < componentTypes.size(); i++) {
            String type = componentTypes.get(i);
            Object value = index < array.length() ? array.get(index) : null;
            index++;

            // label 组件不占 {n} 索引
            if ("label".equalsIgnoreCase(type)) {
                continue;
            }

            if (value == null || value == JSONObject.NULL) {
                values.add("");
                continue;
            }

            if ("dropdown".equalsIgnoreCase(type)) {
                // 下拉框返回的是选项索引，映射回选项文本
                List<String> options = dropdownOptions.get(i);
                int optionIndex = value instanceof Number ? ((Number) value).intValue() : -1;
                if (options != null && optionIndex >= 0 && optionIndex < options.size()) {
                    values.add(options.get(optionIndex));
                } else {
                    values.add(formatResponseValue(value));
                }
                continue;
            }

            values.add(formatResponseValue(value));
        }
        return values;
    }

    // 数值去掉无意义的小数部分（如 12.0 -> 12）
    private String formatResponseValue(Object value) {
        if (value instanceof Number) {
            double d = ((Number) value).doubleValue();
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                return String.valueOf((long) d);
            }
        }
        return String.valueOf(value);
    }

    // 只去除文件名末尾的 .yml 后缀，避免误替换文件名中间的同名串
    private String stripYmlSuffix(String fileName) {
        if (fileName != null && fileName.endsWith(".yml")) {
            return fileName.substring(0, fileName.length() - ".yml".length());
        }
        return fileName;
    }

    private String getString(Map<?, ?> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value instanceof String ? (String) value : defaultValue;
    }

    private int getInt(Map<?, ?> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    private float getFloat(Map<?, ?> map, String key, float defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return defaultValue;
    }

    private boolean getBoolean(Map<?, ?> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    private boolean isCommandSafe(String command) {
        if (command == null) return false;

        if (!plugin.getConfig().getBoolean("settings.enable-command-security", true)) {
            return true;
        }

        String normalizedCommand = command.trim().toLowerCase();
        if (normalizedCommand.startsWith("/")) {
            normalizedCommand = normalizedCommand.substring(1);
        }

        // 剥离命名空间前缀（如 minecraft:op -> op）
        normalizedCommand = normalizedCommand.replaceFirst("^[^\\s:]+:", "");

        List<String> blockedCommands = plugin.getConfig().getStringList("security.blocked-commands");
        for (String blocked : blockedCommands) {
            String normalizedBlocked = blocked.trim().toLowerCase();
            if (normalizedBlocked.isEmpty()) continue;
            // 边界匹配：命令本身或后跟空白（含 tab）才算命中
            if (normalizedCommand.matches("^" + Pattern.quote(normalizedBlocked) + "(\\s|$).*")) {
                return false;
            }
        }

        if (!plugin.getConfig().getBoolean("security.allow-special-chars", false)) {
            if (command.matches(".*[;|&`].*")) {
                return false;
            }
        }

        return true;
    }

    private boolean isValidIconUrl(String url) {
        try {
            if (!plugin.getConfig().getBoolean("icons.allow_url", true)) {
                return false;
            }

            if (plugin.getConfig().getBoolean("icons.url.https-only", true) 
                && !url.startsWith("https://")) {
                return false;
            }

            int maxLength = plugin.getConfig().getInt("icons.url.max-length", 256);
            if (url.length() > maxLength) {
                return false;
            }

            List<String> allowedDomains = plugin.getConfig().getStringList("icons.url.allowed-domains");
            if (!allowedDomains.isEmpty()) {
                // 用 URI 解析取 host，避免 contains 匹配被伪造域名绕过
                String host = new URI(url).getHost();
                if (host == null) {
                    return false;
                }
                host = host.toLowerCase();
                boolean allowed = false;
                for (String domain : allowedDomains) {
                    String allowedDomain = domain.trim().toLowerCase();
                    // host 等于域名或以 "."+域名 结尾才算通过
                    if (host.equals(allowedDomain) || host.endsWith("." + allowedDomain)) {
                        allowed = true;
                        break;
                    }
                }
                if (!allowed) return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String parsePlaceholders(Player player, String text) {
        try {
            if (text == null) return "";

            if (plugin.getConfig().getBoolean("settings.performance.cache-placeholders", false)) {
                return parsePlaceholdersWithCache(player, text);
            }

            // 内置变量 {player}
            text = text.replace("{player}", player.getName());

            if (text.contains("%") && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                text = PlaceholderAPI.setPlaceholders(player, text);
            }

            return text.replace("&", "§");
        } catch (Exception e) {
            plugin.getLogger().warning(plugin.getLogMessage("placeholder.process-error", e.getMessage()));
            return text;
        }
    }

    private String parsePlaceholdersWithCache(Player player, String text) {
        try {
            String cacheKey = player.getUniqueId().toString() + ":" + text;

            long now = System.currentTimeMillis();
            long cacheTime = plugin.getConfig().getInt("settings.performance.cache-refresh", 30) * 1000L;
            if (now - lastCacheRefresh > cacheTime) {
                placeholderCache.clear();
                lastCacheRefresh = now;
            }

            int maxSize = plugin.getConfig().getInt("settings.performance.max-cache-size", 1000);
            if (placeholderCache.size() >= maxSize) {
                placeholderCache.clear();
            }

            return placeholderCache.computeIfAbsent(cacheKey, k -> {
                // 内置变量 {player}
                String processed = text.replace("{player}", player.getName());
                if (processed.contains("%") && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                    processed = PlaceholderAPI.setPlaceholders(player, processed);
                }
                return processed.replace("&", "§");
            });
        } catch (Exception e) {
            plugin.getLogger().warning(plugin.getLogMessage("placeholder.cache-error", e.getMessage()));
            return text.replace("&", "§");
        }
    }

    /** 清理重载后不应继续复用的运行时缓存。 */
    public void clearRuntimeCache() {
        placeholderCache.clear();
        formCooldowns.clear();
        lastCacheRefresh = System.currentTimeMillis();
    }

    /**
     * 按 settings.performance.clear-cache-on-reload 决定是否清理占位符缓存。
     */
    public void clearPlaceholderCache(boolean clear) {
        if (clear) {
            clearRuntimeCache();
        }
    }

    private void executeCommand(Player player, String command, String executeAs) {
        try {
            if (player == null || command == null || command.isEmpty()) {
                return;
            }

            // 玩家已下线直接返回，尤其 execute_as: op 分支不得对离线玩家 setOp
            if (!player.isOnline()) {
                return;
            }

            // 先解析变量，再对最终命令做安全检查
            final String finalCommand = parsePlaceholders(player, command);
            if (finalCommand.isEmpty()) {
                return;
            }

            if (!isCommandSafe(finalCommand)) {
                plugin.getLogger().warning(plugin.getLogMessage("command-exec.unsafe-detected", finalCommand));
                player.sendMessage(plugin.getMessage("error.command-blocked"));
                return;
            }

            long delay = plugin.getConfig().getLong("settings.performance.command-delay", 0);

            Runnable commandTask = switch (executeAs.toLowerCase()) {
                case "console" -> () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
                case "op" -> () -> {
                    // 延迟执行期间玩家可能已下线，离线玩家不能 setOp
                    if (!player.isOnline()) return;
                    boolean wasOp = player.isOp();
                    try {
                        if (!wasOp) player.setOp(true);
                        Bukkit.dispatchCommand(player, finalCommand);
                    } finally {
                        if (!wasOp) player.setOp(false);
                    }
                };
                default -> () -> Bukkit.dispatchCommand(player, finalCommand);
            };

            if (delay > 0) {
                Bukkit.getScheduler().runTaskLater(plugin, commandTask, delay / 50);
            } else if (!Bukkit.isPrimaryThread()) {
                Bukkit.getScheduler().runTask(plugin, commandTask);
            } else {
                commandTask.run();
            }
        } catch (Exception e) {
            plugin.getLogger().warning(plugin.getLogMessage("command-exec.execute-error", e.getMessage()));
            player.sendMessage(plugin.getMessage("error.command-error"));
        }
    }

    private record MenuAction(
        String command,
        String executeAs,
        String submenu
    ) {}

    private FormImage processIcon(Player player, String icon, String iconType) {
        try {
            if (iconType != null) {
                switch (iconType.toLowerCase()) {
                    case "url" -> {
                        if (isValidIconUrl(icon)) {
                            return FormImage.of(FormImage.Type.URL, icon);
                        }
                    }
                    case "java" -> {
                        String bedrockPath = plugin.getConfig().getString("icons.mappings." + icon.toLowerCase());
                        if (bedrockPath != null) {
                            return FormImage.of(FormImage.Type.PATH, bedrockPath);
                        }
                    }
                    case "bedrock" -> {
                        return FormImage.of(FormImage.Type.PATH, icon);
                    }
                }
            }

            if (icon.startsWith("http://") || icon.startsWith("https://")) {
                if (isValidIconUrl(icon)) {
                    return FormImage.of(FormImage.Type.URL, icon);
                }
            }

            String bedrockPath = plugin.getConfig().getString("icons.mappings." + icon.toLowerCase());
            if (bedrockPath != null) {
                return FormImage.of(FormImage.Type.PATH, bedrockPath);
            }

            return getDefaultFormImage();
        } catch (Exception e) {
            if (plugin.getConfig().getBoolean("settings.debug", false)) {
                plugin.getLogger().warning(plugin.getLogMessage("icon.process-error", e.getMessage()));
            }
            return getDefaultFormImage();
        }
    }

    private FormImage getDefaultFormImage() {
        String defaultIcon = plugin.getConfig().getString("icons.default", "textures/items/paper");
        return FormImage.of(FormImage.Type.PATH, defaultIcon);
    }

    public List<String> getMenuList() {
        return new ArrayList<>(menus.keySet());
    }
}
