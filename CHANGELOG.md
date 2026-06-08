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
- `PathPosition.distance` now uses exact `Math.sqrt` instead of an approximation; deprecate `NumberUtils.sqrt`

### Fixed

- Add input validation for numeric values in PathfinderConfiguration
- Guard `PathVector.computeDistance` against null arguments and degenerate `B == C` input
- Correct `PathfinderResult.hasFailed()` JavaDoc - `FALLBACK` is not treated as a failure
- Print full stack trace (not just message) when a processor's `finalizeSearch` throws
- `PathfinderConfiguration.deepCopy` now defensively clones processor and hook lists so the copy is independent of later source mutations
- Document the single-threaded contract of `SearchContext.getSharedData()` in JavaDoc
- `Cost.of` now rejects `+Infinity`, `-Infinity`, and `NaN` (previously only `NaN` and negative values)
- `PathfindingSearch.resultBlocking` now propagates the JDK `CompletionException` directly instead of re-wrapping it in a generic `RuntimeException`
- Clarify in JavaDoc that `PathfindingSearch.exceptionally` is a side-effect callback and does not recover the search
- Default shared executor is lazily allocated and only when an async configuration is built; sync-only configurations never spawn the thread pool
- `api` classes are no longer shaded into the `engine` artifact; `engine` now pulls `api` as a clean transitive dependency
- Reject `NaN` heap costs in `insertOrUpdate`; a `NaN` heuristic now fails fast instead of silently corrupting the open-set ordering
- Correct heuristic JavaDoc: with default weights LINEAR is only bounded-suboptimal (Weighted-A*-like) and SQUARED is not admissible, not "consistent/admissible" as claimed

### Removed

- Remove unused `Pathetic` engine-version utility and its `pathetic.properties` resource
- Remove `Depth.increment()` and make `Depth` immutable
- Remove `Cloneable` and `clone()` from `PathPosition` and `PathVector`; coordinate fields are now `final`
- Remove `Comparable` from `Node`; its cost-based ordering was inconsistent with position-based `equals`
