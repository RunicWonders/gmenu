package cn.ningmo.geysermenu.listeners;

import cn.ningmo.geysermenu.GeyserMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class PlayerListener implements Listener {

    private final GeyserMenu plugin;

    public PlayerListener(GeyserMenu plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 检查是否需要发送更新提示
        if (plugin.isUpdateAvailable() 
            && plugin.getConfig().getBoolean("settings.update.notify-on-join", true)) {
            
            // 检查是否只通知管理员
            if (!plugin.getConfig().getBoolean("settings.update.notify-ops-only", false) 
                || player.isOp()) {
                
                // 发送更新消息
                player.sendMessage(plugin.getMessage("update.player.available", 
                    plugin.getLatestVersion()));
                player.sendMessage(plugin.getMessage("update.player.current", 
                    plugin.getDescription().getVersion()));
                    
                // 发送可点击的下载链接
                TextComponent message = new TextComponent(
                    plugin.getMessage("update.player.download", 
                        "https://github.com/ning-g-mo/gmenu/releases/latest"));
                message.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, 
                    "https://github.com/ning-g-mo/gmenu/releases/latest"));
                message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                    new Text(plugin.getMessage("update.player.click-to-download"))));
                player.spigot().sendMessage(message);
            }
        }
    }
} 