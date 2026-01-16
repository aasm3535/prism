package net.prism.util;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * GLUtils - OpenGL utility methods for error checking and capability querying.
 * <p>
 * Provides error diagnostics with human-readable GL error names and GPU capability introspection.
 */
public final class GLUtils {

    private GLUtils() {}

    /**
     * Checks for OpenGL errors and throws an exception if one occurred.
     * Useful for debugging GPU driver issues and API misuse.
     * <p>
     * Call this after operations that may fail due to invalid state or parameters.
     *
     * @param operation description of the operation being checked (for error messages)
     * @throws RuntimeException if OpenGL error occurred, with detailed error information
     */
    public static void checkError(String operation) {
        int error = GL11.glGetError();
        if (error != GL11.GL_NO_ERROR) {
            // Map error code to human-readable name
            String errorName = switch (error) {
                case GL11.GL_INVALID_ENUM -> "GL_INVALID_ENUM";
                case GL11.GL_INVALID_VALUE -> "GL_INVALID_VALUE";
                case GL11.GL_INVALID_OPERATION -> "GL_INVALID_OPERATION";
                case GL30.GL_INVALID_FRAMEBUFFER_OPERATION -> "GL_INVALID_FRAMEBUFFER_OPERATION";
                case GL11.GL_OUT_OF_MEMORY -> "GL_OUT_OF_MEMORY";
                default -> "Unknown (0x" + Integer.toHexString(error) + ")";
            };
            throw new RuntimeException("OpenGL error during '" + operation + "': " + errorName);
        }
    }

    /**
     * Checks if the GPU and driver support GLSL shader compilation.
     * Performs a safe test by attempting to create a shader program object.
     *
     * @return true if shaders are supported, false if creation fails or throws
     */
    public static boolean areShadersSupported() {
        try {
            return GL20.glCreateProgram() != 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Retrieves the maximum supported GLSL version string from the GPU driver.
     * Example values: "4.60", "3.30 ES", "1.20" (ES for embedded systems).
     *
     * @return GLSL version string or null if not available
     */
    public static String getGLSLVersion() {
        return GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION);
    }

    /**
     * Retrieves human-readable GPU and driver information.
     * Combines GPU renderer name and OpenGL version string.
     * Example: "NVIDIA GeForce RTX 3080 - 4.6.0 NVIDIA 470.00"
     *
     * @return GPU and driver information string
     */
    public static String getGPUInfo() {
        return GL11.glGetString(GL11.GL_RENDERER) + " - " + GL11.glGetString(GL11.GL_VERSION);
    }
}