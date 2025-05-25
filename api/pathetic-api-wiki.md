# Pathetic API Documentation

## Overview

The Pathetic API is a high-performance, backwards-compatible, and asynchronous pathfinding library for 3D environments. It provides a clean interface for configuring and executing pathfinding operations using the A* algorithm with customizable heuristics.

This documentation covers the key components of the Pathetic API and how to use them in your applications.

## Key Components

### Pathfinder Interface

The `Pathfinder` interface is the main entry point for pathfinding operations. It provides methods to:

- Find a path between two positions
- Abort an ongoing pathfinding operation
- Register hooks for monitoring and customizing the pathfinding process

```java
// Example of using the Pathfinder interface
CompletionStage<PathfinderResult> resultFuture = pathfinder.findPath(startPosition, targetPosition);
resultFuture.thenAccept(result -> {
    if (result.successful()) {
        Path path = result.getPath();
        // Use the path...
    } else {
        // Handle failure...
    }
});
```

### PathPosition

The `PathPosition` class represents a position in 3D space within a specific environment. It provides methods for:

- Manipulating positions (add, subtract, interpolate)
- Calculating distances (Manhattan, Octile, Euclidean)
- Comparing positions
- Converting to and from vectors

```java
// Creating a position
PathPosition position = new PathPosition(environment, x, y, z);

// Manipulating positions
PathPosition newPosition = position.add(1, 0, 1);
PathPosition midPoint = position.midPoint(targetPosition);

// Calculating distances
double distance = position.distance(targetPosition);
int manhattanDistance = position.manhattanDistance(targetPosition);
```

### NavigationPointProvider

The `NavigationPointProvider` interface is used to retrieve navigation point data at specific positions. It's a key component for integrating the pathfinding library with your 3D environment.

```java
// Example implementation of NavigationPointProvider
public class MyNavigationPointProvider implements NavigationPointProvider {
    @Override
    public NavigationPoint getNavigationPoint(PathPosition position) {
        // Check if the position is traversable in your environment
        boolean isTraversable = checkTraversable(position);
        return new NavigationPoint() {
            @Override
            public boolean isTraversable() {
                return isTraversable;
            }
        };
    }
}
```

### PathfinderConfiguration

The `PathfinderConfiguration` class is used to configure the pathfinding algorithm. It provides options for:

- Maximum iterations and path length
- Asynchronous processing
- Fallback behavior
- Cost handling
- Heuristic weights
- Custom processors

```java
// Creating a configuration
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

### HeuristicWeights

The `HeuristicWeights` class defines weights for different distance metrics used in the A* algorithm's heuristic function. These weights influence the prioritization of different path characteristics.

```java
// Using predefined weights
HeuristicWeights weights = HeuristicWeights.NATURAL_PATH_WEIGHTS; // For natural-looking paths
HeuristicWeights weights = HeuristicWeights.DIRECT_PATH_WEIGHTS;  // For shortest direct paths

// Creating custom weights
HeuristicWeights customWeights = HeuristicWeights.create(
    0.4,  // Manhattan weight
    0.2,  // Octile weight
    0.5,  // Perpendicular weight
    0.3,  // Height weight
    0.4   // Directional penalty weight
);
```

### PathfinderResult and Path

The `PathfinderResult` interface represents the outcome of a pathfinding operation, while the `Path` interface represents the actual path found.

```java
// Working with results and paths
PathfinderResult result = resultFuture.get();
if (result.successful()) {
    Path path = result.getPath();
    
    // Get path information
    int length = path.length();
    PathPosition start = path.getStart();
    PathPosition end = path.getEnd();
    
    // Manipulate the path
    Path interpolatedPath = path.interpolate(0.5); // Add points for smoother curves
    Path simplifiedPath = path.simplify(0.8);      // Remove unnecessary points
    
    // Iterate through positions in the path
    for (PathPosition position : path) {
        // Use each position...
    }
} else {
    // Check the failure reason
    PathState state = result.getPathState();
    switch (state) {
        case FAILED:
            // General failure
            break;
        case LENGTH_LIMITED:
            // Path exceeded maximum length
            break;
        case MAX_ITERATIONS_REACHED:
            // Reached maximum iterations
            break;
        // Handle other states...
    }
}
```

### Hooks and Processors

The API provides hooks and processors for customizing the pathfinding process:

- `PathfinderHook`: Called on each step of the pathfinding process
- `NodeValidationProcessor`: Validates whether a node is traversable
- `NodeCostProcessor`: Calculates custom costs for transitions between nodes

```java
// Example of a custom hook
PathfinderHook myHook = new PathfinderHook() {
    @Override
    public void onPathfindingStep(PathfindingContext context) {
        // Monitor or modify the pathfinding process
        Depth depth = context.getDepth();
        System.out.println("Current depth: " + depth.getValue());
    }
};
pathfinder.registerPathfindingHook(myHook);

// Example of a custom validation processor
NodeValidationProcessor myValidator = new NodeValidationProcessor() {
    @Override
    public boolean isValid(NodeEvaluationContext context) {
        // Custom validation logic
        PathPosition position = context.getCurrentPathPosition();
        return position.getY() < 100; // Example: Don't go above Y=100
    }
};

// Example of a custom cost processor
NodeCostProcessor myCostProcessor = new NodeCostProcessor() {
    @Override
    public Cost calculateCostContribution(NodeEvaluationContext context) {
        // Custom cost calculation
        PathPosition current = context.getCurrentPathPosition();
        PathPosition previous = context.getPreviousPathPosition();
        
        // Example: Add extra cost for vertical movement
        double heightDifference = Math.abs(current.getY() - previous.getY());
        return new Cost(heightDifference * 2.0);
    }
};
```

## Usage Examples

### Basic Pathfinding

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
        System.out.println("Path found with " + path.length() + " positions");
        
        // Use the path...
        for (PathPosition position : path) {
            System.out.println("Position: " + position);
        }
    } else {
        System.out.println("Pathfinding failed: " + result.getPathState());
    }
});
```

### Advanced Configuration

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
```

### Path Manipulation

```java
// Assuming we have a successful result with a path
Path path = result.getPath();

// Interpolate the path for smoother curves
Path smoothPath = path.interpolate(0.5);

// Simplify the path to reduce the number of points
Path simplifiedPath = path.simplify(0.8);

// Join two paths
Path combinedPath = path.join(anotherPath);

// Trim the path to a specific length
Path trimmedPath = path.trim(10);

// Mutate positions in the path
Path elevatedPath = path.mutatePositions(pos -> pos.add(0, 1, 0));
```

## Best Practices

1. **Configure appropriately**: Adjust the configuration based on your specific needs. For example, use higher maxIterations for complex environments.

2. **Choose the right heuristic weights**: Use NATURAL_PATH_WEIGHTS for natural-looking paths or DIRECT_PATH_WEIGHTS for shortest direct paths. Create custom weights for specific requirements.

3. **Implement an efficient NavigationPointProvider**: This is critical for performance as it will be called frequently during pathfinding.

4. **Use asynchronous processing**: For applications where responsiveness is important, use async=true in the configuration.

5. **Handle failures gracefully**: Check the PathState in the result to understand why pathfinding failed and take appropriate action.

6. **Clean up resources**: If you're creating many pathfinders, make sure to clean up resources when they're no longer needed.

7. **Test with different scenarios**: Test your pathfinding setup with various start and target positions to ensure it works correctly in all cases.
