package cn.ningmo.geysermenu;

public enum FormType {
    SIMPLE,
    MODAL,
    CUSTOM;
    
    public static FormType fromString(String type) {
        if (type == null || type.isEmpty()) {
            return SIMPLE;
        }
        
        return switch (type.toLowerCase()) {
            case "modal" -> MODAL;
            case "custom" -> CUSTOM;
            default -> SIMPLE;
        };
    }
}
