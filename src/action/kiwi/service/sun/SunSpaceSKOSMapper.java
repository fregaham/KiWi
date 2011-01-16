package kiwi.service.sun;

public final class SunSpaceSKOSMapper {

    private int level;

    private String name;
    
    public SunSpaceSKOSMapper(String name, int level) {
        this.level = level;
        this.name = name;
    }

    /**
     * @return the level
     */
    public int getLevel() {
        return level;
    }

    /**
     * @param level the level to set
     */
    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
}
