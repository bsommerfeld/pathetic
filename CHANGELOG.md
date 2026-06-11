## Changelog

### Added

- Package-level JavaDoc for every `api` and `engine` package

### Changed

- Searches complete 2.5-3x faster end-to-end than 5.5.0 and short searches start about 7x faster (measured with the bundled benchmarks)
- Pack node keys relative to the search start, lifting all absolute coordinate limits (formerly X/Z in [-33554432, 33554431], Y in [-2048, 2047])
- A single search explores at most +-2097151 blocks (X/Z) and +-524287 blocks (Y) around its start; positions beyond this radius are treated as non-navigable
- Switch the engine's open set to the quaternary min-heap, keyed by dense per-search node ids
- `AbstractPathfinder.initializeSearch` now receives the start position and expected node count; subclasses must key the open set by dense non-negative ids
- Open-set, closed-set, and reopen G-cost lookups are id-indexed array accesses behind a single hash map per neighbor
- Deprecate the `gridCellSize`/`bloomFilterSize`/`bloomFilterFpp` options (marked for removal); the closed set no longer uses bloom-filtered grid regions
- Defer neighbor node construction until after the closed-set check, skipping allocation and heuristic work for closed neighbors
- Size per-search session structures from the same node estimate as the open-set heap; short searches set up in a fraction of the time

### Fixed

- Reuse the reopen-decision G-cost when reopening a closed node instead of recomputing it
- A reopen attempt vetoed by a validator no longer lowers the recorded closed-set G-cost
- Runtime exceptions from custom extensions during a search now produce a `FAILED` result instead of escaping through the future
- A non-finite heuristic or cost now fails the search with a descriptive error instead of corrupting the open-set ordering
- `QuaternaryPrimitiveMinHeap.extractMin` now throws `NoSuchElementException` on an empty heap instead of corrupting internal state
- `QuaternaryPrimitiveMinHeap` rejects ids outside the non-negative int range instead of truncating and aliasing them

### Removed

- Remove `SpatialData`; the engine's closed set is id-indexed and no longer bloom-filtered
- Remove the shaded Guava dependency and the checkerframework/javax.annotation relocations from the engine artifact
