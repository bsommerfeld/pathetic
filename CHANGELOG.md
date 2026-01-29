## Changelog

### Added

- add JetBrains annotations dependency
- add pathfindingHooks to PathfinderConfiguration
- introduced PathfindingSearch which represents a single search operation
- introduced validationProcessors in PathfinderConfigurationBuilder
- introduced costProcessors in PathfinderConfigurationBuilder

### Changed

- marked getEnvironmentContext Nullable in SearchContext
- replaced CompletionStage<PathfinderResult> return from Pathfinder with [PathfindingSearch](https://github.com/bsommerfeld/pathetic/wiki/PathfindingSearch/)
- simplified exception handling in AbstractPathfinder

### Fixed

### Removed

- deprecated nodeValidationProcessors in PathfinderConfigurationBuilder
- deprecated nodeCostProcessors in PathfinderConfigurationBuilder
- deprecated Pathfinder#registerPathfindingHooks

