## Changelog

### Added

- Add `executorService` option to PathfinderConfiguration [[#194](https://github.com/bsommerfeld/pathetic/pull/194)] by @lynxplay

### Changed

- Cache neighbor offset lists in `NeighborStrategies` to avoid per-iteration allocations
- Tighten `PathImpl` constructor parameter from `Iterable<PathPosition>` to `Collection<PathPosition>`
- Reject all `null` arguments in `Validators` factories (`allOf`, `anyOf`, `noneOf`, `not`) with `NullPointerException`
- Size the open-set heap per search from `manhattanDistance * branchingFactor`, capped by `maxIterations`
- Deprecate `PathfinderFactory.createPathfinder()` no-arg overload (always-traversable provider routes through walls)

### Fixed

- Add input validation for numeric values in PathfinderConfiguration
- Guard `PathVector.computeDistance` against null arguments and degenerate `B == C` input
- Correct `PathfinderResult.hasFailed()` JavaDoc - `FALLBACK` is not treated as a failure
- Print full stack trace (not just message) when a processor's `finalizeSearch` throws
- `PathfinderConfiguration.deepCopy` now defensively clones processor and hook lists so the copy is independent of later source mutations
- Document the single-threaded contract of `SearchContext.getSharedData()` in JavaDoc

### Removed

- Remove unused `Pathetic` engine-version utility and its `pathetic.properties` resource
