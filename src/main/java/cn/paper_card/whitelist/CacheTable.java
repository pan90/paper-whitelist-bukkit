package cn.paper_card.whitelist;

import cn.paper_card.database.api.Parser;
import cn.paper_card.database.api.Util;
import cn.paper_card.paper_whitelist.api.WhitelistInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

// SQLite
class CacheTable extends Parser<WhitelistInfo> {

    private final @NotNull String name = "whitelist";

    private final @NotNull Connection connection;

    private PreparedStatement statementInsert = null;

    private PreparedStatement statementDelete = null;

    private PreparedStatement statementQuery = null;

    CacheTable(@NotNull Connection connection) throws SQLException {
        this.connection = connection;
        this.create();
    }

    // 建表
    private void create() throws SQLException {
        Util.executeSQL(this.connection, """
                CREATE TABLE IF NOT EXISTS %s
                (
                    name   VARCHAR(64) NOT NULL,
                    uuid   CHAR(64)    NOT NULL,
                    remark VARCHAR(64) NOT NULL,
                    c_time INTEGER     NOT NULL
                );""".formatted(this.name));
    }

    void close() throws SQLException {
        Util.closeAllStatements(this.getClass(), this);
    }

    private @NotNull PreparedStatement getStatementInsert() throws SQLException {
        if (this.statementInsert == null) {
            this.statementInsert = this.connection.prepareStatement("""
                    INSERT INTO %s(name, uuid, remark, c_time)
                    VALUES (?, ?, ?, ?);""".formatted(this.name));
        }
        return this.statementInsert;
    }

    private @NotNull PreparedStatement getStatementDelete() throws SQLException {
        if (this.statementDelete == null) {
            this.statementDelete = this.connection.prepareStatement("""
                    DELETE
                    FROM %s
                    WHERE uuid = ?;""".formatted(this.name));
        }
        return this.statementDelete;
    }

    private @NotNull PreparedStatement getStatementQuery() throws SQLException {
        if (this.statementQuery == null) {
            this.statementQuery = this.connection.prepareStatement("""
                    SELECT name, uuid, remark, c_time
                    FROM %s
                    WHERE uuid = ?
                    LIMIT 1;""".formatted(this.name));
        }
        return this.statementQuery;
    }

    // 插入
    int insert(@NotNull WhitelistInfo info) throws SQLException {
        final PreparedStatement ps = this.getStatementInsert();
        ps.setString(1, info.name());
        ps.setString(2, info.userId().toString());
        ps.setString(3, info.remark());
        ps.setLong(4, info.createTime());
        return ps.executeUpdate();
    }

    int delete(@NotNull UUID uuid) throws SQLException {
        final PreparedStatement ps = getStatementDelete();
        ps.setString(1, uuid.toString());
        return ps.executeUpdate();
    }

    @Nullable WhitelistInfo query(@NotNull UUID uuid) throws SQLException {
        final PreparedStatement ps = this.getStatementQuery();
        ps.setString(1, uuid.toString());
        final ResultSet resultSet = ps.executeQuery();
        return this.parseOne(resultSet);
    }


    @Override
    public @NotNull WhitelistInfo parseRow(@NotNull ResultSet resultSet) throws SQLException {
        // name uuid remark ctime
        final String name = resultSet.getString(1);
        final String uuidStr = resultSet.getString(2);
        final String remark = resultSet.getString(3);
        final long c_time = resultSet.getLong(4);

        return new WhitelistInfo(
                name,
                UUID.fromString(uuidStr),
                remark,
                c_time
        );
    }
}
