package cn.ningmo.geysermenu;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
                        sender.sendMessage(plugin.getMessage("error.no-permission"));
                        return true;
                    }
                    sender.sendMessage(plugin.getMessage("reload.start"));
                    plugin.reloadConfig();
                    plugin.reloadMessages();
                    plugin.getMenuManager().loadMenus();
                    sender.sendMessage(plugin.getMessage("reload.success"));
                    return true;
                    
                case "open":
                    if (!sender.hasPermission("geysermenu.open")) {
                        sender.sendMessage(plugin.getMessage("error.no-permission"));
                        return true;
                    }
                    if (args.length < 3) {
                        sender.sendMessage(plugin.getMessage("command.usage", plugin.getRawMessage("command.open.usage")));
                        return true;
                    }
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(plugin.getMessage("error.player-not-found", args[1]));
                        return true;
                    }
                    plugin.getMenuManager().openMenu(target, args[2]);
                    sender.sendMessage(plugin.getMessage("command.open.success", target.getName()));
                    return true;
                    
                case "help":
                    sendHelpMessage(sender);
                    return true;
            }
        }
        
        // 如果没有参数或参数不匹配任何子命令
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!player.hasPermission("geysermenu.use")) {
                sender.sendMessage(plugin.getMessage("error.no-permission"));
                return true;
            }
            plugin.getMenuManager().openMenu(player, plugin.getConfig().getString("settings.default-menu", "menu.yml"));
        } else {
            // 如果是控制台且没有有效的子命令，显示帮助信息
            if (args.length == 0) {
                sendHelpMessage(sender);
            } else {
                sender.sendMessage(plugin.getMessage("error.unknown-command"));
            }
        }
        return true;
    }
    
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(plugin.getRawMessage("command.help.header"));
        sender.sendMessage(plugin.getRawMessage("command.help.menu"));
        sender.sendMessage(plugin.getRawMessage("command.help.help"));
        if (sender.hasPermission("geysermenu.reload")) {
            sender.sendMessage(plugin.getRawMessage("command.help.reload"));
        }
        if (sender.hasPermission("geysermenu.open")) {
            sender.sendMessage(plugin.getRawMessage("command.help.open"));
        }
        sender.sendMessage(plugin.getRawMessage("command.help.footer"));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        try {
            if (args.length == 1) {
                // 基础命令
                completions.add("help");
                
                // 权限命令
                if (sender.hasPermission("geysermenu.reload")) {
                    completions.add("reload");
                }
                if (sender.hasPermission("geysermenu.open")) {
                    completions.add("open");
                }
                
                // 过滤已输入的内容
                return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
            } else if (args.length == 2 && args[0].equalsIgnoreCase("open") && sender.hasPermission("geysermenu.open")) {
                // 补全在线玩家名
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            } else if (args.length == 3 && args[0].equalsIgnoreCase("open") && sender.hasPermission("geysermenu.open")) {
                // 补全菜单名
                return plugin.getMenuManager().getMenuList().stream()
                    .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Tab补全时发生错误: " + e.getMessage());
        }
        
        return completions;
    }
} 