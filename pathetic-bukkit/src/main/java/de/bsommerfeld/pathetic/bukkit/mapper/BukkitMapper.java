package de.bsommerfeld.pathetic.bukkit.mapper;

import java.util.Arrays;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;
import de.bsommerfeld.pathetic.api.wrapper.PathEnvironment;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.api.wrapper.PathVector;
import de.bsommerfeld.pathetic.engine.util.ErrorLogger;

@UtilityClass
public class BukkitMapper {

  private final boolean IS_NEWER_WORLD;

  static {
    IS_NEWER_WORLD =
        Arrays.stream(World.class.getMethods())
            .anyMatch(method -> "getMinHeight".equalsIgnoreCase(method.getName()));
  }

  @NonNull
  public Location toLocation(@NonNull PathPosition pathPosition) {
    return new Location(
        toWorld(pathPosition.getPathEnvironment()),
        pathPosition.getX(),
        pathPosition.getY(),
        pathPosition.getZ());
  }

  @NonNull
  public PathPosition toPathPosition(@NonNull Location location) {

    if (location.getWorld() == null) throw ErrorLogger.logFatalError("World is null");

    return new PathPosition(
        toPathWorld(location.getWorld()),
        location.getBlockX(),
        location.getBlockY(),
        location.getBlockZ());
  }

  @NonNull
  public Vector toVector(PathVector pathVector) {
    return new Vector(pathVector.getX(), pathVector.getY(), pathVector.getZ());
  }

  @NonNull
  public PathVector toPathVector(Vector vector) {
    return new PathVector(vector.getX(), vector.getY(), vector.getZ());
  }

  public World toWorld(@NonNull PathEnvironment pathEnvironment) {
    return Bukkit.getWorld(pathEnvironment.getUuid());
  }

  @NonNull
  public PathEnvironment toPathWorld(@NonNull World world) {
    return new PathEnvironment(
        world.getUID(), world.getName(), getMinHeight(world), getMaxHeight(world));
  }

  private int getMinHeight(World world) {
    return IS_NEWER_WORLD ? world.getMinHeight() : 0;
  }

  private int getMaxHeight(World world) {
    return IS_NEWER_WORLD ? world.getMaxHeight() : 256;
  }
}
