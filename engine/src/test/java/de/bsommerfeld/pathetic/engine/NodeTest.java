package de.bsommerfeld.pathetic.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.bsommerfeld.pathetic.api.pathing.heuristic.HeuristicStrategies;
import de.bsommerfeld.pathetic.api.pathing.heuristic.HeuristicWeights;
import de.bsommerfeld.pathetic.api.pathing.heuristic.IHeuristicStrategy;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        assertEquals(0, node.getDepth());
        assertNull(node.getParent());
    }

    @Test
    void testGetPosition() {
        assertEquals(position, node.getPosition());
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
        assertTrue(node.getHeuristic() > 0);
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
        double expectedFCost = 10.5 + node.getHeuristic();
        assertEquals(expectedFCost, node.getFCost());
    }

    @Test
    void testIsTarget() {
        // Current node is not the target
        assertFalse(node.isTarget(target));

        // Create a node at the target position
        Node targetNode = new Node(target, start, target, weights, strategy, 0);
        assertTrue(targetNode.isTarget(target));
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
    void testNotComparable() {
        /*
         * Node intentionally does not implement Comparable. A cost-based natural ordering would
         * contradict the position-based equals/hashCode and silently break sorted containers
         * (TreeSet/TreeMap). The heap orders nodes via explicit primitive cost keys, so no natural
         * ordering is needed.
         */
        assertFalse(node instanceof Comparable, "Node must not implement Comparable");
    }
}