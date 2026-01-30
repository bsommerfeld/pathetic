
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


<div align="left">
    <img src="https://github.com/user-attachments/assets/74a14831-4ca5-4090-a569-b24aa6be06b6" width="800" alt="Showcase">
    <img src="https://github.com/user-attachments/assets/69ca0f04-4add-485e-837e-a0a82b63a003" width="800" alt="Showcase">
</div>

All demos from a real Paper server. <br>
Minecraft is a pathfinding hell — Pathetic just walked in, pissed on Hades' leg, and asked for a lighter.

> Most libraries need minutes or give up entirely on paths longer than 1000 positions.  
> Pathetic does 20 000 in the time you need to blink twice.  
> You're welcome.

---

### Drop-in and watch the magic

```xml
<dependencies>
    <dependency>
        <groupId>de.bsommerfeld.pathetic</groupId>
        <artifactId>engine</artifactId>
        <version>5.4.5</version>
    </dependency>
    <dependency>
        <groupId>de.bsommerfeld.pathetic</groupId>
        <artifactId>api</artifactId>
        <version>5.4.5</version>
    </dependency>
</dependencies>
```

```kotlin
implementation("de.bsommerfeld.pathetic:engine:5.4.5")
implementation("de.bsommerfeld.pathetic:api:5.4.5")
```

```java
Pathfinder pf = factory.createPathfinder(config);

pf.findPath(start, goal, context)
.ifPresent(result -> {
    moveThatEntity(result.path())
}).orElse(result -> System.out.println("No path found!"));
```

### Features that hurt other libraries' feelings

- **Hand-rolled primitive binary min-heap that makes FibonacciHeap look like a participation trophy**  
  → Zero allocations, perfect cache locality, O(log n) decrease-key that’s faster in practice than every “amortized O(1)” heap in the Java ecosystem.  
  → Two contiguous primitive arrays + fastutil Long2IntOpenHashMap. Your CPU prefetcher just sent us a thank-you note.
- **Bloomfilter first-line-of-defense** → closed-set lookups go from "please wait"(O(n)) to "already done"(O(1))
- **Composite Heuristics from Hell™** – Manhattan + Octile + real perpendicular deviation + height penalty, all weighted, all running in parallel. Choose linear (accurate as fuck) or squared (2–3× faster, still consistent). One heuristic? Adorable.
- **Perpendicular tie-breaking** → paths so straight your NPCs look like they're cheating
- **Processor pipeline** → walking, swimming, flying, restricted areas - just drop a lambda, peasant 
- **Pure Java 8+** → works on your grandma's server and still murders modern rigs
- **100 % async & concurrent** → because blocking the main thread is for libraries that hate their users
- **[X: 26 bit] [Z: 26 bit] [Y: 12 bit]** → One primitive long. No more millions of BlockPos objects crying for GC. Supports coordinates up to ±33,000,000. Zero allocations, zero tears.
- **We use LongOpenHashSet.** That means ~6 bytes per closed node instead of 64+ bytes of boxed Java sadness. At 500k nodes, that’s 30-40 MB of saved heap that the "competition" just hands straight to the Garbage Collector.

### Yeah but how is it actually this fast?

We used to use FibonacciHeaps like all the other “serious” libraries.  
It was fine.  
It was fast enough.  
It was also allocating objects like a crypto miner on Christmas.

So we took it out back and replaced it with a hand-rolled, array-backed, zero-allocation primitive binary heap.

What changed?

| Metric                     | Old FibonacciHeap       | New PrimitiveMinHeap               | Your server now
|----------------------------|--------------------------|------------------------------------|-------------------
| Allocations per run        | Objects everywhere       | **0** (Zero. Zilch. Nada.)         | GC went on vacation
| Cache efficiency           | Pointer chasing hell     | Contiguous array bliss             | CPU utilization dropped 50 %
| Benchmark (1k nodes)       | Baseline                 | **~4.5× faster** | Teleportation enabled
| Benchmark (10k nodes)      | Baseline                 | **~3× faster** | Your players noticed
| Large Scale (50k nodes)    | Exponential degradation  | **Linearly stable** | The dragon never stood a chance

Result: 10 000 concurrent pathfinds now finish before you can blink.

We kept the old FibonacciHeap code in a branch called `archaeology`.

---

### Your hardware is just a sub 🥀

<div align="left">
    <img src="https://github.com/user-attachments/assets/5f49f4bf-6683-44e4-9851-d09557d443ea" width="800" alt="Benchmark Showcase">
</div>
<br>

**Now Doom isn't the only thing that runs everywhere.**

We didn’t build this library to just "run." We trained Pathetic to subjugate your hardware, make your RAM cry, and then politely ask: *"Another round?"*

While other pathfinding libraries beg for more heap, more cores, and more mercy, while being sorry for being a *bad* pathfinder, Pathetic puts a belt around your scheduler and whispers: "You’re going to sit still and swallow 20,000 nodes in ~60ms — and you’re going to thank me for it."

Pathetic doesn’t ask your hardware for permission. Your hardware needs permission from Pathetic to send a heartbeat. Welcome to the food chain.

**Safe-word?** `OutOfMemoryError` — but you’ll never reach it.

### We could’ve called it HyperQuantumPathUltra Enterprise God Mode Edition™

We called it Pathetic instead. <br>
Because that’s what every other library became the moment we released this.

Some say "this README reads like they belong in a hospital." <br>
**And you're goddamn right.** <br>

---

### Hang out with us

[Wiki](https://github.com/bsommerfeld/pathetic/wiki) – for mortals who still read docs <br>
[Discord](https://discord.gg/zGx9BSzKfJ) – come cry or worship <br>
Issues, PRs, death threats → right here: https://github.com/bsommerfeld/pathetic/issues

**Powered by JetBrains** (they saw the name and still sponsored us – legends recognize legends ❤️) <br>
[![JetBrains logo.](https://resources.jetbrains.com/storage/products/company/brand/logos/jetbrains.svg)](https://jb.gg/OpenSource)
