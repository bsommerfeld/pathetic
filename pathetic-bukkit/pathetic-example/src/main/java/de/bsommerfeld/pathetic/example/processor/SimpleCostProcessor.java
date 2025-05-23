package de.bsommerfeld.pathetic.example.processor;

import de.bsommerfeld.pathetic.api.pathing.processing.Cost;
import de.bsommerfeld.pathetic.api.pathing.processing.NodeCostCalculator;
import de.bsommerfeld.pathetic.api.pathing.processing.context.NodeEvaluationContext;
import de.bsommerfeld.pathetic.api.provider.NavigationPointProvider;
import de.bsommerfeld.pathetic.bukkit.provider.BukkitNavigationPoint;
import org.bukkit.Material;

public class SimpleCostProcessor implements NodeCostCalculator {

  @Override
  public Cost calculateCostContribution(NodeEvaluationContext context) {
    NavigationPointProvider provider = context.getNavigationPointProvider();

    BukkitNavigationPoint beneath =
        (BukkitNavigationPoint)
            provider.getNavigationPoint(context.getCurrentPathPosition().subtract(0, 1, 0));

    if (beneath.getMaterial() == Material.STONE) {
      return Cost.of(20);
    }

    return Cost.ZERO;
  }
}
