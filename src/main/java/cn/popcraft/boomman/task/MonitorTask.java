package cn.popcraft.boomman.task;

import cn.popcraft.boomman.BoomMan;
import cn.popcraft.boomman.config.ConfigManager;
import cn.popcraft.boomman.monitor.ChunkMonitor;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class MonitorTask {

    private final BoomMan plugin;
    private final ConfigManager config;
    private final ChunkMonitor chunkMonitor;
    private BukkitRunnable task;
    private boolean running = false;

    public MonitorTask(BoomMan plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.chunkMonitor = plugin.getChunkMonitor();
    }

    public void start() {
        if (running) {
            return;
        }

        running = true;
        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!config.isResetEnabled()) {
                    return;
                }

                try {
                    List<ChunkMonitor.ChunkData> laggingChunks = chunkMonitor.checkAllChunks();
                    
                    for (ChunkMonitor.ChunkData chunkData : laggingChunks) {
                        chunkMonitor.handleLaggingChunk(chunkData);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("监控任务执行时发生错误: " + e.getMessage());
                }
            }
        };

        long interval = config.getCheckInterval();
        task.runTaskTimerAsynchronously(plugin, interval, interval);
        
        plugin.getLogger().info("区块监控任务已启动! 检查间隔: " + interval + " tick");
    }

    public void stop() {
        running = false;
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void restart() {
        stop();
        start();
    }

    public boolean isRunning() {
        return running;
    }
}
