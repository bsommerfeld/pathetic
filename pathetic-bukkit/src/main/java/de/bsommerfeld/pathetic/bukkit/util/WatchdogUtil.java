package de.bsommerfeld.pathetic.bukkit.util;

import de.bsommerfeld.pathetic.engine.util.ErrorLogger;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;

@UtilityClass
public class WatchdogUtil {

  private Class<?> watchdogClazz = null;
  private Method tickMethod = null;

  static {
    try {
      watchdogClazz = Class.forName("org.spigotmc.WatchdogThread");
      tickMethod = watchdogClazz.getDeclaredMethod("tick");
    } catch (ClassNotFoundException | NoSuchMethodException e) {
      throw ErrorLogger.logFatalError(e.getMessage(), e);
    }
  }

  public void tickWatchdog() {
    if (Bukkit.isPrimaryThread())
      if (tickMethod != null && watchdogClazz != null) {
        try {
          tickMethod.invoke(watchdogClazz);
        } catch (IllegalAccessException | InvocationTargetException e) {
          throw ErrorLogger.logFatalError(e.getMessage(), e);
        }
      }
  }
}
