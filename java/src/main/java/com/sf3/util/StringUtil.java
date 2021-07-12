/**
 *
 */
package com.sf3.util;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utilities for String manipulation.
 */
public class StringUtil {
    /** Keine Instanz */
    private StringUtil() {
    }

    /** Repeats the string. */
    public static String repeatString(String source, int count) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < count; i++) {
            buf.append(source);
        }
        return buf.toString();
    }

    /** prepends the string with zeros until the given length. */
    public static String getAsZerofilledString(long n, int length) {
        String c = String.valueOf(n);
        return StringUtil.repeatString("0", length - c.length()) + c;
    }

    private static final String[] BYTE_UNITS = new String[]{"B", "K", "M", "G", "T"};
    private static final NumberFormat BYTE_FOMATTER = new DecimalFormat("#,##0.00");

    /** returns a human readable bytes value. */
    public static String formatBytes(long bytes) {
        double dbytes = bytes;
        int unit = 0;
        while (dbytes > 1024 && unit < BYTE_UNITS.length) {
            dbytes = dbytes / 1024;
            unit++;
        }

        return BYTE_FOMATTER.format(dbytes) + " " + BYTE_UNITS[unit];
    }

    /** returns a human readable string of a miliseconds value. */
    public static String millisecondsToString(long msecs) {
        return secondsToString(msecs / 1000);
    }

    /** returns a human readable string of a seconds value. */
    public static String secondsToString(long secs) {
        StringBuffer buf = new StringBuffer();

        int days = (int) (secs / (3600 * 24));
        int hours = (int) ((secs % (3600 * 24)) / 3600);
        int minutes = (int) ((secs % 3600) / 60);
        int seconds = (int) ((secs % 60));

        if (days > 0) {
            buf.append(days).append(" Tage ");
        }

        buf.append(hours).append("h ").append(minutes).append("m ").append(seconds)
                .append("s");

        return buf.toString();
    }


    /**
     * Interpretiert die Strings als Blöcke die alle nebeneinander geschrieben
     * werden sollen. D.h. alle ersten Zeilen der String werden nebeneinander
     * geschrieben, dann alle zweiten Zeilen usw.
     */
    public static String blockConcat(Object... strings) {
        return blockConcat(Arrays.asList(strings));
    }

    /**
     * Interpretiert die Strings als Blöcke die alle nebeneinander geschrieben
     * werden sollen. D.h. alle ersten Zeilen der String werden nebeneinander
     * geschrieben, dann alle zweiten Zeilen usw.
     */
    public static String blockConcat(Iterable<Object> strings) {
        // erst die Strings in Blöcke parsen und die Maximale Anzahl an Zeilen feststellen
        List<Block> blocks = new ArrayList<Block>();
        int rows = 0;
        for (Object object : strings) {
            Block block = new Block(String.valueOf(object));
            blocks.add(block);
            rows = Math.max(rows, block.lines.size());
        }

        int numBlocks = blocks.size();

        // Jetzt die Blöcke Zeile für Zeile zusammenbasteln
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < rows; i++) {
            StringBuilder line = new StringBuilder();
            for (int b = 0; b < numBlocks; b++) {
                line.append(blocks.get(b).getString(i));
            }
            rtrim(line);

            buf.append(line).append("\n");
        }
        // den letzen '\n' wieder entfernen
        buf.setLength(buf.length() - 1);

        return buf.toString();
    }

    /** Entfernt alle Steuerzeichen und whitespaces rechts vom Stringbuilder */
    public static void rtrim(StringBuilder buf) {
        int len = buf.length() - 1;

        while ((len >= 0) && (buf.charAt(len) <= ' ')) {
            len--;
        }
        buf.setLength(len + 1);
    }

    private static final Splitter LINE_SPLITTER = Splitter.on('\n');

    /** Ein String-Block */
    private static class Block {
        /** Die Zeilen des Blocks */
        List<String> lines = new ArrayList<String>();
        /** Maximale Länge des Blocks */
        int maxLength = 0;

        /**
         *
         */
        public Block(String source) {
            Iterables.addAll(lines, LINE_SPLITTER.split(source));
            for (String s : lines) {
                maxLength = Math.max(maxLength, s.length());
            }
        }

        public String getString(int row) {
            String s = row < lines.size() ? lines.get(row) : "";
            return Strings.padEnd(s, maxLength, ' ');
        }
    }

    public static void main(String[] args) {
        String a = "\nfsfsafsd";
        String b = "[1.0 0.0 0.0 0.0]\n[0.0 1.0 0.0 0.0]\n[0.0 0.0 1.0 0.0]\n[0.0 0.0 0.0 1.0]\n";

        System.out.println(a + b);
        System.out.println(blockConcat(Arrays.asList(a, " ", b)));
    }
}
