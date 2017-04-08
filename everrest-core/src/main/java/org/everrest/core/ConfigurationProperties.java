package org.everrest.core;

import java.util.Collection;
import java.util.Map;

public abstract class ConfigurationProperties {
    /**
     * @return immutable configuration properties
     */
    public abstract Map<String, Object> getProperties();

    /**
     * Gets property value by name.
     *
     * @param name property name
     * @return the property value name or {@code null} if property witt specified name is not set
     */
    public abstract Object getProperty(String name);

    /**
     * Returns an immutable Collection of property names.
     *
     * @return an immutable Collection of property name
     */
    public abstract Collection<String> getPropertyNames();

    /**
     * Sets configuration property. If already set, existing value of property will be updated.
     * If specified value is {@code null} property is removed.
     *
     * @param name  property name
     * @param value property value
     */
    public abstract void setProperty(String name, Object value);

    /**
     * Removes configuration property.
     *
     * @param name property name
     */
    public abstract void removeProperty(String name);

    public Object getProperty(String name, Object def) {
        Object value = getProperty(name);
        if (value == null) {
            return def;
        }
        return value;
    }

    public String getStringProperty(String name, String def) {
        Object value = getProperty(name);
        if (value == null) {
            return def;
        }
        return String.valueOf(value);
    }

    public boolean getBooleanProperty(String name, boolean def) {
        Object value = getProperty(name);
        if (value instanceof Boolean) {
            return (boolean) value;
        } else if (value instanceof String) {
            String str = (String) value;
            return "true".equalsIgnoreCase(str) || "yes".equalsIgnoreCase(str) || "on".equalsIgnoreCase(str) || "1".equals(str);
        }
        return def;
    }

    public double getDoubleProperty(String name, double def) {
        Object value = getProperty(name);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            String str = (String) value;
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    public int getIntegerProperty(String name, int def) {
        Object value = getProperty(name);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            String str = (String) value;
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }
}
