# The Engine
Ah, pathetics core. This is where the magic happens.

## How does it work?
A quick overview over the key features:
<ul>
  <li><b>Advanced A* Algorithm:</b> Employs multiple distance metrics (Manhattan, Octile, Perpendicular) and height differences for pathfinding, optimized for 3D worlds like Minecraft.</li>
  <li><b>Asynchronous Pathfinding:</b> Non-blocking operations using <code>CompletableFuture</code> to minimize server impact during pathfinding.</li>
  <li><b>Fibonacci Heap for Efficient Queuing:</b> The open set (frontier) is managed using a <b>Fibonacci heap</b>, ensuring optimal node retrieval with faster <code>insert</code> and <code>extract min</code> operations.</li>
  <li><b>Customizable Heuristics:</b> Fine-tune pathfinding behavior using <code>HeuristicWeights</code> for balanced navigation in any world configuration.</li>
  <li><b>Regional Grid Optimization:</b> Uses <code>ExpiringHashMap</code> and <b>Bloom filters</b> to efficiently track explored regions, minimizing memory overhead.</li>
  <li><b>Dynamic Path Filters:</b> Define custom filters to modify node validity or prioritize paths based on criteria such as passability, block type, or world boundaries.</li>
</ul>

## What to do with it?
To use the engine to bend it for your own purposes, you can use the following dependency:

```xml

<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
<groupId>de.bsommerfeld</groupId>
<artifactId>engine</artifactId>
<version>VERSION</version>
</dependency>

<dependency>
<groupId>de.bsommerfeld</groupId>
<artifactId>api</artifactId>
<version>VERSION</version>
</dependency>
```

<h4>Gradle</h4>

```groovy
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}

dependencies {
    implementation 'de.bsommerfeld:engine:VERSION'
    implementation 'de.bsommerfeld:api:VERSION'
}
```

After that it is required to implement your own NavigationPointProvider and PathfinderFactory.
You can see a full example in the [pathetic-bukkit](https://github.com/bsommerfeld/pathetic/tree/production/pathetic-bukkit) module.
