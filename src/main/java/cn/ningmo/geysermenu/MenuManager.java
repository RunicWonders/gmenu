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
    
    public MenuManager(GeyserMenu plugin) {
        this.plugin = plugin;
        this.menus = new HashMap<>();
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
            section.getKeys(false).forEach(menuKey -> {
                try {
                    ConfigurationSection menu = section.getConfigurationSection(menuKey);
                    if (!menu.getBoolean("enable", true)) return;

                    String fileName = menu.getString("file");
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
        } catch (Exception e) {
            plugin.getLogger().severe("加载菜单时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void openMenu(Player player, String menuName) {
        if (!FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
            player.sendMessage(plugin.getMessage("error.bedrock-only"));
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
        
        String title = parsePlaceholders(player, menuSection.getString("title", "菜单"));
        String subtitle = parsePlaceholders(player, menuSection.getString("subtitle", ""));
        String content = parsePlaceholders(player, menuSection.getString("content", ""));
        String footer = parsePlaceholders(player, menuSection.getString("footer", ""));
        
        // 获取Floodgate玩家实例
        FloodgatePlayer floodgatePlayer = FloodgateApi.getInstance().getPlayer(player.getUniqueId());
        
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
        final Map<Integer, MenuAction> actionMap = new HashMap<>();
        
        for (int i = 0; i < items.size(); i++) {
            Map<?, ?> item = items.get(i);
            String text = parsePlaceholders(player, (String) item.get("text"));
            String description = parsePlaceholders(player, (String) item.get("description"));
            String icon = (String) item.get("icon");
            String iconType = (String) item.get("icon_type");
            String iconPath = (String) item.get("icon_path");
            String command = (String) item.get("command");
            String executeAs = (String) item.get("execute_as");
            String submenu = (String) item.get("submenu");
            
            // 如果有描述，添加到按钮文本中
            if (description != null && !description.isEmpty()) {
                text = text + "\n§7" + description;
            }
            
            // 处理图标
            if (icon != null && !icon.isEmpty()) {
                if (iconType != null && iconType.equalsIgnoreCase("url")) {
                    form.button(text, FormImage.Type.URL, icon);
                } else if (iconType != null && iconType.equalsIgnoreCase("path") && iconPath != null) {
                    form.button(text, FormImage.Type.PATH, iconPath);
                } else {
                    form.button(text, FormImage.Type.PATH, "textures/items/" + icon.replace("minecraft:", "") + ".png");
                }
            } else {
                form.button(text);
            }
            
            // 创建菜单动作
            MenuAction action = new MenuAction();
            action.command = command;
            action.executeAs = executeAs;
            action.submenu = submenu;
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
    }
    
    private String parsePlaceholders(Player player, String text) {
        if (text == null) return "";
        
        try {
            // 处理换行符
            text = text.replace("\\n", "\n");
            
            // 处理PAPI变量
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                text = PlaceholderAPI.setPlaceholders(player, text);
            }
            
            // 处理颜色代码
            return text.replace("&", "§");
        } catch (Exception e) {
            plugin.getLogger().warning("处理变量时出错: " + e.getMessage());
            return text;
        }
    }
    
    private void executeCommand(Player player, String command, String executeAs) {
        if (command == null || command.isEmpty()) return;
        
        // 检查命令安全性
        if (command.contains("//") || command.toLowerCase().contains("op ")) {
            plugin.getLogger().warning("检测到可能不安全的命令: " + command);
            return;
        }
        
        try {
            final String finalCommand = parsePlaceholders(player, command);
            switch (executeAs.toLowerCase()) {
                case "console":
                    Bukkit.getScheduler().runTask(plugin, () -> 
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand));
                    break;
                case "op":
                    boolean wasOp = player.isOp();
                    if (wasOp) {
                        // 如果玩家本来就是OP，直接执行
                        Bukkit.getScheduler().runTask(plugin, () -> 
                            Bukkit.dispatchCommand(player, finalCommand));
                    } else {
                        try {
                            player.setOp(true);
                            Bukkit.getScheduler().runTask(plugin, () -> 
                                Bukkit.dispatchCommand(player, finalCommand));
                        } finally {
                            // 确保在任何情况下都恢复原状
                            Bukkit.getScheduler().runTask(plugin, () -> 
                                player.setOp(false));
                        }
                    }
                    break;
                case "player":
                default:
                    Bukkit.getScheduler().runTask(plugin, () -> 
                        Bukkit.dispatchCommand(player, finalCommand));
                    break;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("执行命令时出错: " + e.getMessage());
            player.sendMessage(plugin.getMessage("error.command-error"));
        }
    }
    
    // 添加MenuAction内部类
    private static class MenuAction {
        String command;
        String executeAs;
        String submenu;
    }
    
    /**
     * 获取所有已启用的菜单名称
     * @return 菜单名称列表
     */
    public List<String> getMenuList() {
        return new ArrayList<>(menus.keySet());
    }
} 