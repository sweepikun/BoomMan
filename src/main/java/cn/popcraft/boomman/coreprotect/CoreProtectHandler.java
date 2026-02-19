package cn.popcraft.boomman.coreprotect;

import cn.popcraft.boomman.BoomMan;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class CoreProtectHandler {

    private final BoomMan plugin;
    private CoreProtectAPI api;
    private final long startTime;

    public CoreProtectHandler(BoomMan plugin) {
        this.plugin = plugin;
        this.startTime = System.currentTimeMillis() / 1000;
        initializeAPI();
    }

    private void initializeAPI() {
        try {
            Object coreProtect = plugin.getServer().getPluginManager().getPlugin("CoreProtect");
            if (coreProtect != null && coreProtect instanceof CoreProtect) {
                CoreProtect cp = (CoreProtect) coreProtect;
                if (cp.isEnabled()) {
                    this.api = cp.getAPI();
                    plugin.getLogger().info("CoreProtect API 连接成功!");
                    plugin.getLogger().info("CoreProtect API 版本: " + api.APIVersion());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "初始化 CoreProtect API 时发生错误", e);
        }
    }

    public boolean isCoreProtectEnabled() {
        return api != null;
    }

    public CoreProtectAPI getApi() {
        return api;
    }

    public long getStartTime() {
        return startTime;
    }

    public CompletableFuture<Boolean> rollbackChunk(World world, int chunkX, int chunkZ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (api == null) {
                    return false;
                }

                Location center = new Location(world, chunkX * 16 + 8, 64, chunkZ * 16 + 8);
                
                List<String> restrictUsers = new ArrayList<>();
                List<String> excludeUsers = null;
                List<Object> restrictBlocks = null;
                List<Object> excludeBlocks = null;
                List<Integer> actionList = null;
                
                int radius = 8;
                
                int timeSeconds = (int) (System.currentTimeMillis() / 1000 - startTime);
                if (timeSeconds < 60) {
                    timeSeconds = 60;
                }

                List<String[]> result = api.performRollback(
                    timeSeconds,
                    restrictUsers,
                    excludeUsers,
                    restrictBlocks,
                    excludeBlocks,
                    actionList,
                    radius,
                    center
                );

                if (result != null && !result.isEmpty()) {
                    plugin.getLogger().info("区块重置成功: " + world.getName() + "," + chunkX + "," + chunkZ + ", 影响方块数: " + result.size());
                    return true;
                }
                return false;
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "执行区块重置时发生错误", e);
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> restoreChunk(World world, int chunkX, int chunkZ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (api == null) {
                    return false;
                }

                Location center = new Location(world, chunkX * 16 + 8, 64, chunkZ * 16 + 8);
                
                List<String> restrictUsers = new ArrayList<>();
                List<String> excludeUsers = null;
                List<Object> restrictBlocks = null;
                List<Object> excludeBlocks = null;
                List<Integer> actionList = null;
                
                int radius = 8;
                
                int timeSeconds = (int) (System.currentTimeMillis() / 1000 - startTime);

                List<String[]> result = api.performRestore(
                    timeSeconds,
                    restrictUsers,
                    excludeUsers,
                    restrictBlocks,
                    excludeBlocks,
                    actionList,
                    radius,
                    center
                );

                if (result != null && !result.isEmpty()) {
                    plugin.getLogger().info("区块恢复成功: " + world.getName() + "," + chunkX + "," + chunkZ + ", 影响方块数: " + result.size());
                    return true;
                }
                return false;
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "执行区块恢复时发生错误", e);
                return false;
            }
        });
    }

    public CompletableFuture<List<String[]>> lookupChunk(World world, int chunkX, int chunkZ, int timeSeconds) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (api == null) {
                    return new ArrayList<>();
                }

                Location center = new Location(world, chunkX * 16 + 8, 64, chunkZ * 16 + 8);
                
                List<String> restrictUsers = null;
                List<String> excludeUsers = null;
                List<Object> restrictBlocks = null;
                List<Object> excludeBlocks = null;
                List<Integer> actionList = null;
                
                int radius = 8;

                return api.performLookup(
                    timeSeconds,
                    restrictUsers,
                    excludeUsers,
                    restrictBlocks,
                    excludeBlocks,
                    actionList,
                    radius,
                    center
                );
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "查询区块历史时发生错误", e);
                return new ArrayList<>();
            }
        });
    }

    public void teleportPlayersInChunk(World world, int chunkX, int chunkZ) {
        int minX = chunkX * 16;
        int maxX = minX + 15;
        int minZ = chunkZ * 16;
        int maxZ = minZ + 15;

        Location spawn = world.getSpawnLocation();

        for (Player player : world.getPlayers()) {
            Location loc = player.getLocation();
            if (loc.getBlockX() >= minX && loc.getBlockX() <= maxX &&
                loc.getBlockZ() >= minZ && loc.getBlockZ() <= maxZ) {
                player.teleport(spawn);
                player.sendMessage("§c[BoomMan] 你所在的区块已被重置，已传送到出生点!");
            }
        }
    }
}
