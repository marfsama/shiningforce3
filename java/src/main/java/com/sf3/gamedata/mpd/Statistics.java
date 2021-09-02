package com.sf3.gamedata.mpd;

import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

/** Statistics and tabular view of fields from multiple files. */
public class Statistics {
    private Map<String, File> files = new LinkedHashMap<>();
    /** Fields in insertion order. */
    private List<String> fields = new ArrayList<>();

    public void addField(String file, String property, Object value) {
        if (!fields.contains(property)) {
            fields.add(property);
        }
        getFile(file).addField(property, value);
    }

    private File getFile(String filename) {
        return files.computeIfAbsent(filename, File::new);
    }

    public String toMarkdown() {
        StringBuffer buffer = new StringBuffer();
        // header with the field names
        buffer.append("|File|").append(String.join("|", fields)).append("|\n");
        buffer.append("|---".repeat(fields.size()+1)).append("|\n");
        for (File file : files.values()) {
            buffer.append("|")
                    .append(file.getName())
                    .append("|")
                    .append(fields.stream().map(file::getField).map(String::valueOf).collect(Collectors.joining("|")))
                    .append("|\n");
        }

        return buffer.toString();
    }
    public String toMarkdownTransposed() {
        StringBuffer buffer = new StringBuffer();
        // header with the file names
        buffer.append("|Field|").append(String.join("|", files.keySet())).append("|\n");
        buffer.append("|---".repeat(files.size()+1)).append("|\n");
        // field rows
        for (String field : fields) {
            buffer.append("|")
                    .append(field)
                    .append("|")
                    .append(files.values().stream()
                            .map(file -> file.getField(field))
                            .map(String::valueOf)
                            .collect(Collectors.joining("|")))
                    .append("|\n");
        }


        return buffer.toString();
    }


    @Data
    private static class File {
        private final String name;
        private final Map<String, Object> fields = new HashMap<>();

        public Object getField(String name) {
            return fields.getOrDefault(name, "");
        }

        public void addField(String property, Object value) {
            fields.put(property, value);
        }
    }
}
