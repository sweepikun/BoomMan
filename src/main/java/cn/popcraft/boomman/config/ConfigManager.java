package cn.popcraft.boomman.config;

import cn.popcraft.boomman.BoomMan;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class ConfigManager {

    private final BoomMan plugin;
    private FileConfiguration config;

    private boolean monitorEnabled;
    private int checkInterval;
    private int tickTimeThreshold;
    private int entityCountThreshold;
    private int tileEntityCountThreshold;
    private int tickMsPerPlayer;
    private boolean resetEnabled;
    private int beforeResetWarning;
    private int cooldownSeconds;
    private List<String> excludeWorlds;
    private boolean autoSnapshot;
    private int restoreTimeout;
    private boolean bStatsEnabled;

    public ConfigManager(BoomMan plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        this.config = plugin.getConfig();

        this.monitorEnabled = config.getBoolean("monitor.enabled", true);
        this.checkInterval = config.getInt("monitor.check-interval", 20);
        this.tickTimeThreshold = config.getInt("thresholds.tick-time-ms", 20);
        this.entityCountThreshold = config.getInt("thresholds.entity-count", 50);
        this.tileEntityCountThreshold = config.getInt("thresholds.tile-entity-count", 20);
        this.tickMsPerPlayer = config.getInt("thresholds.tick-ms-per-player", 2);
        this.resetEnabled = config.getBoolean("reset.enabled", true);
        this.beforeResetWarning = config.getInt("reset.before-reset-warning", 5);
        this.cooldownSeconds = config.getInt("reset.cooldown-seconds", 60);
        this.excludeWorlds = config.getStringList("reset.exclude-worlds");
        if (this.excludeWorlds == null) {
            this.excludeWorlds = new ArrayList<>();
        }
        this.autoSnapshot = config.getBoolean("coreprotect.auto-snapshot", true);
        this.restoreTimeout = config.getInt("coreprotect.restore-timeout", 300);
        this.bStatsEnabled = config.getBoolean("bstats.enabled", true);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public boolean isMonitorEnabled() {
        return monitorEnabled;
    }

    public int getCheckInterval() {
        return checkInterval;
    }

    public int getTickTimeThreshold() {
        return tickTimeThreshold;
    }

    public int getEntityCountThreshold() {
        return entityCountThreshold;
    }

    public int getTileEntityCountThreshold() {
        return tileEntityCountThreshold;
    }

    public int getTickMsPerPlayer() {
        return tickMsPerPlayer;
    }

    public boolean isResetEnabled() {
        return resetEnabled;
    }

    public int getBeforeResetWarning() {
        return beforeResetWarning;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public List<String> getExcludeWorlds() {
        return excludeWorlds;
    }

    public boolean isAutoSnapshot() {
        return autoSnapshot;
    }

    public int getRestoreTimeout() {
        return restoreTimeout;
    }

    public boolean isBStatsEnabled() {
        return bStatsEnabled;
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        loadConfig();
    }
}
