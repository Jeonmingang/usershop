package com.minkang.ultimate.usershop.util;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.util.Base64;

public class ItemSerializer {

    public static String serializeToBase64(ItemStack item) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(item);
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }

    public static ItemStack deserializeFromBase64(String data) {
        if (data == null || data.isEmpty()) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            Object obj = dataInput.readObject();
            dataInput.close();
            if (obj instanceof ItemStack) {
                return (ItemStack) obj;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    // Spigot's BukkitObject streams (avoid illegal imports)
    private static class BukkitObjectOutputStream extends ObjectOutputStream {
        public BukkitObjectOutputStream(OutputStream out) throws IOException {
            super(out);
            enableReplaceObject(true);
        }
    }
    private static class BukkitObjectInputStream extends ObjectInputStream {
        public BukkitObjectInputStream(InputStream in) throws IOException {
            super(in);
            enableResolveObject(true);
        }
    }
}
