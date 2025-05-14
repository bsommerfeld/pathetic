package de.bsommerfeld.pathetic.resolver;

import de.bsommerfeld.pathetic.provider.ChunkDataProvider;
import de.bsommerfeld.pathetic.provider.paper.PaperChunkDataProvider;
import de.bsommerfeld.pathetic.provider.spigot.SpigotChunkDataProviderResolver;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class ChunkDataProviderResolver {

  private final ChunkDataProvider chunkDataProvider;

  public ChunkDataProviderResolver(int major, int minor) {
    String version = "1." + major + "." + minor;

    if (isPaper()) {
      chunkDataProvider = new PaperChunkDataProvider();
    } else {
      chunkDataProvider = SpigotChunkDataProviderResolver.resolve(major, minor);
    }

    log.debug(
        "Detected version v{}, using {}", version, chunkDataProvider.getClass().getSimpleName());
  }

  private boolean isPaper() {
    try {
      Class.forName("io.papermc.paper.configuration.GlobalConfiguration");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }
}
