package cn.popcraft.boomman.command;

import cn.popcraft.boomman.BoomMan;
import cn.popcraft.boomman.monitor.ChunkMonitor;
import cn.popcraft.boomman.recorder.ResetRecorder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class BoomManCommand implements CommandExecutor {

    private final BoomMan plugin;

    public BoomManCommand(BoomMan plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "help":
                sendHelp(sender);
                break;
            case "reload":
                reloadConfig(sender);
                break;
            case "status":
                showStatus(sender);
                break;
            case "records":
            case "record":
                showRecords(sender, args);
                break;
            case "restore":
                restoreChunk(sender, args);
                break;
            case "check":
                manualCheck(sender);
                break;
            default:
                sender.sendMessage("§c未知命令，请使用 /boomman help 查看帮助");
                break;
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== BoomMan 帮助 ===");
        sender.sendMessage("§e/boomman help §7- 显示帮助");
        sender.sendMessage("§e/boomman reload §7- 重载配置");
        sender.sendMessage("§e/boomman status §7- 查看插件状态");
        sender.sendMessage("§e/boomman records [页码] §7- 查看重置记录");
        sender.sendMessage("§e/boomman records <世界名> [页码] §7- 查看指定世界记录");
        sender.sendMessage("§e/boomman restore <世界名> <x> <z> §7- 恢复指定区块");
        sender.sendMessage("§e/boomman check §7- 手动检查卡顿区块");
    }

    private void reloadConfig(CommandSender sender) {
        if (!sender.hasPermission("boomman.admin")) {
            sender.sendMessage("§c你没有权限执行此命令");
            return;
        }

        plugin.getConfigManager().reloadConfig();
        
        if (plugin.getMonitorTask() != null) {
            plugin.getMonitorTask().restart();
        }
        
        sender.sendMessage("§a配置已重载!");
    }

    private void showStatus(CommandSender sender) {
        sender.sendMessage("§6=== BoomMan 状态 ===");
        sender.sendMessage("§e版本: §a" + plugin.getDescription().getVersion());
        sender.sendMessage("§e监控状态: §a" + (plugin.getMonitorTask() != null && plugin.getMonitorTask().isRunning() ? "运行中" : "已停止"));
        sender.sendMessage("§eCoreProtect: §a" + (plugin.getCoreProtectHandler().isCoreProtectEnabled() ? "已连接" : "未连接"));
        
        ResetRecorder recorder = plugin.getResetRecorder();
        int totalResets = recorder.getTotalResetCount();
        int totalRestores = recorder.getTotalRestoreCount();
        
        sender.sendMessage("§e总重置次数: §a" + totalResets);
        sender.sendMessage("§e总恢复次数: §a" + totalRestores);
    }

    private void showRecords(CommandSender sender, String[] args) {
        int page = 1;
        String worldName = null;

        if (args.length >= 2) {
            if (args.length == 3) {
                try {
                    worldName = args[1];
                    page = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ex) {
                        sender.sendMessage("§c页码必须是数字");
                        return;
                    }
                }
            } else if (args.length == 2) {
                try {
                    page = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    worldName = args[1];
                }
            }
        }

        int limit = 10;
        int offset = (page - 1) * limit;

        List<ResetRecorder.ResetRecord> records;
        if (worldName != null) {
            records = plugin.getResetRecorder().getRecordsByWorld(worldName, limit);
        } else {
            records = plugin.getResetRecorder().getRecentRecords(limit);
        }

        sender.sendMessage("§6=== 重置记录 (第 " + page + " 页) ===");
        
        if (records.isEmpty()) {
            sender.sendMessage("§7暂无记录");
            return;
        }

        for (ResetRecorder.ResetRecord record : records) {
            String status = record.isRestored() ? "§a已恢复" : "§c未恢复";
            sender.sendMessage(String.format(
                "§e#%d §7- %s, %d, %d §7- %s",
                record.getId(),
                record.getWorldName(),
                record.getChunkX(),
                record.getChunkZ(),
                status
            ));
            sender.sendMessage(String.format(
                "  §7时间: %s §7原因: %s",
                record.getFormattedTime(),
                record.getReason()
            ));
        }
    }

    private void restoreChunk(CommandSender sender, String[] args) {
        if (!sender.hasPermission("boomman.admin")) {
            sender.sendMessage("§c你没有权限执行此命令");
            return;
        }

        if (args.length < 4) {
            sender.sendMessage("§c用法: /boomman restore <世界名> <x> <z>");
            return;
        }

        String worldName = args[1];
        
        int chunkX, chunkZ;
        try {
            chunkX = Integer.parseInt(args[2]);
            chunkZ = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§c区块坐标必须是数字");
            return;
        }

        sender.sendMessage("§e正在恢复区块...");
        
        plugin.getChunkMonitor().restoreChunk(worldName, chunkX, chunkZ);
        
        sender.sendMessage("§a恢复请求已发送!");
    }

    private void manualCheck(CommandSender sender) {
        if (!sender.hasPermission("boomman.admin")) {
            sender.sendMessage("§c你没有权限执行此命令");
            return;
        }

        sender.sendMessage("§e正在检查卡顿区块...");
        
        List<ChunkMonitor.ChunkData> laggingChunks = plugin.getChunkMonitor().checkAllChunks();
        
        if (laggingChunks.isEmpty()) {
            sender.sendMessage("§a未发现卡顿区块");
        } else {
            sender.sendMessage("§c发现 " + laggingChunks.size() + " 个卡顿区块:");
            for (ChunkMonitor.ChunkData data : laggingChunks) {
                sender.sendMessage(String.format(
                    "§e%s, %d, %d §7- tick: %dms, 实体: %d, 方块实体: %d",
                    data.getWorldName(),
                    data.getChunkX(),
                    data.getChunkZ(),
                    data.getTickTime(),
                    data.getEntityCount(),
                    data.getTileEntityCount()
                ));
            }
        }
    }
}
