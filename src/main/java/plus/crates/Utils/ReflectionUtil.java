package plus.crates.Utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ReflectionUtil {

    public static final String NMS_PATH = getNMSPackageName();
    public static final String CB_PATH = getCBPackageName();

    public static String getNMSPackageName() {
        try {
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            String[] parts = packageName.split("\\.");
            if (parts.length >= 4) {
                return "net.minecraft.server." + parts[3];
            } else {
                // Fallback for newer versions - try to get from Bukkit class
                try {
                    Class<?> craftServerClass = Bukkit.getServer().getClass();
                    String serverClassName = craftServerClass.getName();
                    // Extract version from class name like org.bukkit.craftbukkit.v1_21_R1.CraftServer
                    if (serverClassName.contains("craftbukkit.v")) {
                        String version = serverClassName.substring(serverClassName.indexOf("craftbukkit.v") + 13);
                        version = version.substring(0, version.indexOf("."));
                        return "net.minecraft.server." + version;
                    }
                } catch (Exception e2) {
                    // Ignore
                }
                return "net.minecraft.server";
            }
        } catch (Exception e) {
            // Fallback for any parsing errors
            return "net.minecraft.server";
        }
    }

    public static String getCBPackageName() {
        try {
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            String[] parts = packageName.split("\\.");
            if (parts.length >= 4) {
                return "org.bukkit.craftbukkit." + parts[3];
            } else {
                // Fallback for newer versions
                return "org.bukkit.craftbukkit";
            }
        } catch (Exception e) {
            // Fallback for any parsing errors
            return "org.bukkit.craftbukkit";
        }
    }

    /**
     * Class stuff
     */

    public static Class getClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
        }
        return null;
    }

    public static Class getNMSClass(String className) {
        return getClass(NMS_PATH + "." + className);
    }

    public static Class getCBClass(String className) {
        return getClass(CB_PATH + "." + className);
    }

    /**
     * Field stuff
     */

    public static Field getField(Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);

            if (!field.isAccessible()) {
                field.setAccessible(true);
            }

            return field;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <T> T getField(Class<?> clazz, String fieldName, Object instance) {
        try {
            return (T) getField(clazz, fieldName).get(instance);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void setField(Class<?> clazz, String fieldName, Object instance, Object value) {
        try {
            getField(clazz, fieldName).set(instance, value);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method stuff
     */

    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... params) {
        try {
            Method method = clazz.getDeclaredMethod(methodName, params);

            if (!method.isAccessible()) {
                method.setAccessible(true);
            }

            return method;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <T> T invokeMethod(Method method, Object instance, Object... args) {
        try {
            return (T) method.invoke(instance, args);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Constructor getConstructor(Class<?> clazz, Class<?>... params) {
        try {
            Constructor constructor = clazz.getConstructor(params);

            if (!constructor.isAccessible()) {
                constructor.setAccessible(true);
            }

            return constructor;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <T> T invokeConstructor(Constructor constructor, Object... args) {
        try {
            return (T) constructor.newInstance(args);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object getBlockPosition(Player player) {
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            // Try different possible class names for BlockPosition
            Class<?> blockPositionClass = null;
            String[] possibleNames = {"BlockPosition", "BlockPos", "core.BlockPosition", "core.BlockPos"};
            
            for (String className : possibleNames) {
                blockPositionClass = getNMSClass(className);
                if (blockPositionClass != null) break;
            }
            
            if (blockPositionClass != null) {
                // Try different constructor signatures
                try {
                    Constructor constructor = blockPositionClass.getConstructor(ReflectionUtil.getNMSClass("Entity"));
                    return constructor.newInstance(handle);
                } catch (Exception e1) {
                    try {
                        // Try with int parameters (x, y, z)
                        Constructor constructor = blockPositionClass.getConstructor(int.class, int.class, int.class);
                        return constructor.newInstance(
                            player.getLocation().getBlockX(),
                            player.getLocation().getBlockY(),
                            player.getLocation().getBlockZ()
                        );
                    } catch (Exception e2) {
                        // Fallback: create from location
                        Constructor constructor = blockPositionClass.getConstructor(double.class, double.class, double.class);
                        return constructor.newInstance(
                            player.getLocation().getX(),
                            player.getLocation().getY(),
                            player.getLocation().getZ()
                        );
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void sendPacket(Player player, Object packet) {
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            
            // Try different possible field names for player connection
            Object playerConnection = null;
            String[] possibleFields = {"playerConnection", "connection", "b"};
            
            for (String fieldName : possibleFields) {
                try {
                    Field field = handle.getClass().getField(fieldName);
                    playerConnection = field.get(handle);
                    if (playerConnection != null) break;
                } catch (Exception e) {
                    // Try next field name
                }
            }
            
            if (playerConnection != null) {
                // Try different method names for sending packets
                String[] possibleMethods = {"sendPacket", "send", "a"};
                Class<?> packetClass = ReflectionUtil.getNMSClass("Packet");
                
                for (String methodName : possibleMethods) {
                    try {
                        Method method = playerConnection.getClass().getMethod(methodName, packetClass);
                        method.invoke(playerConnection, packet);
                        return; // Success
                    } catch (Exception e) {
                        // Try next method name
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}