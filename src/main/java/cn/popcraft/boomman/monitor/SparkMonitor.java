package cn.popcraft.boomman.monitor;

import cn.popcraft.boomman.BoomMan;
import cn.popcraft.boomman.config.ConfigManager;
import me.lucko.spark.api.Spark;
import me.lucko.spark.api.SparkProvider;
import me.lucko.spark.api.statistic.StatisticWindow;

public class SparkMonitor {

    private final BoomMan plugin;
    private final ConfigManager config;
    private Spark spark;

    public SparkMonitor(BoomMan plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        initSpark();
    }

    private void initSpark() {
        if (!config.isSparkEnabled()) {
            return;
        }

        try {
            this.spark = SparkProvider.get();
            plugin.getLogger().info("Spark API 连接成功!");
        } catch (IllegalStateException e) {
            plugin.getLogger().warning("Spark 插件未安装，已禁用Spark监控功能");
            this.spark = null;
        }
    }

    public void checkAndLog() {
        if (!config.isSparkEnabled() || spark == null) {
            return;
        }

        try {
            var mspt = spark.mspt();
            if (mspt == null) {
                return;
            }

            var msptLastMin = mspt.poll(StatisticWindow.MillisPerTick.MINUTES_1);
            if (msptLastMin == null) {
                return;
            }

            double mean = msptLastMin.mean();
            double percentile95 = msptLastMin.percentile95th();

            int threshold = config.getSparkMsptThreshold();

            if (mean > threshold) {
                plugin.getLogger().warning("§c[Spark警报] §eMSPT!过高 " +
                    "平均: " + String.format("%.1f", mean) + "ms, " +
                    "95%: " + String.format("%.1f", percentile95) + "ms " +
                    "(阈值: " + threshold + "ms)");
            }

            var tps = spark.tps();
            if (tps != null) {
                double tps10s = tps.poll(StatisticWindow.TicksPerSecond.SECONDS_10);
                double tps1m = tps.poll(StatisticWindow.TicksPerSecond.MINUTES_1);

                if (tps10s < 18.0) {
                    plugin.getLogger().warning("§c[Spark警报] §eTPS过低! " +
                        "10秒: " + String.format("%.1f", tps10s) + ", " +
                        "1分钟: " + String.format("%.1f", tps1m));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Spark 监控出错: " + e.getMessage());
        }
    }
}
