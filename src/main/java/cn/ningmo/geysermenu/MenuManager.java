package cn.ningmo.geysermenu;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.bukkit.configuration.ConfigurationSection;
import java.util.List;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.geysermc.cumulus.response.SimpleFormResponse;
import org.geysermc.cumulus.util.FormImage;
import java.util.ArrayList;
import java.net.URL;
import java.util.UUID;

public class MenuManager {
    private final GeyserMenu plugin;
    private final Map<String, YamlConfiguration> menus;
    private final Map<String, String> placeholderCache; // 用于PAPI变量缓存
    private long lastCacheRefresh; // 缓存刷新时间记录
    private final Map<UUID, Long> formCooldowns = new HashMap<>();
    private static final long FORM_COOLDOWN = 500; // 500ms冷却时间

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
            
            // 从配置文件加载菜单
            ConfigurationSection section = plugin.getConfig().getConfigurationSection("menus");
            if (section != null) {
                section.getKeys(false).forEach(menuKey -> {
                    try {
                        ConfigurationSection menu = section.getConfigurationSection(menuKey);
                        if (menu == null || !menu.getBoolean("enable", true)) return;

                        String fileName = menu.getString("file");
                        if (fileName == null) {
                            plugin.getLogger().warning("菜单 " + menuKey + " 缺少file配置");
                            return;
                        }
                        // 检查文件名安全性
                        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                            plugin.getLogger().warning("检测到不安全的菜单文件名: " + fileName);
                        } else {
                            File menuFile = new File(menuFolder, fileName);
                            if (menuFile.exists()) {
                                menus.put(fileName, YamlConfiguration.loadConfiguration(menuFile));
                                if (plugin.getConfig().getBoolean("settings.debug")) {
                                    plugin.getLogger().info("已加载菜单: " + fileName);
                                }
                            } else {
                                plugin.getLogger().warning("菜单文件不存在: " + fileName);
                            }
                        }
                    } catch (NullPointerException e) {
                        plugin.getLogger().severe("读取菜单 "+menuKey+" 的参数时出现问题, 你是否有值未输入?");
                    }
                });
            }
        } catch (Exception e) {
            plugin.getLogger().severe("加载菜单时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void openMenu(Player player, String menuName) {
        // 将 now 变量移到方法作用域
        long now = System.currentTimeMillis();
        
        try {
            // 检查表单冷却
            Long lastOpen = formCooldowns.get(player.getUniqueId());
            if (lastOpen != null && now - lastOpen < FORM_COOLDOWN) {
                // 如果冷却中则忽略请求
                if (plugin.getConfig().getBoolean("settings.debug")) {
                    plugin.getLogger().info("玩家 " + player.getName() + " 打开菜单过快,请求被忽略");
                }
                return;
            }
            
            // 更新最后打开时间
            formCooldowns.put(player.getUniqueId(), now);
            
            // 参数检查
            if (player == null || menuName == null) {
                plugin.getLogger().warning("无效的参数: player=" + player + ", menuName=" + menuName);
                return;
            }

            // Floodgate检查
            FloodgateApi floodgateApi = FloodgateApi.getInstance();
            if (floodgateApi == null) {
                plugin.getLogger().severe("Floodgate API 不可用!");
                return;
            }

            if (!floodgateApi.isFloodgatePlayer(player.getUniqueId())) {
                player.sendMessage(plugin.getMessage("error.bedrock-only"));
                return;
            }

            // 权限检查
            String permission = plugin.getConfig().getString("menus." + menuName.replace(".yml", "") + ".permission");
            if (permission != null && !player.hasPermission(permission)) {
                player.sendMessage(plugin.getMessage("error.no-menu-permission"));
                return;
            }
            
            // 菜单检查
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

            // 获取Floodgate玩家实例
            FloodgatePlayer floodgatePlayer = floodgateApi.getPlayer(player.getUniqueId());
            if (floodgatePlayer == null) {
                plugin.getLogger().warning("无法获取玩家 " + player.getName() + " 的Floodgate实例");
                return;
            }

            // 构建表单
            SimpleForm.Builder form = SimpleForm.builder()
                .title(parsePlaceholders(player, menuSection.getString("title", "菜单")));

            // 处理内容
            StringBuilder content = new StringBuilder();
            
            // 添加副标题
            String subtitle = parsePlaceholders(player, menuSection.getString("subtitle", ""));
            if (!subtitle.isEmpty()) {
                content.append(subtitle).append("\n\n");
            }
            
            // 添加主要内容
            String mainContent = parsePlaceholders(player, menuSection.getString("content", ""));
            if (!mainContent.isEmpty()) {
                content.append(mainContent);
                if (!mainContent.endsWith("\n")) {
                    content.append("\n");
                }
            }
            
            // 添加页脚
            String footer = parsePlaceholders(player, menuSection.getString("footer", ""));
            if (!footer.isEmpty()) {
                if (content.length() > 0) {
                    content.append("\n");
                }
                content.append(footer);
            }

            // 设置表单内容
            if (content.length() > 0) {
                form.content(content.toString());
            }

            // 处理按钮
            List<MenuAction> actions = new ArrayList<>();
            List<Map<?, ?>> itemList = menuSection.getMapList("items");
            if (itemList != null && !itemList.isEmpty()) {
                for (Map<?, ?> item : itemList) {
                    // 处理按钮文本和图标
                    String text = parsePlaceholders(player, getString(item, "text", "未命名"));
                    String description = parsePlaceholders(player, getString(item, "description", ""));
                    
                    // 处理图标
                    String icon = getString(item, "icon", plugin.getConfig().getString("icons.default", "paper"));
                    String iconType = getString(item, "icon_type", null);
                    String iconPath = getString(item, "icon_path", null);
                    FormImage formImage = processIcon(player, icon, iconType);

                    // 添加按钮
                    if (description != null && !description.isEmpty()) {
                        form.button(text + "\n" + description, formImage);
                    } else {
                        form.button(text, formImage);
                    }

                    // 记录动作
                    String command = getString(item, "command", null);
                    String executeAs = getString(item, "execute_as", "player");
                    String submenu = getString(item, "submenu", null);
                    actions.add(new MenuAction(command, executeAs, submenu));
                }
            }

            // 发送表单
            form.responseHandler((form1, response) -> {
                if (response == null || response.isEmpty()) return;  // 玩家关闭表单
                
                try {
                    // 移除可能的换行符和空格
                    String cleanResponse = response.trim();
                    int clickedButton = Integer.parseInt(cleanResponse);  // SimpleForm 的响应直接就是按钮索引
                    if (clickedButton >= 0 && clickedButton < actions.size()) {
                        MenuAction action = actions.get(clickedButton);
                        if (action.submenu() != null) {
                            // 打开子菜单
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                openMenu(player, action.submenu());
                            });
                        } else if (action.command() != null) {
                            // 执行命令
                            executeCommand(player, action.command(), action.executeAs());
                        }
                    }
                } catch (NumberFormatException e) {
                    if (plugin.getConfig().getBoolean("settings.debug", false)) {
                        plugin.getLogger().warning("处理表单响应时出错: " + e.getMessage());
                    }
                }
            });

            floodgatePlayer.sendForm(form);

        } catch (Exception e) {
            plugin.getLogger().severe("打开菜单时出错: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage(plugin.getMessage("error.form-error"));
        } finally {
            // 清理过期的冷却记录
            formCooldowns.entrySet().removeIf(entry -> 
                now - entry.getValue() > FORM_COOLDOWN * 2);
        }
    }
    
    // 安全获取字符串值的辅助方法
    private String getString(Map<?, ?> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value instanceof String ? (String) value : defaultValue;
    }
    
    // 检查菜单权限
    private boolean hasMenuPermission(Player player, String menuKey) {
        String permission = plugin.getConfig().getString("menus." + menuKey + ".permission");
        return permission == null || player.hasPermission(permission);
    }

    // 检查命令安全性
    private boolean isCommandSafe(String command) {
        if (command == null) return false;

        // 检查是否启用命令安全检查
        if (!plugin.getConfig().getBoolean("settings.enable-command-security", true)) {
            return true;
        }
        
        // 检查命令黑名单
        List<String> blockedCommands = plugin.getConfig().getStringList("security.blocked-commands");
        for (String blocked : blockedCommands) {
            if (command.trim().equalsIgnoreCase(blocked.trim())) {
                return false;
            }
        }
        
        // 检查特殊字符
        if (!plugin.getConfig().getBoolean("security.allow-special-chars", false)) {
            if (command.matches(".*[;|&`].*")) {
                return false;
            }
        }
        
        return true;
    }

    // 检查URL安全性
    private boolean isValidIconUrl(String url) {
        try {
            if (!plugin.getConfig().getBoolean("icons.allow_url", true)) {
                return false;
            }

            // 检查HTTPS
            if (plugin.getConfig().getBoolean("icons.url.https-only", true) 
                && !url.startsWith("https://")) {
                return false;
            }

            // 检查URL长度
            int maxLength = plugin.getConfig().getInt("icons.url.max-length", 256);
            if (url.length() > maxLength) {
                return false;
            }

            // 检查允许的域名
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

    // 处理PAPI变量缓存
    private String parsePlaceholders(Player player, String text) {
        try {
            if (text == null) return "";
            
            // 检查是否启用了变量缓存
            if (plugin.getConfig().getBoolean("settings.performance.cache-placeholders", false)) {
                return parsePlaceholdersWithCache(player, text);
            }
            
            // 处理PAPI变量
            if (text.contains("%") && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                text = PlaceholderAPI.setPlaceholders(player, text);
            }
            
            // 处理颜色代码
            return text.replace("&", "§");
        } catch (Exception e) {
            plugin.getLogger().warning("处理变量时出错: " + e.getMessage());
            return text;
        }
    }
    
    private String parsePlaceholdersWithCache(Player player, String text) {
        try {
            String cacheKey = player.getName() + ":" + text;
            
            // 检查缓存是否需要刷新
            long now = System.currentTimeMillis();
            long cacheTime = plugin.getConfig().getInt("settings.performance.cache-refresh", 30) * 1000L;
            if (now - lastCacheRefresh > cacheTime) {
                placeholderCache.clear();
                lastCacheRefresh = now;
            }
            
            // 检查缓存大小限制
            int maxSize = plugin.getConfig().getInt("settings.performance.max-cache-size", 1000);
            if (placeholderCache.size() >= maxSize) {
                placeholderCache.clear();
            }
            
            // 使用缓存
            return placeholderCache.computeIfAbsent(cacheKey, k -> {
                String processed = text;
                if (processed.contains("%") && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                    processed = PlaceholderAPI.setPlaceholders(player, processed);
                }
                return processed.replace("&", "§");
            });
        } catch (Exception e) {
            plugin.getLogger().warning("处理缓存变量时出错: " + e.getMessage());
            return text.replace("&", "§");
        }
    }
    
    private void executeCommand(Player player, String command, String executeAs) {
        try {
            if (player == null || command == null || command.isEmpty()) {
                return;
            }
            
            // 检查命令安全性
            if (!isCommandSafe(command)) {
                plugin.getLogger().warning("检测到不安全的命令: " + command);
                return;
            }
            
            final String finalCommand = parsePlaceholders(player, command);
            if (finalCommand.isEmpty()) {
                return;
            }

            // 获取命令延迟
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

            // 应用延迟
            if (delay > 0) {
                Bukkit.getScheduler().runTaskLater(plugin, commandTask, delay / 50);
            } else if (!Bukkit.isPrimaryThread()) {
                Bukkit.getScheduler().runTask(plugin, commandTask);
            } else {
                commandTask.run();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("执行命令时出错: " + e.getMessage());
            player.sendMessage(plugin.getMessage("error.command-error"));
        }
    }
    
    // 使用Record替换内部类
    private record MenuAction(
        String command,
        String executeAs,
        String submenu
    ) {}

    // 修改图标处理相关代码
    private FormImage processIcon(Player player, String icon, String iconType) {
        try {
            // 如果指定了图标类型
            if (iconType != null) {
                switch (iconType.toLowerCase()) {
                    case "java" -> {
                        // 使用映射转换Java版材质路径到基岩版
                        String bedrockPath = plugin.getConfig().getString("icons.mappings." + icon.toLowerCase());
                        if (bedrockPath != null) {
                            return FormImage.of(FormImage.Type.PATH, bedrockPath);
                        }
                    }
                    case "bedrock" -> {
                        // 直接使用基岩版材质路径
                        return FormImage.of(FormImage.Type.PATH, icon);
                    }
                }
            }
            
            // 默认尝试作为Java版材质处理
            String bedrockPath = plugin.getConfig().getString("icons.mappings." + icon.toLowerCase());
            if (bedrockPath != null) {
                return FormImage.of(FormImage.Type.PATH, bedrockPath);
            }
            
            // 如果没有映射，使用默认图标
            return getDefaultFormImage();
        } catch (Exception e) {
            if (plugin.getConfig().getBoolean("settings.debug", false)) {
                plugin.getLogger().warning("处理图标时出错: " + e.getMessage());
            }
            return getDefaultFormImage();
        }
    }

    private FormImage getDefaultFormImage() {
        String defaultIcon = plugin.getConfig().getString("icons.default", "textures/items/paper");
        return FormImage.of(FormImage.Type.PATH, defaultIcon);
    }

    /**
     * 获取所有已启用的菜单名称
     * @return 菜单名称列表
     */
    public List<String> getMenuList() {
        return new ArrayList<>(menus.keySet());
    }
}
