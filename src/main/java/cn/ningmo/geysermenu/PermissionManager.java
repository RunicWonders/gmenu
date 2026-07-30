package cn.ningmo.geysermenu;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.Bukkit;

public class PermissionManager {
    private final GeyserMenu plugin;
    
    public static final String PERMISSION_USE = "geysermenu.use";
    public static final String PERMISSION_RELOAD = "geysermenu.reload";
    public static final String PERMISSION_OPEN = "geysermenu.open";
    public static final String PERMISSION_ADMIN = "geysermenu.admin";
    public static final String PERMISSION_MENU_ALL = "geysermenu.menu.*";
    
    public PermissionManager(GeyserMenu plugin) {
        this.plugin = plugin;
        registerPermissions();
    }
    
    private void registerPermissions() {
        PluginManager pm = Bukkit.getPluginManager();
        
        registerPermission(pm, PERMISSION_USE, plugin.getPermissionDescription("use"), PermissionDefault.TRUE);
        registerPermission(pm, PERMISSION_RELOAD, plugin.getPermissionDescription("reload"), PermissionDefault.OP);
        registerPermission(pm, PERMISSION_OPEN, plugin.getPermissionDescription("open"), PermissionDefault.OP);
        registerPermission(pm, PERMISSION_ADMIN, plugin.getPermissionDescription("admin"), PermissionDefault.OP);
        registerPermission(pm, PERMISSION_MENU_ALL, plugin.getPermissionDescription("menu-all"), PermissionDefault.OP);
        
        registerMenuPermissions();
    }
    
    private void registerPermission(PluginManager pm, String name, String description, PermissionDefault defaultValue) {
        if (pm.getPermission(name) == null) {
            pm.addPermission(new Permission(name, description, defaultValue));
        }
    }
    
    private void registerMenuPermissions() {
        var section = plugin.getConfig().getConfigurationSection("menus");
        if (section == null) return;
        
        PluginManager pm = Bukkit.getPluginManager();
        
        for (String menuKey : section.getKeys(false)) {
            var menuSection = section.getConfigurationSection(menuKey);
            if (menuSection == null) {
                // 标量型条目不是有效的菜单配置节，跳过并告警
                plugin.getLogger().warning("配置项 menus." + menuKey + " 不是有效的菜单配置节，已跳过权限注册");
                continue;
            }
            
            String permission = menuSection.getString("permission");
            if (permission != null && !permission.isEmpty()) {
                registerPermission(pm, permission, 
                    plugin.getPermissionDescription("menu-template").replace("{0}", menuKey), PermissionDefault.TRUE);
            }
        }
    }
    
    public boolean hasPermission(CommandSender sender, String permission) {
        if (sender == null || permission == null) {
            return false;
        }
        
        if (sender.hasPermission(PERMISSION_ADMIN)) {
            return true;
        }
        
        return sender.hasPermission(permission);
    }
    
    public boolean hasMenuPermission(Player player, String menuKey) {
        if (player == null || menuKey == null) {
            return false;
        }
        
        if (hasPermission(player, PERMISSION_ADMIN)) {
            return true;
        }
        
        if (hasPermission(player, PERMISSION_MENU_ALL)) {
            return true;
        }
        
        String permission = plugin.getConfig().getString("menus." + menuKey + ".permission");
        if (permission == null || permission.isEmpty()) {
            return true;
        }
        
        return hasPermission(player, permission);
    }
    
    public boolean hasUsePermission(CommandSender sender) {
        return hasPermission(sender, PERMISSION_USE);
    }
    
    public boolean hasReloadPermission(CommandSender sender) {
        return hasPermission(sender, PERMISSION_RELOAD);
    }
    
    public boolean hasOpenPermission(CommandSender sender) {
        return hasPermission(sender, PERMISSION_OPEN);
    }
    
    public boolean hasAdminPermission(CommandSender sender) {
        return hasPermission(sender, PERMISSION_ADMIN);
    }
    
    public void refreshMenuPermissions() {
        registerMenuPermissions();
    }
}
