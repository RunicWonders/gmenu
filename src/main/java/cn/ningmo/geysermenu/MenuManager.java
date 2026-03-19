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
import java.util.HashMap;
import java.util.Map;
import org.geysermc.cumulus.util.FormImage;
import java.util.ArrayList;
import java.util.UUID;

public class MenuManager {
    private final GeyserMenu plugin;
    private final Map<String, YamlConfiguration> menus;
    private final Map<String, String> placeholderCache;
    private long lastCacheRefresh;
    private final Map<UUID, Long> formCooldowns = new HashMap<>();
    private static final long FORM_COOLDOWN = 500;

    public MenuManager(GeyserMenu plugin) {
        this.plugin = plugin;
        this.menus = new HashMap<>();
        this.placeholderCache = new HashMap<>();
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
                                if (plugin.getConfig().getBoolean("settings.debug")) {
                                    plugin.getLogger().info(plugin.getLogMessage("menu.load.success", fileName));
                                }
                            } else {
                                plugin.getLogger().warning(plugin.getLogMessage("menu.load.missing-file", fileName));
                            }
                        }
                    } catch (NullPointerException e) {
                        plugin.getLogger().severe(plugin.getLogMessage("menu.load.read-error", menuKey));
                    }
                });
            }
        } catch (Exception e) {
            plugin.getLogger().severe(plugin.getLogMessage("menu.load.error", e.getMessage()));
            e.printStackTrace();
        }
    }
    
    public void openMenu(Player player, String menuName) {
        long now = System.currentTimeMillis();
        
        try {
            Long lastOpen = formCooldowns.get(player.getUniqueId());
            if (lastOpen != null && now - lastOpen < FORM_COOLDOWN) {
                if (plugin.getConfig().getBoolean("settings.debug")) {
                    plugin.getLogger().info(plugin.getLogMessage("menu.open.cooldown", player.getName()));
                }
                return;
            }
            
            formCooldowns.put(player.getUniqueId(), now);
            
            if (player == null || menuName == null) {
                plugin.getLogger().warning(plugin.getLogMessage("menu.open.invalid-params", 
                    player != null ? player.getName() : "null", menuName != null ? menuName : "null"));
                return;
            }

            FloodgateApi floodgateApi = FloodgateApi.getInstance();
            if (floodgateApi == null) {
                plugin.getLogger().severe(plugin.getLogMessage("menu.open.floodgate-unavailable"));
                return;
            }

            if (!floodgateApi.isFloodgatePlayer(player.getUniqueId())) {
                player.sendMessage(plugin.getMessage("error.bedrock-only"));
                return;
            }

            String permission = plugin.getConfig().getString("menus." + menuName.replace(".yml", "") + ".permission");
            if (!plugin.getPermissionManager().hasMenuPermission(player, menuName.replace(".yml", ""))) {
                player.sendMessage(plugin.getMessage("error.no-menu-permission"));
                return;
            }
            
            YamlConfiguration menuConfig = menus.get(menuName);
            if (menuConfig == null) {
                player.sendMessage(plugin.getMessage("error.menu-not-found"));
                return;
            }
            
            ConfigurationSection menuSection = menuConfig.getConfigurationSection("menu");
            if (menuSection == null) {
                player.sendMessage(plugin.getMessage("error.menu-format-error"));
                return;
            }

            FloodgatePlayer floodgatePlayer = floodgateApi.getPlayer(player.getUniqueId());
            if (floodgatePlayer == null) {
                plugin.getLogger().warning(plugin.getLogMessage("menu.open.player-instance-error", player.getName()));
                return;
            }

            FormType formType = FormType.fromString(menuSection.getString("type", "simple"));
            
            switch (formType) {
                case MODAL -> openModalForm(player, floodgatePlayer, menuSection);
                case CUSTOM -> openCustomForm(player, floodgatePlayer, menuSection);
                default -> openSimpleForm(player, floodgatePlayer, menuSection);
            }

        } catch (Exception e) {
            plugin.getLogger().severe(plugin.getLogMessage("menu.open.error", e.getMessage()));
            e.printStackTrace();
            player.sendMessage(plugin.getMessage("error.form-error"));
        } finally {
            formCooldowns.entrySet().removeIf(entry -> 
                now - entry.getValue() > FORM_COOLDOWN * 2);
        }
    }
    
    private void openSimpleForm(Player player, FloodgatePlayer floodgatePlayer, ConfigurationSection menuSection) {
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
            if (response == null || response.isEmpty()) return;
            
            try {
                String cleanResponse = response.trim();
                int clickedButton = Integer.parseInt(cleanResponse);
                if (clickedButton >= 0 && clickedButton < actions.size()) {
                    MenuAction action = actions.get(clickedButton);
                    if (action.submenu() != null) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            openMenu(player, action.submenu());
                        });
                    } else if (action.command() != null) {
                        executeCommand(player, action.command(), action.executeAs());
                    }
                }
            } catch (NumberFormatException e) {
                if (plugin.getConfig().getBoolean("settings.debug", false)) {
                    plugin.getLogger().warning(plugin.getLogMessage("form.response-error", e.getMessage()));
                }
            }
        });

        floodgatePlayer.sendForm(form);
    }
    
    private void openModalForm(Player player, FloodgatePlayer floodgatePlayer, ConfigurationSection menuSection) {
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
            if (response == null || response.isEmpty()) return;
            
            try {
                boolean confirmed = Boolean.parseBoolean(response.trim());
                ConfigurationSection actionSection = confirmed ? onButton1 : onButton2;
                
                if (actionSection != null) {
                    String command = actionSection.getString("command");
                    String executeAs = actionSection.getString("execute_as", "player");
                    String submenu = actionSection.getString("submenu");
                    
                    if (submenu != null) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            openMenu(player, submenu);
                        });
                    } else if (command != null) {
                        executeCommand(player, command, executeAs);
                    }
                }
            } catch (Exception e) {
                if (plugin.getConfig().getBoolean("settings.debug", false)) {
                    plugin.getLogger().warning(plugin.getLogMessage("form.modal-response-error", e.getMessage()));
                }
            }
        });
        
        floodgatePlayer.sendForm(form);
    }
    
    private void openCustomForm(Player player, FloodgatePlayer floodgatePlayer, ConfigurationSection menuSection) {
        String title = parsePlaceholders(player, menuSection.getString("title", "自定义表单"));
        
        CustomForm.Builder form = CustomForm.builder().title(title);
        
        List<Map<?, ?>> components = menuSection.getMapList("components");
        List<String> componentTypes = new ArrayList<>();
        
        if (components != null && !components.isEmpty()) {
            for (Map<?, ?> component : components) {
                String type = getString(component, "type", "label");
                String text = parsePlaceholders(player, getString(component, "text", ""));
                componentTypes.add(type);
                
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
                        int defaultIndex = getInt(component, "default", 0);
                        if (options.isEmpty()) {
                            options.add("无选项");
                        }
                        form.dropdown(text, defaultIndex, options.toArray(new String[0]));
                    }
                    
                    case "slider" -> {
                        int min = getInt(component, "min", 0);
                        int max = getInt(component, "max", 100);
                        int step = getInt(component, "step", 1);
                        int defaultVal = getInt(component, "default", min);
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
            if (response == null || response.isEmpty()) return;
            
            try {
                String[] values = parseCustomFormResponse(response);
                
                if (onSubmit != null) {
                    String command = onSubmit.getString("command");
                    String executeAs = onSubmit.getString("execute_as", "player");
                    String submenu = onSubmit.getString("submenu");
                    
                    if (command != null) {
                        String processedCommand = command;
                        for (int i = 0; i < values.length && i < componentTypes.size(); i++) {
                            processedCommand = processedCommand.replace("{" + i + "}", values[i]);
                        }
                        executeCommand(player, processedCommand, executeAs);
                    }
                    
                    if (submenu != null) {
                        String processedSubmenu = submenu;
                        for (int i = 0; i < values.length && i < componentTypes.size(); i++) {
                            processedSubmenu = processedSubmenu.replace("{" + i + "}", values[i]);
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
        
        floodgatePlayer.sendForm(form);
    }
    
    private String[] parseCustomFormResponse(String response) {
        response = response.trim();
        if (response.startsWith("[") && response.endsWith("]")) {
            response = response.substring(1, response.length() - 1);
        }
        
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        boolean escaped = false;
        
        for (char c : response.toCharArray()) {
            if (escaped) {
                current.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                inString = !inString;
            } else if (c == ',' && !inString) {
                values.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        
        if (current.length() > 0) {
            values.add(current.toString().trim());
        }
        
        for (int i = 0; i < values.size(); i++) {
            String val = values.get(i);
            if (val.startsWith("\"") && val.endsWith("\"")) {
                val = val.substring(1, val.length() - 1);
            }
            values.set(i, val);
        }
        
        return values.toArray(new String[0]);
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
        
        List<String> blockedCommands = plugin.getConfig().getStringList("security.blocked-commands");
        for (String blocked : blockedCommands) {
            String normalizedBlocked = blocked.trim().toLowerCase();
            if (normalizedCommand.startsWith(normalizedBlocked + " ") || 
                normalizedCommand.equals(normalizedBlocked)) {
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
                boolean allowed = false;
                for (String domain : allowedDomains) {
                    if (url.contains(domain)) {
                        allowed = true;
                        break;
                    }
                }
                if (!allowed) return false;
            }

            return !url.contains("..");
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
                String processed = text;
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
    
    private void executeCommand(Player player, String command, String executeAs) {
        try {
            if (player == null || command == null || command.isEmpty()) {
                return;
            }
            
            if (!isCommandSafe(command)) {
                plugin.getLogger().warning(plugin.getLogMessage("command-exec.unsafe-detected", command));
                return;
            }
            
            final String finalCommand = parsePlaceholders(player, command);
            if (finalCommand.isEmpty()) {
                return;
            }

            long delay = plugin.getConfig().getLong("settings.performance.command-delay", 0);
            
            Runnable commandTask = switch (executeAs.toLowerCase()) {
                case "console" -> () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
                case "op" -> () -> {
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
