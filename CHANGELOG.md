## Changelog

### Added

- `PathfinderHook.onPathfindingStart` default hook, called once before the search loop begins
- `PathfindingContext.target()` and `PathfindingContext.environmentContext()` accessors
- Package-level JavaDoc for every `api` and `engine` package

### Changed

- `PathfinderConfiguration.build()` now fails when no provider is set instead of silently defaulting to an always-traversable one; the deprecated no-arg `createPathfinder()` keeps its documented default
### Fixed

### Removed

