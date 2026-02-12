package com.splunk.splunkjenkins.utils;

import hudson.console.ConsoleNote;

import java.io.ByteArrayOutputStream;

/**
 * Utilities for decoding Jenkins console output by removing annotations.
 */
public class PlainTextConsoleUtils {

    /**
     * Finds the index of a byte pattern within a byte array
     *
     * @param buf the byte array to search
     * @param start the starting index for the search
     * @param end the ending index for the search
     * @param matches the byte pattern to find
     * @return the index of the pattern, or -1 if not found
     */
    public static int arrayIndexOf(byte[] buf, int start, int end, byte[] matches) {
        int e = end - matches.length + 1;

        OUTER:
        for (int i = start; i < e; i++) {
            if (buf[i] == matches[0]) {
                // check for the rest of the match
                for (int j = 1; j < matches.length; j++) {
                    if (buf[i + j] != matches[j])
                        continue OUTER;
                }
                return i; // found it
            }
        }
        return -1; // not found
    }

    /**
     * the logical extracted from PlainTextConsoleOutputStream
     * console annotation will be removed, e.g.
     * Input:Started by user ESC[8mha:AAAAlh+LCAAAAAAAAP9b85aBtbiIQTGjNKU4P08vOT+vOD8nVc83PyU1x6OyILUoJzMv2y+/JJUBAhiZGBgqihhk0NSjKDWzXb3RdlLBUSYGJk8GtpzUvPSSDB8G5tKinBIGIZ+sxLJE/ZzEvHT94JKizLx0a6BxUmjGOUNodHsLgAzOEgYu/dLi1CL9vNKcHACFIKlWvwAAAA==ESC[0manonymous
     * Output:Started by user anonymous
     *
     * @param in     the byte array
     * @param length how many bytes we want to read in
     * @param out    write max(length) to out
     * @see hudson.console.PlainTextConsoleOutputStream
     */
    public static void decodeConsole(byte[] in, int length, ByteArrayOutputStream out) {
        int next = arrayIndexOf(in, 0, length, ConsoleNote.PREAMBLE);
        // perform byte[]->char[] while figuring out the char positions of the BLOBs
        int written = 0;
        while (next >= 0) {
            //text prior to PREAMBLE
            if (next > written) {
                out.write(in, written, next - written);
                written = next;
            }
            int endPos = arrayIndexOf(in, next + ConsoleNote.PREAMBLE.length, length, ConsoleNote.POSTAMBLE);
            if (endPos < 0) {
                break;
            } else {
                //skips to POSTAMBLE
                written = endPos + ConsoleNote.POSTAMBLE.length;
                next = arrayIndexOf(in, written, length, ConsoleNote.PREAMBLE);
            }
        }
        // finish the remaining bytes->chars conversion
        out.write(in, written, length - written);
    }

}
