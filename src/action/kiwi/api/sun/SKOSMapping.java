package kiwi.api.sun;

import java.io.Serializable;

/**
 * Transport object used to  transport information about the SKOS mapping.
 *
 * @author mradules
 */
public final class SKOSMapping implements Serializable {

    /**
     * A version number for this class so that serialization
     * can occur without worrying about the underlying class
     * changing between serialization and deserialization.
     */
    private static final long serialVersionUID = 5165L;

    /**
     * The level of nesting.
     */
    private int level;

    /**
     * The prefix name.
     */
    private String name;

    public SKOSMapping(String name, int level) {
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

    @Override
    public String toString() {
        return "SKOSMapping [level=" + level + ", name=" + name + "]";
    }
}
