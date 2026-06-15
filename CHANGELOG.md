## Changelog

### Added

- `PathfinderHook.onPathfindingStart` default hook, called once before the search loop begins
- `PathfindingContext.target()` and `PathfindingContext.environmentContext()` accessors
- Package-level JavaDoc for every `api` and `engine` package

### Changed

- `PathfinderConfiguration.build()` now fails when no provider is set instead of silently defaulting to an always-traversable one; the deprecated no-arg `createPathfinder()` keeps its documented default
- Fewer allocations on the per-search pathfinding hot path
- A* per-search state moved off the pathfinder's `ThreadLocal` into a stack-local `SearchState` passed through the loop; concurrent searches stay isolated by the call stack
### Fixed

### Removed

