package cn.ningmo.geysermenu.listeners;

import cn.ningmo.geysermenu.GeyserMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

public class PlayerListener implements Listener {

    // 更新提示中的插件下载链接
    private static final String DOWNLOAD_URL = "https://github.com/RunicWonders/gmenu/releases/latest";

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
                    plugin.getPluginMeta().getVersion()));
                    
                // 发送可点击的下载链接
                Component message = Component.text(
                    plugin.getMessage("update.player.download", DOWNLOAD_URL))
                    .clickEvent(ClickEvent.openUrl(DOWNLOAD_URL))
                    .hoverEvent(HoverEvent.showText(Component.text(
                        plugin.getMessage("update.player.click-to-download"))));
                player.sendMessage(message);
            }
        }
    }
} 