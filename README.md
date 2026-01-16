# Prism

Lightweight OpenGL shader management library for Java with fluent API, caching, and preprocessor support.

## Installation

### Gradle

Add to your `build.gradle`:

```gradle
repositories {
    mavenLocal()
}

dependencies {
    implementation 'net.prism:Prism:1.0.0'
}
```

## Quick Start

### Basic Usage - Load and Render

```java
import net.prism.Prism;
import net.prism.shader.ShaderProgram;

public class Example {
    public static void main(String[] args) {
        // Load shader from classpath resources (expects basic.vert and basic.frag)
        ShaderProgram shader = Prism.load("shaders/basic");
        
        // Use shader with uniforms
        shader.bind()
              .uniform("u_color", 1.0f, 0.0f, 0.0f, 1.0f)
              .uniform("u_time", (float) System.currentTimeMillis() / 1000f);
        
        // Render with the bound shader
        // ... rendering code ...
        
        shader.unbind();
    }
}
```

### Create from Source Code

```java
String vertexSource = """
    #version 330 core
    layout(location = 0) in vec3 position;
    layout(location = 1) in vec3 color;
    
    out VS_OUT {
        vec3 color;
    } vs_out;
    
    void main() {
        gl_Position = vec4(position, 1.0);
        vs_out.color = color;
    }
""";

String fragmentSource = """
    #version 330 core
    in VS_OUT {
        vec3 color;
    } fs_in;
    
    out vec4 FragColor;
    
    void main() {
        FragColor = vec4(fs_in.color, 1.0);
    }
""";

ShaderProgram shader = Prism.create(vertexSource, fragmentSource);
shader.bind();
// ... render ...
shader.unbind();
```

### Shader Manager - Lazy Loading

```java
import net.prism.manager.ShaderManager;

ShaderManager manager = Prism.manager();

// Register shaders for lazy loading
manager.registerResource("basic", "shaders/basic")
       .registerResource("advanced", "shaders/advanced")
       .registerFile("custom", "C:/shaders/custom.vert", "C:/shaders/custom.frag");

// Shaders are compiled only on first access
ShaderProgram basic = manager.get("basic");           // Compiles on first call
ShaderProgram adv = manager.get("advanced");          // Cached, no recompilation

// Safe nullable access
ShaderProgram optional = manager.getOrNull("unknown"); // Returns null if not found

// Hot reload
manager.reload("basic");

// Cleanup all shaders
manager.dispose();
```

### Uniform Methods - Type Safe

```java
shader.bind();

// Scalar uniforms
shader.uniform("u_int", 42)
      .uniform("u_uint", 100)
      .uniform("u_float", 3.14f)
      .uniform("u_double", 2.718);

// Boolean uniforms
shader.uniform("u_active", true)
      .uniformIfExists("u_optional", false);

// Vector uniforms
shader.uniform("u_vec2", 1.0f, 2.0f)
      .uniform("u_vec3", 1.0f, 2.0f, 3.0f)
      .uniform("u_vec4", 1.0f, 0.0f, 0.0f, 1.0f);

// Array uniforms
int[] ints = {1, 2, 3, 4, 5};
float[] floats = {0.1f, 0.2f, 0.3f, 0.4f, 0.5f};
shader.uniform("u_ints", ints)
      .uniform("u_floats", floats);

// Matrix uniforms (column-major, no transpose)
FloatBuffer matrix = BufferUtils.createFloatBuffer(16);
shader.uniformMatrix4("u_transform", false, matrix)
      .uniformMatrix3("u_normalMatrix", false, matrix);

shader.unbind();
```

### Preprocessor - Include Support

**common.glsl:**
```glsl
float rand(vec2 co) {
    return fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453);
}

vec3 mix3(vec3 a, vec3 b, float t) {
    return a * (1.0 - t) + b * t;
}
```

**shader.vert:**
```glsl
#version 330 core
#include "common.glsl"

layout(location = 0) in vec3 position;

void main() {
    gl_Position = vec4(position + vec3(rand(position.xy), 0.0), 1.0);
}
```

Usage:
```java
ShaderProgram shader = Prism.load("shaders/shader");  // Includes resolved automatically
```

### Error Handling

```java
import net.prism.exception.ShaderException;

try {
    ShaderProgram shader = Prism.load("shaders/broken");
} catch (ShaderException e) {
    switch (e.getType()) {
        case COMPILATION_FAILED:
            System.err.println("GLSL Compilation Error:");
            System.err.println(e.getMessage());
            break;
        case LINKING_FAILED:
            System.err.println("Program Linking Error:");
            System.err.println(e.getMessage());
            break;
        case RESOURCE_NOT_FOUND:
            System.err.println("Shader File Not Found:");
            System.err.println(e.getMessage());
            break;
        case IO_ERROR:
            System.err.println("I/O Error Reading Shader:");
            System.err.println(e.getMessage());
            break;
        default:
            System.err.println("Unknown Error: " + e.getMessage());
    }
}
```

### Builder Pattern

```java
import net.prism.shader.ShaderType;
import net.prism.shader.ShaderProgram;

// Build complex shader programs with multiple stages
ShaderProgram shader = ShaderProgram.builder()
    .attach(ShaderType.VERTEX, vertexSource)
    .attach(ShaderType.FRAGMENT, fragmentSource)
    .attach(ShaderType.GEOMETRY, geometrySource)  // Optional
    .build();
```

## Performance Tips

- Use `ShaderManager` for multiple shaders (caches compilations)
- Bind shaders only when needed (state change cost)
- Batch uniform updates per shader binding
- Use `uniformIfExists()` for optional uniforms
- Uniform locations are cached (O(1) lookup after first call)

## Architecture

- **Shader** - Compiled shader object (immutable, AutoCloseable)
- **ShaderProgram** - Linked program with uniform/attribute management (30+ uniform methods)
- **ShaderLoader** - Loads shaders from resources/files with preprocessor
- **ShaderManager** - Centralized lifecycle, lazy loading, hot-reload
- **UniformCache** - Memoizes uniform location lookups (HashMap based)
- **GLUtils** - Error checking and GPU info queries
- **IOUtils** - Efficient stream reading with UTF-8 encoding

## Building

```bash
# Build library
./gradlew build

# Build with documentation
./gradlew javadoc

# Publish to local Maven
./gradlew publishToMavenLocal

# Run tests
./gradlew test
```

## License

MIT License - See LICENSE file

## Links

- GitHub: https://github.com/aasm3535/prism
- Issues: https://github.com/aasm3535/prism/issues
- Releases: https://github.com/aasm3535/prism/releases
- **ShaderException** - Categorized error types

## Performance

- Uniform locations are cached (O(1) lookups)
- Binding state tracked to skip redundant glUseProgram calls
- Lazy loading reduces startup time
- Batch uniform assignment with method chaining

## Building

```bash
./gradlew build
./gradlew test
./gradlew javadoc
```

## Structure

```
src/main/java/net/prism/
  Prism.java
  exception/ShaderException.java
  manager/ShaderManager.java
  shader/
    Shader.java
    ShaderProgram.java
    ShaderType.java
    ShaderLoader.java
  uniform/UniformCache.java
  util/
    GLUtils.java
    IOUtils.java
```

## License

MIT License
