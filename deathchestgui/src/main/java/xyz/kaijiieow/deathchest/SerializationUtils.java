package xyz.kaijiieow.deathchest;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class SerializationUtils {

    /**
     * แปลง ItemStack[] เป็น String (Base64)
     */
    public static String itemStackArrayToBase64(ItemStack[] items) throws IllegalStateException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            
            // เขียนขนาดของ array ลงไปก่อน
            dataOutput.writeInt(items.length);
            
            // วนลูปเขียนของทีละชิ้น
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("ไม่สามารถแปลง items to base64 ได้", e);
        }
    }

    /**
     * แปลง String (Base64) กลับเป็น ItemStack[]
     */
    public static ItemStack[] itemStackArrayFromBase64(String data) throws IllegalStateException {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            
            // อ่านขนาดของ array
            int size = dataInput.readInt();
            ItemStack[] items = new ItemStack[size];
            
            // วนลูปอ่านของทีละชิ้น
            for (int i = 0; i < size; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }
            
            dataInput.close();
            return items;
        } catch (Exception e) {
            throw new IllegalStateException("ไม่สามารถแปลง base64 to items ได้", e);
        }
    }
}