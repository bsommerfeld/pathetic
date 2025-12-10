## Changelog

### Added

- Clean Install IDE Configuration
- A specialized, array-backed binary heap tailored for A*
- Introduced overloaded getOffset(PathPosition) method in INeighborStrategy
- Implemented support for reopening Closed Set nodes to handle inconsistent heuristics (configurable via `reopenClosedNodes`).

### Changed

- Revamp README for better engagement – add humor, benchmarks, and real-world demos to combat stagnation
- optimized core data structures for reduced GC pressure and cleaned up legacy code
- Switched internal Open Set implementation from Fibonacci to `PrimitiveMinHeap`.
- ~3x to 4.5x faster pathfinding in benchmarks due to improved CPU cache locality.
- Achieved *true* zero-allocation operations within the Open Set (eliminated millions of `Node` wrapper and `Double` boxing objects).
- Renamed NodeCostProcessor -> CostProcessor
- Renamed NodeValidationProcessor -> ValidationProcessor
- Renamed NodeEvaluationContext -> EvaluationContext

### Fixed

- removed legacy lazy computing of hcost
- Fixed subtle yet crucial a control flow bug where updates to nodes in the Open Set were ignored, ensuring the optimal path is always prioritized.

### Removed

- Several before deprecated Methods (see 5.4.0)
- Tuple3 + Tests
- ComputingCache + Tests
- `org.jheaps` dependency
- Experimental annotation