package net.prism.shader;

import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.GL43;

/**
 * ShaderType - Enumeration of OpenGL shader types with metadata and extension mapping.
 * <p>
 * Provides bidirectional mapping between shader types, file extensions, and OpenGL constants.
 * Supports all modern GPU pipeline stages including compute shaders (GL 4.3+).
 */
public enum ShaderType {

    /** Vertex shader stage (GL 2.0+) - processes each vertex independently */
    VERTEX(GL20.GL_VERTEX_SHADER, "vert", "vertex"),
    /** Fragment shader stage (GL 2.0+) - produces final pixel colors */
    FRAGMENT(GL20.GL_FRAGMENT_SHADER, "frag", "fragment"),
    /** Geometry shader stage (GL 3.2+) - generates primitives from input vertices */
    GEOMETRY(GL32.GL_GEOMETRY_SHADER, "geom", "geometry"),
    /** Tessellation control shader (GL 4.0+) - tessellation level control */
    TESS_CONTROL(GL40.GL_TESS_CONTROL_SHADER, "tesc", "tess_control"),
    /** Tessellation evaluation shader (GL 4.0+) - evaluates tessellated vertices */
    TESS_EVALUATION(GL40.GL_TESS_EVALUATION_SHADER, "tese", "tess_evaluation"),
    /** Compute shader (GL 4.3+) - general-purpose GPU computing */
    COMPUTE(GL43.GL_COMPUTE_SHADER, "comp", "compute");

    private final int glType;
    private final String extension;
    private final String name;

    /**
     * Constructs a shader type with OpenGL constant, file extension, and display name.
     *
     * @param glType OpenGL shader type constant (GL_VERTEX_SHADER, GL_FRAGMENT_SHADER, etc.)
     * @param extension primary file extension without leading dot
     * @param name human-readable shader type name for debug output
     */
    ShaderType(int glType, String extension, String name) {
        this.glType = glType;
        this.extension = extension;
        this.name = name;
    }

    /**
     * Returns the OpenGL shader type constant for use with glCreateShader.
     *
     * @return GL constant for this shader type
     */
    public int glType() {
        return glType;
    }

    /**
     * Returns the primary file extension for this shader type.
     *
     * @return extension without leading dot (e.g., "vert", "frag")
     */
    public String extension() {
        return extension;
    }

    /**
     * Returns the human-readable display name for diagnostics and error messages.
     *
     * @return shader type name (e.g., "vertex", "fragment")
     */
    public String displayName() {
        return name;
    }

    /**
     * Maps a file extension (with or without leading dot) to its corresponding shader type.
     * Supports both canonical extensions and common aliases (vs→vertex, fs→fragment, etc.).
     * <p>
     * This enables flexible file naming conventions while maintaining type safety.
     *
     * @param ext file extension with or without leading dot
     * @return ShaderType enum value
     * @throws IllegalArgumentException if extension is not recognized
     */
    public static ShaderType fromExtension(String ext) {
        String normalized = ext.startsWith(".") ? ext.substring(1) : ext;
        
        // First, check canonical extensions
        for (ShaderType type : values()) {
            if (type.extension.equals(normalized)) {
                return type;
            }
        }
        
        // Then, check common aliases for backward compatibility and cross-platform support
        return switch (normalized) {
            case "vs", "vsh" -> VERTEX;
            case "fs", "fsh", "ps" -> FRAGMENT;
            case "gs", "gsh" -> GEOMETRY;
            case "cs", "csh" -> COMPUTE;
            default -> throw new IllegalArgumentException("Unknown shader extension: " + ext);
        };
    }
}