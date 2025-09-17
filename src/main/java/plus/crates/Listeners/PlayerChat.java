package plus.crates.Listeners;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;

import plus.crates.Crates.KeyCrate;
import plus.crates.CratesPlus;

public class PlayerChat implements Listener {
    private final CratesPlus cratesPlus;

    public PlayerChat(CratesPlus cratesPlus) {
        this.cratesPlus = cratesPlus;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        String message = event.getMessage();
        
        // Check if player is waiting for input
        if (cratesPlus.getConfigHandler().getWaitingForInput().containsKey(playerUUID)) {
            event.setCancelled(true); // Cancel the chat message
            
            String inputType = cratesPlus.getConfigHandler().getWaitingForInput().get(playerUUID);
            
            try {
                // Handle different input types
                switch (inputType) {
                    case "create_crate":
                        handleCreateCrateInput(player, message);
                        break;
                    case "rename_crate":
                        handleRenameCrateInput(player, message);
                        break;
                    default:
                        // Unknown input type, remove from waiting
                        cratesPlus.getConfigHandler().getWaitingForInput().remove(playerUUID);
                        player.sendMessage("§cUnknown input type. Operation cancelled.");
                        break;
                }
            } catch (Exception e) {
                cratesPlus.getLogger().warning("Error handling chat input for player " + player.getName() + ": " + e.getMessage());
                cratesPlus.getConfigHandler().getWaitingForInput().remove(playerUUID);
                player.sendMessage("§cAn error occurred while processing your input. Please try again.");
            }
        }
    }
    
    private void handleCreateCrateInput(Player player, String message) {
        UUID playerUUID = player.getUniqueId();
        
        // Remove from waiting list
        cratesPlus.getConfigHandler().getWaitingForInput().remove(playerUUID);
        
        if (message.equalsIgnoreCase("cancel")) {
            cratesPlus.removeCreating(playerUUID);
            player.sendMessage(cratesPlus.getPluginPrefix() + "§eCrate creation cancelled.");
            return;
        }
        
        String crateName = message.trim();
        
        // Validate crate name
        if (crateName.isEmpty() || crateName.length() > 16) {
            player.sendMessage(cratesPlus.getPluginPrefix() + "§cInvalid crate name! Name must be 1-16 characters long.");
            cratesPlus.removeCreating(playerUUID);
            return;
        }
        
        // Check if crate already exists
        if (cratesPlus.getConfigHandler().getCrates().containsKey(crateName.toLowerCase())) {
            player.sendMessage(cratesPlus.getPluginPrefix() + "§cA crate with that name already exists!");
            cratesPlus.removeCreating(playerUUID);
            return;
        }
        
        // Create the crate
        try {
            // Create a basic KeyCrate
            KeyCrate newCrate = new KeyCrate(cratesPlus.getConfigHandler(), crateName);
            
            // Add to config handler
            cratesPlus.getConfigHandler().getCrates().put(crateName.toLowerCase(), newCrate);
            
            // Save config
            cratesPlus.saveConfig();
            
            player.sendMessage(cratesPlus.getPluginPrefix() + "§aSuccessfully created crate: " + "§e" + crateName);
            player.sendMessage(cratesPlus.getPluginPrefix() + "§7Use /crate edit " + crateName + " to configure this crate.");
            
        } catch (Exception e) {
            player.sendMessage(cratesPlus.getPluginPrefix() + "§cError creating crate: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cratesPlus.removeCreating(playerUUID);
        }
    }
    
    private void handleRenameCrateInput(Player player, String message) {
        UUID playerUUID = player.getUniqueId();
        
        // Remove from waiting list
        cratesPlus.getConfigHandler().getWaitingForInput().remove(playerUUID);
        
        if (message.equalsIgnoreCase("cancel")) {
            player.sendMessage(cratesPlus.getPluginPrefix() + "§eCrate rename cancelled.");
            return;
        }
        
        String newName = message.trim();
        
        // Validate new name
        if (newName.isEmpty() || newName.length() > 16) {
            player.sendMessage(cratesPlus.getPluginPrefix() + "§cInvalid crate name! Name must be 1-16 characters long.");
            return;
        }
        
        // Get the old crate name from the renaming map (if it exists)
        String oldName = cratesPlus.getSettingsHandler().getLastCrateEditing().get(playerUUID.toString());
        
        if (oldName == null) {
            player.sendMessage(cratesPlus.getPluginPrefix() + "§cNo crate rename operation in progress.");
            return;
        }
        
        // Check if new name already exists
        if (cratesPlus.getConfigHandler().getCrates().containsKey(newName.toLowerCase())) {
            player.sendMessage(cratesPlus.getPluginPrefix() + "§cA crate with that name already exists!");
            return;
        }
        
        // Perform the rename
        try {
            // Get the old crate
            plus.crates.Crates.Crate oldCrate = cratesPlus.getConfigHandler().getCrates().get(oldName.toLowerCase());
            if (oldCrate == null) {
                player.sendMessage(cratesPlus.getPluginPrefix() + "§cOriginal crate not found!");
                return;
            }
            
            // Remove old crate and add with new name
            cratesPlus.getConfigHandler().getCrates().remove(oldName.toLowerCase());
            cratesPlus.getConfigHandler().getCrates().put(newName.toLowerCase(), oldCrate);
            
            // Update crate name
            oldCrate.setName(newName);
            
            // Save config
            cratesPlus.saveConfig();
            
            player.sendMessage(cratesPlus.getPluginPrefix() + "§aSuccessfully renamed crate from " + "§e" + oldName + "§a" + " to " + "§e" + newName);
            
        } catch (Exception e) {
            player.sendMessage(cratesPlus.getPluginPrefix() + "§cError renaming crate: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Clean up
            cratesPlus.getSettingsHandler().getLastCrateEditing().remove(playerUUID.toString());
        }
    }
}
