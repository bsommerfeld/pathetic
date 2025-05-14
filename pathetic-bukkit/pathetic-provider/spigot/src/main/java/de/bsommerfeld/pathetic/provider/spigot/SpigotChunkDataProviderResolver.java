package de.bsommerfeld.pathetic.provider.spigot;

import de.bsommerfeld.pathetic.provider.ChunkDataProvider;
import de.bsommerfeld.pathetic.provider.v1_12.v1_12ChunkDataProviderImpl;
import de.bsommerfeld.pathetic.provider.v1_15.v1_15ChunkDataProviderImpl;
import de.bsommerfeld.pathetic.provider.v1_16.v1_16ChunkDataProviderImpl;
import de.bsommerfeld.pathetic.provider.v1_17.v1_17ChunkDataProviderImpl;
import de.bsommerfeld.pathetic.provider.v1_18.v1_18ChunkDataProviderImpl;
import de.bsommerfeld.pathetic.provider.v1_18_R2.v1_18_R2ChunkDataProviderImpl;
import de.bsommerfeld.pathetic.provider.v1_19_R2.v1_19_R2ChunkDataProviderImpl;
import de.bsommerfeld.pathetic.provider.v1_19_R3.v1_19_R3ChunkDataProviderImpl;
import de.bsommerfeld.pathetic.provider.v1_20_R1.v1_20_R1ChunkDataProviderImpl;
import de.bsommerfeld.pathetic.provider.v1_20_R2.v1_20_R2ChunkDataProviderImpl;
import de.bsommerfeld.pathetic.provider.v1_20_R3.v1_20_R3ChunkDataProviderImpl;
import de.bsommerfeld.pathetic.provider.v1_20_R4.v1_20_R4ChunkDataProviderImpl;
import de.bsommerfeld.pathetic.provider.v1_21_R1.v1_21_R1ChunkDataProviderImpl;
import de.bsommerfeld.pathetic.provider.v1_21_R2.v1_21_R2ChunkDataProviderImpl;
import de.bsommerfeld.pathetic.provider.v1_21_R3ChunkDataProviderImpl;
import de.bsommerfeld.pathetic.provider.v1_21_R4ChunkDataProviderImpl;
import de.bsommerfeld.pathetic.provider.v1_8.v1_8ChunkDataProviderImpl;

public class SpigotChunkDataProviderResolver {

  private SpigotChunkDataProviderResolver() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  public static ChunkDataProvider resolve(int major, int minor) {
    final ChunkDataProvider chunkDataProvider;
    switch (major) {
      case 21:
        if(minor == 5) {
          chunkDataProvider = new v1_21_R4ChunkDataProviderImpl();
          break;
        }
        if(minor == 4) {
          chunkDataProvider = new v1_21_R3ChunkDataProviderImpl();
          break;
        }
        if(minor == 2 || minor == 3) {
          chunkDataProvider = new v1_21_R2ChunkDataProviderImpl();
          break;
        }
        chunkDataProvider = new v1_21_R1ChunkDataProviderImpl();
        break;
      case 20:
        if (minor == 5 || minor == 6) {
          chunkDataProvider = new v1_20_R4ChunkDataProviderImpl();
          break;
        } else if (minor == 3 || minor == 4) {
          chunkDataProvider = new v1_20_R3ChunkDataProviderImpl();
          break;
        } else if (minor == 2) {
          chunkDataProvider = new v1_20_R2ChunkDataProviderImpl();
          break;
        } else if (minor == 1) {
          chunkDataProvider = new v1_20_R1ChunkDataProviderImpl();
          break;
        }
      case 19:
        if (minor == 2 || minor == 3) {
          chunkDataProvider = new v1_19_R2ChunkDataProviderImpl();
          break;
        }
        if (minor == 4) {
          chunkDataProvider = new v1_19_R3ChunkDataProviderImpl();
          break;
        }
      case 18:
        if (minor == 2) {
          chunkDataProvider = new v1_18_R2ChunkDataProviderImpl();
          break;
        }
        chunkDataProvider = new v1_18ChunkDataProviderImpl();
        break;
      case 17:
        chunkDataProvider = new v1_17ChunkDataProviderImpl();
        break;
      case 16:
        chunkDataProvider = new v1_16ChunkDataProviderImpl();
        break;
      case 15:
        chunkDataProvider = new v1_15ChunkDataProviderImpl();
        break;
      case 12:
        chunkDataProvider = new v1_12ChunkDataProviderImpl();
        break;
      case 8:
        chunkDataProvider = new v1_8ChunkDataProviderImpl();
        break;
      default:
        throw new IllegalArgumentException("Unsupported version: " + major + "." + minor);
    }
    return chunkDataProvider;
  }
}
