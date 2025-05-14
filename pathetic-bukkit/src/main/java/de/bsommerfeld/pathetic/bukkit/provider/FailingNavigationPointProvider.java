package de.bsommerfeld.pathetic.bukkit.provider;

import de.bsommerfeld.pathetic.api.provider.NavigationPointProvider;
import de.bsommerfeld.pathetic.api.provider.NavigationPoint;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.bukkit.provider.world.WorldDomain;
import de.bsommerfeld.pathetic.bukkit.util.BukkitVersionUtil;
import de.bsommerfeld.pathetic.bukkit.util.ChunkUtil;
import de.bsommerfeld.pathetic.resolver.ChunkDataProviderResolver;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;

/**
 * The {@code FailingNavigationPointProvider} class implements the {@link NavigationPointProvider}
 * interface and provides a default implementation for retrieving navigation point data from a
 * Minecraft world. It utilizes chunk snapshots to efficiently access information, even in
 * asynchronous contexts.
 *
 * <p>This provider also uses NMS (net.minecraft.server) utilities to bypass the Spigot AsyncCatcher
 * and fetch snapshots natively from an asynchronous context. This allows for more flexible and
 * efficient access to world data.
 *
 * <p>Note: While this provider is designed to efficiently retrieve navigation point data, it may
 * encounter failures or null results if the pathfinder is not permitted to load chunks or if chunks
 * are not loaded in the world. Developers using this provider should handle potential failures
 * gracefully.
 */
public class FailingNavigationPointProvider implements NavigationPointProvider {

  /**
   * A map storing chunk snapshots for each world, indexed by the world's UUID. This allows for
   * efficient retrieval of chunk data without repeatedly accessing the world.
   */
  protected static final Map<UUID, WorldDomain> SNAPSHOTS_MAP = new ConcurrentHashMap<>();

  /**
   * A resolver responsible for providing access to chunk data based on the server's version. This
   * ensures compatibility across different Minecraft versions.
   */
  protected static final ChunkDataProviderResolver CHUNK_DATA_PROVIDER_RESOLVER;

  static {
    BukkitVersionUtil.Version version = BukkitVersionUtil.getVersion();
    CHUNK_DATA_PROVIDER_RESOLVER =
        new ChunkDataProviderResolver((int) version.getMajor(), (int) version.getMinor());
  }

  /**
   * Invalidates the cached chunk snapshot for the specified world and chunk coordinates. This
   * method should be called when a chunk is modified to ensure that the provider uses the updated
   * chunk data.
   *
   * @param worldUUID The UUID of the world.
   * @param chunkX The X coordinate of the chunk.
   * @param chunkZ The Z coordinate of the chunk.
   */
  public static void invalidateChunk(UUID worldUUID, int chunkX, int chunkZ) {
    if (SNAPSHOTS_MAP.containsKey(worldUUID)) {
      WorldDomain worldDomain = SNAPSHOTS_MAP.get(worldUUID);
      long chunkKey = ChunkUtil.getChunkKey(chunkX, chunkZ);
      worldDomain.removeSnapshot(chunkKey);
    }
  }

  /**
   * Fetches the navigation point data at the given position. This method retrieves the chunk
   * snapshot containing the position and extracts the relevant information, such as the material
   * and block state.
   *
   * @param position The position to fetch the navigation point for.
   * @return An optional containing the navigation point if found, or an empty optional if the chunk
   *     is not loaded or the position is invalid.
   */
  private static Optional<NavigationPoint> fetchNavigationPoint(PathPosition position) {
    Optional<ChunkSnapshot> chunkSnapshotOptional = getChunkSnapshot(position);

    int chunkX = position.getFlooredX() >> 4;
    int chunkZ = position.getFlooredZ() >> 4;

    if (chunkSnapshotOptional.isPresent()) {
      int x = position.getFlooredX() - chunkX * 16;
      int z = position.getFlooredZ() - chunkZ * 16;

      Material material =
          ChunkUtil.getMaterial(chunkSnapshotOptional.get(), x, position.getFlooredY(), z);
      BlockState blockState =
          CHUNK_DATA_PROVIDER_RESOLVER
              .getChunkDataProvider()
              .getBlockState(chunkSnapshotOptional.get(), x, position.getFlooredY(), z);
      return Optional.of(new BukkitNavigationPoint(material, blockState));
    }

    return Optional.empty();
  }

  /**
   * Retrieves the chunk snapshot containing the given position. This method first checks if a
   * snapshot is already cached for the world. If not, it attempts to load the chunk and create a
   * new snapshot.
   *
   * @param position The position used to determine the chunk coordinates.
   * @return An optional containing the chunk snapshot if found, or an empty optional if the chunk
   *     is not loaded or the world is invalid.
   */
  protected static Optional<ChunkSnapshot> getChunkSnapshot(PathPosition position) {
    int chunkX = position.getFlooredX() >> 4;
    int chunkZ = position.getFlooredZ() >> 4;

    if (SNAPSHOTS_MAP.containsKey(position.getPathEnvironment().getUuid())) {

      WorldDomain worldDomain = SNAPSHOTS_MAP.get(position.getPathEnvironment().getUuid());
      long chunkKey = ChunkUtil.getChunkKey(chunkX, chunkZ);

      Optional<ChunkSnapshot> snapshot = worldDomain.getSnapshot(chunkKey);
      if (snapshot.isPresent()) return snapshot;
    }

    World world = Bukkit.getWorld(position.getPathEnvironment().getUuid());
    if (world == null) return Optional.empty();

    if (world.isChunkLoaded(chunkX, chunkZ))
      return Optional.ofNullable(
          processChunkSnapshot(
              position,
              chunkX,
              chunkZ,
              CHUNK_DATA_PROVIDER_RESOLVER
                  .getChunkDataProvider()
                  .getSnapshot(world, chunkX, chunkZ)));

    return Optional.empty();
  }

  /**
   * Processes a chunk snapshot by caching it in the {@link #SNAPSHOTS_MAP}. This method ensures
   * that future requests for the same chunk can be served from the cache.
   *
   * @param position The position used to access the world's UUID.
   * @param chunkX The X coordinate of the chunk.
   * @param chunkZ The Z coordinate of the chunk.
   * @param chunkSnapshot The chunk snapshot to process.
   * @return The processed chunk snapshot.
   */
  protected static ChunkSnapshot processChunkSnapshot(
      PathPosition position, int chunkX, int chunkZ, ChunkSnapshot chunkSnapshot) {
    WorldDomain worldDomain =
        SNAPSHOTS_MAP.computeIfAbsent(
            position.getPathEnvironment().getUuid(), uuid -> new WorldDomain());
    worldDomain.addSnapshot(ChunkUtil.getChunkKey(chunkX, chunkZ), chunkSnapshot);
    return chunkSnapshot;
  }

  /** {@inheritDoc} */
  @Override
  public NavigationPoint getNavigationPoint(@NonNull PathPosition position) {
    Optional<NavigationPoint> point = fetchNavigationPoint(position);
    return point.orElse(null);
  }
}
