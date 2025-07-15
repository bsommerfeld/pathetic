## Changelog

### Added

- Added workflow to automate release descriptions
- Added CHANGELOG.md to keep track of changes
- Added tests for all util classes

### Changed

- Now using custom Iterables class instead of Guavas
- Offset got replaced with a new INeighborStrategy to provide more flexible offsets and control over the path density
- HeuristicMode got replaced with IHeuristicStrategy, allowing the user more freedom regarding custom heuristics
- Moved heuristic API to the api module.

### Fixed

- Fixed PathPosition toString formatting by @steve
- Fixed initial node processing by @steve

### Removed

- Removed HeuristicMode
- Removed Offset