package com.sf3.util;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sf3.gamedata.utils.BitReaderStream;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Utils {
    public static String readString(ImageInputStream stream, int length) throws IOException {
        byte[] buffer = new byte[length];
        stream.readFully(buffer);
        return new String(buffer).trim();
    }

    /**
     * Convert a JSON string to pretty print version
     */
    public static String toPrettyFormat(String jsonString)
    {
        JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();
        return new GsonBuilder().setPrettyPrinting().create().toJson(json);
    }

    public static String toBits(int num_bits, char[] codeword) {
        return IntStream.range(0, codeword.length)
                .map(i -> codeword[i])
                .mapToObj(Utils::toBin)
                .collect(Collectors.joining())
                .substring(0, num_bits);
    }

    public static String toBits(int num_bits, byte[] codeword) {
        return IntStream.range(0, codeword.length)
                .map(i -> codeword[i] & 0xff)
                .mapToObj(Utils::toBin)
                .collect(Collectors.joining())
                .substring(0, num_bits);
    }


    public static String toBin(int b) {
        return String.format("%8s", Integer.toBinaryString(b)).replace(' ','0');
    }

    public static String bitsToString(BitReaderStream stream, int numBits) {
        return IntStream.range(0, numBits)
                .mapToObj(i -> stream.nextBit())
                .map(b -> b ? "1" : "0")
                .collect(Collectors.joining());
    }
}
