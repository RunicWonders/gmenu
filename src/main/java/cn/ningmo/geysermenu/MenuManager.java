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

public class MenuManager {
    private final GeyserMenu plugin;
    private final Map<String, YamlConfiguration> menus;
    private final Map<String, String> placeholderCache; // 用于PAPI变量缓存
    private long lastCacheRefresh; // 缓存刷新时间记录
    
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
        try {
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

            // 处理菜单内容
            String title = parsePlaceholders(player, menuSection.getString("title", "菜单"));
            String subtitle = parsePlaceholders(player, menuSection.getString("subtitle", ""));
            String content = parsePlaceholders(player, menuSection.getString("content", ""));
            String footer = parsePlaceholders(player, menuSection.getString("footer", ""));
            
            // 创建表单
            SimpleForm.Builder form = SimpleForm.builder()
                .title(title);
                
            // 创建表单内容
            StringBuilder contentBuilder = new StringBuilder();
            
            // 添加副标题
            if (!subtitle.isEmpty()) {
                contentBuilder.append(subtitle).append("\n\n");
            }
            
            // 添加内容
            if (!content.isEmpty()) {
                contentBuilder.append(content);
                // 如果内容不是以换行结束，添加换行
                if (!content.endsWith("\n")) {
                    contentBuilder.append("\n");
                }
            }
            
            // 添加页脚
            if (!footer.isEmpty()) {
                // 如果已有内容，确保有足够的间距
                if (contentBuilder.length() > 0) {
                    contentBuilder.append("\n");
                }
                contentBuilder.append(footer);
            }
            
            // 设置表单内容
            if (contentBuilder.length() > 0) {
                form.content(contentBuilder.toString());
            }
            
            List<Map<?, ?>> items = menuSection.getMapList("items");
            if (items == null) {
                items = new ArrayList<>();
            }
            final Map<Integer, MenuAction> actionMap = new HashMap<>();
            
            for (int i = 0; i < items.size(); i++) {
                Map<?, ?> item = items.get(i);
                if (item == null) continue;

                // 安全获取值
                String text = parsePlaceholders(player, getString(item, "text", "未命名按钮"));
                String description = parsePlaceholders(player, getString(item, "description", ""));
                String icon = getString(item, "icon", null);
                String iconType = getString(item, "icon_type", null);
                String iconPath = getString(item, "icon_path", null);
                String command = getString(item, "command", null);
                String executeAs = getString(item, "execute_as", "player");
                String submenu = getString(item, "submenu", null);
                
                // 如果有描述，添加到按钮文本中
                if (description != null && !description.isEmpty()) {
                    text = text + "\n§7" + description;
                }
                
                // 处理图标
                if (icon != null && !icon.isEmpty()) {
                    if (iconType != null && iconType.equalsIgnoreCase("url")) {
                        if (isValidIconUrl(icon)) {
                            form.button(text, FormImage.Type.URL, icon);
                        } else {
                            plugin.getLogger().warning("检测到不安全的图标URL: " + icon);
                            form.button(text);
                        }
                    } else if (iconType != null && iconType.equalsIgnoreCase("path") && iconPath != null) {
                        form.button(text, FormImage.Type.PATH, iconPath);
                    } else {
                        form.button(text, FormImage.Type.PATH, "textures/items/" + icon.replace("minecraft:", "") + ".png");
                    }
                } else {
                    form.button(text);
                }
                
                // 创建菜单动作
                MenuAction action = new MenuAction(command, executeAs, submenu);
                actionMap.put(i, action);
            }
            
            form.responseHandler((form1, responseData) -> {
                SimpleFormResponse response = form1.parseResponse(responseData);
                if (response.getClickedButtonId() >= 0) {
                    MenuAction action = actionMap.get(response.getClickedButtonId());
                    if (action != null) {
                        if (action.submenu != null) {
                            // 打开子菜单
                            openMenu(player, action.submenu);
                        } else if (action.command != null) {
                            // 执行命令
                            executeCommand(player, action.command, action.executeAs);
                        }
                    }
                }
            });
            
            // 错误处理
            try {
                floodgatePlayer.sendForm(form.build());
            } catch (Exception e) {
                plugin.getLogger().warning("发送表单时出错: " + e.getMessage());
                player.sendMessage(plugin.getMessage("error.form-error"));
            }
        } catch (Exception e) {
            plugin.getLogger().severe("打开菜单时发生错误: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage(plugin.getMessage("error.form-error"));
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
        
        // 检查命令黑名单
        List<String> blockedCommands = plugin.getConfig().getStringList("security.blocked-commands");
        for (String blocked : blockedCommands) {
            if (command.toLowerCase().contains(blocked.toLowerCase())) {
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
        if (text == null) return "";
        
        try {
            // 处理换行符
            text = text.replace("\\n", "\n");
            
            // 处理PAPI变量
            if (text.contains("%") && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                if (plugin.getConfig().getBoolean("settings.performance.cache-placeholders", false)) {
                    // 检查缓存是否需要刷新
                    long now = System.currentTimeMillis();
                    long cacheTime = plugin.getConfig().getInt("settings.performance.cache-refresh", 30) * 1000L;
                    if (now - lastCacheRefresh > cacheTime) {
                        placeholderCache.clear();
                        lastCacheRefresh = now;
                    }
                    
                    // 使用缓存
                    String cacheKey = player.getName() + ":" + text;
                    String cached = placeholderCache.get(cacheKey);
                    if (cached == null) {
                        cached = PlaceholderAPI.setPlaceholders(player, text);
                        placeholderCache.put(cacheKey, cached);
                    }
                    text = cached;
                } else {
                    text = PlaceholderAPI.setPlaceholders(player, text);
                }
            }
            
            // 处理颜色代码
            return text.replace("&", "§");
        } catch (Exception e) {
            plugin.getLogger().warning("处理变量时出错: " + e.getMessage());
            return text;
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
    
    /**
     * 获取所有已启用的菜单名称
     * @return 菜单名称列表
     */
    public List<String> getMenuList() {
        return new ArrayList<>(menus.keySet());
    }
} 