package wuliu_pics.common;

import org.jdbi.v3.core.Jdbi;

public class DB {
    public final String path;
    public final Jdbi jdbi;

    public DB(String dbPath) {
        this.path = dbPath;
        this.jdbi = Jdbi.create("jdbc:sqlite:" + dbPath);
    }

    public void createTables() {
        this.jdbi.useHandle(
                handle -> handle.createScript(Stmt.CREATE_TABLES).execute());
    }
}
