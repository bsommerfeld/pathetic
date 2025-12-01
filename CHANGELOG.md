## Changelog

### Added

- Clean Install IDE Configuration
- A specialized, array-backed binary heap tailored for A*
- Introduced overloaded getOffset(PathPosition) method in INeighborStrategy

### Changed

- Revamp README for better engagement – add humor, benchmarks, and real-world demos to combat stagnation
- optimized core data structures for reduced GC pressure and cleaned up legacy code
- Switched internal Open Set implementation from `JHeaps` (Fibonacci) to `PrimitiveMinHeap`.
- ~3x to 4.5x faster pathfinding in benchmarks due to improved CPU cache locality.
- Achieved *true* zero-allocation operations within the Open Set (eliminated millions of `Node` wrapper and `Double` boxing objects).

### Fixed

- removed legacy lazy computing of hcost

### Removed

- Several before deprecated Methods (see 5.4.0)
- Tuple3 + Tests
- ComputingCache + Tests
- `org.jheaps` (bloated object-based heaps are no longer needed)