## Changelog

### Added

### Changed

- Pack node keys relative to the search start, lifting all absolute coordinate limits (formerly X/Z in [-33554432, 33554431], Y in [-2048, 2047])
- A single search explores at most +-2097151 blocks (X/Z) and +-524287 blocks (Y) around its start; positions beyond this radius are treated as non-navigable

### Fixed

- Reuse the reopen-decision G-cost when reopening a closed node instead of recomputing it
- A reopen attempt vetoed by a validator no longer lowers the recorded closed-set G-cost
- Runtime exceptions from custom extensions during a search now produce a `FAILED` result instead of escaping through the future
- A non-finite heuristic or cost now fails the search with a descriptive error instead of corrupting the open-set ordering
- `QuaternaryPrimitiveMinHeap.extractMin` now throws `NoSuchElementException` on an empty heap instead of corrupting internal state
- `QuaternaryPrimitiveMinHeap` rejects ids outside the non-negative int range instead of truncating and aliasing them

### Removed
