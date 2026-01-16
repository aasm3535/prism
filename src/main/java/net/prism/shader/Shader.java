package net.prism.shader;

import net.prism.exception.ShaderException;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

/**
 * Shader - Represents a single compiled OpenGL shader object (vertex, fragment, geometry, etc.).
 * <p>
 * Immutable after construction with lifecycle management through AutoCloseable.
 * Handles GLSL compilation with detailed error diagnostics and proper resource cleanup.
 */
public class Shader implements AutoCloseable {

    private final int id;
    private final ShaderType type;
    private final String source;
    private boolean disposed = false;

    /**
     * Constructs and compiles a new shader from GLSL source code.
     * Compilation is performed immediately in the calling thread's OpenGL context.
     *
     * @param type the shader type (vertex, fragment, geometry, etc.)
     * @param source GLSL source code as a string
     * @throws ShaderException if compilation fails with GLSL compiler errors
     */
    public Shader(ShaderType type, String source) {
        this.type = type;
        this.source = source;
        this.id = compile(type, source);
    }

    /**
     * Compiles GLSL source code into an OpenGL shader object.
     * Performs comprehensive error checking with detailed compiler diagnostic output.
     * <p>
     * Process:
     * 1. Creates shader object handle from OpenGL
     * 2. Uploads source code to GPU
     * 3. Invokes GLSL compiler
     * 4. Validates compilation status and extracts error logs if compilation failed
     *
     * @param type the shader type constant for glCreateShader
     * @param source GLSL source code
     * @return OpenGL shader object ID (handle)
     * @throws ShaderException if shader creation or compilation fails
     */
    private static int compile(ShaderType type, String source) {
        int shaderId = GL20.glCreateShader(type.glType());

        if (shaderId == 0) {
            throw new ShaderException(
                    ShaderException.ErrorType.COMPILATION_FAILED,
                    "Failed to create shader object for type: " + type.displayName()
            );
        }

        GL20.glShaderSource(shaderId, source);
        GL20.glCompileShader(shaderId);

        // Check compilation status
        if (GL20.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(shaderId, 8192);
            GL20.glDeleteShader(shaderId);
            throw ShaderException.compilationFailed(type.displayName(), log);
        }

        return shaderId;
    }

    /**
     * Returns the OpenGL shader object ID (handle).
     * Used for attaching to shader programs via glAttachShader.
     *
     * @return OpenGL shader ID
     */
    public int id() {
        return id;
    }

    /**
     * Returns the shader type for debugging and error messages.
     *
     * @return ShaderType enum value
     */
    public ShaderType type() {
        return type;
    }

    /**
     * Returns the original GLSL source code.
     * Useful for debugging and diagnostics.
     *
     * @return source code string
     */
    public String source() {
        return source;
    }

    /**
     * Indicates whether this shader has been disposed and its GPU resources freed.
     *
     * @return true if closed, false otherwise
     */
    public boolean isDisposed() {
        return disposed;
    }

    /**
     * Releases the OpenGL shader object and frees GPU memory.
     * Safe to call multiple times (idempotent).
     * Called automatically if this shader is used in try-with-resources.
     */
    @Override
    public void close() {
        if (!disposed) {
            GL20.glDeleteShader(id);
            disposed = true;
        }
    }
}