package cn.paper_card.paper_whitelist;

import cn.paper_card.database.api.DatabaseApi;
import cn.paper_card.paper_whitelist.api.WhitelistCodeInfo;
import cn.paper_card.paper_whitelist.api.WhitelistCodeService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Random;
import java.util.UUID;

class WhitelistCodeServiceImpl implements WhitelistCodeService {

    private final @NotNull DatabaseApi.MySqlConnection mySqlConnection;
    private Connection connection = null;
    private WhitelistCodeTable table = null;

    WhitelistCodeServiceImpl(DatabaseApi.@NotNull MySqlConnection mySqlConnection) {
        this.mySqlConnection = mySqlConnection;
    }

    private @NotNull WhitelistCodeTable getTable() throws SQLException {
        final Connection newCon = this.mySqlConnection.getRawConnection();

        if (this.connection != null && this.connection == newCon && this.table != null) return this.table;

        if (this.table != null) this.table.close();
        this.table = new WhitelistCodeTable(newCon, "whitelist_code");
        this.connection = newCon;

        return this.table;
    }

    void destroy() throws SQLException {
        synchronized (this.mySqlConnection) {
            final WhitelistCodeTable t = this.table;
            this.connection = null;
            this.table = null;
            if (t != null) t.close();
        }
    }

    private int randomCode() {
        final int min = 1;
        final int max = 999999;
        return new Random().nextInt(max - min + 1) + min;
    }

    @Override
    public @NotNull WhitelistCodeInfo create(@NotNull UUID id, @NotNull String name) throws SQLException {
        final int code = this.randomCode();
        final long cur = System.currentTimeMillis();
        synchronized (this.mySqlConnection) {
            try {
                final WhitelistCodeTable t = this.getTable();

                t.deleteTimeBefore(cur); // 删除过期的验证码

                // 数据库保证：防止验证码重复

                final WhitelistCodeInfo info = new WhitelistCodeInfo(code,
                        id,
                        name,
                        cur,
                        cur + 2 * 60 * 1000L
                );
                final int updated = t.updateByUuid(info);
                this.mySqlConnection.setLastUseTime();

                if (updated == 1) {
                    this.mySqlConnection.setLastUseTime();
                    return info;
                }

                if (updated == 0) {
                    final int inserted = t.insert(info);
                    this.mySqlConnection.setLastUseTime();
                    if (inserted != 1) throw new RuntimeException();
                    return info;
                }

                throw new RuntimeException();
            } catch (SQLException e) {
                try {
                    this.mySqlConnection.handleException(e);
                } catch (SQLException ignored) {
                }
                throw e;
            }
        }
    }

    @Override
    public @Nullable WhitelistCodeInfo take(int code) throws SQLException {
        synchronized (this.mySqlConnection) {
            try {
                final WhitelistCodeTable t = this.getTable();
                final WhitelistCodeInfo info = t.queryByCode(code);

                if (info == null) {
                    this.mySqlConnection.setLastUseTime();
                    return null;
                }

                final int deleted = t.deleteByCode(code);
                this.mySqlConnection.setLastUseTime();

                if (deleted != 1) throw new RuntimeException();

                return info;
            } catch (SQLException e) {
                try {
                    this.mySqlConnection.handleException(e);
                } catch (SQLException ignored) {
                }
                throw e;
            }
        }
    }

    @Override
    public @Nullable WhitelistCodeInfo query(@NotNull UUID id) throws SQLException {
        synchronized (this.mySqlConnection) {
            try {
                final WhitelistCodeTable t = this.getTable();
                final WhitelistCodeInfo info = t.queryByUuid(id);
                this.mySqlConnection.setLastUseTime();
                return info;
            } catch (SQLException e) {
                try {
                    this.mySqlConnection.handleException(e);
                } catch (SQLException ignored) {
                }
                throw e;
            }
        }
    }

    @Override
    public @Nullable WhitelistCodeInfo take(@NotNull UUID uuid) throws SQLException {
        synchronized (this.mySqlConnection) {
            try {
                final WhitelistCodeTable t = this.getTable();
                final WhitelistCodeInfo info = t.queryByUuid(uuid);
                this.mySqlConnection.setLastUseTime();
                if (info != null) t.deleteByCode(info.code());
                return info;
            } catch (SQLException e) {
                try {
                    this.mySqlConnection.handleException(e);
                } catch (SQLException ignored) {
                }
                throw e;
            }
        }
    }

    @Override
    public int deleteExpires() throws SQLException {
        synchronized (this.mySqlConnection) {
            try {
                final WhitelistCodeTable t = this.getTable();
                final int i = t.deleteTimeBefore(System.currentTimeMillis());
                this.mySqlConnection.setLastUseTime();
                return i;
            } catch (SQLException e) {
                try {
                    this.mySqlConnection.handleException(e);
                } catch (SQLException ignored) {
                }
                throw e;
            }
        }
    }
}
