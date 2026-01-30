## Changelog

### Added

### Changed

- PathfindingSearch#exceptionally now accepts a Consumer instead of a Function
- aborting the current pathfinding operation now triggers #orElse instead of exceptionally
- #abort now returns void

### Fixed

- added defensive cleanup for PathfindingSession
- fixed issue with algorithm cleanup not being performed consistently
- fixed abort behavior to ensure cooperative cancellation

### Removed

