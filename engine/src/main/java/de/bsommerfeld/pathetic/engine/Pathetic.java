package de.bsommerfeld.pathetic.engine;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Pathetic {

  private static final String PROPERTIES_FILE = "pathetic.properties";

  private static String engineVersion;

  private Pathetic() {
    throw new AssertionError("Pathetic is a utility class and should not be instantiated");
  }

  private static void loadEngineVersion() {
    try (InputStream inputStream =
        Pathetic.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
      Properties properties = new Properties();
      properties.load(inputStream);

      engineVersion = properties.getProperty("engine.version");
    } catch (IOException e) {
      throw new IllegalStateException("Error loading engine version", e);
    }
  }

  public static String getOrLoadEngineVersion() {
    if (engineVersion == null) loadEngineVersion();
    return engineVersion;
  }
}
