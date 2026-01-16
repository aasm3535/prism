package net.prism.shader;

import net.prism.exception.ShaderException;
import net.prism.uniform.UniformCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * ShaderProgram - Manages a linked OpenGL shader program with uniform state management.
 * <p>
 * Provides a fluent API for binding, uniform assignment, and attribute binding.
 * Maintains state to minimize redundant glUseProgram calls and caches uniform locations for performance.
 * Thread-safe binding state tracking via static variable; assumes single OpenGL context thread.
 */
public class ShaderProgram implements AutoCloseable {

    private final int programId;
    private final List<Shader> attachedShaders;
    private final UniformCache uniforms;

    private boolean disposed = false;
    // Tracks currently bound program ID to avoid redundant glUseProgram calls
    private static int currentlyBound = 0;

    /**
     * Constructs a shader program from a compiled program ID and attached shaders.
     * Internal constructor; use Builder or factory methods for instantiation.
     *
     * @param programId OpenGL program object ID (handle)
     * @param shaders list of compiled and attached shader objects
     */
    private ShaderProgram(int programId, List<Shader> shaders) {
        this.programId = programId;
        this.attachedShaders = shaders;
        this.uniforms = new UniformCache(programId);
    }

    // === Binding ===

    /**
     * Activates this shader program for rendering (glUseProgram).
     * Optimized to skip redundant calls if this program is already bound.
     *
     * @return this for method chaining
     */
    public ShaderProgram bind() {
        if (currentlyBound != programId) {
            GL20.glUseProgram(programId);
            currentlyBound = programId;
        }
        return this;
    }

    /**
     * Deactivates this shader program (glUseProgram(0)).
     * Resets global binding state tracking.
     *
     * @return this for method chaining
     */
    public ShaderProgram unbind() {
        GL20.glUseProgram(0);
        currentlyBound = 0;
        return this;
    }

    /**
     * Executes an action with this program bound.
     * Does not unbind after execution; use useAndUnbind for automatic cleanup.
     *
     * @param action callback to execute with this program active
     * @return this for method chaining
     */
    public ShaderProgram use(Consumer<ShaderProgram> action) {
        bind();
        action.accept(this);
        return this;
    }

    /**
     * Executes an action with this program bound and automatically unbinds afterward.
     * Ensures unbinding even if action throws an exception (try-finally semantics).
     *
     * @param action callback to execute with this program active
     * @return this for method chaining
     */
    public ShaderProgram useAndUnbind(Consumer<ShaderProgram> action) {
        bind();
        try {
            action.accept(this);
        } finally {
            unbind();
        }
        return this;
    }

    /**
     * Checks if this program is currently the active shader program.
     *
     * @return true if bound, false otherwise
     */
    public boolean isBound() {
        return currentlyBound == programId;
    }

    // === Uniforms - int ===

    /**
     * Sets a single integer uniform.
     *
     * @param name uniform variable name in the shader
     * @param value integer value
     * @return this for method chaining
     */
    public ShaderProgram uniform(@NotNull String name, int value) {
        GL20.glUniform1i(uniforms.location(name), value);
        return this;
    }

    /**
     * Sets a 2-component integer uniform (ivec2).
     *
     * @param name uniform variable name
     * @param x first component
     * @param y second component
     * @return this for method chaining
     */
    public ShaderProgram uniform(@NotNull String name, int x, int y) {
        GL20.glUniform2i(uniforms.location(name), x, y);
        return this;
    }

    /**
     * Sets a 3-component integer uniform (ivec3).
     *
     * @param name uniform variable name
     * @param x first component
     * @param y second component
     * @param z third component
     * @return this for method chaining
     */
    public ShaderProgram uniform(@NotNull String name, int x, int y, int z) {
        GL20.glUniform3i(uniforms.location(name), x, y, z);
        return this;
    }

    /**
     * Sets a 4-component integer uniform (ivec4).
     *
     * @param name uniform variable name
     * @param x first component
     * @param y second component
     * @param z third component
     * @param w fourth component
     * @return this for method chaining
     */
    public ShaderProgram uniform(@NotNull String name, int x, int y, int z, int w) {
        GL20.glUniform4i(uniforms.location(name), x, y, z, w);
        return this;
    }

    // === Uniforms - float ===

    /**
     * Sets a single floating-point uniform.
     *
     * @param name uniform variable name
     * @param value floating-point value
     * @return this for method chaining
     */
    public ShaderProgram uniform(@NotNull String name, float value) {
        GL20.glUniform1f(uniforms.location(name), value);
        return this;
    }

    /**
     * Sets a 2-component float uniform (vec2).
     *
     * @param name uniform variable name
     * @param x first component
     * @param y second component
     * @return this for method chaining
     */
    public ShaderProgram uniform(@NotNull String name, float x, float y) {
        GL20.glUniform2f(uniforms.location(name), x, y);
        return this;
    }

    /**
     * Sets a 3-component float uniform (vec3).
     *
     * @param name uniform variable name
     * @param x first component
     * @param y second component
     * @param z third component
     * @return this for method chaining
     */
    public ShaderProgram uniform(@NotNull String name, float x, float y, float z) {
        GL20.glUniform3f(uniforms.location(name), x, y, z);
        return this;
    }

    /**
     * Sets a 4-component float uniform (vec4).
     *
     * @param name uniform variable name
     * @param x first component
     * @param y second component
     * @param z third component
     * @param w fourth component
     * @return this for method chaining
     */
    public ShaderProgram uniform(@NotNull String name, float x, float y, float z, float w) {
        GL20.glUniform4f(uniforms.location(name), x, y, z, w);
        return this;
    }

    // === Uniforms - boolean ===

    /**
     * Sets a boolean uniform (stored as 0/1 integer in GLSL).
     *
     * @param name uniform variable name
     * @param value boolean value (true→1, false→0)
     * @return this for method chaining
     */
    public ShaderProgram uniform(@NotNull String name, boolean value) {
        GL20.glUniform1i(uniforms.location(name), value ? 1 : 0);
        return this;
    }

    // === Uniforms - arrays ===

    /**
     * Sets an array of floating-point values (vec1 array).
     *
     * @param name uniform array variable name
     * @param values float array
     * @return this for method chaining
     */
    public ShaderProgram uniform(@NotNull String name, float[] values) {
        GL20.glUniform1fv(uniforms.location(name), values);
        return this;
    }

    /**
     * Sets an array of integer values (int array).
     *
     * @param name uniform array variable name
     * @param values integer array
     * @return this for method chaining
     */
    public ShaderProgram uniform(@NotNull String name, int[] values) {
        GL20.glUniform1iv(uniforms.location(name), values);
        return this;
    }

    // === Uniforms - matrices ===

    /**
     * Sets a 2×2 matrix uniform (mat2).
     * Caller is responsible for maintaining correct matrix layout (row vs column major).
     *
     * @param name uniform variable name
     * @param transpose whether to transpose the matrix before passing to shader (usually false)
     * @param matrix FloatBuffer containing 4 elements in row-major or column-major order
     * @return this for method chaining
     */
    public ShaderProgram uniformMatrix2(@NotNull String name, boolean transpose, FloatBuffer matrix) {
        GL20.glUniformMatrix2fv(uniforms.location(name), transpose, matrix);
        return this;
    }

    /**
     * Sets a 3×3 matrix uniform (mat3).
     *
     * @param name uniform variable name
     * @param transpose whether to transpose the matrix before passing to shader
     * @param matrix FloatBuffer containing 9 elements
     * @return this for method chaining
     */
    public ShaderProgram uniformMatrix3(@NotNull String name, boolean transpose, FloatBuffer matrix) {
        GL20.glUniformMatrix3fv(uniforms.location(name), transpose, matrix);
        return this;
    }

    /**
     * Sets a 4×4 matrix uniform (mat4) from a FloatBuffer.
     *
     * @param name uniform variable name
     * @param transpose whether to transpose the matrix
     * @param matrix FloatBuffer containing 16 elements
     * @return this for method chaining
     */
    public ShaderProgram uniformMatrix4(@NotNull String name, boolean transpose, FloatBuffer matrix) {
        GL20.glUniformMatrix4fv(uniforms.location(name), transpose, matrix);
        return this;
    }

    /**
     * Sets a 4×4 matrix uniform (mat4) from a float array.
     * Allocates a temporary FloatBuffer on the stack for efficiency.
     *
     * @param name uniform variable name
     * @param transpose whether to transpose the matrix
     * @param matrix float array containing 16 elements (4×4 in row-major order)
     * @return this for method chaining
     */
    public ShaderProgram uniformMatrix4(@NotNull String name, boolean transpose, float[] matrix) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.floats(matrix);
            GL20.glUniformMatrix4fv(uniforms.location(name), transpose, buffer);
        }
        return this;
    }

    // === Uniform queries ===

    /**
     * Retrieves the OpenGL uniform location for direct access.
     * Location is cached for subsequent calls.
     *
     * @param name uniform variable name
     * @return uniform location or -1 if not found/not active
     */
    public int getUniformLocation(@NotNull String name) {
        return uniforms.location(name);
    }

    /**
     * Checks if a uniform exists in this shader program.
     * Useful for conditional uniform assignment in flexible shader pipelines.
     *
     * @param name uniform variable name
     * @return true if uniform exists and is active, false otherwise
     */
    public boolean hasUniform(@NotNull String name) {
        return uniforms.exists(name);
    }

    /**
     * Sets a uniform only if it exists in the shader program.
     * Prevents errors when using shader variants with different uniform sets.
     *
     * @param name uniform variable name
     * @param value floating-point value
     * @return this for method chaining
     */
    public ShaderProgram uniformIfExists(@NotNull String name, float value) {
        if (hasUniform(name)) {
            uniform(name, value);
        }
        return this;
    }

    // === Texture samplers ===

    /**
     * Sets a sampler uniform to bind a texture unit.
     * Maps uniform name to a texture unit index (0-based).
     *
     * @param name sampler uniform variable name
     * @param textureUnit texture unit index (0 = GL_TEXTURE0, 1 = GL_TEXTURE1, etc.)
     * @return this for method chaining
     */
    public ShaderProgram sampler(@NotNull String name, int textureUnit) {
        return uniform(name, textureUnit);
    }

    // === Attribute locations ===

    /**
     * Retrieves the OpenGL attribute location for a vertex attribute.
     *
     * @param name attribute variable name in vertex shader
     * @return attribute location or -1 if not found/not active
     */
    public int getAttributeLocation(@NotNull String name) {
        return GL20.glGetAttribLocation(programId, name);
    }

    /**
     * Binds a vertex attribute to a specific index before linking.
     * Typically used when manual attribute location control is required.
     *
     * @param index vertex attribute index (0-15 typically)
     * @param name attribute variable name
     * @return this for method chaining
     */
    public ShaderProgram bindAttributeLocation(int index, @NotNull String name) {
        GL20.glBindAttribLocation(programId, index, name);
        return this;
    }

    // === Getters ===

    /**
     * Returns the OpenGL program object ID.
     *
     * @return program handle for direct OpenGL calls
     */
    public int id() {
        return programId;
    }

    /**
     * Indicates whether this program has been disposed and its resources freed.
     *
     * @return true if closed, false otherwise
     */
    public boolean isDisposed() {
        return disposed;
    }

    // === Resource Cleanup ===

    /**
     * Releases all OpenGL resources associated with this shader program.
     * Detaches and deletes all shader objects, then deletes the program.
     * Safe to call multiple times (idempotent).
     * Automatically called if this program is used in try-with-resources.
     */
    @Override
    public void close() {
        if (!disposed) {
            if (isBound()) {
                unbind();
            }

            for (Shader shader : attachedShaders) {
                GL20.glDetachShader(programId, shader.id());
                shader.close();
            }

            GL20.glDeleteProgram(programId);
            disposed = true;
        }
    }

    // === Builder Pattern ===

    /**
     * Returns a new builder for constructing shader programs with custom shader stages.
     *
     * @return new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Convenience factory method for creating programs with vertex and fragment shaders.
     * Equivalent to builder().vertex(vSrc).fragment(fSrc).build() but more concise.
     *
     * @param vertexSource GLSL vertex shader source
     * @param fragmentSource GLSL fragment shader source
     * @return compiled and linked shader program
     * @throws ShaderException if compilation or linking fails
     */
    public static ShaderProgram create(String vertexSource, String fragmentSource) {
        return builder()
                .vertex(vertexSource)
                .fragment(fragmentSource)
                .build();
    }

    /**
     * Builder for fluent shader program construction supporting all shader stages.
     * <p>
     * Compiles shaders as they are attached, then links the program during build().
     * Provides comprehensive error handling with detailed diagnostics.
     */
    public static class Builder {
        /** Constructor creates a new builder instance with empty shader list */
        private final List<Shader> shaders = new ArrayList<>();

        /**
         * Attaches a shader of the specified type to this program.
         *
         * @param type shader stage (vertex, fragment, geometry, etc.)
         * @param source GLSL source code
         * @return this builder for method chaining
         * @throws ShaderException if compilation fails
         */
        public Builder attach(ShaderType type, String source) {
            shaders.add(new Shader(type, source));
            return this;
        }

        /**
         * Attaches a vertex shader stage.
         *
         * @param source GLSL vertex shader source
         * @return this builder for method chaining
         */
        public Builder vertex(String source) {
            return attach(ShaderType.VERTEX, source);
        }

        /**
         * Attaches a fragment/pixel shader stage.
         *
         * @param source GLSL fragment shader source
         * @return this builder for method chaining
         */
        public Builder fragment(String source) {
            return attach(ShaderType.FRAGMENT, source);
        }

        /**
         * Attaches a geometry shader stage (GL 3.2+).
         *
         * @param source GLSL geometry shader source
         * @return this builder for method chaining
         */
        public Builder geometry(String source) {
            return attach(ShaderType.GEOMETRY, source);
        }

        /**
         * Attaches a compute shader stage (GL 4.3+).
         *
         * @param source GLSL compute shader source
         * @return this builder for method chaining
         */
        public Builder compute(String source) {
            return attach(ShaderType.COMPUTE, source);
        }

        /**
         * Sets an optional debug label for this program (for driver diagnostics).
         *
         * @param label descriptive name for debugging
         * @return this builder for method chaining
         */
        public Builder label(@Nullable String label) {
            // Label support reserved for future use (KHR_debug extension)
            return this;
        }

        /**
         * Compiles and links all attached shaders into a complete program.
         * Performs comprehensive error checking and validation.
         * <p>
         * Process:
         * 1. Validates that at least one shader was attached
         * 2. Creates OpenGL program object
         * 3. Attaches all compiled shaders
         * 4. Invokes linker to resolve cross-shader references
         * 5. Validates program (warnings may be logged)
         * 6. Returns new ShaderProgram instance
         *
         * @return compiled and ready-to-use shader program
         * @throws ShaderException if validation fails or no shaders attached
         */
        public ShaderProgram build() {
            if (shaders.isEmpty()) {
                throw new ShaderException(
                        ShaderException.ErrorType.INVALID_STATE,
                        "No shaders attached to program"
                );
            }

            int programId = GL20.glCreateProgram();

            if (programId == 0) {
                throw new ShaderException(
                        ShaderException.ErrorType.LINKING_FAILED,
                        "Failed to create shader program"
                );
            }

            // Attach all compiled shader objects to the program
            for (Shader shader : shaders) {
                GL20.glAttachShader(programId, shader.id());
            }

            // Invoke linker to resolve cross-shader references and optimize
            GL20.glLinkProgram(programId);

            // Check linking success
            if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                String log = GL20.glGetProgramInfoLog(programId, 8192);

                // Clean up on failure: detach and delete all shaders and program
                for (Shader shader : shaders) {
                    GL20.glDetachShader(programId, shader.id());
                    shader.close();
                }
                GL20.glDeleteProgram(programId);

                throw ShaderException.linkingFailed(log);
            }

            // Optional: Validate program for current OpenGL state (non-fatal warnings)
            GL20.glValidateProgram(programId);
            if (GL20.glGetProgrami(programId, GL20.GL_VALIDATE_STATUS) == GL11.GL_FALSE) {
                String log = GL20.glGetProgramInfoLog(programId, 8192);
                System.err.println("[Prism] Shader validation warning: " + log);
            }

            return new ShaderProgram(programId, shaders);
        }
    }
}