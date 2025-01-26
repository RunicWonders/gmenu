package cn.ningmo.geysermenu;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import java.util.ArrayList;
import java.util.List;

public class MenuCommand implements CommandExecutor, TabCompleter {
    private final GeyserMenu plugin;
    
    public MenuCommand(GeyserMenu plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "reload":
                    if (!sender.hasPermission("geysermenu.reload")) {
                        sender.sendMessage(plugin.getMessage("no-permission"));
                        return true;
                    }
                    plugin.reloadConfig();
                    plugin.getMenuManager().loadMenus();
                    sender.sendMessage(plugin.getMessage("reload-success"));
                    return true;
                    
                case "open":
                    if (!sender.hasPermission("geysermenu.open")) {
                        sender.sendMessage(plugin.getMessage("no-permission"));
                        return true;
                    }
                    if (args.length < 3) {
                        sender.sendMessage(plugin.getPrefix() + "§c用法: /gmenu open <玩家名> <菜单名>");
                        return true;
                    }
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(plugin.getPrefix() + "§c找不到玩家: " + args[1]);
                        return true;
                    }
                    plugin.getMenuManager().openMenu(target, args[2]);
                    sender.sendMessage(plugin.getPrefix() + "§a已为玩家 " + target.getName() + " 打开菜单!");
                    return true;
                    
                case "help":
                    sendHelpMessage(sender);
                    return true;
            }
        }
        
        // 如果是玩家且没有指定子命令，打开默认菜单
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!player.hasPermission("geysermenu.use")) {
                sender.sendMessage(plugin.getMessage("no-permission"));
                return true;
            }
            plugin.getMenuManager().openMenu(player, plugin.getConfig().getString("settings.default-menu", "menu.yml"));
        } else {
            // 如果是控制台且没有指定子命令，显示帮助信息
            sendHelpMessage(sender);
        }
        return true;
    }
    
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("§6========== GeyserMenu 帮助 ==========");
        sender.sendMessage("§f/gmenu §7- 打开默认菜单");
        sender.sendMessage("§f/gmenu help §7- 显示此帮助信息");
        if (sender.hasPermission("geysermenu.reload")) {
            sender.sendMessage("§f/gmenu reload §7- 重载插件配置");
        }
        if (sender.hasPermission("geysermenu.open")) {
            sender.sendMessage("§f/gmenu open <玩家名> <菜单名> §7- 为指定玩家打开菜单");
        }
        sender.sendMessage("§6==================================");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("help");
            if (sender.hasPermission("geysermenu.reload")) {
                completions.add("reload");
            }
            if (sender.hasPermission("geysermenu.open")) {
                completions.add("open");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("open")) {
            // 补全在线玩家名
            Bukkit.getOnlinePlayers().forEach(player -> completions.add(player.getName()));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("open")) {
            // 补全菜单名
            completions.addAll(plugin.getMenuManager().getMenuList());
        }
        
        return completions;
    }
} 