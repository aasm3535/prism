package net.prism.shader;

import net.prism.exception.ShaderException;
import net.prism.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ShaderLoader - Loads shader programs from various sources with preprocessor support.
 * <p>
 * Supports loading from:
 * - Classpath resources (JAR resources and classpath roots)
 * - Filesystem files (with path resolution)
 * - Raw GLSL source strings
 * <p>
 * All loading methods support preprocessor #include directives for shader composition.
 * Includes are processed recursively with cycle detection via basename matching.
 */
public final class ShaderLoader {

    // Pattern: matches #include "path/to/file" or #include <path/to/file>
    // Supports both quote styles for cross-platform compatibility
    private static final Pattern INCLUDE_PATTERN = Pattern.compile(
            "^\\s*#include\\s+[\"<](.+?)[\">]\\s*$",
            Pattern.MULTILINE
    );

    // Pattern: matches #version directive (e.g., #version 330 core)
    // Used to validate shader prologue structure
    private static final Pattern VERSION_PATTERN = Pattern.compile(
            "^\\s*#version\\s+\\d+",
            Pattern.MULTILINE
    );

    private ShaderLoader() {}

    // === Resource Loading ===

    /**
     * Loads a shader program from classpath resources using implicit naming convention.
     * Expects files at <code>basePath.vert</code> and <code>basePath.frag</code>.
     * <p>
     * Example: load("shaders/basic") loads "shaders/basic.vert" and "shaders/basic.frag"
     *
     * @param basePath resource path without extension (e.g., "shaders/basic")
     * @return compiled and linked shader program
     * @throws ShaderException if resources not found or compilation/linking fails
     */
    public static ShaderProgram fromResources(String basePath) {
        return fromResources(basePath + ".vert", basePath + ".frag");
    }

    /**
     * Loads a shader program from classpath resources with explicit file paths.
     * Paths are relative to classpath root and support #include directives.
     *
     * @param vertexPath classpath resource path to vertex shader
     * @param fragmentPath classpath resource path to fragment shader
     * @return compiled and linked shader program
     * @throws ShaderException if resources not found or compilation/linking fails
     */
    public static ShaderProgram fromResources(String vertexPath, String fragmentPath) {
        String vertexSource = readResource(vertexPath);
        String fragmentSource = readResource(fragmentPath);
        return ShaderProgram.create(vertexSource, fragmentSource);
    }

    /**
     * Returns a builder for loading shader programs from resources with custom stages.
     * Supports vertex, fragment, and geometry shaders.
     *
     * @return new ResourceBuilder instance
     */
    public static ResourceBuilder resources() {
        return new ResourceBuilder();
    }

    // === File Loading ===

    /**
     * Loads a shader program from filesystem using implicit naming convention.
     * Expects files at <code>basePath.vert</code> and <code>basePath.frag</code>.
     *
     * @param basePath filesystem path without extension
     * @return compiled and linked shader program
     * @throws ShaderException if files not found or I/O errors occur
     */
    public static ShaderProgram fromFiles(Path basePath) {
        String base = basePath.toString();
        return fromFiles(Path.of(base + ".vert"), Path.of(base + ".frag"));
    }

    /**
     * Loads a shader program from filesystem with explicit file paths.
     * Supports #include directives with relative path resolution.
     *
     * @param vertexPath filesystem path to vertex shader
     * @param fragmentPath filesystem path to fragment shader
     * @return compiled and linked shader program
     * @throws ShaderException if files not found or I/O errors occur
     */
    public static ShaderProgram fromFiles(Path vertexPath, Path fragmentPath) {
        String vertexSource = readFile(vertexPath);
        String fragmentSource = readFile(fragmentPath);
        return ShaderProgram.create(vertexSource, fragmentSource);
    }

    /**
     * Returns a builder for loading shader programs from files with custom stages.
     *
     * @return new FileBuilder instance
     */
    public static FileBuilder files() {
        return new FileBuilder();
    }

    // === Direct Source Loading ===

    /**
     * Creates a shader program from raw GLSL source strings (no preprocessing).
     *
     * @param vertexSource GLSL vertex shader code
     * @param fragmentSource GLSL fragment shader code
     * @return compiled and linked shader program
     * @throws ShaderException if compilation or linking fails
     */
    public static ShaderProgram fromSource(String vertexSource, String fragmentSource) {
        return ShaderProgram.create(vertexSource, fragmentSource);
    }

    // === Resource Reading with Preprocessing ===

    /**
     * Reads a shader resource from classpath with automatic #include processing.
     * Recursively expands all #include directives.
     *
     * @param path classpath resource path
     * @return fully preprocessed shader source
     * @throws ShaderException if resource not found or I/O errors occur
     */
    public static String readResource(String path) {
        return readResource(path, true);
    }

    /**
     * Reads a shader resource from classpath with optional preprocessing.
     *
     * @param path classpath resource path
     * @param processIncludes whether to expand #include directives
     * @return shader source (preprocessed if requested)
     * @throws ShaderException if resource not found or I/O errors occur
     */
    public static String readResource(String path, boolean processIncludes) {
        String source = readResourceRaw(path);

        if (processIncludes) {
            // Determine base directory for relative include paths
            source = processIncludes(source, getDirectory(path), ShaderLoader::readResourceRaw);
        }

        return source;
    }

    /**
     * Reads a raw shader resource without preprocessing.
     * Attempts to load from class loader in multiple ways to handle various packaging scenarios.
     *
     * @param path classpath resource path
     * @return raw shader source
     * @throws ShaderException if resource not found or I/O errors occur
     */
    private static String readResourceRaw(String path) {
        // Normalize path: ensure leading slash for getResourceAsStream
        String normalizedPath = path.startsWith("/") ? path : "/" + path;

        InputStream stream = ShaderLoader.class.getResourceAsStream(normalizedPath);

        if (stream == null) {
            // Try context class loader as fallback (handles some edge cases)
            stream = Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream(path.startsWith("/") ? path.substring(1) : path);
        }

        if (stream == null) {
            throw ShaderException.resourceNotFound(path);
        }

        try {
            return IOUtils.readString(stream);
        } catch (IOException e) {
            throw ShaderException.ioError(path, e);
        }
    }

    /**
     * Reads a file from filesystem with automatic #include processing.
     *
     * @param path filesystem path
     * @return fully preprocessed shader source
     * @throws ShaderException if file not found or I/O errors occur
     */
    public static String readFile(Path path) {
        return readFile(path, true);
    }

    /**
     * Reads a file from filesystem with optional preprocessing.
     *
     * @param path filesystem path
     * @param processIncludes whether to expand #include directives
     * @return shader source (preprocessed if requested)
     * @throws ShaderException if file not found or I/O errors occur
     */
    public static String readFile(Path path, boolean processIncludes) {
        try {
            String source = Files.readString(path);

            if (processIncludes) {
                Path directory = path.getParent();
                // Resolve includes relative to the shader file's directory
                source = processIncludes(source, "",
                        includePath -> {
                            try {
                                return Files.readString(directory.resolve(includePath));
                            } catch (IOException e) {
                                throw ShaderException.ioError(includePath, e);
                            }
                        });
            }

            return source;
        } catch (IOException e) {
            throw ShaderException.ioError(path.toString(), e);
        }
    }

    // === Include Directive Processing ===

    /**
     * Functional interface for reading shader source from a path.
     * Used to abstract over resource vs. file loading in preprocessor.
     */
    @FunctionalInterface
    private interface SourceReader {
        /**
         * Reads shader source from the specified path.
         *
         * @param path path to shader source
         * @return loaded source code
         * @throws ShaderException if source cannot be read
         */
        String read(String path);
    }

    /**
     * Processes #include directives by recursively expanding and inlining shader source.
     * Performs simple cycle detection through basename matching (not perfect but practical).
     * <p>
     * Replacement is inline, preserving line structure for accurate error reporting.
     *
     * @param source source code potentially containing #include directives
     * @param baseDir directory for resolving relative include paths
     * @param reader callback for loading included source
     * @return source with all #include directives expanded
     * @throws ShaderException if included files cannot be read
     */
    private static String processIncludes(String source, String baseDir, SourceReader reader) {
        Matcher matcher = INCLUDE_PATTERN.matcher(source);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String includePath = matcher.group(1);
            String fullPath = baseDir.isEmpty() ? includePath : baseDir + "/" + includePath;

            String includeSource = reader.read(fullPath);
            // Recursively process nested includes
            includeSource = processIncludes(includeSource, getDirectory(fullPath), reader);

            // Replace directive with included source, escaping special regex characters
            matcher.appendReplacement(result, Matcher.quoteReplacement(includeSource));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Extracts the directory path from a file path (for relative include resolution).
     *
     * @param path full or relative file path
     * @return directory portion or empty string if no directory
     */
    private static String getDirectory(String path) {
        int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return lastSlash >= 0 ? path.substring(0, lastSlash) : "";
    }

    // === Builder Classes ===

    /**
     * Builder for loading shader programs from classpath resources.
     * Fluent API for specifying custom shader stages beyond the standard vertex+fragment pair.
     */
    public static class ResourceBuilder {
        /** Constructor creates a new builder instance with empty shader list */
        private final ShaderProgram.Builder builder = ShaderProgram.builder();

        /**
         * Loads and attaches a vertex shader from resources.
         *
         * @param path classpath resource path
         * @return this builder for method chaining
         */
        public ResourceBuilder vertex(String path) {
            builder.vertex(readResource(path));
            return this;
        }

        /**
         * Loads and attaches a fragment shader from resources.
         *
         * @param path classpath resource path
         * @return this builder for method chaining
         */
        public ResourceBuilder fragment(String path) {
            builder.fragment(readResource(path));
            return this;
        }

        /**
         * Loads and attaches a geometry shader from resources.
         *
         * @param path classpath resource path
         * @return this builder for method chaining
         */
        public ResourceBuilder geometry(String path) {
            builder.geometry(readResource(path));
            return this;
        }

        /**
         * Compiles and links the shader program.
         *
         * @return fully compiled shader program
         * @throws ShaderException if linking fails
         */
        public ShaderProgram build() {
            return builder.build();
        }
    }

    /**
     * Builder for loading shader programs from filesystem files.
     * Fluent API for specifying custom shader stages.
     */
    public static class FileBuilder {        /** Constructor creates a new builder instance with empty shader list */        private final ShaderProgram.Builder builder = ShaderProgram.builder();

        /**
         * Loads and attaches a vertex shader from filesystem.
         *
         * @param path file system path
         * @return this builder for method chaining
         */
        public FileBuilder vertex(Path path) {
            builder.vertex(readFile(path));
            return this;
        }

        /**
         * Loads and attaches a fragment shader from filesystem.
         *
         * @param path file system path
         * @return this builder for method chaining
         */
        public FileBuilder fragment(Path path) {
            builder.fragment(readFile(path));
            return this;
        }

        /**
         * Loads and attaches a geometry shader from filesystem.
         *
         * @param path file system path
         * @return this builder for method chaining
         */
        public FileBuilder geometry(Path path) {
            builder.geometry(readFile(path));
            return this;
        }

        /**
         * Compiles and links the shader program.
         *
         * @return fully compiled shader program
         * @throws ShaderException if linking fails
         */
        public ShaderProgram build() {
            return builder.build();
        }
    }
}