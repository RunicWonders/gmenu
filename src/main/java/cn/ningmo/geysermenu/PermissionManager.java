package cn.ningmo.geysermenu;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.Bukkit;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PermissionManager {
    private final GeyserMenu plugin;
    private final Map<UUID, Map<String, Boolean>> permissionCache;
    private final Map<String, Permission> registeredPermissions;
    
    public static final String PERMISSION_USE = "geysermenu.use";
    public static final String PERMISSION_RELOAD = "geysermenu.reload";
    public static final String PERMISSION_OPEN = "geysermenu.open";
    public static final String PERMISSION_ADMIN = "geysermenu.admin";
    public static final String PERMISSION_MENU_ALL = "geysermenu.menu.*";
    
    public PermissionManager(GeyserMenu plugin) {
        this.plugin = plugin;
        this.permissionCache = new ConcurrentHashMap<>();
        this.registeredPermissions = new HashMap<>();
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
        Permission perm = pm.getPermission(name);
        if (perm == null) {
            perm = new Permission(name, description, defaultValue);
            pm.addPermission(perm);
        }
        registeredPermissions.put(name, perm);
    }
    
    private void registerMenuPermissions() {
        if (plugin.getConfig() == null) return;
        
        var section = plugin.getConfig().getConfigurationSection("menus");
        if (section == null) return;
        
        PluginManager pm = Bukkit.getPluginManager();
        
        for (String menuKey : section.getKeys(false)) {
            var menuSection = section.getConfigurationSection(menuKey);
            if (menuSection == null) continue;
            
            String permission = menuSection.getString("permission");
            if (permission != null && !permission.isEmpty()) {
                registerPermission(pm, permission, 
                    plugin.getPermissionDescription("menu-template").replace("{0}", menuKey), PermissionDefault.OP);
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
        
        if (sender instanceof Player) {
            Player player = (Player) sender;
            Map<String, Boolean> playerCache = permissionCache.get(player.getUniqueId());
            if (playerCache != null) {
                Boolean cached = playerCache.get(permission);
                if (cached != null) {
                    return cached;
                }
            }
            
            boolean hasPermission = sender.hasPermission(permission);
            
            if (playerCache == null) {
                playerCache = new HashMap<>();
                permissionCache.put(player.getUniqueId(), playerCache);
            }
            playerCache.put(permission, hasPermission);
            
            return hasPermission;
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
    
    public void clearCache(UUID playerId) {
        permissionCache.remove(playerId);
    }
    
    public void clearAllCache() {
        permissionCache.clear();
    }
    
    public void refreshMenuPermissions() {
        registerMenuPermissions();
    }
    
    public void onPlayerQuit(UUID playerId) {
        permissionCache.remove(playerId);
    }
}
