# Pathetic

**A high-performance pathfinding library for Java.**

[![Mentioned in Awesome Java](https://awesome.re/mentioned-badge.svg)](https://github.com/akullpp/awesome-java)
[![Maven Central](https://img.shields.io/maven-central/v/de.bsommerfeld.pathetic/api.svg?label=Maven%20Central)](https://central.sonatype.com/search?q=de.bsommerfeld.pathetic)
[![Build Status](https://img.shields.io/github/actions/workflow/status/bsommerfeld/pathetic/build.yml?branch=production)](https://github.com/bsommerfeld/pathetic/actions)
[![License](https://img.shields.io/github/license/bsommerfeld/pathetic)](https://github.com/bsommerfeld/pathetic/blob/main/LICENSE)

> "I used to use library X… then I tried Pathetic and suddenly my server stopped struggling." <br>
> — every future user, probably

### Why Pathetic?

**Built to handle scale:** Most Java pathfinding libraries struggle with a few hundred concurrent requests. Pathetic was designed from the ground up for high-throughput, low-latency pathfinding.

| Scenario                 | Pathetic                | Typical alternatives         | Notes                         |
| ------------------------ | ----------------------- | ---------------------------- | ----------------------------- |
| 10k concurrent paths     | ~7 ms                   | ~300 ms +                    | Measured on real workloads    |
| One 20k distance path    | ~60 ms                  | Minutes or timeout           | Consistently reproducible     |
| CPU under heavy load     | <2% on 16 cores         | 20–100% or OOM               | Efficient resource usage      |
| Memory                   | Spark shows a flat line | Hundreds of MB of GC churn   | Minimal garbage collection    |


![ezgif-425417d69c8935bb](https://github.com/user-attachments/assets/74a14831-4ca5-4090-a569-b24aa6be06b6)
![ezgif-47d8f87ff2b608e9](https://github.com/user-attachments/assets/69ca0f04-4add-485e-837e-a0a82b63a003)

All demos from a real Paper server. <br>
Minecraft presents challenging pathfinding scenarios — Pathetic handles them with ease.

> Most libraries need minutes or give up entirely on paths longer than 1000 positions.
> Pathetic completes 20,000-node paths in the time it takes to blink.
### Drop-in and watch the magic

```xml
<dependencies>
    <dependency>
        <groupId>de.bsommerfeld.pathetic</groupId>
        <artifactId>engine</artifactId>
        <version>LATEST</version>
    </dependency>
    <dependency>
        <groupId>de.bsommerfeld.pathetic</groupId>
        <artifactId>api</artifactId>
        <version>LATEST</version>
    </dependency>
</dependencies>
```

```kotlin
implementation("de.bsommerfeld.pathetic:engine:LATEST")
implementation("de.bsommerfeld.pathetic:api:LATEST")
```

```java
Pathfinder pf = factory.createPathfinder(config);

pf.findPath(start, goal, context).thenAccept(result -> {
    if (result.state() == FOUND) moveThatEntity(result.path());
});
```

### Key Features

- **Custom primitive binary min-heap optimized for A\***
  Zero allocations, perfect cache locality, O(log n) decrease-key that outperforms typical "amortized O(1)" heaps in practice.
  Two contiguous primitive arrays + fastutil Long2IntOpenHashMap for optimal CPU cache utilization.
- **Bloom filter first-line-of-defense** — closed-set lookups go from O(n) to O(1)
- **Composite heuristics** — Manhattan + Octile + perpendicular deviation + height penalty, all weighted. Choose linear (highly accurate) or squared (2–3× faster, still consistent).
- **Perpendicular tie-breaking** — produces naturally straight paths
- **Processor pipeline** — walking, swimming, flying, restricted areas - simply provide a lambda
- **Pure Java 8+** — compatible with legacy and modern environments alike
- **100% async & concurrent** — never blocks your main thread
- **Packed coordinates: [X: 26 bit] [Z: 26 bit] [Y: 12 bit]** — One primitive long per position instead of object allocations. Supports coordinates up to ±33,000,000.
- **LongOpenHashSet for closed nodes** — ~6 bytes per node instead of 64+ bytes with boxed objects. At 500k nodes, that's 30-40 MB of heap saved.

### How is it this fast?

We originally used jheaps FibonacciHeap like many other libraries. It worked, but it allocated objects heavily during pathfinding operations.

We replaced it with a custom array-backed, zero-allocation primitive binary heap.

The results:

| Metric                     | Old FibonacciHeap        | New PrimitiveMinHeap               | Impact                       |
|----------------------------|--------------------------|------------------------------------|-----------------------------|
| Allocations per run        | Many objects             | **0**                              | Minimal GC pressure         |
| Cache efficiency           | Pointer chasing          | Contiguous arrays                  | ~50% CPU reduction          |
| Benchmark (1k nodes)       | Baseline                 | **~4.5× faster**                   | Noticeable improvement      |
| Benchmark (10k nodes)      | Baseline                 | **~3× faster**                     | Significant at scale        |
| Large Scale (50k nodes)    | Exponential degradation  | **Linearly stable**                | Handles large worlds        |

Result: 10,000 concurrent pathfinds complete in milliseconds.

The old FibonacciHeap code is preserved in a branch called `archaeology`.

### Why "Pathetic"?

We could have called it something like "HyperQuantumPathUltra Enterprise Edition" — but we went with Pathetic instead. Sometimes the best names are the unexpected ones.

### Community & Resources

- [Wiki](https://github.com/bsommerfeld/pathetic/wiki) — documentation and guides
- [Discord](https://discord.gg/zGx9BSzKfJ) — community discussion and support
- [Issues & PRs](https://github.com/bsommerfeld/pathetic/issues) — bug reports, feature requests, and contributions welcome

**Powered by JetBrains** — Thank you for supporting open source!
[![JetBrains logo.](https://resources.jetbrains.com/storage/products/company/brand/logos/jetbrains.svg)](https://jb.gg/OpenSource)
