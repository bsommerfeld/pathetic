package de.bsommerfeld.pathetic.example.filter;

import de.bsommerfeld.pathetic.api.pathing.filter.PathFilter;
import de.bsommerfeld.pathetic.api.pathing.filter.PathValidationContext;
import de.bsommerfeld.pathetic.api.provider.NavigationPointProvider;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.bukkit.provider.BukkitNavigationPoint;
import java.util.EnumSet;
import org.bukkit.Material;

/**
 * A PathFilter that excludes nodes which are located on or near dangerous materials like lava.
 *
 * @api.Note Due to the radius check, this filter can be computationally expensive.
 */
public class DangerousMaterialsFilter implements PathFilter {

  private final EnumSet<Material> dangerousMaterials;
  private final int radius;

  /**
   * Constructor to initialize the filter with a list of dangerous materials and a radius to check
   * around each node.
   *
   * @param dangerousMaterials The list of materials to avoid, e.g., List.of(Material.LAVA,
   *     Material.CACTUS).
   * @param radius The radius around each node to check for the dangerous materials.
   */
  public DangerousMaterialsFilter(EnumSet<Material> dangerousMaterials, int radius) {
    this.dangerousMaterials = dangerousMaterials;
    this.radius = radius;
  }

  /**
   * Filters out nodes that are located on or near any of the specified dangerous materials.
   *
   * @param context The context of the current pathfinding validation.
   * @return true if the node is safe (i.e., not near the dangerous materials), false otherwise.
   */
  @Override
  public boolean filter(PathValidationContext context) {
    PathPosition position = context.getPosition();
    NavigationPointProvider navigationPointProvider = context.getNavigationPointProvider();
    for (int x = -radius; x <= radius; x++) {
      for (int y = -radius; y <= radius; y++) {
        for (int z = -radius; z <= radius; z++) {
          BukkitNavigationPoint bukkitNavigationPoint =
              (BukkitNavigationPoint) navigationPointProvider.getNavigationPoint(position.add(x, y, z));
          if (dangerousMaterials.contains(bukkitNavigationPoint.getMaterial())) {
            return false; // The node is near a dangerous material, so it's excluded.
          }
        }
      }
    }

    return true; // The node is safe.
  }
}
