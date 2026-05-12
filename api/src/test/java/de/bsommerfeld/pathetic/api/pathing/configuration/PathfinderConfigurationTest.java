package de.bsommerfeld.pathetic.api.pathing.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.bsommerfeld.pathetic.api.pathing.hook.PathfinderHook;
import de.bsommerfeld.pathetic.api.pathing.processing.CostProcessor;
import de.bsommerfeld.pathetic.api.pathing.processing.ValidationProcessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class PathfinderConfigurationTest {

  /*
   * deepCopy must produce a configuration that is independent of subsequent mutations to the
   * source's backing collections, even when the user retains a reference to the original mutable
   * list passed into the builder.
   */
  @Test
  void deepCopyIsolatesValidatorListFromSourceListMutation() {
    ValidationProcessor v1 = ctx -> true;
    ValidationProcessor v2 = ctx -> true;
    List<ValidationProcessor> mutableSource = new ArrayList<>(Arrays.asList(v1));

    PathfinderConfiguration original =
        PathfinderConfiguration.builder().validationProcessors(mutableSource).build();
    PathfinderConfiguration copy = PathfinderConfiguration.deepCopy(original);

    // Sanity: copy starts with the same elements
    assertEquals(1, copy.getNodeValidationProcessors().size());
    assertSame(v1, copy.getNodeValidationProcessors().get(0));

    // Mutate the backing list the user still owns. Original config WILL leak the mutation
    // (its unmodifiable wrapper is over the user's list); the deepCopy MUST NOT.
    mutableSource.add(v2);

    assertEquals(
        1, copy.getNodeValidationProcessors().size(), "deepCopy must isolate validator list from source mutation");
    assertTrue(copy.getNodeValidationProcessors().contains(v1));
  }

  @Test
  void deepCopyIsolatesCostProcessorListFromSourceListMutation() {
    CostProcessor c1 = ctx -> de.bsommerfeld.pathetic.api.pathing.processing.Cost.ZERO;
    CostProcessor c2 = ctx -> de.bsommerfeld.pathetic.api.pathing.processing.Cost.ZERO;
    List<CostProcessor> mutableSource = new ArrayList<>(Arrays.asList(c1));

    PathfinderConfiguration original =
        PathfinderConfiguration.builder().costProcessor(mutableSource).build();
    PathfinderConfiguration copy = PathfinderConfiguration.deepCopy(original);

    mutableSource.add(c2);

    assertEquals(1, copy.getNodeCostProcessors().size());
    assertSame(c1, copy.getNodeCostProcessors().get(0));
  }

  @Test
  void deepCopyIsolatesHookListFromSourceListMutation() {
    PathfinderHook h1 = ctx -> {};
    PathfinderHook h2 = ctx -> {};
    List<PathfinderHook> mutableSource = new ArrayList<>(Arrays.asList(h1));

    PathfinderConfiguration original =
        PathfinderConfiguration.builder().pathfindingHooks(mutableSource).build();
    PathfinderConfiguration copy = PathfinderConfiguration.deepCopy(original);

    mutableSource.add(h2);

    assertEquals(1, copy.pathfindingHooks().size());
    assertSame(h1, copy.pathfindingHooks().get(0));
  }

  @Test
  void deepCopyUsesFreshListContainers() {
    PathfinderConfiguration original =
        PathfinderConfiguration.builder()
            .validationProcessors(new ArrayList<>(Arrays.asList((ValidationProcessor) ctx -> true)))
            .build();
    PathfinderConfiguration copy = PathfinderConfiguration.deepCopy(original);

    // The list instances must be different objects, even if their contents match.
    assertNotSame(
        original.getNodeValidationProcessors(),
        copy.getNodeValidationProcessors(),
        "deepCopy must produce a new list container, not share the source's wrapper");
  }

  @Test
  void deepCopySharesServiceReferences() {
    // Provider, executor, strategies, individual processors and hooks are user-supplied
    // interface implementations / external services. They have no clone contract and are
    // intentionally shared by reference.
    PathfinderConfiguration original = PathfinderConfiguration.builder().build();
    PathfinderConfiguration copy = PathfinderConfiguration.deepCopy(original);

    assertSame(original.getProvider(), copy.getProvider());
    assertSame(original.executorService(), copy.executorService());
    assertSame(original.getHeuristicStrategy(), copy.getHeuristicStrategy());
    assertSame(original.getNeighborStrategy(), copy.getNeighborStrategy());
    assertSame(original.getHeuristicWeights(), copy.getHeuristicWeights());
  }

  @Test
  void deepCopyPreservesPrimitiveFields() {
    PathfinderConfiguration original =
        PathfinderConfiguration.builder()
            .maxIterations(2500)
            .maxLength(17)
            .async(true)
            .fallback(false)
            .gridCellSize(8)
            .bloomFilterSize(2048)
            .bloomFilterFpp(0.005)
            .reopenClosedNodes(true)
            .build();
    PathfinderConfiguration copy = PathfinderConfiguration.deepCopy(original);

    assertEquals(2500, copy.getMaxIterations());
    assertEquals(17, copy.getMaxLength());
    assertTrue(copy.isAsync());
    assertEquals(false, copy.isFallback());
    assertEquals(8, copy.getGridCellSize());
    assertEquals(2048, copy.getBloomFilterSize());
    assertEquals(0.005, copy.getBloomFilterFpp());
    assertTrue(copy.shouldReopenClosedNodes());
  }
}
