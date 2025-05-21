package de.bsommerfeld.pathetic.example.command;

import de.bsommerfeld.pathetic.api.pathing.Pathfinder;
import de.bsommerfeld.pathetic.api.pathing.result.PathfinderResult;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.bukkit.mapper.BukkitMapper;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public class PatheticCommand implements TabExecutor {

  // Map to store player sessions using their unique IDs
  private static final Map<UUID, PlayerSession> SESSION_MAP = new HashMap<>();

  // Pathfinder instance to handle pathfinding logic
  private final Pathfinder pathfinder;

  // Constructor to initialize the pathfinder
  public PatheticCommand(Pathfinder pathfinder) {
    this.pathfinder = pathfinder;
  }

  // Handle command execution
  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

    // Ensure the sender is a player
    if (!(sender instanceof Player)) return false;

    // Ensure the command has exactly one argument
    if (args.length != 1) return false;

    // Cast sender to Player
    Player player = (Player) sender;

    // Retrieve or create a new player session
    PlayerSession playerSession =
        SESSION_MAP.computeIfAbsent(player.getUniqueId(), k -> new PlayerSession());

    // Handle different commands
    switch (args[0]) {
      case "pos1":
        // Set position 1 to the player's current location
        playerSession.setPos1(player.getLocation());
        player.sendMessage("Position 1 set to " + player.getLocation());
        break;

      case "pos2":
        // Set position 2 to the player's current location
        playerSession.setPos2(player.getLocation());
        player.sendMessage("Position 2 set to " + player.getLocation());
        break;

      case "start":
        // Ensure both positions are set before starting pathfinding
        if (!playerSession.isComplete()) {
          player.sendMessage("Set both positions first!");
          return false;
        }

        // Convert Bukkit locations to pathfinding positions
        PathPosition start = BukkitMapper.toPathPosition(playerSession.getPos1());
        PathPosition target = BukkitMapper.toPathPosition(playerSession.getPos2());

        // Inform the player that pathfinding is starting
        player.sendMessage("Starting pathfinding... [Distance: " + start.distance(target) + "]");

        /*
         * Initiate pathfinding with the start and target positions, and a list of path filters.
         * The path filters are used to customize the pathfinding process. In this example, we use
         * the PassablePathFilter, MinimumHeightFilter, and DangerousMaterialsFilter to filter out
         * invalid paths.
         */
        CompletionStage<PathfinderResult> pathfindingResult =
            pathfinder.findPath(start, target);

        // Handle the pathfinding result
        pathfindingResult.thenAccept(
            result -> {
              player.sendMessage("State: " + result.getPathState().name());
              player.sendMessage("Path length: " + result.getPath().length());

              // If pathfinding is successful, show the path to the player
              if (result.successful() || result.hasFallenBack()) {
                result
                    .getPath()
                    .forEach(
                        position -> {
                          Location location = BukkitMapper.toLocation(position);
                          player.sendBlockChange(
                              location, Material.YELLOW_STAINED_GLASS.createBlockData());
                        });
              } else {
                player.sendMessage("Path not found!");
              }
            });
        break;

      default:
        player.sendMessage("Invalid argument!");
        return false;
    }

    return false;
  }

  // Provide tab completion for the command
  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String label, String[] args) {
    return Arrays.asList("pos1", "pos2", "start");
  }

  // Class to manage player session data
  private static class PlayerSession {

    private Location pos1;
    private Location pos2;

    public void setPos1(Location pos1) {
      this.pos1 = pos1;
    }

    public void setPos2(Location pos2) {
      this.pos2 = pos2;
    }

    // Check if both positions are set
    public boolean isComplete() {
      return pos1 != null && pos2 != null;
    }

    public Location getPos1() {
      return pos1;
    }

    public Location getPos2() {
      return pos2;
    }
  }
}
