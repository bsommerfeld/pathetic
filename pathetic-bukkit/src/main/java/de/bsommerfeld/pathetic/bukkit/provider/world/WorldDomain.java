package de.bsommerfeld.pathetic.bukkit.provider.world;

import de.bsommerfeld.pathetic.engine.util.ExpiringHashMap;
import java.util.Map;
import java.util.Optional;
import org.bukkit.ChunkSnapshot;

public class WorldDomain {

  private final Map<Long, ExpiringHashMap.Entry<ChunkSnapshot>> chunkSnapshotMap =
      new ExpiringHashMap<>();

  public Optional<ChunkSnapshot> getSnapshot(long key) {
    ExpiringHashMap.Entry<ChunkSnapshot> entry = chunkSnapshotMap.get(key);
    if (entry == null) return Optional.empty();
    return Optional.ofNullable(entry.getValue());
  }

  public void addSnapshot(final long key, final ChunkSnapshot snapshot) {
    chunkSnapshotMap.put(key, new ExpiringHashMap.Entry<>(snapshot));
  }

  public void removeSnapshot(final long key) {
    chunkSnapshotMap.remove(key);
  }

  public boolean containsSnapshot(final long key) {
    return chunkSnapshotMap.get(key) != null;
  }
}
