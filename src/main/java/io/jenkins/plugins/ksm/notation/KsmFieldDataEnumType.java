package io.jenkins.plugins.ksm.notation;

public enum KsmFieldDataEnumType {
    STANDARD ("field"),
    CUSTOM ("custom_field"),
    FILE ("file");

    private String name;

    KsmFieldDataEnumType(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }
    public static KsmFieldDataEnumType getEnumByString(String name) {
        for(KsmFieldDataEnumType e : KsmFieldDataEnumType.values()){
            if(e.name.equals(name)) return e;
        }
        return null;
    }
}
