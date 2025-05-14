package de.bsommerfeld.pathetic.bukkit.util;

import de.bsommerfeld.pathetic.engine.util.ErrorLogger;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;

@UtilityClass
public class ChunkUtil {

  private static Method materialMethod;
  private static Method blockTypeMethod;

  static {
    if (BukkitVersionUtil.getVersion().isUnder(13, 0)) {
      try {
        materialMethod = Material.class.getDeclaredMethod("getMaterial", int.class);
        blockTypeMethod =
            ChunkSnapshot.class.getDeclaredMethod(
                "getBlockTypeId", int.class, int.class, int.class);
      } catch (NoSuchMethodException e) {
        throw ErrorLogger.logFatalError(e.getMessage(), e);
      }
    }
  }

  public long getChunkKey(final int x, final int z) {
    return x & 0xFFFFFFFFL | (z & 0xFFFFFFFFL) << 32;
  }

  /** Get the block type from a chunk snapshot at the given coordinates */
  @SneakyThrows
  public Material getMaterial(ChunkSnapshot snapshot, int x, int y, int z) {
    if (BukkitVersionUtil.getVersion().isUnder(13, 0)) {
      if (materialMethod == null || blockTypeMethod == null)
        throw ErrorLogger.logFatalError("Reflection Failure");
      try {
        return (Material) materialMethod.invoke(null, blockTypeMethod.invoke(snapshot, x, y, z));
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw ErrorLogger.logFatalError(e.getMessage(), e);
      }
    }
    return snapshot.getBlockType(x, y, z);
  }

}
