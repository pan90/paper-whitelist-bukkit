package cn.paper_card.whitelist;

import cn.paper_card.database.api.Parser;
import cn.paper_card.database.api.Util;
import cn.paper_card.paper_whitelist.api.WhitelistCodeInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

class WhitelistCodeTable extends Parser<WhitelistCodeInfo> {


    private PreparedStatement statementInsert = null;

    private PreparedStatement statementUpdateByUuid = null;

    private PreparedStatement statementQueryByCode = null;

    private PreparedStatement statementDeleteByCode = null;

    private PreparedStatement statementDeleteTimeBefore = null;

    private PreparedStatement statementQueryByUuid = null;

    private final @NotNull String name;

    private final @NotNull Connection connection;


    WhitelistCodeTable(@NotNull Connection connection, @NotNull String name) throws SQLException {
        this.connection = connection;
        this.name = name;
        this.createTable(connection);
    }

    private void createTable(@NotNull Connection connection) throws SQLException {
        final String sql2 = """
                CREATE TABLE IF NOT EXISTS %s (
                    code    INT UNIQUE NOT NULL,
                    uid1    BIGINT NOT NULL,
                    uid2    BIGINT NOT NULL,
                    name    VARCHAR(64) NOT NULL,
                    c_time  BIGINT NOT NULL,
                    expires BIGINT NOT NULL,
                    PRIMARY KEY(uid1, uid2)
                )""".formatted(this.name);
        Util.executeSQL(connection, sql2);
    }

    void close() throws SQLException {
        Util.closeAllStatements(this.getClass(), this);
    }

    private @NotNull PreparedStatement getStatementInsert() throws SQLException {
        if (this.statementInsert == null) {
            this.statementInsert = this.connection.prepareStatement("""
                    INSERT INTO %s (code, uid1, uid2, name, c_time, expires) VALUES (?, ?, ?, ?, ?, ?)
                    """.formatted(this.name));
        }
        return this.statementInsert;
    }


    private @NotNull PreparedStatement getStatementUpdateByUuid() throws SQLException {
        if (this.statementUpdateByUuid == null) {
            this.statementUpdateByUuid = this.connection.prepareStatement("""
                    UPDATE %s SET code=?, name=?, c_time=?, expires=? WHERE uid1=? AND uid2=? LIMIT 1
                    """.formatted(this.name));
        }

        return this.statementUpdateByUuid;
    }

    private @NotNull PreparedStatement getStatementQueryByCode() throws SQLException {
        if (this.statementQueryByCode == null) {
            this.statementQueryByCode = this.connection.prepareStatement
                    ("SELECT code, uid1, uid2, name, c_time, expires FROM %s WHERE code=? LIMIT 1".formatted(this.name));
        }
        return this.statementQueryByCode;
    }

    private @NotNull PreparedStatement getStatementDeleteByCode() throws SQLException {
        if (this.statementDeleteByCode == null) {
            this.statementDeleteByCode = this.connection.prepareStatement
                    ("DELETE FROM %s WHERE code=? LIMIT 1".formatted(this.name));
        }
        return this.statementDeleteByCode;
    }

    private @NotNull PreparedStatement getStatementDeleteTimeBefore() throws SQLException {
        if (this.statementDeleteTimeBefore == null) {
            this.statementDeleteTimeBefore = this.connection.prepareStatement
                    ("DELETE FROM %s WHERE expires<?".formatted(this.name));
        }
        return this.statementDeleteTimeBefore;
    }

    private @NotNull PreparedStatement getStatementQueryByUuid() throws SQLException {
        if (this.statementQueryByUuid == null) {
            this.statementQueryByUuid = this.connection.prepareStatement
                    ("SELECT code, uid1, uid2, name, c_time, expires FROM %s WHERE uid1=? AND uid2=? LIMIT 1".formatted(this.name));
        }
        return this.statementQueryByUuid;
    }

    int insert(@NotNull WhitelistCodeInfo info) throws SQLException {
        final PreparedStatement ps = this.getStatementInsert();
        ps.setInt(1, info.code());
        ps.setLong(2, info.id().getMostSignificantBits());
        ps.setLong(3, info.id().getLeastSignificantBits());
        ps.setString(4, info.name());
        ps.setLong(5, info.createTime());
        ps.setLong(6, info.expires());
        return ps.executeUpdate();
    }

    int updateByUuid(@NotNull WhitelistCodeInfo info) throws SQLException {
        final PreparedStatement ps = this.getStatementUpdateByUuid();
        ps.setInt(1, info.code());
        ps.setString(2, info.name());
        ps.setLong(3, info.createTime());
        ps.setLong(4, info.expires());
        ps.setLong(5, info.id().getMostSignificantBits());
        ps.setLong(6, info.id().getLeastSignificantBits());
        return ps.executeUpdate();
    }

    @Override
    public @NotNull WhitelistCodeInfo parseRow(@NotNull ResultSet resultSet) throws SQLException {
        final int code = resultSet.getInt(1);
        final long uid1 = resultSet.getLong(2);
        final long uid2 = resultSet.getLong(3);
        final String name = resultSet.getString(4);
        final long createTime = resultSet.getLong(5);
        final long expires = resultSet.getLong(6);
        return new WhitelistCodeInfo(code, new UUID(uid1, uid2), name, createTime, expires);
    }

    @Nullable WhitelistCodeInfo queryByCode(int code) throws SQLException {
        final PreparedStatement ps = this.getStatementQueryByCode();
        ps.setInt(1, code);
        final ResultSet resultSet = ps.executeQuery();
        return this.parseOne(resultSet);
    }

    @Nullable WhitelistCodeInfo queryByUuid(@NotNull UUID uuid) throws SQLException {
        final PreparedStatement ps = this.getStatementQueryByUuid();
        ps.setLong(1, uuid.getMostSignificantBits());
        ps.setLong(2, uuid.getLeastSignificantBits());
        final ResultSet resultSet = ps.executeQuery();
        return this.parseOne(resultSet);
    }

    int deleteByCode(int code) throws SQLException {
        final PreparedStatement ps = this.getStatementDeleteByCode();
        ps.setInt(1, code);
        return ps.executeUpdate();
    }

    int deleteTimeBefore(long time) throws SQLException {
        final PreparedStatement ps = this.getStatementDeleteTimeBefore();
        ps.setLong(1, time);
        return ps.executeUpdate();
    }
}
