package ru.flashcpu.minecraft;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.inventory.ItemStack;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class FlashCpuEngine extends JavaPlugin {

    private String flashPath;
    private File tickIn, tickOut;

    @Override
    public void onEnable() {
        // Автоматически создает папку плагина и файл config.yml при первом запуске
        this.saveDefaultConfig();
        
        // Считываем путь к флешке из конфигурации сервера. Если там пусто, используем "F:/"
        flashPath = getConfig().getString("flash-path", "F:/");
        
        File flashFolder = new File(flashPath);
        if (!flashFolder.exists()) { 
            getLogger().severe("!!! APPARATNAYA PANIKA: Flashka po puti '" + flashPath + "' ne naydena! Proverte config.yml");
            Bukkit.shutdown(); 
            return; 
        }

        tickIn = new File(flashFolder, "tick_in.dat");
        tickOut = new File(flashFolder, "tick_out.dat");
        
        getLogger().info("Kross-versionniy Flash-CPU uspeshno svyazan po puti: " + flashPath);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!flashFolder.exists()) { Bukkit.shutdown(); this.cancel(); return; }
                try {
                    StringBuilder memoryDump = new StringBuilder();
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        Location loc = player.getLocation();
                        ItemStack handItem = player.getInventory().getItemInMainHand();
                        String itemName = (handItem != null && handItem.getType() != null) ? handItem.getType().name() : "AIR";
                        memoryDump.append(player.getName()).append(":").append(loc.getX()).append(",").append(loc.getY()).append(",").append(loc.getZ()).append(":").append(player.getHealth()).append(":").append(itemName).append("\n");
                    }
                    if (memoryDump.length() > 0) {
                        FileWriter writer = new FileWriter(tickIn); writer.write(memoryDump.toString()); writer.close();
                        int timeout = 0;
                        while (!tickOut.exists()) { Thread.sleep(2); timeout++; if (timeout > 25) return; }
                        List<String> instructions = Files.readAllLines(Paths.get(tickOut.getAbsolutePath())); tickOut.delete();
                        for (String line : instructions) {
                            String[] parts = line.split(":"); if (parts.length < 2) continue;
                            String action = parts[0], targetPlayer = parts[1]; Player player = Bukkit.getPlayer(targetPlayer);
                            if (player != null && player.isOnline()) {
                                if (action.equals("TELEPORT")) {
                                    String[] coords = parts[2].split(",");
                                    player.teleport(new Location(player.getWorld(), Double.parseDouble(coords[0]), Double.parseDouble(coords[1]), Double.parseDouble(coords[2])));
                                }
                                if (action.equals("DAMAGE")) player.damage(Double.parseDouble(parts[2]));
                                if (action.equals("CLEAR_ITEM")) { Material mat = Material.matchMaterial(parts[2]); if (mat != null) player.getInventory().remove(mat); }
                                if (action.equals("MESSAGE")) player.sendMessage(parts[2].replace("&", "§"));
                            }
                        }
                    }
                } catch (Exception e) {}
            }
        }.runTaskTimer(this, 0L, 1L);
    }
}
