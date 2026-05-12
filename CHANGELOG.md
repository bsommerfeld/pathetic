## Changelog

### Added

- Add `executorService` option to PathfinderConfiguration [[#194](https://github.com/bsommerfeld/pathetic/pull/194)] by @lynxplay

### Changed

- Cache neighbor offset lists in `NeighborStrategies` to avoid per-iteration allocations

### Fixed

- Add input validation for numeric values in PathfinderConfiguration
- Guard `PathVector.computeDistance` against null arguments and degenerate `B == C` input

### Removed

- Remove unused `Pathetic` engine-version utility and its `pathetic.properties` resource
