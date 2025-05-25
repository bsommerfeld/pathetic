# Pathetic Engine Documentation

## Overview

The Pathetic Engine is the implementation component of the Pathetic library, providing the actual pathfinding algorithms and logic. It implements the interfaces defined in the Pathetic API and offers a high-performance A* pathfinding algorithm optimized for 3D environments.

This documentation covers the key components of the Pathetic Engine and how they integrate with the Pathetic API.

## Key Components

### AStarPathfinder

The `AStarPathfinder` class is the primary implementation of the `Pathfinder` interface, providing an A* (A-star) pathfinding algorithm. It uses a heuristic to guide the search towards the target and considers both the actual cost from the start (G-cost) and the estimated cost to the target (H-cost).

Key features of the AStarPathfinder include:

- Grid-based approach for optimizing visited node checks
- Bloom filters for efficient memory usage
- Region-based data management
- Support for custom node validation and cost processors

```java
// Creating an AStarPathfinder
PathfinderConfiguration config = PathfinderConfiguration.builder()
    .maxIterations(1000)
    .maxLength(100)
    .async(true)
    .provider(myNavigationPointProvider)
    .heuristicWeights(HeuristicWeights.NATURAL_PATH_WEIGHTS)
    .build();

PathfinderFactory factory = new AStarPathfinderFactory();
Pathfinder pathfinder = factory.createPathfinder(config);
```

### AbstractPathfinder

The `AbstractPathfinder` class provides a base implementation of the `Pathfinder` interface with common functionality for different pathfinding algorithms. It handles:

- Asynchronous pathfinding execution
- Path reconstruction
- Error handling
- Hook registration and invocation
- Fallback mechanisms
- Iteration and length limits

This class is extended by specific algorithm implementations like `AStarPathfinder`.

### Node

The `Node` class represents a position in the pathfinding search space. It contains:

- The position in 3D space
- References to parent nodes for path reconstruction
- G-cost (actual cost from start)
- H-cost (estimated cost to target)
- F-cost (combined G-cost and H-cost)
- Depth in the search tree

Nodes are used internally by the pathfinding algorithm to track the search progress and reconstruct the final path.

### PathImpl

The `PathImpl` class implements the `Path` interface from the API, representing a sequence of positions that form a path through 3D space. It provides methods for:

- Iterating through positions
- Getting the path length
- Interpolating positions for smoother curves
- Simplifying the path
- Joining paths
- Trimming paths
- Mutating positions

### PathfinderResultImpl

The `PathfinderResultImpl` class implements the `PathfinderResult` interface from the API, representing the outcome of a pathfinding operation. It contains:

- The path state (success, failure, etc.)
- The resulting path
- Methods to check if the pathfinding was successful or failed

## Integration with the API

The Pathetic Engine integrates with the Pathetic API through several key mechanisms:

### Factory Pattern

The engine uses the factory pattern to create pathfinder instances:

```java
// Creating a pathfinder using a factory
PathfinderFactory factory = new AStarPathfinderFactory();
Pathfinder pathfinder = factory.createPathfinder(config);
```

### Implementation of API Interfaces

The engine implements the interfaces defined in the API:

- `AStarPathfinder` implements `Pathfinder`
- `PathImpl` implements `Path`
- `PathfinderResultImpl` implements `PathfinderResult`

### Configuration

The engine uses the `PathfinderConfiguration` from the API to configure the pathfinding algorithm:

```java
// Configuring the pathfinder
PathfinderConfiguration config = PathfinderConfiguration.builder()
    .maxIterations(1000)
    .maxLength(100)
    .async(true)
    .fallback(true)
    .negativeCostsAllowed(false)
    .provider(myNavigationPointProvider)
    .heuristicWeights(HeuristicWeights.NATURAL_PATH_WEIGHTS)
    .nodeValidationProcessors(myValidationProcessors)
    .nodeCostProcessors(myCostProcessors)
    .build();
```

### Navigation Point Provider

The engine uses the `NavigationPointProvider` from the API to retrieve navigation point data:

```java
// The engine calls this method during pathfinding
NavigationPoint point = navigationPointProvider.getNavigationPoint(position);
boolean isTraversable = point.isTraversable();
```

### Hooks and Processors

The engine supports hooks and processors from the API:

- `PathfinderHook` for monitoring the pathfinding process
- `NodeValidationProcessor` for validating nodes
- `NodeCostProcessor` for calculating custom costs

## Advanced Usage

### Custom Pathfinding Algorithms

While the Pathetic Engine provides an A* implementation, you can create your own pathfinding algorithms by extending the `AbstractPathfinder` class:

```java
public class MyCustomPathfinder extends AbstractPathfinder {
    
    public MyCustomPathfinder(PathfinderConfiguration config) {
        super(config);
    }
    
    @Override
    protected void processSuccessors(
        PathPosition requestStart,
        PathPosition requestTarget,
        Node currentNode,
        int currentSearchDepth,
        FibonacciHeap<Double, Node> openSet,
        SearchContext searchContext) {
        // Implement your custom algorithm logic here
    }
    
    // Override other methods as needed
}
```

### Performance Optimization

The Pathetic Engine includes several optimizations for performance:

1. **Grid-based Region Management**: Divides the search space into grid regions for efficient memory usage.

2. **Bloom Filters**: Uses Bloom filters to quickly check if a position has been visited.

3. **Fibonacci Heap**: Uses a Fibonacci heap for the open set to efficiently get the node with the lowest F-cost.

4. **Asynchronous Processing**: Supports asynchronous pathfinding to avoid blocking the main thread.

5. **Expiring Hash Map**: Uses an expiring hash map to manage memory for long-running applications.

### Memory Management

For applications that run for extended periods, the engine includes memory management features:

```java
// The engine automatically manages memory for grid regions
private final Map<Tuple3<Integer>, ExpiringHashMap.Entry<GridRegionData>> visitedRegionGrid =
    new ExpiringHashMap<>();
```

### Error Handling

The engine includes robust error handling to prevent crashes:

```java
// The engine catches and handles exceptions during pathfinding
try {
    // Pathfinding logic
} catch (Throwable throwable) {
    return handlePathingException(originalStart, originalTarget, throwable);
}
```

## Best Practices

1. **Choose the Right Algorithm**: The A* algorithm is suitable for most cases, but consider custom algorithms for specific requirements.

2. **Optimize Your NavigationPointProvider**: Since this is called frequently during pathfinding, ensure it's efficient.

3. **Use Asynchronous Processing**: For applications where responsiveness is important, use async=true in the configuration.

4. **Adjust Grid Cell Size**: The default grid cell size is 12, but you can adjust it based on your environment.

5. **Clean Up Resources**: Call `performAlgorithmCleanup()` when you're done with a pathfinder to free up resources.

6. **Handle Failures Gracefully**: Check the PathState in the result to understand why pathfinding failed and take appropriate action.

7. **Test with Different Scenarios**: Test your pathfinding setup with various start and target positions to ensure it works correctly in all cases.

## Integration Examples

### Basic Integration

```java
// Create a navigation point provider
NavigationPointProvider provider = new MyNavigationPointProvider();

// Create a configuration
PathfinderConfiguration config = PathfinderConfiguration.builder()
    .maxIterations(1000)
    .maxLength(100)
    .async(true)
    .provider(provider)
    .heuristicWeights(HeuristicWeights.NATURAL_PATH_WEIGHTS)
    .build();

// Create a pathfinder factory and get a pathfinder
PathfinderFactory factory = new AStarPathfinderFactory();
Pathfinder pathfinder = factory.createPathfinder(config);

// Define start and target positions
PathPosition start = new PathPosition(environment, 0, 0, 0);
PathPosition target = new PathPosition(environment, 10, 5, 10);

// Find a path
CompletionStage<PathfinderResult> resultFuture = pathfinder.findPath(start, target);

// Handle the result
resultFuture.thenAccept(result -> {
    if (result.successful()) {
        Path path = result.getPath();
        // Use the path...
    } else {
        // Handle failure...
    }
});
```

### Advanced Integration with Custom Processors

```java
// Create custom processors
List<NodeValidationProcessor> validationProcessors = new ArrayList<>();
validationProcessors.add(new TerrainValidator());
validationProcessors.add(new HeightValidator());

List<NodeCostProcessor> costProcessors = new ArrayList<>();
costProcessors.add(new TerrainCostProcessor());
costProcessors.add(new HeightCostProcessor());

// Create a configuration with custom processors
PathfinderConfiguration config = PathfinderConfiguration.builder()
    .maxIterations(2000)
    .maxLength(200)
    .async(true)
    .fallback(true)
    .negativeCostsAllowed(true)
    .provider(provider)
    .heuristicWeights(HeuristicWeights.create(0.4, 0.2, 0.5, 0.3, 0.4))
    .nodeValidationProcessors(validationProcessors)
    .nodeCostProcessors(costProcessors)
    .build();

// Create and use the pathfinder as before
PathfinderFactory factory = new AStarPathfinderFactory();
Pathfinder pathfinder = factory.createPathfinder(config);

// Register a hook for monitoring
pathfinder.registerPathfindingHook(new PathfinderHook() {
    @Override
    public void onPathfindingStep(PathfindingContext context) {
        // Monitor the pathfinding process
        Depth depth = context.getDepth();
        System.out.println("Current depth: " + depth.getValue());
    }
});

// Find a path
CompletionStage<PathfinderResult> resultFuture = pathfinder.findPath(start, target);
```

### Integration with Fallback Handling

```java
// Create a configuration with fallback enabled
PathfinderConfiguration config = PathfinderConfiguration.builder()
    .maxIterations(1000)
    .maxLength(100)
    .async(true)
    .fallback(true)  // Enable fallback
    .provider(provider)
    .heuristicWeights(HeuristicWeights.NATURAL_PATH_WEIGHTS)
    .build();

// Create and use the pathfinder
PathfinderFactory factory = new AStarPathfinderFactory();
Pathfinder pathfinder = factory.createPathfinder(config);

// Find a path
CompletionStage<PathfinderResult> resultFuture = pathfinder.findPath(start, target);

// Handle the result with fallback checking
resultFuture.thenAccept(result -> {
    if (result.successful()) {
        Path path = result.getPath();
        // Use the path...
    } else if (result.hasFallenBack()) {
        // Handle fallback case
        Path fallbackPath = result.getPath();
        // Use the fallback path...
    } else {
        // Handle complete failure...
    }
});
```
