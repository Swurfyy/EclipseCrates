package plus.crates.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class LinfootUtil {

    public static Enchantment getEnchantmentFromNiceName(String name) {
        Enchantment enchantment = null;
        try {
            enchantment = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(name.toLowerCase()));
        } catch (Exception ignored) {
        }

        if (enchantment != null)
            return enchantment;

        switch (name.toLowerCase()) {
            case "sharpness":
                enchantment = Enchantment.SHARPNESS;
                break;
            case "unbreaking":
                enchantment = Enchantment.UNBREAKING;
                break;
            case "efficiency":
                enchantment = Enchantment.EFFICIENCY;
                break;
            case "protection":
                enchantment = Enchantment.PROTECTION;
                break;
            case "power":
                enchantment = Enchantment.POWER;
                break;
            case "punch":
                enchantment = Enchantment.PUNCH;
                break;
            case "infinite":
                enchantment = Enchantment.INFINITY;
                break;
        }

        return enchantment;
    }

    public static ItemStack buildItemStack(ItemStack itemStack, String name, List<String> lore) {
        if (name == null && lore == null) {
            return itemStack;
        }
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (name != null) {
            itemMeta.displayName(LegacyComponentSerializer.legacySection().deserialize("§r" + name));
        }
        if (lore != null) {
            itemMeta.lore(lore.stream().map(line -> LegacyComponentSerializer.legacySection().deserialize(line)).toList());
        }
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public static void copyConfigSection(FileConfiguration config, String fromPath, String toPath) {
        Map<String, Object> vals = config.getConfigurationSection(fromPath).getValues(true);
        String toDot = toPath.equals("") ? "" : ".";
        for (String s : vals.keySet()) {
            Object val = vals.get(s);
            if (val instanceof List)
                val = new ArrayList<>((List<?>) val);
            config.set(toPath + toDot + s, val);
        }
    }

    // System.out.println(versionCompare("1.6", "1.8")); // -1 as 1.8 is newer
    // System.out.println(versionCompare("1.7", "1.8")); // -1 as 1.8 is newer
    // System.out.println(versionCompare("1.8", "1.8")); // 0 as same
    // System.out.println(versionCompare("1.9", "1.8")); // 1 as 1.9 is newer
    public static int versionCompare(String str1, String str2) {
        String[] vals1 = str1.split("\\.");
        String[] vals2 = str2.split("\\.");
        int i = 0;
        while (i < vals1.length && i < vals2.length && vals1[i].equals(vals2[i])) {
            i++;
        }
        if (i < vals1.length && i < vals2.length) {
            int diff = Integer.valueOf(vals1[i]).compareTo(Integer.valueOf(vals2[i]));
            return Integer.signum(diff);
        }
        return Integer.signum(vals1.length - vals2.length);
    }

}
