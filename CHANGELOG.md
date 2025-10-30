## Changelog

### Added

- Path has a new `collect()` method.
- Added more tests to AStarPathfinderTest
- Introduced PathUtils class
- Introduced DistanceCalculator interface
- Introduced PathfindingProgress class

### Changed

- Optimized tie-breaker calculations
- Cost now only can be positive
- PathImpl now delegates several functions to PathUtils class
- Refactored heuristic implementations with DistanceCalculators

### Fixed

- Fixed SquaredHeuristicStrategy with now a matching squared manhattan, octile and heightDiff calculation which increases performance even further

### Removed

- Deprecated negativeCost configuration
- Deprecated several specific distance calculations inside PathPosition
- Several Path post-processing methods are now Deprecated and PathUtils should be used
- Deprecated PathPosition#isInSameBlock

