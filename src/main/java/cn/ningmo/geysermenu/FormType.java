package cn.ningmo.geysermenu;

import org.bukkit.Bukkit;

public enum FormType {
    SIMPLE,
    MODAL,
    CUSTOM;
    
    public static FormType fromString(String type) {
        if (type == null || type.isEmpty()) {
            return SIMPLE;
        }
        
        return switch (type.toLowerCase()) {
            case "simple" -> SIMPLE;
            case "modal" -> MODAL;
            case "custom" -> CUSTOM;
            default -> {
                // 非法类型回退为 SIMPLE，输出警告提示管理员修正配置
                Bukkit.getLogger().warning("[GeyserMenu] 未知的表单类型 '" + type + "'，已回退为 simple");
                yield SIMPLE;
            }
        };
    }
}
