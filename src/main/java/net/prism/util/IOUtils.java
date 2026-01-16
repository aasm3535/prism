package net.prism.util;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * IOUtils - File and stream I/O utilities for shader resource loading.
 * <p>
 * Provides efficient buffered reading of text streams with UTF-8 encoding.
 */
public final class IOUtils {

    private IOUtils() {}

    /**
     * Reads the complete contents of an InputStream into a String.
     * Uses UTF-8 encoding and buffered reading for efficiency.
     * <p>
     * The input stream is closed automatically (via try-with-resources).
     *
     * @param stream InputStream to read from (typically from classpath resource)
     * @return complete stream contents as a String
     * @throws IOException if read operation fails
     */
    public static String readString(InputStream stream) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {

            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[8192];  // 8KB buffer for efficient reading
            int read;

            while ((read = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, read);
            }

            return sb.toString();
        }
    }
}