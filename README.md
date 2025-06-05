# Pathetic

<div align="center">
  <h3>A high-performance pathfinding library for 3D environments</h3>
</div>

<p align="center">
  <a href="https://github.com/bsommerfeld/pathetic/actions/workflows/build.yml">
    <img src="https://github.com/bsommerfeld/pathetic/actions/workflows/build.yml/badge.svg" alt="Build Status" />
  </a>
  <a href="https://github.com/bsommerfeld/pathetic/blob/main/LICENSE">
    <img src="https://img.shields.io/github/license/bsommerfeld/pathetic" alt="License" />
  </a>
  <a href="https://javadocs.pathetic.ollieee.xyz/">
    <img src="https://img.shields.io/badge/javadocs-online-blue" alt="Javadocs" />
  </a>
  <a href="https://discord.gg/zGx9BSzKfJ">
    <img src="https://img.shields.io/discord/999275610385948692?color=7289DA&label=discord&logo=discord&logoColor=white" alt="Discord" />
  </a>
</p>

## Overview

Pathetic is a highly configurable A* pathfinding library for Java, designed for extensibility through custom node validation and cost processing. It's ideal for 3D environments and game development.

- ⚡ **High Performance**: Optimized for real-time pathfinding in 3D environments
- 🔄 **Asynchronous**: Non-blocking pathfinding operations
- 🧩 **Extensible**: Customizable through hooks, validators, and cost processors
- 🛠️ **Configurable**: Fine-tune the algorithm with custom heuristic weights
- 🔍 **Robust**: Handles edge cases with fallback mechanisms

> **Note**: [pathetic-bukkit](https://github.com/bsommerfeld/pathetic-bukkit) was originally part of the repository but has been moved to its own repository for better modularity and to serve a wider range of applications.

## Modules

Pathetic consists of two main modules:

### 🔌 API

The API module defines the interfaces and classes that form the public API of the library. It provides a clean interface for configuring and executing pathfinding operations.

### ⚙️ Engine

The Engine module implements the interfaces defined in the API and provides the actual pathfinding algorithms and logic.

## Installation

### Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <!-- API only -->
    <dependency>
        <groupId>com.github.bsommerfeld.pathetic</groupId>
        <artifactId>api</artifactId>
        <version>5.0.0</version>
    </dependency>

    <!-- Engine implementation -->
    <dependency>
        <groupId>com.github.bsommerfeld.pathetic</groupId>
        <artifactId>engine</artifactId>
        <version>5.0.0</version>
    </dependency>
</dependencies>
```

### Gradle

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    // API only
    implementation 'com.github.bsommerfeld.pathetic:api:5.0.0'

    // Engine implementation
    implementation 'com.github.bsommerfeld.pathetic:engine:5.0.0'
}
```

## Quick Start

```java
// Create a navigation point provider
NavigationPointProvider provider = new MyNavigationPointProvider();

// Create a configuration
PathfinderConfiguration config = PathfinderConfiguration.builder()
    .maxIterations(1000)
    .maxLength(100)
    .async(true)
    .provider(provider)
    .heuristicWeights(HeuristicWeights.NATURAL_PATH_WEIGHTS)
    .build();

// Create a pathfinder
PathfinderFactory factory = new AStarPathfinderFactory();
Pathfinder pathfinder = factory.createPathfinder(config);

// Define start and target positions
PathPosition start = new PathPosition(environment, 0, 0, 0);
PathPosition target = new PathPosition(environment, 10, 5, 10);

// Find a path
CompletionStage<PathfinderResult> resultFuture = pathfinder.findPath(start, target);

// Handle the result
resultFuture.thenAccept(result -> {
    if (result.successful()) {
        Path path = result.getPath();
        // Use the path...
    } else {
        // Handle failure...
    }
});
```

## Documentation

- [Javadocs](https://javadocs.pathetic.ollieee.xyz/)
- [API Wiki](api/pathetic-api-wiki.md)
- [Engine Wiki](engine/pathetic-engine-wiki.md)

> **Coming Soon**: A comprehensive wiki will be built in the repository's GitHub Wiki section to provide more detailed documentation, tutorials, and examples.

## Example Implementation

For a practical implementation of Pathetic, check out [pathetic-bukkit](https://github.com/bsommerfeld/pathetic-bukkit).

## Contributing

We welcome contributions! Here's how you can help:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

For major changes, please open an issue first to discuss what you would like to change.

## Community

- Join our [Discord server](https://discord.gg/zGx9BSzKfJ) to get in touch with the developers and community
- Report bugs or request features through [GitHub Issues](https://github.com/bsommerfeld/pathetic/issues)

## Acknowledgements

- Special thanks to [@Ollie](https://github.com/olijeffers0n), the co-founder of Pathetic, who helped build the library in its early stages

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Powered by

[![JetBrains logo.](https://resources.jetbrains.com/storage/products/company/brand/logos/jetbrains.svg)](https://jb.gg/OpenSource)
