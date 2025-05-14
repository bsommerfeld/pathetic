package de.bsommerfeld.pathetic.bukkit.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;

@UtilityClass
public class BukkitVersionUtil {

  private static final double CURRENT_MAJOR;
  private static final double CURRENT_MINOR;

  static {
    Pattern pattern = Pattern.compile(".*\\(.*MC.\\s*([a-zA-z0-9\\-.]+)\\s*\\)");
    Matcher matcher = pattern.matcher(Bukkit.getVersion());

    if (!matcher.matches() || matcher.group(1) == null) {
      throw new IllegalStateException("Cannot parse version String '" + Bukkit.getVersion() + "'");
    }

    String[] elements = matcher.group(1).split("\\.");
    if (elements.length < 1) {
      throw new IllegalStateException("Invalid server version '" + matcher.group(1) + "'");
    }

    int[] values = new int[3];
    for (int i = 0; i < Math.min(values.length, elements.length); i++) {
      values[i] = Integer.parseInt(elements[i].trim());
    }

    CURRENT_MAJOR = values[1];
    CURRENT_MINOR = values[2];
  }

  public static Version getVersion() {
    return new Version(CURRENT_MAJOR, CURRENT_MINOR);
  }

  @Getter
  public static class Version {

    private final double major;
    private final double minor;

    public static Version of(double major, double minor) {
      return new Version(major, minor);
    }

    public Version(double major, double minor) {
      this.major = major;
      this.minor = minor;
    }

    public boolean isUnder(double major, double minor) {
      if (this.major < major) return true;
      return this.major == major && this.minor < minor;
    }

    public boolean isUnder(Version that) {
      return this.isUnder(that.major, that.minor);
    }

    public boolean isOver(Version that) {
      if (this.major > that.major) return true;
      return this.major == that.major && this.minor > that.minor;
    }

    public boolean isEqual(Version that) {
      return this.major == that.major && this.minor == that.minor;
    }
  }
}
