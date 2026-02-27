package cn.popcraft.boomman.monitor;

import cn.popcraft.boomman.BoomMan;
import cn.popcraft.boomman.config.ConfigManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RedstoneMonitor implements Listener {

    private final BoomMan plugin;
    private final ConfigManager config;
    private final Map<String, AtomicInteger> redstoneActivity = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> hopperActivity = new ConcurrentHashMap<>();

    public RedstoneMonitor(BoomMan plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockRedstoneChange(BlockRedstoneEvent event) {
        if (!config.isRedstoneMonitorEnabled()) {
            return;
        }

        Block block = event.getBlock();
        String key = getBlockKey(block);

        int oldCurrent = event.getOldCurrent();
        int newCurrent = event.getNewCurrent();

        if (newCurrent > oldCurrent && newCurrent > 0) {
            redstoneActivity.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (!config.isRedstoneMonitorEnabled()) {
            return;
        }

        if (event.getInitiator() instanceof InventoryHolder) {
            InventoryHolder holder = (InventoryHolder) event.getInitiator();
            if (holder instanceof org.bukkit.block.BlockState) {
                org.bukkit.block.BlockState blockState = (org.bukkit.block.BlockState) holder;
                Block block = blockState.getBlock();
                String key = getBlockKey(block);

                hopperActivity.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
            }
        }
    }

    private String getBlockKey(Block block) {
        return block.getWorld().getName() + ":" + block.getX() + "," + block.getY() + "," + block.getZ();
    }

    public void checkAndLog() {
        if (!config.isRedstoneMonitorEnabled()) {
            return;
        }

        int threshold = config.getRedstoneThreshold();
        int checkInterval = config.getRedstoneCheckInterval();

        for (Map.Entry<String, AtomicInteger> entry : redstoneActivity.entrySet()) {
            int count = entry.getValue().getAndSet(0);
            int adjustedThreshold = threshold * checkInterval;
            
            if (count > adjustedThreshold) {
                String[] parts = entry.getKey().split(":");
                if (parts.length == 2) {
                    String world = parts[0];
                    String coords = parts[1];
                    plugin.getLogger().warning("§c[红石警报] §e" + world + "," + coords + 
                        " §7每秒红石活动: " + String.format("%.1f", (double) count / checkInterval) + 
                        " (阈值: " + threshold + "/s)");
                }
            }
        }

        for (Map.Entry<String, AtomicInteger> entry : hopperActivity.entrySet()) {
            int count = entry.getValue().getAndSet(0);
            int adjustedThreshold = threshold * checkInterval;
            
            if (count > adjustedThreshold) {
                String[] parts = entry.getKey().split(":");
                if (parts.length == 2) {
                    String world = parts[0];
                    String coords = parts[1];
                    plugin.getLogger().warning("§c[漏斗警报] §e" + world + "," + coords + 
                        " §7每秒漏斗活动: " + String.format("%.1f", (double) count / checkInterval) + 
                        " (阈值: " + threshold + "/s)");
                }
            }
        }
    }

    public void clear() {
        redstoneActivity.clear();
        hopperActivity.clear();
    }
}
