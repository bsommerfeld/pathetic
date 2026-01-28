## Changelog

### Added

- introduced `.of(double, double, double)` factory methods for PathVector and PathPosition
- introduced `Depth#value()`
- introduced `Cost#value()`

### Changed

- introduced `MinHeap`, `Resizable`, `Siftable` interfaces to abstract the heap implementations
- extracted release workflow into [own repository](https://github.com/bsommerfeld/release-changelog)

### Fixed

### Removed

- deprecated `Depth#getValue()`
- deprecated `Pathfinder#abort()`
- deprecated `Cost#getValue()`
- ErrorLogger has been removed
- tinylog dependencies have been removed
- deprecated PathUtils

