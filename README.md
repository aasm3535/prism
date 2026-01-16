# Prism

Lightweight OpenGL shader management library for Java with fluent API, caching, and preprocessor support.

## Features

- Fluent API for shader binding and uniform management
- Automatic uniform location caching for performance
- Preprocessor support for #include directives
- Centralized shader manager with lazy loading
- Comprehensive error handling with GLSL diagnostics
- Type-safe uniform methods for all GLSL types
- Thread-safe manager using ConcurrentHashMap

## Requirements

- Java 11+
- Gradle 6.0+
- LWJGL 3.x
- OpenGL 3.2+

## Installation

```gradle
dependencies {
    implementation project(':Prism')
}
```

## Quick Start

### Load and Render

```java
ShaderProgram shader = Prism.load("shaders/basic");
shader.bind()
      .uniform("u_color", 1.0f, 0.0f, 0.0f, 1.0f)
      .uniform("u_time", time);

// Render...

shader.unbind();
```

### Create from Source

```java
String vertex = """
    #version 330 core
    layout(location = 0) in vec3 position;
    void main() { gl_Position = vec4(position, 1.0); }
""";

String fragment = """
    #version 330 core
    out vec4 FragColor;
    void main() { FragColor = vec4(1.0, 0.5, 0.2, 1.0); }
""";

ShaderProgram shader = Prism.create(vertex, fragment);
```

### Shader Manager

```java
ShaderManager manager = Prism.manager();

manager.registerResource("basic", "shaders/basic")
       .registerResource("advanced", "shaders/advanced");

ShaderProgram basic = manager.get("basic");      // Lazy-loaded
ShaderProgram adv = manager.getOrNull("advanced");

manager.reload("basic");  // Hot-reload
manager.dispose();        // Cleanup
```

### Uniforms

```java
shader.uniform("u_int", 42)
      .uniform("u_float", 3.14f)
      .uniform("u_vec3", x, y, z)
      .uniform("u_floats", floatArray)
      .uniformMatrix4("u_transform", false, matrix);
```

## Preprocessor

Include shaders using #include directives:

```glsl
#version 330 core
#include "common.glsl"

void main() { /* ... */ }
```

## Error Handling

```java
try {
    ShaderProgram shader = Prism.load("shaders/broken");
} catch (ShaderException e) {
    System.err.println("Type: " + e.getType());
    System.err.println("Message: " + e.getMessage());
}
```

## Architecture

- **Shader** - Compiled shader object (immutable, AutoCloseable)
- **ShaderProgram** - Linked program with uniform and attribute management
- **ShaderLoader** - Loads shaders with preprocessor support
- **ShaderManager** - Centralized lifecycle and caching
- **UniformCache** - Memoizes uniform location lookups
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
