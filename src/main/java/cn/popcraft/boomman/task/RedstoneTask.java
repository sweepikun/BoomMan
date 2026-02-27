package cn.popcraft.boomman.task;

import cn.popcraft.boomman.BoomMan;
import cn.popcraft.boomman.config.ConfigManager;
import cn.popcraft.boomman.monitor.RedstoneMonitor;
import cn.popcraft.boomman.monitor.SparkMonitor;
import org.bukkit.scheduler.BukkitRunnable;

public class RedstoneTask {

    private final BoomMan plugin;
    private final ConfigManager config;
    private final RedstoneMonitor redstoneMonitor;
    private final SparkMonitor sparkMonitor;
    private BukkitRunnable task;
    private boolean running = false;

    public RedstoneTask(BoomMan plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.redstoneMonitor = plugin.getRedstoneMonitor();
        this.sparkMonitor = plugin.getSparkMonitor();
    }

    public void start() {
        if (running) {
            return;
        }

        running = true;
        task = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (redstoneMonitor != null) {
                        redstoneMonitor.checkAndLog();
                    }
                    if (sparkMonitor != null) {
                        sparkMonitor.checkAndLog();
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("红石/Spark监控任务执行时发生错误: " + e.getMessage());
                }
            }
        };

        long interval = config.getRedstoneCheckInterval() * 20L;
        task.runTaskTimerAsynchronously(plugin, interval, interval);
        
        plugin.getLogger().info("红石/Spark监控任务已启动! 检查间隔: " + config.getRedstoneCheckInterval() + " 秒");
    }

    public void stop() {
        running = false;
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public boolean isRunning() {
        return running;
    }
}
