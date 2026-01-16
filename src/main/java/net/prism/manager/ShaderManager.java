package net.prism.manager;

import net.prism.shader.ShaderLoader;
import net.prism.shader.ShaderProgram;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * ShaderManager - Centralized shader program management with lazy loading and caching.
 * <p>
 * Manages the lifecycle of shader programs with support for:
 * - Lazy loading via supplier registration
 * - Automatic caching to avoid redundant compilations
 * - Thread-safe operations via ConcurrentHashMap
 * - Batch reloading and cleanup
 * <p>
 * Thread-safe for concurrent access from multiple threads.
 */
public class ShaderManager {
    /** Constructor creates a new manager instance with empty caches */
    // Cached compiled shader programs, keyed by name
    private final Map<String, ShaderProgram> shaders = new ConcurrentHashMap<>();
    
    // Registered loaders for lazy loading, keyed by name
    private final Map<String, Supplier<ShaderProgram>> loaders = new ConcurrentHashMap<>();

    /**
     * Registers a shader with a custom loader for lazy loading.
     * The loader will be invoked on first access if not already loaded.
     * <p>
     * Allows flexible shader creation strategies (e.g., conditional compilation,
     * runtime-generated shaders, or dynamic source generation).
     *
     * @param name unique shader identifier
     * @param loader supplier that creates the shader program when needed
     * @return this manager for method chaining
     */
    public ShaderManager register(@NotNull String name, @NotNull Supplier<ShaderProgram> loader) {
        loaders.put(name, loader);
        return this;
    }

    /**
     * Convenience method to register a shader from a classpath resource.
     * Uses implicit naming convention: basePath.vert and basePath.frag.
     *
     * @param name unique shader identifier
     * @param basePath classpath resource path without extension
     * @return this manager for method chaining
     * @see ShaderLoader#fromResources(String)
     */
    public ShaderManager registerResource(@NotNull String name, @NotNull String basePath) {
        return register(name, () -> ShaderLoader.fromResources(basePath));
    }

    /**
     * Convenience method to register a shader from filesystem files.
     * Uses implicit naming convention: basePath.vert and basePath.frag.
     *
     * @param name unique shader identifier
     * @param basePath filesystem path without extension
     * @return this manager for method chaining
     * @see ShaderLoader#fromFiles(Path)
     */
    public ShaderManager registerFile(@NotNull String name, @NotNull Path basePath) {
        return register(name, () -> ShaderLoader.fromFiles(basePath));
    }

    /**
     * Adds a pre-compiled shader program to the cache.
     * Useful for programmatically created shaders or shared instances.
     *
     * @param name unique shader identifier
     * @param program the shader program to cache
     * @return this manager for method chaining
     */
    public ShaderManager add(@NotNull String name, @NotNull ShaderProgram program) {
        shaders.put(name, program);
        return this;
    }

    /**
     * Retrieves or lazily loads a shader program by name.
     * Returns cached instance if available, otherwise invokes registered loader and caches result.
     * <p>
     * Subsequent calls return the same cached instance.
     *
     * @param name shader identifier
     * @return loaded shader program
     * @throws IllegalArgumentException if shader is not registered or loaded
     */
    @NotNull
    public ShaderProgram get(@NotNull String name) {
        ShaderProgram program = shaders.get(name);

        if (program == null) {
            Supplier<ShaderProgram> loader = loaders.get(name);
            if (loader == null) {
                throw new IllegalArgumentException("Unknown shader: " + name);
            }

            // Load on first access
            program = loader.get();
            shaders.put(name, program);
        }

        return program;
    }

    /**
     * Safely retrieves a shader, returning null if not found instead of throwing.
     * Useful for optional shader variants in flexible pipelines.
     *
     * @param name shader identifier
     * @return loaded shader program or null if not found/not registered
     */
    @Nullable
    public ShaderProgram getOrNull(@NotNull String name) {
        try {
            return get(name);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Checks whether a shader is registered or already loaded.
     *
     * @param name shader identifier
     * @return true if shader can be retrieved, false otherwise
     */
    public boolean has(@NotNull String name) {
        return shaders.containsKey(name) || loaders.containsKey(name);
    }

    /**
     * Reloads a single shader by clearing its cache and reinvoking the loader on next access.
     * Disposes the old shader program to release GPU resources.
     * <p>
     * Useful for hot-reloading during development or shader debugging.
     *
     * @param name shader identifier
     * @return this manager for method chaining
     */
    public ShaderManager reload(@NotNull String name) {
        ShaderProgram old = shaders.remove(name);
        if (old != null) {
            old.close();
        }

        // Will be reloaded on next get() call
        return this;
    }

    /**
     * Reloads all cached shader programs.
     * Useful for shader hot-reload workflows during development.
     *
     * @return this manager for method chaining
     */
    public ShaderManager reloadAll() {
        for (String name : shaders.keySet()) {
            reload(name);
        }
        return this;
    }

    /**
     * Removes and disposes a shader program from the cache.
     * The shader can be re-registered after removal.
     *
     * @param name shader identifier
     * @return this manager for method chaining
     */
    public ShaderManager remove(@NotNull String name) {
        ShaderProgram program = shaders.remove(name);
        if (program != null) {
            program.close();
        }
        loaders.remove(name);
        return this;
    }

    /**
     * Disposes all managed shader programs and clears internal state.
     * Should be called during application shutdown to prevent resource leaks.
     * <p>
     * After calling this method, the manager is empty and requires re-registration
     * for further use.
     */
    public void dispose() {
        for (ShaderProgram program : shaders.values()) {
            program.close();
        }
        shaders.clear();
        loaders.clear();
    }

    /**
     * Returns the count of currently loaded (compiled) shader programs.
     *
     * @return number of cached shader programs
     */
    public int loadedCount() {
        return shaders.size();
    }

    /**
     * Returns the count of registered shaders (both loaded and unloaded).
     *
     * @return number of registered loaders
     */
    public int registeredCount() {
        return loaders.size();
    }
}