## Changelog

### Added

### Changed

- `findPath` now rejects start/target positions outside the packable coordinate range with `IllegalArgumentException`

### Fixed

- Reuse the reopen-decision G-cost when reopening a closed node instead of recomputing it
- A reopen attempt vetoed by a validator no longer lowers the recorded closed-set G-cost
- Runtime exceptions from custom extensions during a search now produce a `FAILED` result instead of escaping through the future
- A non-finite heuristic or cost now fails the search with a descriptive error instead of corrupting the open-set ordering
- Neighbors outside the packable coordinate range are skipped instead of aborting the search

### Removed
