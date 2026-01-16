package net.prism;

import net.prism.exception.ShaderException;
import net.prism.manager.ShaderManager;
import net.prism.shader.ShaderLoader;
import net.prism.shader.ShaderProgram;
import net.prism.util.GLUtils;

/**
 * Prism - Lightweight OpenGL Shader Management Library
 * <p>
 * Provides a fluent API for shader compilation, linking, and uniform management with performance optimizations.
 * Supports both resource-based and file-based shader loading with preprocessor include directive support.
 * <p>
 * Usage example:
 * <pre>{@code
 * ShaderProgram shader = Prism.load("shaders/basic");
 * shader.bind()
 *       .uniform("u_time", time)
 *       .uniform("u_resolution", width, height);
 * // ... rendering ...
 * shader.unbind();
 * }</pre>
 *
 * @author Prism Contributors
 * @version 1.0.0
 */
public final class Prism {

    /** Library version number following semantic versioning */
    public static final String VERSION = "1.0.0";
    
    /** Library display name */
    public static final String NAME = "Prism";

    /** Global shader manager singleton for managing shader lifecycle and caching */
    private static final ShaderManager MANAGER = new ShaderManager();

    private Prism() {}

    // === Quick loading ===

    /**
     * Loads a shader program from classpath resources with implicit extensions.
     * Expects both <code>basePath.vert</code> and <code>basePath.frag</code> to exist.
     *
     * @param basePath resource path without extension (e.g., "shaders/basic")
     * @return compiled and linked shader program
     * @throws ShaderException if compilation, linking, or file loading fails
     */
    public static ShaderProgram load(String basePath) {
        return ShaderLoader.fromResources(basePath);
    }

    /**
     * Creates a shader program from raw source strings with immediate compilation.
     *
     * @param vertexSource GLSL vertex shader source code
     * @param fragmentSource GLSL fragment shader source code
     * @return compiled and linked shader program
     * @throws ShaderException if compilation or linking fails
     */
    public static ShaderProgram create(String vertexSource, String fragmentSource) {
        return ShaderProgram.create(vertexSource, fragmentSource);
    }

    // === Global manager ===

    /**
     * Returns the global shader manager for managing shader lifecycle.
     * Supports lazy loading, caching, and batch resource cleanup.
     *
     * @return singleton ShaderManager instance
     */
    public static ShaderManager manager() {
        return MANAGER;
    }

    /**
     * Retrieves a shader from the global manager by name.
     * Triggers lazy loading if the shader hasn't been loaded yet.
     *
     * @param name shader identifier
     * @return shader program, cached for subsequent calls
     * @throws IllegalArgumentException if shader is not registered
     */
    public static ShaderProgram shader(String name) {
        return MANAGER.get(name);
    }

    // === Utilities ===

    /**
     * Retrieves OpenGL GPU and driver information.
     *
     * @return GPU vendor and version string
     */
    public static String gpuInfo() {
        return GLUtils.getGPUInfo();
    }

    /**
     * Retrieves the maximum supported GLSL version string.
     *
     * @return GLSL version (e.g., "4.60" or "3.30 ES")
     */
    public static String glslVersion() {
        return GLUtils.getGLSLVersion();
    }

    /**
     * Checks if OpenGL shader compilation is supported on this platform.
     *
     * @return true if shader programs can be created and linked
     */
    public static boolean isSupported() {
        return GLUtils.areShadersSupported();
    }

    /**
     * Disposes all managed shader programs and clears global state.
     * Should be called during application shutdown to prevent resource leaks.
     */
    public static void dispose() {
        MANAGER.dispose();
    }

    /**
     * Prints library information and OpenGL capabilities to stdout.
     * Useful for debugging and runtime capability verification.
     */
    public static void printInfo() {
        System.out.println("=== " + NAME + " v" + VERSION + " ===");
        System.out.println("GPU: " + gpuInfo());
        System.out.println("GLSL: " + glslVersion());
        System.out.println("Shaders supported: " + isSupported());
    }
}