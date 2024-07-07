package io.github.sunmingyang0115.teaforall.util;

import io.github.sunmingyang0115.teaforall.mixin.PlayerEntityMixin;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Set;
import java.util.regex.Pattern;

public class TagDB {
    /**
     * Simple method to store data into entities by using tags
     * Warning: beware of multiple TagDB instances
     */
    static final String prefix = "io.github.sunmingyang0115.teaforall-missiles";
    static final String pair_separator = "=";
    static final String key_separator =  ",";

    HashMap<String, String> data;
    String oldTag;
    Entity that;
    public TagDB(Entity _that) {
        data = new HashMap<>();
        that = _that;
        unpackTag(that.getCommandTags());
//        data = new HashMap<>();
    }

    /**
     * checks if key exists
     * @param key to test
     * @return True if exists; False if not
     */
    public boolean contains(String key) {
        return data.containsKey(key);
    }

    /**
     * Puts value to key
     * @param key to put
     * @param value to put
     */
    public void put(String key, String value) {
        data.put(key, value);
    }
    public void putInt(String key, int value) {
        this.put(key, String.valueOf(value));
    }
    public void putFloat(String key, float value) {
        this.put(key, String.valueOf(value));
    }
    public void putDouble(String key, double value) {
        this.put(key, String.valueOf(value));
    }
    public void putVec3d(String key, Vec3d value) {
        this.putDouble(key+"-Vec3d-x", value.getX());
        this.putDouble(key+"-Vec3d-y", value.getY());
        this.putDouble(key+"-Vec3d-z", value.getZ());
    }

    public void remove(String key) {
        data.remove(key);
    }
    public void removeInt(String key) {
        this.remove(key);
    }
    public void removeFloat(String key) {
        this.remove(key);
    }
    public void removeDouble(String key) {
        this.remove(key);
    }
    public void removeVec3d(String key, Vec3d value) {
        this.removeDouble(key+"-Vec3d-x");
        this.removeDouble(key+"-Vec3d-y");
        this.removeDouble(key+"-Vec3d-z");
    }

    /**
     * Gets stored value from key
     * @param key to obtain
     * @return value or Null if does not exist
     */
    public String get(String key) {
        return data.get(key);
    }
    public int getInt(String key) {
        return Integer.parseInt(this.get(key));
    }
    public float getFloat(String key) {
        return Float.parseFloat(this.get(key));
    }
    public double getDouble(String key) {
        return Double.parseDouble(this.get(key));
    }
    public Vec3d getVec3d(String key) {
        return new Vec3d(getDouble(key+"-Vec3d-x"), getDouble(key+"-Vec3d-y"), getDouble(key+"-Vec3d-z"));
    }

    /**
     * removes old command tag and replaces with new one
     */
    public void write() {
        that.removeCommandTag(getRawString(that.getCommandTags()));
        that.addCommandTag(tagFormat());
    }

    private String tagFormat() {
        StringBuilder tag = new StringBuilder(prefix);
        for (String s : data.keySet()) {
            tag.append(s).append(pair_separator).append(data.get(s)).append(key_separator);
        }
        return tag.toString();
    }
    private void unpackTag(Set<String> commandTags) {
        String dataAsString = fetchData(commandTags);
        for (String s : dataAsString.split(key_separator)) {
            String[] parts = s.split(pair_separator);
            if (parts.length != 2) continue;
            data.put(parts[0],parts[1]);
        }
    }
    private String fetchData(Set<String> commandTags) {
        if (commandTags == null)
            return "";
        return getRawString(commandTags).replace(prefix, "");
    }

    private String getRawString(Set<String> commandTags) {
        for (String s : commandTags) {
            if (s.startsWith(prefix)) {
                return s;
            }
        }
        return "";
    }

}
