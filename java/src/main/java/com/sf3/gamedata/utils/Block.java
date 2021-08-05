package com.sf3.gamedata.utils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** A block in the file.*/
public class Block {
    private final String name;
    private final int start;
    private final int length;
    private final Map<String, Object> properties;

    public Block(String name, int start, int length) {
        this.name = name;
        this.start = start;
        this.length = length;
        this.properties = new LinkedHashMap<>();
    }

    public String getName() {
        return name;
    }

    public int getStart() {
        return start;
    }

    public int getLength() {
        return length;
    }

    public void addBlock(Block block) {
        getProperties().put(block.getName(), block);
    }

    public Block createBlock(String name, int start, int length) {
        Block block = new Block(name, start, length);
        addBlock(block);
        return block;
    }

    public void addProperty(String name, Object object) {
        getProperties().put(name, object);
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public boolean hasProperty(String property) {
        return getProperties().containsKey(property) || getProperties().containsKey("_"+property);
    }

    public void removeProperty(String name) {
        getProperties().remove(name);
    }

    public <E> Stream<E> valuesStream(Class<E> filterType) {
        return getProperties().values().stream()
                .filter(filterType::isInstance)
                .map(filterType::cast);
    }

    public int getInt(String name) {
        Object value = getObject(name);
        if (value == null) {
            throw new IllegalStateException("Property "+name+" is null");
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof HexValue) {
            return ((HexValue) value).getValue();
        }
        throw new IllegalStateException("Property "+name+" is not an integer: "+value);
    }

    public <E extends Block> E getBlock(String name) {
        Object value = getObject(name);
        if (value instanceof Block) {
            return  (E) value;
        }

        throw new IllegalStateException("Property "+name+" is not a block, but a "+value.getClass()+" :"+value);
    }

    public <E> E  getObject(String name) {
        Object value = getProperties().get(name);
        if (value == null) {
            // try again with underscore
            value = getProperties().get("_"+name);
        }
        if (value == null) {
            throw new IllegalStateException("Property "+name+" is null");
        }
        return (E) value;
    }

    @Override
    public String toString() {
        return toJson();
    }

    public String toJson() {
        return "{" +
                getProperties().entrySet().stream()
                    .filter(entry -> !entry.getKey().startsWith("_"))
                    .map(entry -> "'"+entry.getKey()+"'="+ getValue(entry.getValue()))
                    .collect(Collectors.joining(","))+
                    "}";
    }

    private Object getValue(Object entry) {
        if (entry instanceof String) {
            String s = (String) entry;
            return "\""+s+"\"";
        }

        return entry;
    }
}
