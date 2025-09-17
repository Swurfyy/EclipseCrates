package plus.crates.Handlers;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import plus.crates.Crates.Crate;
import plus.crates.Crates.Winning;
import plus.crates.CratesPlus;
import plus.crates.Events.PlayerInputEvent;
import plus.crates.Utils.GUI;
import plus.crates.Utils.LegacyMaterial;
import plus.crates.Utils.ReflectionUtil;
import plus.crates.Utils.SignInputHandler;

import java.lang.reflect.Constructor;
import java.util.*;

public class SettingsHandler implements Listener {
    private HashMap<UUID, String> renaming = new HashMap<>();
    private CratesPlus cratesPlus;
    private GUI settings;
    private GUI crates;
    private HashMap<String, String> lastCrateEditing = new HashMap<>();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public SettingsHandler(CratesPlus cratesPlus) {
        this.cratesPlus = cratesPlus;
        Bukkit.getPluginManager().registerEvents(this, cratesPlus);
        setupSettingsInventory();
        setupCratesInventory();
    }

    // Helper method for text formatting with MiniMessage support
    private String formatText(String text) {
        if (text == null) return "";
        // Convert legacy color codes to MiniMessage format
        return text.replace("&", "<")
                  .replace("§", "<")
                  .replace("<0", "<black>")
                  .replace("<1", "<dark_blue>")
                  .replace("<2", "<dark_green>")
                  .replace("<3", "<dark_aqua>")
                  .replace("<4", "<dark_red>")
                  .replace("<5", "<dark_purple>")
                  .replace("<6", "<gold>")
                  .replace("<7", "<gray>")
                  .replace("<8", "<dark_gray>")
                  .replace("<9", "<blue>")
                  .replace("<a", "<green>")
                  .replace("<b", "<aqua>")
                  .replace("<c", "<red>")
                  .replace("<d", "<light_purple>")
                  .replace("<e", "<yellow>")
                  .replace("<f", "<white>")
                  .replace("<k", "<obfuscated>")
                  .replace("<l", "<bold>")
                  .replace("<m", "<strikethrough>")
                  .replace("<n", "<underline>")
                  .replace("<o", "<italic>")
                  .replace("<r", "<reset>");
    }

    public void setupSettingsInventory() {
        settings = new GUI("CratesPlus Settings");

        ItemStack itemStack;
        ItemMeta itemMeta;
        List<String> lore;

        itemStack = new ItemStack(Material.CHEST);
        itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(miniMessage.deserialize("<green>Edit Crates"));
        lore = new ArrayList<>();
        lore.add("");
        itemMeta.lore(lore.stream().map(line -> miniMessage.deserialize(line)).toList());
        itemStack.setItemMeta(itemMeta);
        settings.setItem(1, itemStack, new GUI.ClickHandler() {
            @Override
            public void doClick(Player player, GUI gui) {
                GUI.ignoreClosing.add(player.getUniqueId());
                openCrates(player);
            }
        });

        Material material;
        try {
            material = Material.valueOf("BARRIER");
        } catch (Exception i) {
            material = LegacyMaterial.REDSTONE_TORCH_ON.getMaterial();
        }

        itemStack = new ItemStack(material);
        itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(miniMessage.deserialize("<green>Reload Config"));
        lore = new ArrayList<>();
        lore.add("");
        itemMeta.lore(lore.stream().map(line -> miniMessage.deserialize(line)).toList());
        itemStack.setItemMeta(itemMeta);
        settings.setItem(5, itemStack, new GUI.ClickHandler() {
            @Override
            public void doClick(Player player, GUI gui) {
                player.closeInventory();
                cratesPlus.reloadConfig();
                player.sendMessage("§aReloaded config");
            }
        });
    }

    public void setupCratesInventory() {
        crates = new GUI("Crates");

        ItemStack itemStack;
        ItemMeta itemMeta;

        for (Map.Entry<String, Crate> entry : cratesPlus.getConfigHandler().getCrates().entrySet()) {
            Crate crate = entry.getValue();

            itemStack = new ItemStack(Material.CHEST);
            itemMeta = itemStack.getItemMeta();
            itemMeta.displayName(miniMessage.deserialize(formatText(crate.getName(true))));
            itemStack.setItemMeta(itemMeta);
            final String crateName = crate.getName();
            crates.addItem(itemStack, new GUI.ClickHandler() {
                @Override
                public void doClick(Player player, GUI gui) {
                    GUI.ignoreClosing.add(player.getUniqueId());
                    openCrate(player, crateName);
                }
            });
        }
    }

    public void openSettings(final Player player) {
        Bukkit.getScheduler().runTaskLater(cratesPlus, () -> settings.open(player), 1L);
    }

    public void openCrates(final Player player) {
        Bukkit.getScheduler().runTaskLater(cratesPlus, () -> crates.open(player), 1L);
    }

    public void openCrateWinnings(final Player player, String crateName) {
        Crate crate = cratesPlus.getConfigHandler().getCrates().get(crateName.toLowerCase());
        if (crate == null) {
            player.sendMessage("§cUnable to find " + crateName + " crate");
            return;
        }

        if (crate.containsCommandItem()) {
            player.sendMessage("§cYou can not currently edit a crate in the GUI which has command items");
            player.closeInventory();
            return;
        }

        final GUI gui = new GUI("Edit " + crate.getName(false) + " Crate Winnings");

        for (Winning winning : crate.getWinnings()) {
            gui.addItem(winning.getWinningItemStack());
        }

        Bukkit.getScheduler().runTaskLater(cratesPlus, () -> gui.open(player), 1L);

    }

    public void openCrate(final Player player, final String crateName) {
        Crate crate = cratesPlus.getConfigHandler().getCrates().get(crateName.toLowerCase());
        if (crate == null) {
            return; // TODO Error handling here
        }

        final GUI gui = new GUI("Edit " + crate.getName(false) + " Crate");

        ItemStack itemStack;
        ItemMeta itemMeta;
        List<String> lore;


        // Rename Crate

        itemStack = new ItemStack(Material.NAME_TAG);
        itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(miniMessage.deserialize("<green>Rename Crate"));
        lore = new ArrayList<>();
        lore.add("");
        itemMeta.lore(lore.stream().map(line -> miniMessage.deserialize(line)).toList());
        itemStack.setItemMeta(itemMeta);
        gui.setItem(0, itemStack, new GUI.ClickHandler() {
            @Override
            public void doClick(Player player, GUI gui) {
                player.closeInventory();
                renaming.put(player.getUniqueId(), crateName);
                try {
                    //Send fake sign cause 1.13
                    player.sendBlockChange(player.getLocation(), Material.valueOf("SIGN").createBlockData());

                    Constructor signConstructor = ReflectionUtil.getNMSClass("PacketPlayOutOpenSignEditor").getConstructor(ReflectionUtil.getNMSClass("BlockPosition"));
                    Object packet = signConstructor.newInstance(ReflectionUtil.getBlockPosition(player));
                    SignInputHandler.injectNetty(player);
                    ReflectionUtil.sendPacket(player, packet);

                    player.sendBlockChange(player.getLocation(), player.getLocation().getBlock().getBlockData());
                } catch (Exception e) {
                    player.sendMessage(cratesPlus.getPluginPrefix() + "§cPlease use /crate rename <old> <new>");
                    renaming.remove(player.getUniqueId());
                }
            }
        });


        // Edit Crate Winnings

        itemStack = new ItemStack(Material.DIAMOND);
        itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(miniMessage.deserialize("<red>Edit Crate Winnings"));
        lore = new ArrayList<>();
        lore.add("");
        itemMeta.lore(lore.stream().map(line -> miniMessage.deserialize(line)).toList());
        itemStack.setItemMeta(itemMeta);
        gui.setItem(2, itemStack, new GUI.ClickHandler() {
            @Override
            public void doClick(Player player, GUI gui) {
                player.sendMessage("§cThis feature is currently disabled!");
//                GUI.ignoreClosing.add(player.getUniqueId());
//                openCrateWinnings(player, crateName);
            }
        });


        // Edit Crate Color

        itemStack = new ItemStack(LegacyMaterial.WOOL.getMaterial(), 1);
        itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(miniMessage.deserialize("<green>Edit Crate Color"));
        lore = new ArrayList<>();
        lore.add("");
        itemMeta.lore(lore.stream().map(line -> miniMessage.deserialize(line)).toList());
        itemStack.setItemMeta(itemMeta);
        gui.setItem(4, itemStack, new GUI.ClickHandler() {
            @Override
            public void doClick(Player player, GUI gui) {
                GUI.ignoreClosing.add(player.getUniqueId());
                openCrateColor(player, crate);
            }
        });


        // Delete Crate

        Material material;

        try {
            material = Material.valueOf("BARRIER");
        } catch (Exception i) {
            material = LegacyMaterial.REDSTONE_TORCH_ON.getMaterial();
        }

        itemStack = new ItemStack(material);
        itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(miniMessage.deserialize("<green>Delete Crate"));
        lore = new ArrayList<>();
        lore.add("");
        itemMeta.lore(lore.stream().map(line -> miniMessage.deserialize(line)).toList());
        itemStack.setItemMeta(itemMeta);
        gui.setItem(6, itemStack, new GUI.ClickHandler() {
            @Override
            public void doClick(Player player, GUI gui) {
                GUI.ignoreClosing.add(player.getUniqueId());
                confirmDelete(player, crate);
            }
        });

        Bukkit.getScheduler().runTaskLater(cratesPlus, () -> gui.open(player), 1L);

    }

    private void openCrateColor(final Player player, final Crate crate) {
        GUI gui = new GUI("Edit Crate Color");

        ItemStack aqua = new ItemStack(LegacyMaterial.WOOL.getMaterial(), 1);
        ItemMeta aquaMeta = aqua.getItemMeta();
        aquaMeta.displayName(miniMessage.deserialize("<aqua>Aqua"));
        aqua.setItemMeta(aquaMeta);
        gui.addItem(aqua, getColorClickHandler(crate, "§b"));

        ItemStack black = new ItemStack(LegacyMaterial.WOOL.getMaterial(), 1);
        ItemMeta blackMeta = black.getItemMeta();
        blackMeta.displayName(miniMessage.deserialize("<black>Black"));
        black.setItemMeta(blackMeta);
        gui.addItem(black, getColorClickHandler(crate, "§0"));

        ItemStack blue = new ItemStack(LegacyMaterial.WOOL.getMaterial(), 1);
        ItemMeta blueMeta = blue.getItemMeta();
        blueMeta.displayName(miniMessage.deserialize("<blue>Blue"));
        blue.setItemMeta(blueMeta);
        gui.addItem(blue, getColorClickHandler(crate, "§9"));

        ItemStack darkAqua = new ItemStack(LegacyMaterial.WOOL.getMaterial(), 1);
        ItemMeta darkAquaMeta = darkAqua.getItemMeta();
        darkAquaMeta.displayName(miniMessage.deserialize("<dark_aqua>Dark Aqua"));
        darkAqua.setItemMeta(darkAquaMeta);
        gui.addItem(darkAqua, getColorClickHandler(crate, "§3"));

        ItemStack darkBlue = new ItemStack(LegacyMaterial.WOOL.getMaterial(), 1);
        ItemMeta darkBlueMeta = darkBlue.getItemMeta();
        darkBlueMeta.displayName(miniMessage.deserialize("<dark_blue>Dark Blue"));
        darkBlue.setItemMeta(darkBlueMeta);
        gui.addItem(darkBlue, getColorClickHandler(crate, "§1"));

        ItemStack darkGray = new ItemStack(LegacyMaterial.WOOL.getMaterial(), 1);
        ItemMeta darkGrayMeta = darkGray.getItemMeta();
        darkGrayMeta.displayName(miniMessage.deserialize("<dark_gray>Dark Gray"));
        darkGray.setItemMeta(darkGrayMeta);
        gui.addItem(darkGray, getColorClickHandler(crate, "§8"));

        ItemStack darkGreen = new ItemStack(LegacyMaterial.WOOL.getMaterial(), 1);
        ItemMeta darkGreenMeta = darkGreen.getItemMeta();
        darkGreenMeta.displayName(miniMessage.deserialize("<dark_green>Dark Green"));
        darkGreen.setItemMeta(darkGreenMeta);
        gui.addItem(darkGreen, getColorClickHandler(crate, "§2"));

        ItemStack darkPurple = new ItemStack(LegacyMaterial.WOOL.getMaterial(), 1);
        ItemMeta darkPurpleMeta = darkPurple.getItemMeta();
        darkPurpleMeta.displayName(miniMessage.deserialize("<dark_purple>Dark Purple"));
        darkPurple.setItemMeta(darkPurpleMeta);
        gui.addItem(darkPurple, getColorClickHandler(crate, "§5"));

        ItemStack darkRed = new ItemStack(LegacyMaterial.WOOL.getMaterial(), 1);
        ItemMeta darkRedMeta = darkRed.getItemMeta();
        darkRedMeta.displayName(miniMessage.deserialize("<dark_red>Dark Red"));
        darkRed.setItemMeta(darkRedMeta);
        gui.addItem(darkRed, getColorClickHandler(crate, "§4"));

        ItemStack gold = new ItemStack(LegacyMaterial.WOOL.getMaterial(), 1);
        ItemMeta goldMeta = gold.getItemMeta();
        goldMeta.displayName(miniMessage.deserialize("<gold>Gold"));
        gold.setItemMeta(goldMeta);
        gui.addItem(gold, getColorClickHandler(crate, "§6"));

        ItemStack gray = new ItemStack(LegacyMaterial.WOOL.getMaterial(), 1);
        ItemMeta grayMeta = gray.getItemMeta();
        grayMeta.displayName(miniMessage.deserialize("<gray>Gray"));
        gray.setItemMeta(grayMeta);
        gui.addItem(gray, getColorClickHandler(crate, "§7"));

        ItemStack green = new ItemStack(LegacyMaterial.WOOL.getMaterial(), 1);
        ItemMeta greenMeta = green.getItemMeta();
        greenMeta.displayName(miniMessage.deserialize("<green>Green"));
        green.setItemMeta(greenMeta);
        gui.addItem(green, getColorClickHandler(crate, "§a"));

        ItemStack lightPurple = new ItemStack(LegacyMaterial.WOOL.getMaterial(), 1);
        ItemMeta lightPurpleMeta = lightPurple.getItemMeta();
        lightPurpleMeta.displayName(miniMessage.deserialize("<light_purple>Light Purple"));
        lightPurple.setItemMeta(lightPurpleMeta);
        gui.addItem(lightPurple, getColorClickHandler(crate, "§d"));

        ItemStack red = new ItemStack(LegacyMaterial.WOOL.getMaterial(), 1);
        ItemMeta redMeta = red.getItemMeta();
        redMeta.displayName(miniMessage.deserialize("<red>Red"));
        red.setItemMeta(redMeta);
        gui.addItem(red, getColorClickHandler(crate, "§c"));

        ItemStack white = new ItemStack(LegacyMaterial.WOOL.getMaterial(), 1);
        ItemMeta whiteMeta = white.getItemMeta();
        whiteMeta.displayName(miniMessage.deserialize("<white>White"));
        white.setItemMeta(whiteMeta);
        gui.addItem(white, getColorClickHandler(crate, "§f"));

        ItemStack yellow = new ItemStack(LegacyMaterial.WOOL.getMaterial(), 1);
        ItemMeta yellowMeta = yellow.getItemMeta();
        yellowMeta.displayName(miniMessage.deserialize("<yellow>Yellow"));
        yellow.setItemMeta(yellowMeta);
        gui.addItem(yellow, getColorClickHandler(crate, "§e"));

        gui.open(player);
    }

    private GUI.ClickHandler getColorClickHandler(Crate crate, String colorCode) {
        return new GUI.ClickHandler() {
            @Override
            public void doClick(Player player, GUI gui) {
                GUI.ignoreClosing.add(player.getUniqueId());
                // Convert color code to ChatColor for backward compatibility
                try {
                    org.bukkit.ChatColor color = org.bukkit.ChatColor.valueOf(colorCode.substring(1).toUpperCase());
                    if (color != null) {
                        crate.setColor(color);
                        player.sendMessage(color.name());
                    }
                } catch (Exception e) {
                    // Fallback for unknown colors
                    player.sendMessage("Color applied");
                }
                openCrate(player, crate.getName());
            }
        };
    }

    private void confirmDelete(final Player player, final Crate crate) {
        final GUI gui = new GUI("Confirm Delete of \"" + crate.getName(false) + "\"");

        ItemStack crateItem = new ItemStack(crate.getBlock(), 1);
        ItemMeta crateMeta = crateItem.getItemMeta();
        crateMeta.displayName(miniMessage.deserialize(formatText(crate.getName())));
        crateItem.setItemMeta(crateMeta);
        gui.setItem(3, crateItem);

        ItemStack cancel = new ItemStack(LegacyMaterial.WOOL.getMaterial(), 1);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.displayName(miniMessage.deserialize("<red>Cancel"));
        cancel.setItemMeta(cancelMeta);
        gui.setItem(16, cancel, new GUI.ClickHandler() {
            @Override
            public void doClick(Player player, GUI gui) {
                GUI.ignoreClosing.add(player.getUniqueId());
                openCrate(player, crate.getName(false));
            }
        });

        ItemStack confirm = new ItemStack(LegacyMaterial.WOOL.getMaterial(), 1);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.displayName(miniMessage.deserialize("<green>Confirm"));
        confirm.setItemMeta(confirmMeta);
        gui.setItem(18, confirm, new GUI.ClickHandler() {
            @Override
            public void doClick(Player player, GUI gui) {
                player.closeInventory();
                player.sendMessage("WILL DELETE");
            }
        });

        gui.open(player);
    }

    public HashMap<String, String> getLastCrateEditing() {
        return lastCrateEditing;
    }

    @EventHandler
    public void onPlayerInput(final PlayerInputEvent event) {
        if (renaming.containsKey(event.getPlayer().getUniqueId())) {
            String name = renaming.get(event.getPlayer().getUniqueId());
            renaming.remove(event.getPlayer().getUniqueId());
            String newName = "";
            for (String line : event.getLines()) {
                newName += line;
            }
            if (!name.isEmpty() && !newName.isEmpty())
                Bukkit.dispatchCommand(event.getPlayer(), "crate rename " + name + " " + newName);
            cratesPlus.getSettingsHandler().openCrate(event.getPlayer(), newName);
        } else if (cratesPlus.isCreating(event.getPlayer().getUniqueId())) {
            cratesPlus.removeCreating(event.getPlayer().getUniqueId());
            String name = "";
            for (String line : event.getLines()) {
                name += line;
            }
            if (!name.isEmpty()) {
                final String finalName = name;
                Bukkit.getScheduler().runTask(cratesPlus, () -> Bukkit.dispatchCommand(event.getPlayer(), "crate create " + finalName));
            }
        }
    }

}
