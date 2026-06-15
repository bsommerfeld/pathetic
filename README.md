# Pathetic 🥀

**The pathfinding library that's too good for this pathetic world.**

[![Mentioned in Awesome Java](https://awesome.re/mentioned-badge.svg)](https://github.com/akullpp/awesome-java)
[![Maven Central](https://img.shields.io/maven-central/v/de.bsommerfeld.pathetic/api.svg?label=Maven%20Central)](https://central.sonatype.com/search?q=de.bsommerfeld.pathetic)
[![Build Status](https://img.shields.io/github/actions/workflow/status/bsommerfeld/pathetic/build.yml?branch=production)](https://github.com/bsommerfeld/pathetic/actions)
[![License](https://img.shields.io/github/license/bsommerfeld/pathetic)](https://github.com/bsommerfeld/pathetic/blob/production/LICENSE)

> “I used to use library X… then I tried Pathetic and suddenly my server stopped crying.” <br>
> — every future user, probably

### Listen up, peasants

**It exists for one simple reason:** The rest of the Java pathfinding world collectively shits itself above a few hundred concurrent requests.

| Scenario                 | Pathetic                | The "competition"            | Your tears                    |
| ------------------------ |-------------------------| ---------------------------- | ----------------------------- |
| 10k concurrent paths     | ~7 ms                   | ~300 ms +                    | Priceless                     |
| One 20k distance path    | ~43 ms                  | Minutes, timeout, or suicide | We measured twice             |
| One 40k distance path    | ~62 ms                  | Heat death of the universe   | Linear scaling, btw           |
| CPU when the world burns | <2% on 16 cores         | 20–100% or instant OOM       | Eco-mode                      |
| Memory                   | Spark shows a flat line | Hundreds of MB of GC tears   | Spark thinks nothing happened |

<div align="left">
    <img src="https://github.com/user-attachments/assets/74a14831-4ca5-4090-a569-b24aa6be06b6" width="800" alt="Showcase">
    <img src="https://github.com/user-attachments/assets/f8db228b-748f-4f35-bd1b-9ddb56d64fa7" width="800" alt="Showcase">
</div>

All demos from a real Paper server. <br>
Minecraft is a pathfinding hell — Pathetic just walked in, pissed on Hades' leg, and asked for a lighter.

> Most libraries need minutes or give up entirely on paths longer than 1000 positions.  
> Pathetic does 20 000 before you finish a single blink.  
> You're welcome.

---

### Drop-in and watch the magic

```xml
<dependencies>
    <dependency>
        <groupId>de.bsommerfeld.pathetic</groupId>
        <artifactId>engine</artifactId>
        <version>5.5.2</version>
    </dependency>
</dependencies>
```

```kotlin
implementation("de.bsommerfeld.pathetic:engine:5.5.2")
```

```java
Pathfinder pf = factory.createPathfinder(config);

pf.findPath(start, goal, context)
.ifPresent(result -> {
    moveThatEntity(result.getPath());
}).orElse(result -> System.out.println("No path found!"));
```

### Features that hurt other libraries' feelings

- **Hand-rolled primitive quaternary min-heap that makes FibonacciHeap look like a participation trophy**  
  → Zero allocations, perfect cache locality, O(log n) decrease-key that’s faster in practice than every “amortized O(1)” heap in the Java ecosystem.  
  → Three contiguous primitive arrays, position tracking by plain array index — not a single hash-map call inside the heap. Your CPU prefetcher just sent us a thank-you note.
- **Dense node ids** → exactly one hash lookup per neighbor, then *everything* — open set, closed set, G-costs, heap position — is a primitive array access. Closed-set check? `closed[id]`. Done.
- **Composite Heuristics from Hell™** – Manhattan + Octile + real perpendicular deviation + height penalty, all weighted, all running in parallel. Choose linear (accurate as fuck) or squared (2–3× faster, greedy at range — speed has a price). One heuristic? Adorable.
- **Perpendicular tie-breaking** → paths so straight your NPCs look like they're cheating
- **Processor pipeline** → walking, swimming, flying, restricted areas - just drop a lambda, peasant 
- **Pure Java 8+** → works on your grandma's server and still murders modern rigs
- **100 % async & concurrent** → because blocking the main thread is for libraries that hate their users
- **[X: 22 bit] [Z: 22 bit] [Y: 20 bit], packed relative to the search start** → One primitive long. No more millions of BlockPos objects crying for GC. Absolute coordinates? **Unlimited.** Full int range, no world border required, borderless-world-proof. A single search roams ±2,000,000 blocks around its start — try exceeding that without running out of RAM first. Zero allocations, zero tears.
- **We don’t even box the closed set.** A dense-id boolean array instead of 64+ bytes of boxed Java sadness per node. At 500k nodes, that’s tens of MB of saved heap that the "competition" just hands straight to the Garbage Collector.

### Yeah but how is it actually this fast?

We made stupid decisions. Just like every other pathfinding library out there. But we **grew** — unlike the "competition".

It started with FibonacciHeaps, like all the other “serious” libraries.  
It was fine.  
It was fast enough.  
It was also allocating objects like a crypto miner on Christmas.

So we replaced it with a hand-rolled primitive binary heap and felt superior — until we caught it paying a hash-map update on every single sift step.
First Fibonacci, then Primitive... We decided to pull a hat-trick.

Today it’s a quaternary min-heap on dense node ids: one hash lookup at the front door when a neighbor shows up — and that’s the last hash anything computes. Open set, closed set, G-costs, heap position.. all plain array accesses. The heap itself doesn’t even know what a hash map is.

“Just use a HashMap” passes every job interview on the planet — and here we are, ripping them out like rotten floorboards. You must think we’re crazy. The numbers think otherwise:

| Metric                 | FibonacciHeap            | PrimitiveMinHeap                  | Quaternary + dense ids (today)
|------------------------|--------------------------|-----------------------------------|-------------------
| Allocations per run    | Objects everywhere       | **0** (Zero. Zilch. Nada.)        | Still **0**. Tradition.
| Cache efficiency       | Pointer chasing hell     | Contiguous array bliss            | Contiguous bliss, half the tree depth
| Decrease-key           | Amortized excuses        | Hash-map update per sift level    | **One array write**
| The numbers            | Baseline                 | ~3–4.5× faster than Fibonacci     | **2.5–3× faster than *that*** — end-to-end, measured, same worlds, same seeds

Result: 10 000 concurrent pathfinds finish before you even **start to** blink.

The FibonacciHeap lives in a branch called `archaeology`. The binary heap is still in the codebase — as the participation trophy. We keep our mistakes where we can see them; that’s the difference between a library that grew and a library that rots.

Other libraries are like a \$20 bet in a casino, winning you \$10. We guarantee you the jackpot. **Every. Fucking. Time.**

---

### Your hardware is just a sub 🥀

<!-- TODO: re-screenshot the heap benchmark — this one still highlights PrimitiveMinHeap as the hero -->
<div align="left">
    <img src="https://github.com/user-attachments/assets/5f49f4bf-6683-44e4-9851-d09557d443ea" width="800" alt="Benchmark Showcase">
</div>
<br>

**Doom isn't the only thing that runs everywhere.**

We didn’t build this library to just "run." We trained Pathetic to subjugate your hardware, make your RAM cry, and then politely ask: *"Another round?"*

While other pathfinding libraries beg for more heap, more cores, and more mercy, while being sorry for being a *bad* pathfinder, Pathetic puts a belt around your scheduler and whispers: "You’re going to sit still and swallow 20,000 nodes in ~43ms — and you’re going to thank me for it."

Beg for 10,000 paths in a single breath and the slowest thing in the room is your own for-loop. They all finish before you do — but you’re used to that, aren’t you?

**Safe-word?** `OutOfMemoryError` — but you’ll never reach it.

### Now remove Minecraft 🥀

Everything above ran inside a real Paper server. Chunk access, block states, world checks — that’s Minecraft, and Minecraft is the bottleneck. The numbers were already ones no trashbin ("competition") out there can replicate, and we were *handicapped* the whole time.

Strip the world away and you see what Pathetic truly is:

| Workload                 | Real Paper server  | Pathetic raw (no world attached)
| ------------------------ |--------------------| --------------------------------
| One 20k distance path    | ~43 ms             | **~16 ms**
| One 40k distance path    | ~62 ms             | **~34 ms**
| 10k concurrent paths     | sustained, all day | **~4.5 ms wall — 0.45 µs per path**
| Starting a search        | already included   | **0.11 µs**

Read that again. The pathfinding itself is basically free — most of those milliseconds are us waiting for Minecraft to hand over block data. The trashbins don’t have that excuse. They’re slow before the world is even involved.

---

### We could’ve called it HyperQuantumPathUltra Enterprise God Mode Edition™

We called it Pathetic instead. <br>
Because that’s what every other library became the moment we released this.

Some say "this README reads like they belong in a hospital." <br>
**And you're goddamn right.** <br>

### Hang out with us

[Wiki](https://github.com/bsommerfeld/pathetic/wiki) – for mortals who still read docs <br>
[Discord](https://discord.gg/zGx9BSzKfJ) – come cry or worship <br>
Issues, PRs, death threats → right here: https://github.com/bsommerfeld/pathetic/issues

**Powered by JetBrains** (they saw the name and still sponsored us – legends recognize legends ❤️) <br>
[![JetBrains logo.](https://resources.jetbrains.com/storage/products/company/brand/logos/jetbrains.svg)](https://jb.gg/OpenSource)

---

<div align="center">
    
<br> <br>
    
**Don't be pathetic, use Pathetic.** 🥀  

Add `Powered by [Pathetic](https://github.com/bsommerfeld/pathetic)` to your project's page to show dominance.

</div>
