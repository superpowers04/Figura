package net.blancworks.figura.utils;

public enum ColorUtils {;
    public static void split(int value, int[] array) {
        int len = array.length;
        for (int i = 0; i < len; i++) {
            int shift = ((len) * 8) - ((i + 1) * 8);
            array[i] = (value >> shift) & 0xFF;
        }
    }
}
