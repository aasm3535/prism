package net.prism.uniform;

import org.lwjgl.opengl.GL20;

import java.util.HashMap;
import java.util.Map;

/**
 * UniformCache - Performance-optimized cache for shader uniform variable locations.
 * <p>
 * Uniform location lookups (glGetUniformLocation) are relatively expensive GPU driver calls.
 * This cache stores previously resolved locations to avoid redundant lookups.
 * <p>
 * HashMap-based caching is thread-unsafe by design; assumes single OpenGL context thread.
 */
public class UniformCache {

    private final int programId;
    
    // Memoization map: uniform name → OpenGL location handle
    private final Map<String, Integer> cache = new HashMap<>();

    // Constant indicating uniform not found or not active in shader
    private static final int NOT_FOUND = -1;

    /**
     * Constructs a cache for a specific shader program.
     *
     * @param programId OpenGL shader program object ID
     */
    public UniformCache(int programId) {
        this.programId = programId;
    }

    /**
     * Gets the OpenGL uniform location for the given variable name.
     * Returns cached value if available, otherwise queries OpenGL and caches the result.
     * <p>
     * Location -1 indicates the uniform is not found or not active in the compiled program.
     *
     * @param name uniform variable name in the shader
     * @return OpenGL location handle (may be -1 if not found)
     */
    public int location(String name) {
        return cache.computeIfAbsent(name, this::queryLocation);
    }

    /**
     * Checks whether a uniform exists and is active in this shader program.
     * Uses cached lookup if available.
     *
     * @param name uniform variable name
     * @return true if uniform exists and is active (location != -1)
     */
    public boolean exists(String name) {
        return location(name) != NOT_FOUND;
    }

    /**
     * Clears the location cache.
     * Useful if shader is modified or program is relinked.
     */
    public void clear() {
        cache.clear();
    }

    /**
     * Queries OpenGL for the uniform location (expensive driver call).
     * Called only when location is not in cache.
     *
     * @param name uniform variable name
     * @return OpenGL location or -1 if not found/not active
     */
    private int queryLocation(String name) {
        return GL20.glGetUniformLocation(programId, name);
    }
}