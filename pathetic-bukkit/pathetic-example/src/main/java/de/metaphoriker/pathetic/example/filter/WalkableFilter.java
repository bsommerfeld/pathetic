package de.metaphoriker.pathetic.example.filter;

import de.metaphoriker.pathetic.api.pathing.filter.PathFilter;
import de.metaphoriker.pathetic.api.pathing.filter.PathValidationContext;
import de.metaphoriker.pathetic.api.provider.NavigationPointProvider;
import de.metaphoriker.pathetic.api.wrapper.PathPosition;
import de.metaphoriker.pathetic.bukkit.provider.BukkitNavigationPoint;

public class WalkableFilter implements PathFilter {

  @Override
  public boolean filter(PathValidationContext pathValidationContext) {

    PathPosition above = pathValidationContext.getPosition().add(0, 1, 0);
    NavigationPointProvider navigationPointProvider = pathValidationContext.getNavigationPointProvider();

    var currentNavigationPoint = (BukkitNavigationPoint) navigationPointProvider.getNavigationPoint(pathValidationContext.getPosition());
    var aboveNavigationPoint = (BukkitNavigationPoint) navigationPointProvider.getNavigationPoint(above);

    PathPosition below = pathValidationContext.getPosition().subtract(0, 1, 0);
    var belowNavigationPoint = (BukkitNavigationPoint) navigationPointProvider.getNavigationPoint(below);


    return currentNavigationPoint.isTraversable() && aboveNavigationPoint.isTraversable() && belowNavigationPoint.getMaterial().isSolid();
  }
}
