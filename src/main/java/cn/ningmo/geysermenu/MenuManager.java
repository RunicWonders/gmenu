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
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.nio.file.Paths;
import java.util.ArrayList;
import org.apache.commons.io.IOUtils;
import java.net.URL;
import java.net.URLConnection;
import java.io.InputStream;

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
                    FormImage formImage = processIcon(icon, iconType, iconPath);

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
                    int clickedButton = Integer.parseInt(response);  // SimpleForm 的响应直接就是按钮索引
                    if (clickedButton >= 0 && clickedButton < actions.size()) {
                        MenuAction action = actions.get(clickedButton);
                        if (action.submenu() != null) {
                            // 打开子菜单
                            openMenu(player, action.submenu());
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

    // 将图片转换为Base64编码
    private String imageToBase64(String imagePath) {
        try {
            // 检查路径是否为空
            if (imagePath == null || imagePath.trim().isEmpty()) {
                plugin.getLogger().warning("图片路径为空");
                return null;
            }

            // 规范化路径
            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                plugin.getLogger().warning("图片文件不存在: " + imagePath);
                return null;
            }

            // 检查文件是否可读
            if (!imageFile.canRead()) {
                plugin.getLogger().warning("无法读取图片文件: " + imagePath);
                return null;
            }

            // 获取MIME类型
            String mimeType = Files.probeContentType(imageFile.toPath());
            if (mimeType == null || !mimeType.startsWith("image/")) {
                mimeType = "image/png"; // 默认使用 PNG
            }

            // 读取文件内容
            byte[] fileContent = Files.readAllBytes(imageFile.toPath());
            return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(fileContent);
        } catch (IOException e) {
            plugin.getLogger().warning("处理图片文件失败: " + imagePath + " - " + e.getMessage());
            return null;
        }
    }

    // 添加新的辅助方法来处理图标路径
    private String resolveIconPath(String iconPath) {
        try {
            // 如果是绝对路径，直接返回
            if (new File(iconPath).isAbsolute()) {
                return iconPath;
            }

            // 如果是相对于插件目录的路径
            if (iconPath.startsWith("plugins/")) {
                return new File(plugin.getServer().getWorldContainer(), iconPath).getAbsolutePath();
            }

            // 默认从插件的图标目录加载
            String customPath = plugin.getConfig().getString("icons.custom_path", "plugins/GeyserMenu/icons");
            return new File(plugin.getServer().getWorldContainer(), 
                customPath + File.separator + new File(iconPath).getName()).getAbsolutePath();
        } catch (Exception e) {
            plugin.getLogger().warning("解析图标路径失败: " + iconPath + " - " + e.getMessage());
            return null;
        }
    }

    // 修改图标处理相关代码
    private FormImage processIcon(String icon, String iconType, String iconPath) {
        try {
            // 处理自定义路径图标
            if ("path".equals(iconType)) {
                String resolvedPath = resolveIconPath(iconPath);
                if (resolvedPath != null) {
                    String base64 = imageToBase64(resolvedPath);
                    if (base64 != null) {
                        return FormImage.of(FormImage.Type.URL, base64);
                    }
                }
                plugin.getLogger().warning("无法加载自定义图标: " + iconPath);
            }
            
            // 处理URL图标
            else if ("url".equals(iconType)) {
                // 检查是否允许URL图标
                if (!plugin.getConfig().getBoolean("icons.allow_url", true)) {
                    plugin.getLogger().warning("URL图标已被禁用");
                    return getDefaultFormImage();
                }
                
                // 检查URL安全性
                if (isUrlSafe(iconPath)) {
                    String base64 = fetchAndConvertIcon(iconPath);
                    if (base64 != null) {
                        return FormImage.of(FormImage.Type.URL, base64);
                    }
                } else {
                    plugin.getLogger().warning("不安全的图标URL: " + iconPath);
                }
            }
            
            // 处理 Minecraft 物品图标
            else {
                String texturePath = formatMinecraftIcon(icon);
                if (plugin.getConfig().getBoolean("settings.debug", false)) {
                    plugin.getLogger().info("使用材质路径: " + texturePath);
                }
                return FormImage.of(FormImage.Type.PATH, texturePath);
            }
            
            return getDefaultFormImage();
        } catch (Exception e) {
            plugin.getLogger().warning("处理图标时出错: " + e.getMessage());
            return getDefaultFormImage();
        }
    }

    // 添加辅助方法
    private boolean isUrlSafe(String url) {
        try {
            if (url == null || url.isEmpty()) {
                return false;
            }
            
            // 检查URL长度
            int maxLength = plugin.getConfig().getInt("icons.url.max-length", 256);
            if (url.length() > maxLength) {
                return false;
            }
            
            // 检查是否需要HTTPS
            if (plugin.getConfig().getBoolean("icons.url.https-only", true) 
                && !url.toLowerCase().startsWith("https://")) {
                return false;
            }
            
            // 检查域名白名单
            URL urlObj = new URL(url);
            String host = urlObj.getHost().toLowerCase();
            List<String> allowedDomains = plugin.getConfig().getStringList("icons.url.allowed-domains");
            return allowedDomains.stream().anyMatch(host::endsWith);
            
        } catch (Exception e) {
            plugin.getLogger().warning("URL安全检查失败: " + e.getMessage());
            return false;
        }
    }

    private FormImage getDefaultFormImage() {
        String defaultIcon = plugin.getConfig().getString("icons.default", "paper");
        String texturePath = formatMinecraftIcon(defaultIcon);
        if (plugin.getConfig().getBoolean("settings.debug", false)) {
            plugin.getLogger().info("使用默认图标: " + texturePath);
        }
        return FormImage.of(FormImage.Type.PATH, texturePath);
    }

    // 添加新的辅助方法
    private String getDefaultIcon() {
        return plugin.getConfig().getString("icons.default", "paper");
    }

    private String fetchAndConvertIcon(String urlString) {
        try {
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            try (InputStream inputStream = connection.getInputStream()) {
                byte[] imageBytes = IOUtils.toByteArray(inputStream);
                String mimeType = connection.getContentType();
                if (mimeType == null) {
                    mimeType = "image/png";
                }
                return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(imageBytes);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("获取URL图标失败: " + urlString + " - " + e.getMessage());
            return null;
        }
    }

    // 修改 formatMinecraftIcon 方法，确保返回基岩版可识别的材质路径
    private String formatMinecraftIcon(String icon) {
        if (icon == null || icon.isEmpty()) {
            return "textures/items/" + getDefaultIcon();
        }
        
        // 移除 minecraft: 前缀
        icon = icon.replace("minecraft:", "");
        
        // 如果已经包含完整路径，直接返回
        if (icon.startsWith("textures/")) {
            return icon;
        }
        
        // 基岭版材质路径映射
        return switch (icon.toLowerCase()) {
            // 方块
            case "grass_block", "grass" -> "textures/blocks/grass_side";
            case "stone" -> "textures/blocks/stone";
            case "dirt" -> "textures/blocks/dirt";
            case "diamond_block" -> "textures/blocks/diamond_block";
            case "oak_log" -> "textures/blocks/log_oak";
            case "oak_planks" -> "textures/blocks/planks_oak";
            
            // 物品
            case "diamond" -> "textures/items/diamond";
            case "diamond_sword" -> "textures/items/diamond_sword";
            case "diamond_pickaxe" -> "textures/items/diamond_pickaxe";
            case "compass" -> "textures/items/compass_item";
            case "clock" -> "textures/items/clock_item";
            case "paper" -> "textures/items/paper";
            case "book" -> "textures/items/book_normal";
            case "writable_book" -> "textures/items/book_writable";
            case "written_book" -> "textures/items/book_written";
            case "nether_star" -> "textures/items/nether_star";
            case "arrow" -> "textures/items/arrow";
            case "chest" -> "textures/blocks/chest_front";
            
            // 默认为物品路径
            default -> {
                if (icon.endsWith("_block")) {
                    yield "textures/blocks/" + icon;
                }
                yield "textures/items/" + icon;
            }
        };
    }

    /**
     * 获取所有已启用的菜单名称
     * @return 菜单名称列表
     */
    public List<String> getMenuList() {
        return new ArrayList<>(menus.keySet());
    }
}
