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
    private String databaseType;
    private String dbHost;
    private int dbPort;
    private String dbName;
    private String dbUsername;
    private String dbPassword;

    public ConfigManager(BoomMan plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        this.config = plugin.getConfig();

        this.monitorEnabled = config.getBoolean("monitor.enabled", true);
        this.checkInterval = config.getInt("monitor.check-interval", 20);
        this.tickTimeThreshold = config.getInt("thresholds.tick-time-ms", 250);
        this.entityCountThreshold = config.getInt("thresholds.entity-count", 600);
        this.tileEntityCountThreshold = config.getInt("thresholds.tile-entity-count", 250);
        this.tickMsPerPlayer = config.getInt("thresholds.tick-ms-per-player", 10);
        this.resetEnabled = config.getBoolean("reset.enabled", true);
        this.beforeResetWarning = config.getInt("reset.before-reset-warning", 10);
        this.cooldownSeconds = config.getInt("reset.cooldown-seconds", 120);
        this.excludeWorlds = config.getStringList("reset.exclude-worlds");
        if (this.excludeWorlds == null) {
            this.excludeWorlds = new ArrayList<>();
        }
        this.autoSnapshot = config.getBoolean("coreprotect.auto-snapshot", true);
        this.restoreTimeout = config.getInt("coreprotect.restore-timeout", 300);
        this.bStatsEnabled = config.getBoolean("bstats.enabled", true);
        
        this.databaseType = config.getString("database.type", "sqlite");
        this.dbHost = config.getString("database.host", "localhost");
        this.dbPort = config.getInt("database.port", 3306);
        this.dbName = config.getString("database.name", "boomman");
        this.dbUsername = config.getString("database.username", "root");
        this.dbPassword = config.getString("database.password", "");
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

    public String getDatabaseType() {
        return databaseType;
    }

    public String getDbHost() {
        return dbHost;
    }

    public int getDbPort() {
        return dbPort;
    }

    public String getDbName() {
        return dbName;
    }

    public String getDbUsername() {
        return dbUsername;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        loadConfig();
    }
}
