## Changelog

### Added

- Add `executorService` option to PathfinderConfiguration [[#194](https://github.com/bsommerfeld/pathetic/pull/194)] by @lynxplay

### Changed

- Cache neighbor offset lists in `NeighborStrategies` to avoid per-iteration allocations
- Tighten `PathImpl` constructor parameter from `Iterable<PathPosition>` to `Collection<PathPosition>`
- Reject all `null` arguments in `Validators` factories (`allOf`, `anyOf`, `noneOf`, `not`) with `NullPointerException`
- Size the open-set heap per search from `manhattanDistance * branchingFactor`, capped by `maxIterations`
- Snapshot pathfinder hooks at search start; mid-search registrations now apply only to future searches
- Deprecate `PathfinderFactory.createPathfinder()` no-arg overload (always-traversable provider routes through walls)

### Fixed

- Add input validation for numeric values in PathfinderConfiguration
- Guard `PathVector.computeDistance` against null arguments and degenerate `B == C` input
- Correct `PathfinderResult.hasFailed()` JavaDoc - `FALLBACK` is not treated as a failure
- Print full stack trace (not just message) when a processor's `finalizeSearch` throws
- `PathfinderConfiguration.deepCopy` now defensively clones processor and hook lists so the copy is independent of later source mutations
- Document the single-threaded contract of `SearchContext.getSharedData()` in JavaDoc
- `Cost.of` now rejects `+Infinity`, `-Infinity`, and `NaN` (previously only `NaN` and negative values)
- Default shared executor is lazily allocated and only when an async configuration is built; sync-only configurations never spawn the thread pool

### Removed

- Remove unused `Pathetic` engine-version utility and its `pathetic.properties` resource
- Remove `Depth.increment()` and make `Depth` immutable
- Remove `Cloneable` and `clone()` from `PathPosition` and `PathVector`; coordinate fields are now `final`
