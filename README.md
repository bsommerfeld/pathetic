
# Pathetic 🥀

**The pathfinding library that's too good for this pathetic world.**

[![Mentioned in Awesome Java](https://awesome.re/mentioned-badge.svg)](https://github.com/akullpp/awesome-java)
[![Maven Central](https://img.shields.io/maven-central/v/de.bsommerfeld.pathetic/api.svg?label=Maven%20Central)](https://central.sonatype.com/search?q=de.bsommerfeld.pathetic)
[![Build Status](https://img.shields.io/github/actions/workflow/status/bsommerfeld/pathetic/build.yml?branch=production)](https://github.com/bsommerfeld/pathetic/actions)
[![License](https://img.shields.io/github/license/bsommerfeld/pathetic)](https://github.com/bsommerfeld/pathetic/blob/main/LICENSE)

> “I used to use library X… then I tried Pathetic and suddenly my server stopped crying.” <br>
> — every future user, probably

### Listen up, peasants

**It exists for one simple reason:** The rest of the Java pathfinding world collectively shits itself above a few hundred concurrent requests.

| Scenario                 | Pathetic                | The "competition"            | Your tears                    |
| ------------------------ | ----------------------- | ---------------------------- | ----------------------------- |
| 10k concurrent paths     | ~7 ms                   | ~300 ms +                    | Priceless                     |
| One 20k distance path    | ~60 ms                  | Minutes, timeout, or suicide | We measured twice             |
| CPU when the world burns | <2% on 16 cores         | 20–100% or instant OOM       | Eco-mode                      |
| Memory                   | Spark shows a flat line | Hundreds of MB of GC tears   | Spark thinks nothing happened |


![ezgif-425417d69c8935bb](https://github.com/user-attachments/assets/74a14831-4ca5-4090-a569-b24aa6be06b6)
![ezgif-47d8f87ff2b608e9](https://github.com/user-attachments/assets/69ca0f04-4add-485e-837e-a0a82b63a003)

All demos from a real Paper server. <br>
Minecraft is a pathfinding hell — Pathetic just walked in, pissed on Hades' leg, and asked for a lighter.

> Most libraries need minutes or give up entirely on paths longer than 1000 positions.  
> Pathetic does 20 000 in the time you need to blink twice.  
> You're welcome.
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

### Features that hurt other libraries' feelings

- **FibonacciHeap + real decrease-key** (because binary heaps are for interns)
- **Bloomfilter first-line-of-defense** → closed-set lookups go from "please wait"(O(n)) to "already done"(O(1))
- **Composite Heuristics from Hell™** – Manhattan + Octile + real perpendicular deviation + height penalty, all weighted, all running in parallel. Choose linear (accurate as fuck) or squared (2–3× faster, still consistent). One heuristic? Adorable.
- **Perpendicular tie-breaking** → paths so straight your NPCs look like they're cheating
- **Processor pipeline** → walking, swimming, flying, restricted areas - just drop a lambda, peasant 
- **Pure Java 8+** → works on your grandma's server and still murders modern rigs
- **100 % async & concurrent** → because blocking the main thread is for libraries that hate their users

### Yeah but how is it actually this fast?

Most A* libs treat the closed set like a HashSet (O(1) but millions of objects → GC hell)  
or like a TreeSet (clean but O(log n) per lookup → death by a thousand cuts).

Pathetic does this instead:
- Fibonacci heap with real decrease-key → open set operations are amortized O(1)
- Bloom filter as first-line defense → 99.9 % of all "is this node closed?" checks are O(1) with zero heap allocations
- Only the ~0.1 % false positives touch the actual backup set

Result: 10 000 concurrent pathfinds allocate almost nothing and run in single-digit milliseconds 

### We could’ve called it HyperQuantumPathUltra Enterprise God Mode Edition™

We called it Pathetic instead. <br>
Because that’s what every other library became the moment we released this.

### Hang out with us

Wiki – for mortals who still read docs <br>
Discord – come cry or worship <br>
Issues, PRs, death threats → right here: https://github.com/bsommerfeld/pathetic/issues

**Powered by JetBrains** (they saw the name and still sponsored us – legends recognize legends ❤️) <br>
JetBrains Logo
