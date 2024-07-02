package io.github.sunmingyang0115.teaforall.util;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class TagDB {
    /**
     * Simple method to store data into entities by using tags
     * Warning: beware of multiple TagDB instances
     */
    static final String prefix = "io.github.sunmingyang0115.teaforall-missiles";
    HashMap<String, String> data;
    String oldTag;  // we keep track of this so that we replace it when we are done with altering the data
    Entity that;
    public TagDB(Entity _that) {
        data = new HashMap<>();
        that = _that;
        unpackTag(that.getCommandTags());
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
//    public <T> void putList(String key, List<T> value) {
//        for (T e : a) {
//            String.valueOf(a.get(0))
//        }
//    }

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
        that.removeCommandTag(oldTag);
        that.addCommandTag(tagFormat());
    }

    private String tagFormat() {
        StringBuilder tag = new StringBuilder(prefix);
        for (String s : data.keySet()) {
            tag.append(s).append("=").append(data.get(s)).append(",");
        }
        return tag.toString();
    }
    private void unpackTag(Set<String> commandTags) {
        String dataAsString = fetchData(commandTags);
        for (String s : dataAsString.split(",")) {
            String[] parts = s.split("=");
            if (parts.length != 2) continue;
            data.put(parts[0],parts[1]);
        }
    }
    private String fetchData(Set<String> commandTags) {
        if (commandTags == null)
            return "";

        for (String s : commandTags) {
            if (s.startsWith(prefix)) {
                oldTag = s;
                return s.replace(prefix, "");
            }
        }
        return "";
    }

}
