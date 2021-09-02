package com.sf3.binaryview;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

/**
 * Wrapper around the list of {@link HighlightGroup}s.
 */
public class HighlightGroups {

    private Map<String, HighlightGroup> groups = new HashMap<>();

    private int currentColor = 0;

    private List<Integer> colors = Arrays.asList(
            0xff0000,
            0x00ff00,
            0x0000ff,
            0xffff00,
            0x00ffff,
            0xff00ff
    );

    private HighlightGroup getGroup(String groupName) {
        HighlightGroup group = groups.get(groupName);
        if (group != null) {
            return group;
        }
        group = new HighlightGroup(groupName, nextColor());
        groups.put(groupName, group);
        return group;
    }

    private int nextColor() {
        try {
            return colors.get(currentColor);
        } finally {
            currentColor = (currentColor + 1) % colors.size();
        }
    }

    public void addGroup(String name, int color) {
        if (!groups.containsKey(name)) {
            groups.put(name, new HighlightGroup(name, color));
        }
    }

    public void addRange(String groupName, int start, int size) {
        HighlightGroup group = getGroup(groupName);
        DataRange dataRange = new DataRange(start, size);
        // don't add duplicates to same group
        if (!group.getHighlights().contains(dataRange)) {
            group.getHighlights().add(dataRange);
        }
    }

    public void addPointer(String groupName, int location, int destination) {
        HighlightGroup group = getGroup(groupName);
        group.getPointers().add(new Pointer(location, destination));
    }


    @Override
    public String toString() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(new ArrayList<HighlightGroup>(groups.values()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
