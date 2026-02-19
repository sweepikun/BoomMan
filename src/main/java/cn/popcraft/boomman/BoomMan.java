package cn.popcraft.boomman;

import cn.popcraft.boomman.command.BoomManCommand;
import cn.popcraft.boomman.config.ConfigManager;
import cn.popcraft.boomman.coreprotect.CoreProtectHandler;
import cn.popcraft.boomman.monitor.ChunkMonitor;
import cn.popcraft.boomman.recorder.ResetRecorder;
import cn.popcraft.boomman.task.MonitorTask;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.plugin.java.JavaPlugin;

public class BoomMan extends JavaPlugin {

    private static BoomMan instance;
    private ConfigManager configManager;
    private CoreProtectHandler coreProtectHandler;
    private ChunkMonitor chunkMonitor;
    private ResetRecorder resetRecorder;
    private MonitorTask monitorTask;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        coreProtectHandler = new CoreProtectHandler(this);
        if (!coreProtectHandler.isCoreProtectEnabled()) {
            getLogger().severe("未找到 CoreProtect 插件，插件将禁用!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        resetRecorder = new ResetRecorder(this);
        resetRecorder.initialize();

        chunkMonitor = new ChunkMonitor(this);

        if (configManager.isMonitorEnabled()) {
            monitorTask = new MonitorTask(this);
            monitorTask.start();
        }

        getServer().getPluginCommand("boomman").setExecutor(new BoomManCommand(this));

        initMetrics();

        getLogger().info("BoomMan 插件已启用!");
    }

    @Override
    public void onDisable() {
        if (monitorTask != null) {
            monitorTask.stop();
        }
        if (resetRecorder != null) {
            resetRecorder.close();
        }
        getLogger().info("BoomMan 插件已禁用!");
    }

    private void initMetrics() {
        if (!configManager.isBStatsEnabled()) {
            return;
        }
        try {
            Metrics metrics = new Metrics(this, 23701);
            metrics.addCustomChart(new SimplePie("plugin_version", () -> getDescription().getVersion()));
        } catch (Exception e) {
            getLogger().warning("BStats 统计初始化失败，已关闭统计功能");
        }
    }

    public static BoomMan getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public CoreProtectHandler getCoreProtectHandler() {
        return coreProtectHandler;
    }

    public ChunkMonitor getChunkMonitor() {
        return chunkMonitor;
    }

    public ResetRecorder getResetRecorder() {
        return resetRecorder;
    }

    public MonitorTask getMonitorTask() {
        return monitorTask;
    }
}
