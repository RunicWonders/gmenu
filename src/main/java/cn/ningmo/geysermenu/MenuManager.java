package cn.ningmo.geysermenu;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.geysermc.cumulus.component.ButtonComponent;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.bukkit.configuration.ConfigurationSection;
import java.util.List;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.geysermc.cumulus.response.SimpleFormResponse;
import org.geysermc.cumulus.util.FormImage;
import org.geysermc.cumulus.CustomForm;
import org.geysermc.cumulus.response.CustomFormResponse;
import java.util.ArrayList;
import java.io.IOException;

public class MenuManager {
    private final GeyserMenu plugin;
    private final Map<String, YamlConfiguration> menus;
    
    public MenuManager(GeyserMenu plugin) {
        this.plugin = plugin;
        this.menus = new HashMap<>();
        loadMenus();
    }
    
    public void loadMenus() {
        File menuFolder = new File(plugin.getDataFolder(), "menus");
        if (!menuFolder.exists()) {
            menuFolder.mkdirs();
        }
        
        menus.clear();
        
        // 从配置文件加载菜单
        ConfigurationSection menuSection = plugin.getConfig().getConfigurationSection("menus");
        if (menuSection != null) {
            for (String menuId : menuSection.getKeys(false)) {
                ConfigurationSection menu = menuSection.getConfigurationSection(menuId);
                if (menu != null && menu.getBoolean("enable", true)) {
                    String fileName = menu.getString("file");
                    if (fileName != null) {
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
                }
            }
        }
    }
    
    public void openMenu(Player player, String menuName) {
        if (!FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
            player.sendMessage(plugin.getMessage("bedrock-only"));
            return;
        }
        
        YamlConfiguration menuConfig = menus.get(menuName);
        if (menuConfig == null) {
            player.sendMessage(plugin.getMessage("menu-not-found"));
            return;
        }
        
        ConfigurationSection menuSection = menuConfig.getConfigurationSection("menu");
        if (menuSection == null) {
            player.sendMessage(plugin.getMessage("menu-format-error"));
            return;
        }
        
        String title = parsePlaceholders(player, menuSection.getString("title", "菜单"));
        
        // 获取Floodgate玩家实例
        FloodgatePlayer floodgatePlayer = FloodgateApi.getInstance().getPlayer(player.getUniqueId());
        
        // 创建表单
        SimpleForm.Builder form = SimpleForm.builder()
            .title(title);
            
        List<Map<?, ?>> items = menuSection.getMapList("items");
        final Map<Integer, MenuAction> actionMap = new HashMap<>();
        
        for (int i = 0; i < items.size(); i++) {
            Map<?, ?> item = items.get(i);
            String text = parsePlaceholders(player, (String) item.get("text"));
            String icon = (String) item.get("icon");
            String command = (String) item.get("command");
            String executeAs = (String) item.get("execute_as");
            String submenu = (String) item.get("submenu");
            
            if (icon != null && !icon.isEmpty()) {
                form.button(text, FormImage.Type.PATH, "textures/items/" + icon.replace("minecraft:", "") + ".png");
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
        
        // 发送表单
        floodgatePlayer.sendForm(form.build());
    }
    
    private String parsePlaceholders(Player player, String text) {
        if (text == null) return "";
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            text = PlaceholderAPI.setPlaceholders(player, text);
        }
        return text.replace("&", "§");
    }
    
    private void executeCommand(Player player, String command, String executeAs) {
        if (command == null || command.isEmpty()) return;
        
        switch (executeAs.toLowerCase()) {
            case "console":
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                break;
            case "op":
                boolean wasOp = player.isOp();
                try {
                    player.setOp(true);
                    Bukkit.dispatchCommand(player, command);
                } finally {
                    if (!wasOp) {
                        player.setOp(false);
                    }
                }
                break;
            case "player":
            default:
                Bukkit.dispatchCommand(player, command);
                break;
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