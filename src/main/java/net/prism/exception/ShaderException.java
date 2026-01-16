package net.prism.exception;

/**
 * ShaderException - Custom exception for shader-related errors with categorized error types.
 * <p>
 * Provides detailed error classification for better debugging and error handling strategies.
 * Includes factory methods for common error scenarios to promote consistency across the codebase.
 */
public class ShaderException extends RuntimeException {

    /** Error classification for this exception */
    private final ErrorType type;

    /**
     * Categorized error types for fine-grained error handling and diagnostics.
     */
    public enum ErrorType {
        /** GLSL compilation errors in individual shaders */
        COMPILATION_FAILED,
        /** Shader program linking phase failures */
        LINKING_FAILED,
        /** Post-link validation warnings */
        VALIDATION_FAILED,
        /** Uniform variable lookup failures */
        UNIFORM_NOT_FOUND,
        /** Missing shader source files or resources */
        RESOURCE_NOT_FOUND,
        /** File I/O and stream reading errors */
        IO_ERROR,
        /** Logical state violations (e.g., no shaders attached) */
        INVALID_STATE
    }

    /**
     * Constructs a ShaderException with error type and message.
     *
     * @param type the error classification
     * @param message descriptive error message with diagnostic information
     */
    public ShaderException(ErrorType type, String message) {
        super(message);
        this.type = type;
    }

    /**
     * Constructs a ShaderException with error type, message, and root cause.
     * Preserves the original exception chain for complete stack trace analysis.
     *
     * @param type the error classification
     * @param message descriptive error message
     * @param cause the underlying exception that triggered this error
     */
    public ShaderException(ErrorType type, String message, Throwable cause) {
        super(message, cause);
        this.type = type;
    }

    /**
     * Returns the error type for specialized error handling and recovery strategies.
     *
     * @return error classification
     */
    public ErrorType getType() {
        return type;
    }

    // === Factory Methods for Common Error Scenarios ===

    /**
     * Factory method for shader compilation failures with error logs.
     * Captures both shader name and GLSL compiler diagnostic information.
     *
     * @param shaderName display name of the shader (e.g., "vertex", "fragment")
     * @param log compiler error output containing line numbers and error details
     * @return ShaderException configured for compilation errors
     */
    public static ShaderException compilationFailed(String shaderName, String log) {
        return new ShaderException(
                ErrorType.COMPILATION_FAILED,
                "Failed to compile shader '%s': %s".formatted(shaderName, log)
        );
    }

    /**
     * Factory method for shader program linking failures.
     * Linking errors occur after individual shader compilation during the linker phase.
     *
     * @param log linker diagnostic output with cross-shader compatibility information
     * @return ShaderException configured for linking errors
     */
    public static ShaderException linkingFailed(String log) {
        return new ShaderException(
                ErrorType.LINKING_FAILED,
                "Failed to link shader program: " + log
        );
    }

    /**
     * Factory method for missing shader resources (classpath or filesystem).
     * Helps distinguish between I/O errors and resource resolution failures.
     *
     * @param path the resource path that could not be located
     * @return ShaderException configured for resource not found errors
     */
    public static ShaderException resourceNotFound(String path) {
        return new ShaderException(
                ErrorType.RESOURCE_NOT_FOUND,
                "Shader resource not found: " + path
        );
    }

    /**
     * Factory method for I/O failures during shader source reading.
     * Preserves the root cause for debugging filesystem and permission issues.
     *
     * @param path the file path being read when the error occurred
     * @param cause the IOException that was thrown during file access
     * @return ShaderException with complete exception chain
     */
    public static ShaderException ioError(String path, Throwable cause) {
        return new ShaderException(
                ErrorType.IO_ERROR,
                "Failed to read shader: " + path,
                cause
        );
    }
}