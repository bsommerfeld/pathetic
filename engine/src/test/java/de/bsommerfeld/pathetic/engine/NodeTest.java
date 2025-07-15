package de.bsommerfeld.pathetic.engine;

import de.bsommerfeld.pathetic.api.pathing.heuristic.HeuristicStrategies;
import de.bsommerfeld.pathetic.api.pathing.heuristic.HeuristicWeights;
import de.bsommerfeld.pathetic.api.pathing.heuristic.IHeuristicStrategy;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeTest {

    private PathPosition position;
    private PathPosition start;
    private PathPosition target;
    private HeuristicWeights weights;
    private IHeuristicStrategy strategy;
    private Node node;

    @BeforeEach
    void setUp() {
        position = new PathPosition(5, 5, 5);
        start = new PathPosition(0, 0, 0);
        target = new PathPosition(10, 10, 10);
        weights = HeuristicWeights.DEFAULT_WEIGHTS;
        strategy = HeuristicStrategies.LINEAR;
        node = new Node(position, start, target, weights, strategy, 0);
    }

    @Test
    void testConstructor() {
        assertEquals(position, node.getPosition());
        assertEquals(start, node.getStart());
        assertEquals(target, node.getTarget());
        assertEquals(0, node.getDepth());
        assertNull(node.getParent());
    }

    @Test
    void testGetPosition() {
        assertEquals(position, node.getPosition());
    }

    @Test
    void testGetStart() {
        assertEquals(start, node.getStart());
    }

    @Test
    void testGetTarget() {
        assertEquals(target, node.getTarget());
    }

    @Test
    void testGetDepth() {
        assertEquals(0, node.getDepth());

        // Test with different depth
        Node deeperNode = new Node(position, start, target, weights, strategy, 5);
        assertEquals(5, deeperNode.getDepth());
    }

    @Test
    void testGetHeuristic() {
        assertNotNull(node.getHeuristic());
        assertTrue(node.getHeuristic().get() > 0);
    }

    @Test
    void testSetAndGetParent() {
        assertNull(node.getParent());

        Node parent = new Node(start, start, target, weights, strategy, 0);
        node.setParent(parent);

        assertEquals(parent, node.getParent());
    }

    @Test
    void testSetAndGetGCost() {
        // G-cost should be 0 when parent is null
        assertEquals(0.0, node.getGCost());

        // Set a parent and G-cost
        Node parent = new Node(start, start, target, weights, strategy, 0);
        node.setParent(parent);
        node.setGCost(10.5);

        assertEquals(10.5, node.getGCost());
    }

    @Test
    void testGetFCost() {
        // Set a parent and G-cost
        Node parent = new Node(start, start, target, weights, strategy, 0);
        node.setParent(parent);
        node.setGCost(10.5);

        // F-cost = G-cost + H-cost
        double expectedFCost = 10.5 + node.getHeuristic().get();
        assertEquals(expectedFCost, node.getFCost());
    }

    @Test
    void testIsTarget() {
        // Current node is not the target
        assertFalse(node.isTarget());

        // Create a node at the target position
        Node targetNode = new Node(target, start, target, weights, strategy, 0);
        assertTrue(targetNode.isTarget());
    }

    @Test
    void testEquals() {
        // Same position
        Node samePositionNode = new Node(position, start, target, weights, strategy, 0);
        assertEquals(node, samePositionNode);

        // Different position
        Node differentPositionNode = new Node(new PathPosition(6, 6, 6), start, target, weights, strategy, 0);
        assertNotEquals(node, differentPositionNode);

        // Null and different type
        assertNotEquals(node, null);
        assertNotEquals(node, "not a node");
    }

    @Test
    void testHashCode() {
        // Same position should have same hash code
        Node samePositionNode = new Node(position, start, target, weights, strategy, 0);
        assertEquals(node.hashCode(), samePositionNode.hashCode());

        // Different position should have different hash code
        Node differentPositionNode = new Node(new PathPosition(6, 6, 6), start, target, weights, strategy, 0);
        assertNotEquals(node.hashCode(), differentPositionNode.hashCode());
    }

    @Test
    void testCompareTo() {
        // Create nodes with different F-costs
        Node node1 = new Node(position, start, target, weights, strategy, 0);
        node1.setParent(new Node(start, start, target, weights, strategy, 0));
        node1.setGCost(5.0);

        Node node2 = new Node(position, start, target, weights, strategy, 0);
        node2.setParent(new Node(start, start, target, weights, strategy, 0));
        node2.setGCost(10.0);

        // node1 has lower F-cost, so should be less than node2
        assertTrue(node1.compareTo(node2) < 0);
        assertTrue(node2.compareTo(node1) > 0);

        // Same F-cost but different depths
        Node node3 = new Node(position, start, target, weights, strategy, 1);
        node3.setParent(new Node(start, start, target, weights, strategy, 0));
        node3.setGCost(5.0);

        // If F-costs and heuristics are equal, compare by depth
        if (node1.getFCost() == node3.getFCost() &&
                node1.getHeuristic().get() == node3.getHeuristic().get()) {
            assertTrue(node1.compareTo(node3) < 0);
        }

        // Same node should be equal
        assertEquals(0, node1.compareTo(node1));
    }
}