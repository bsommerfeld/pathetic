## Changelog

### Added

- Path has a new `collect()` method.
- Added more tests to AStarPathfinderTest

### Changed

- Optimized tie-breaker calculations
- Cost now only can be positive
- Deprecated negativeCost configuration
- PathImpl now delegates several functions to new PathUtils class
- Several Path post-processing methods are now Deprecated and PathUtils should be used

### Fixed

### Removed

