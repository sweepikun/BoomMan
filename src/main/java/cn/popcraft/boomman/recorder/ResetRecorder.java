package cn.popcraft.boomman.recorder;

import cn.popcraft.boomman.BoomMan;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class ResetRecorder {

    private final BoomMan plugin;
    private Connection connection;

    private static final String CREATE_TABLE_SQL = 
        "CREATE TABLE IF NOT EXISTS reset_records (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "world_name VARCHAR(64) NOT NULL, " +
        "chunk_x INTEGER NOT NULL, " +
        "chunk_z INTEGER NOT NULL, " +
        "reset_time BIGINT NOT NULL, " +
        "reason TEXT, " +
        "is_restored BOOLEAN DEFAULT FALSE, " +
        "restore_time BIGINT" +
        ")";

    public ResetRecorder(BoomMan plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        try {
            String dbPath = plugin.getDataFolder().getAbsolutePath() + "/records.db";
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(CREATE_TABLE_SQL);
            }
            
            plugin.getLogger().info("重置记录数据库初始化成功!");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "初始化数据库时发生错误", e);
        }
    }

    public void recordReset(String worldName, int chunkX, int chunkZ, String reason, boolean isRestored) {
        String sql = "INSERT INTO reset_records (world_name, chunk_x, chunk_z, reset_time, reason, is_restored) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, worldName);
            pstmt.setInt(2, chunkX);
            pstmt.setInt(3, chunkZ);
            pstmt.setLong(4, System.currentTimeMillis());
            pstmt.setString(5, reason);
            pstmt.setBoolean(6, isRestored);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "记录重置时发生错误", e);
        }
    }

    public void markAsRestored(String worldName, int chunkX, int chunkZ) {
        String sql = "UPDATE reset_records SET is_restored = TRUE, restore_time = ? WHERE world_name = ? AND chunk_x = ? AND chunk_z = ? AND is_restored = FALSE";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, System.currentTimeMillis());
            pstmt.setString(2, worldName);
            pstmt.setInt(3, chunkX);
            pstmt.setInt(4, chunkZ);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "标记恢复状态时发生错误", e);
        }
    }

    public List<ResetRecord> getRecentRecords(int limit) {
        List<ResetRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM reset_records ORDER BY reset_time DESC LIMIT ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                records.add(new ResetRecord(
                    rs.getInt("id"),
                    rs.getString("world_name"),
                    rs.getInt("chunk_x"),
                    rs.getInt("chunk_z"),
                    rs.getLong("reset_time"),
                    rs.getString("reason"),
                    rs.getBoolean("is_restored"),
                    rs.getLong("restore_time")
                ));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "查询记录时发生错误", e);
        }
        
        return records;
    }

    public List<ResetRecord> getRecordsByWorld(String worldName, int limit) {
        List<ResetRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM reset_records WHERE world_name = ? ORDER BY reset_time DESC LIMIT ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, worldName);
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                records.add(new ResetRecord(
                    rs.getInt("id"),
                    rs.getString("world_name"),
                    rs.getInt("chunk_x"),
                    rs.getInt("chunk_z"),
                    rs.getLong("reset_time"),
                    rs.getString("reason"),
                    rs.getBoolean("is_restored"),
                    rs.getLong("restore_time")
                ));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "查询记录时发生错误", e);
        }
        
        return records;
    }

    public ResetRecord getLatestReset(String worldName, int chunkX, int chunkZ) {
        String sql = "SELECT * FROM reset_records WHERE world_name = ? AND chunk_x = ? AND chunk_z = ? ORDER BY reset_time DESC LIMIT 1";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, worldName);
            pstmt.setInt(2, chunkX);
            pstmt.setInt(3, chunkZ);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return new ResetRecord(
                    rs.getInt("id"),
                    rs.getString("world_name"),
                    rs.getInt("chunk_x"),
                    rs.getInt("chunk_z"),
                    rs.getLong("reset_time"),
                    rs.getString("reason"),
                    rs.getBoolean("is_restored"),
                    rs.getLong("restore_time")
                );
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "查询记录时发生错误", e);
        }
        
        return null;
    }

    public int getTotalResetCount() {
        String sql = "SELECT COUNT(*) FROM reset_records";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "统计记录数时发生错误", e);
        }
        
        return 0;
    }

    public int getTotalRestoreCount() {
        String sql = "SELECT COUNT(*) FROM reset_records WHERE is_restored = TRUE";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "统计恢复数时发生错误", e);
        }
        
        return 0;
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "关闭数据库连接时发生错误", e);
            }
        }
    }

    public static class ResetRecord {
        private final int id;
        private final String worldName;
        private final int chunkX;
        private final int chunkZ;
        private final long resetTime;
        private final String reason;
        private final boolean isRestored;
        private final long restoreTime;

        public ResetRecord(int id, String worldName, int chunkX, int chunkZ, 
                          long resetTime, String reason, boolean isRestored, long restoreTime) {
            this.id = id;
            this.worldName = worldName;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.resetTime = resetTime;
            this.reason = reason;
            this.isRestored = isRestored;
            this.restoreTime = restoreTime;
        }

        public int getId() {
            return id;
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

        public long getResetTime() {
            return resetTime;
        }

        public String getReason() {
            return reason;
        }

        public boolean isRestored() {
            return isRestored;
        }

        public long getRestoreTime() {
            return restoreTime;
        }

        public String getFormattedTime() {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdf.format(new java.util.Date(resetTime));
        }

        public String getFormattedRestoreTime() {
            if (restoreTime == 0) return "未恢复";
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdf.format(new java.util.Date(restoreTime));
        }
    }
}
