# pathetic

[![Mentioned in Awesome Java](https://awesome.re/mentioned-badge.svg)](https://github.com/akullpp/awesome-java)
[![Build Status](https://img.shields.io/github/actions/workflow/status/bsommerfeld/pathetic/build.yml?branch=production)](https://github.com/bsommerfeld/pathetic/actions)
[![JitPack](https://jitpack.io/v/bsommerfeld/pathetic.svg)](https://jitpack.io/#bsommerfeld/pathetic)
[![License](https://img.shields.io/github/license/bsommerfeld/pathetic)](https://github.com/bsommerfeld/pathetic/blob/main/LICENSE)

A high-performance, concurrent pathfinding library for Java.

`pathetic` is a thread-safe pathfinding engine built for demanding, server-side applications. Originally developed for large-scale game servers, it uses an optimized A* algorithm to deliver fast and scalable results in complex environments.

***

## Core Features

* **High-Performance A\***: Utilizes a **Fibonacci Heap** for the open set and optional **Bloom filters** for the closed set, ensuring excellent performance in large search spaces.
* **Concurrent by Design**: Fully thread-safe to handle multiple pathfinding requests simultaneously, making it ideal for microservices and other backend systems.
* **Highly Extensible**: Customize pathfinding behavior using `NodeValidationProcessor` and `NodeCostProcessor` to model complex rules and traversal costs.

***

## Use Cases

While born from gaming, `pathetic` is suited for any problem requiring efficient graph traversal:

* **Logistics & Robotics**: Calculate optimal routes for delivery fleets or autonomous agents.
* **Game Development**: Power NPC navigation in real-time simulations.
* **Network & System Simulation**: Model and analyze data flow or crowd movement.

***

## Documentation & Usage

For installation instructions, getting started guides, and the complete API reference, please visit the official project wiki.

### **[Explore the Wiki](https://github.com/bsommerfeld/pathetic/wiki)**

***

### Project Origin

The name "pathetic" is a tongue-in-cheek nod to the project's humble beginnings. The library was co-founded and initially developed with [@Ollie](https://github.com/olijeffers0n).

***

### Powered by

[![JetBrains logo.](https://resources.jetbrains.com/storage/products/company/brand/logos/jetbrains.svg)](https://jb.gg/OpenSource)

Thanks to JetBrains for sponsoring this project!
