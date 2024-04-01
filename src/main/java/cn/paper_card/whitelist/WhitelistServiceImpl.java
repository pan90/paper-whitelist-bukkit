package cn.paper_card.whitelist;

import cn.paper_card.database.api.DatabaseApi;
import cn.paper_card.paper_whitelist.api.AlreadyWhitelistedException;
import cn.paper_card.paper_whitelist.api.WhitelistInfo;
import cn.paper_card.paper_whitelist.api.WhitelistService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

class WhitelistServiceImpl implements WhitelistService {

    private final @NotNull DatabaseApi.MySqlConnection mySqlConnection;

    private Connection connection = null;
    private WhitelistTable table = null;

    WhitelistServiceImpl(DatabaseApi.@NotNull MySqlConnection mySqlConnection) {
        this.mySqlConnection = mySqlConnection;
    }

    private @NotNull WhitelistTable getTable() throws SQLException {
        final Connection newCon = this.mySqlConnection.getRawConnection();

        if (this.connection != null && this.connection == newCon && this.table != null) return this.table;

        if (this.table != null) this.table.close();
        this.table = new WhitelistTable(newCon, "whitelist");
        this.connection = newCon;

        return this.table;
    }

    void destroy() throws SQLException {
        synchronized (this.mySqlConnection) {
            final WhitelistTable t = this.table;
            this.connection = null;
            this.table = null;
            if (t != null) t.close();
        }
    }

    @Override
    public void add(@NotNull WhitelistInfo whitelistInfo) throws SQLException, AlreadyWhitelistedException {
        synchronized (this.mySqlConnection) {
            try {
                final WhitelistTable t = this.getTable();

                // 先检查重复
                final WhitelistInfo info = t.query(whitelistInfo.userId());
                if (info != null) {
                    this.mySqlConnection.setLastUseTime();
                    throw new AlreadyWhitelistedException(info, "已添加白名单！");
                }

                final int inserted = t.insert(whitelistInfo);
                this.mySqlConnection.setLastUseTime();
                if (inserted != 1) throw new RuntimeException();
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
    public boolean remove(@NotNull UUID userId) throws SQLException {
        synchronized (this.mySqlConnection) {
            try {
                final WhitelistTable t = this.getTable();

                final int deleted = t.delete(userId);
                this.mySqlConnection.setLastUseTime();

                if (deleted == 1) return true;
                if (deleted == 0) return false;

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
    public @Nullable WhitelistInfo query(@NotNull UUID userId) throws SQLException {

        // 网络查询
        

        synchronized (this.mySqlConnection) {
            try {
                final WhitelistTable t = this.getTable();
                final WhitelistInfo info = t.query(userId);
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
    public @NotNull List<WhitelistInfo> queryPage(int limit, int offset) throws SQLException {
        synchronized (this.mySqlConnection) {
            try {
                final WhitelistTable t = this.getTable();

                final List<WhitelistInfo> list = t.queryPage(limit, offset);
                this.mySqlConnection.setLastUseTime();

                return list;
            } catch (SQLException e) {
                try {
                    this.mySqlConnection.handleException(e);
                } catch (SQLException ignored) {
                }
                throw e;
            }
        }
    }


    public int queryCount() throws SQLException {
        synchronized (this.mySqlConnection) {
            try {
                final WhitelistTable t = this.getTable();
                final int count = t.queryCount();
                this.mySqlConnection.setLastUseTime();
                return count;
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
    public @NotNull List<WhitelistInfo> search(@NotNull String keyWord, int limit, int offset) {
        throw new UnsupportedOperationException("TODO！");
    }
}
