package cn.popcraft.boomman.monitor;

import cn.popcraft.boomman.BoomMan;
import cn.popcraft.boomman.config.ConfigManager;
import cn.popcraft.boomman.coreprotect.CoreProtectHandler;
import cn.popcraft.boomman.recorder.ResetRecorder;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkMonitor {

    private final BoomMan plugin;
    private final ConfigManager config;
    private final CoreProtectHandler coreProtectHandler;
    private final ResetRecorder resetRecorder;
    private final ConcurrentHashMap<String, Long> cooldownMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> warningMap = new ConcurrentHashMap<>();

    public ChunkMonitor(BoomMan plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.coreProtectHandler = plugin.getCoreProtectHandler();
        this.resetRecorder = plugin.getResetRecorder();
    }

    public List<ChunkData> checkAllChunks() {
        List<ChunkData> laggingChunks = new ArrayList<>();
        
        for (World world : plugin.getServer().getWorlds()) {
            if (config.getExcludeWorlds().contains(world.getName())) {
                continue;
            }

            Chunk[] chunks = world.getLoadedChunks();
            for (Chunk chunk : chunks) {
                ChunkData data = analyzeChunk(chunk);
                if (data != null && isLagging(data)) {
                    String key = data.getKey();
                    if (!isOnCooldown(key) && !hasPendingWarning(key)) {
                        laggingChunks.add(data);
                    }
                }
            }
        }
        
        return laggingChunks;
    }

    private ChunkData analyzeChunk(Chunk chunk) {
        try {
            int entityCount = chunk.getEntities().length;
            int tileEntityCount = chunk.getTileEntities().length;

            int tickTime = getChunkTickTime(chunk);

            int playerCount = chunk.getWorld().getPlayers().size();
            int expectedTickTime = playerCount * config.getTickMsPerPlayer();
            if (tickTime < expectedTickTime) {
                expectedTickTime = tickTime;
            }

            return new ChunkData(
                chunk.getWorld().getName(),
                chunk.getX(),
                chunk.getZ(),
                tickTime,
                entityCount,
                tileEntityCount,
                expectedTickTime
            );
        } catch (Exception e) {
            plugin.getLogger().warning("分析区块时出错: " + chunk.getWorld().getName() + "," + chunk.getX() + "," + chunk.getZ());
            return null;
        }
    }

    private int getChunkTickTime(Chunk chunk) {
        try {
            World world = chunk.getWorld();
            Class<?> craftWorldClass = world.getClass();
            Object handle = craftWorldClass.getMethod("getHandle").invoke(world);
            Object chunkProvider = handle.getClass().getMethod("getChunkProvider").invoke(handle);
            Object nmsChunk = chunkProvider.getClass().getMethod("getChunkAt", int.class, int.class, boolean.class)
                .invoke(chunkProvider, chunk.getX(), chunk.getZ(), false);
            if (nmsChunk != null) {
                Object result = nmsChunk.getClass().getMethod("e").invoke(nmsChunk);
                if (result instanceof Integer) {
                    return (Integer) result;
                }
            }
        } catch (Exception e) {
        }
        return 0;
    }

    private boolean isLagging(ChunkData data) {
        if (data.getTickTime() > config.getTickTimeThreshold()) {
            return true;
        }
        if (data.getEntityCount() > config.getEntityCountThreshold()) {
            return true;
        }
        if (data.getTileEntityCount() > config.getTileEntityCountThreshold()) {
            return true;
        }
        return false;
    }

    private boolean isOnCooldown(String key) {
        Long lastReset = cooldownMap.get(key);
        if (lastReset == null) {
            return false;
        }
        long cooldownMillis = config.getCooldownSeconds() * 1000L;
        return System.currentTimeMillis() - lastReset < cooldownMillis;
    }

    private boolean hasPendingWarning(String key) {
        Long warningTime = warningMap.get(key);
        if (warningTime == null) {
            return false;
        }
        long warningMillis = config.getBeforeResetWarning() * 1000L;
        return System.currentTimeMillis() - warningTime < warningMillis;
    }

    public void handleLaggingChunk(ChunkData data) {
        String key = data.getKey();
        
        if (!warningMap.containsKey(key)) {
            sendWarning(data);
            warningMap.put(key, System.currentTimeMillis());
            plugin.getLogger().info("区块警告: " + data.getWorldName() + "," + data.getChunkX() + "," + data.getChunkZ());
        } else {
            Long warningTime = warningMap.get(key);
            long elapsed = System.currentTimeMillis() - warningTime;
            long warningMillis = config.getBeforeResetWarning() * 1000L;
            
            if (elapsed >= warningMillis) {
                warningMap.remove(key);
                resetChunk(data);
            }
        }
    }

    private void sendWarning(ChunkData data) {
        World world = plugin.getServer().getWorld(data.getWorldName());
        if (world == null) return;

        int minX = data.getChunkX() * 16;
        int maxX = minX + 15;
        int minZ = data.getChunkZ() * 16;
        int maxZ = minZ + 15;

        String reason = buildReasonShort(data);
        String warningMsg = "§c[BoomMan] 警告! 你所在的区块因 " + reason + " 将在 " + config.getBeforeResetWarning() + " 秒后重置!";

        for (Player player : world.getPlayers()) {
            Location loc = player.getLocation();
            if (loc.getBlockX() >= minX && loc.getBlockX() <= maxX &&
                loc.getBlockZ() >= minZ && loc.getBlockZ() <= maxZ) {
                player.sendMessage(warningMsg);
                player.sendTitle("§c区块即将重置!", "§e" + config.getBeforeResetWarning() + "秒后执行", 10, 70, 20);
            }
        }
    }

    public void resetChunk(ChunkData data) {
        String key = data.getKey();
        
        World world = plugin.getServer().getWorld(data.getWorldName());
        if (world == null) {
            plugin.getLogger().warning("无法找到世界: " + data.getWorldName());
            return;
        }

        coreProtectHandler.teleportPlayersInChunk(world, data.getChunkX(), data.getChunkZ());

        String reason = buildReason(data);
        final String finalReason = reason;
        
        coreProtectHandler.rollbackChunk(world, data.getChunkX(), data.getChunkZ()).thenAccept(success -> {
            if (success) {
                cooldownMap.put(key, System.currentTimeMillis());
                warningMap.remove(key);
                
                resetRecorder.recordReset(
                    data.getWorldName(),
                    data.getChunkX(),
                    data.getChunkZ(),
                    finalReason,
                    false
                );
                
                plugin.getLogger().info("区块重置成功: " + data.getWorldName() + "," + data.getChunkX() + "," + data.getChunkZ());
            }
        });
    }

    public void restoreChunk(String worldName, int chunkX, int chunkZ) {
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("无法找到世界: " + worldName);
            return;
        }

        coreProtectHandler.restoreChunk(world, chunkX, chunkZ).thenAccept(success -> {
            if (success) {
                resetRecorder.markAsRestored(worldName, chunkX, chunkZ);
                plugin.getLogger().info("区块恢复成功: " + worldName + "," + chunkX + "," + chunkZ);
            }
        });
    }

    private String buildReason(ChunkData data) {
        StringBuilder sb = new StringBuilder();
        if (data.getTickTime() > config.getTickTimeThreshold()) {
            sb.append("tick耗时:").append(data.getTickTime()).append("ms;");
        }
        if (data.getEntityCount() > config.getEntityCountThreshold()) {
            sb.append("实体:").append(data.getEntityCount()).append(";");
        }
        if (data.getTileEntityCount() > config.getTileEntityCountThreshold()) {
            sb.append("方块实体:").append(data.getTileEntityCount()).append(";");
        }
        return sb.toString();
    }

    private String buildReasonShort(ChunkData data) {
        List<String> reasons = new ArrayList<>();
        if (data.getTickTime() > config.getTickTimeThreshold()) {
            reasons.add("tick耗时过高");
        }
        if (data.getEntityCount() > config.getEntityCountThreshold()) {
            reasons.add("实体过多");
        }
        if (data.getTileEntityCount() > config.getTileEntityCountThreshold()) {
            reasons.add("方块实体过多");
        }
        return String.join("、", reasons);
    }

    public static class ChunkData {
        private final String worldName;
        private final int chunkX;
        private final int chunkZ;
        private final int tickTime;
        private final int entityCount;
        private final int tileEntityCount;
        private final int expectedTickTime;

        public ChunkData(String worldName, int chunkX, int chunkZ, int tickTime, 
                        int entityCount, int tileEntityCount, int expectedTickTime) {
            this.worldName = worldName;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.tickTime = tickTime;
            this.entityCount = entityCount;
            this.tileEntityCount = tileEntityCount;
            this.expectedTickTime = expectedTickTime;
        }

        public String getWorldName() {
            return worldName;
        }

        public int getChunkX() {
            return chunkX;
        }

        public int getChunkZ() {
            return chunkZ;
        }

        public int getTickTime() {
            return tickTime;
        }

        public int getEntityCount() {
            return entityCount;
        }

        public int getTileEntityCount() {
            return tileEntityCount;
        }

        public int getExpectedTickTime() {
            return expectedTickTime;
        }

        public String getKey() {
            return worldName + ":" + chunkX + ":" + chunkZ;
        }
    }
}
