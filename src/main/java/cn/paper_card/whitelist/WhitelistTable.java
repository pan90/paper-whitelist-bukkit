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

class WhitelistTable extends Parser<WhitelistInfo> {

    private final @NotNull Connection connection;
    private final @NotNull String name;

    private PreparedStatement psInsert = null;

    private PreparedStatement psDelete = null;

    private PreparedStatement psQuery = null;

    WhitelistTable(@NotNull Connection connection, @NotNull String name) throws SQLException {
        this.connection = connection;
        this.name = name;
        this.create();
    }

    private void create() throws SQLException {
        Util.executeSQL(this.connection, """
                CREATE TABLE IF NOT EXISTS %s
                (
                    uid1   BIGINT        NOT NULL,
                    uid2   BIGINT        NOT NULL,
                    remark VARCHAR(1024) NOT NULL,
                    c_time BIGINT        NOT NULL,
                    PRIMARY KEY (uid1, uid2)
                );""".formatted(this.name));
    }

    void close() throws SQLException {
        Util.closeAllStatements(this.getClass(), this);
    }

    private @NotNull PreparedStatement getPsInsert() throws SQLException {
        if (this.psInsert == null) {
            this.psInsert = this.connection.prepareStatement("""
                    INSERT INTO %s (uid1, uid2, remark, c_time)
                    VALUES (?, ?, ?, ?);""".formatted(this.name));
        }
        return this.psInsert;
    }

    private @NotNull PreparedStatement getPsDelete() throws SQLException {
        if (this.psDelete == null) {
            this.psDelete = this.connection.prepareStatement("""
                    DELETE
                    FROM %s
                    WHERE (uid1, uid2) = (?, ?)
                    LIMIT 1;""".formatted(this.name));
        }
        return this.psDelete;
    }

    private @NotNull PreparedStatement getPsQuery() throws SQLException {
        if (this.psQuery == null) {
            this.psQuery = this.connection.prepareStatement("""
                    SELECT uid1, uid2, remark, c_time
                    FROM %s
                    WHERE (uid1, uid2) = (?, ?);""".formatted(this.name));
        }
        return this.psQuery;
    }

    @Nullable WhitelistInfo query(@NotNull UUID userId) throws SQLException {
        final PreparedStatement ps = this.getPsQuery();
        ps.setLong(1, userId.getMostSignificantBits());
        ps.setLong(2, userId.getLeastSignificantBits());
        final ResultSet resultSet = ps.executeQuery();
        return this.parseOne(resultSet);
    }

    int delete(@NotNull UUID userId) throws SQLException {
        final PreparedStatement ps = this.getPsDelete();
        ps.setLong(1, userId.getMostSignificantBits());
        ps.setLong(2, userId.getLeastSignificantBits());
        return ps.executeUpdate();
    }

    int insert(@NotNull WhitelistInfo info) throws SQLException {
        final PreparedStatement ps = this.getPsInsert();
        ps.setLong(1, info.userId().getMostSignificantBits());
        ps.setLong(2, info.userId().getLeastSignificantBits());
        ps.setString(3, info.remark());
        ps.setLong(4, info.createTime());
        return ps.executeUpdate();
    }

    @Override
    public @NotNull WhitelistInfo parseRow(@NotNull ResultSet resultSet) throws SQLException {
        final long uid1 = resultSet.getLong(1);
        final long uid2 = resultSet.getLong(2);
        final String remark = resultSet.getString(3);
        final long cTime = resultSet.getLong(4);
        return new WhitelistInfo(new UUID(uid1, uid2), remark, cTime);
    }
}
