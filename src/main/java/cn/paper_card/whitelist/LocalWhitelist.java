package cn.paper_card.whitelist;

import cn.paper_card.paper_whitelist.api.WhitelistInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

class LocalWhitelist {
    private final @NotNull Connection connection;

    private CacheTable table = null;

    LocalWhitelist(@NotNull Connection connection) {
        this.connection = connection;
    }

    void destroy() throws SQLException {
        final CacheTable t = this.table;

        this.table = null;

        SQLException exception = null;
        try {
            t.close();
        } catch (SQLException e) {
            exception = e;
        }

        try {
            this.connection.close();
        } catch (SQLException e) {
            exception = e;
        }

        if (exception != null) throw exception;
    }

    private @NotNull CacheTable getTable() throws SQLException {
        if (this.table != null) return this.table;

        this.table = new CacheTable(this.connection);
        return this.table;
    }

    boolean delete(@NotNull UUID uuid) throws SQLException {
        synchronized (this) {
            final CacheTable t = this.getTable();
            final int deleted = t.delete(uuid);
            if (deleted == 1) return true;
            if (deleted == 0) return false;
            throw new RuntimeException("删除了%d条数据！".formatted(deleted));
        }
    }


    // 添加或更新数据
    void update(@NotNull WhitelistInfo info) throws SQLException {
        synchronized (this) {
            final CacheTable t = this.getTable();
            final int deleted = t.delete(info.userId());
            final int inserted = t.insert(info);
            if (deleted != 0 && deleted != 1) throw new RuntimeException("删除了%d条数据！".formatted(deleted));
            if (inserted != 1) throw new RuntimeException("插入了%d条数据！".formatted(inserted));
        }
    }

    @Nullable WhitelistInfo query(@NotNull UUID uuid) throws SQLException {
        synchronized (this) {
            final CacheTable t = this.getTable();
            return t.query(uuid);
        }
    }

}
