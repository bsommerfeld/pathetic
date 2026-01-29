## Changelog

### Added

- add JetBrains annotations dependency
- introduced PathfindingSearch which represents a single search operation
- introduced validationProcessors in PathfinderConfigurationBuilder
- introduced costProcessors in PathfinderConfigurationBuilder

### Changed

- marked getEnvironmentContext Nullable in SearchContext
- replaced CompletionStage<PathfinderResult> return from Pathfinder with PathfindingSearch

**EXAMPLE:**

```java
PathfindingSearch pathfindingSearch = pathfinder.findPath(start, target);

// Callbacks, executed once the operation is done
pathfindingSearch.ifPresent(result -> System.out.println("A path is present"))
        .orElse(result -> System.out.println("There is no path (failed)"))
        .exceptionally(ex -> System.out.println("Something went horribly wrong (or pathfinding got aborted)"));

pathfindingSearch.abort(); // This is per search now!
```

### Fixed

### Removed

- deprecated nodeValidationProcessors in PathfinderConfigurationBuilder
- deprecated nodeCostProcessors in PathfinderConfigurationBuilder

