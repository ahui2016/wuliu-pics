package wuliu_pics.common;

public class Stmt {
    public static final String CREATE_TABLES = """
        CREATE TABLE IF NOT EXISTS simplemeta
        (
          id         TEXT   PRIMARY KEY COLLATE NOCASE,
          filename   TEXT   NOT NULL UNIQUE,
          checksum   TEXT   NOT NULL,
          size       INT    NOT NULL,
          type       TEXT   NOT NULL,
          like       INT    NOT NULL,
          label      TEXT   NOT NULL,
          notes      TEXT   NOT NULL,
          ctime      TEXT   NOT NULL,
          utime      TEXT   NOT NULL
        );
        
        CREATE INDEX IF NOT EXISTS idx_simplemeta_checksum   ON simplemeta(checksum);
        CREATE INDEX IF NOT EXISTS idx_simplemeta_size       ON simplemeta(size);
        CREATE INDEX IF NOT EXISTS idx_simplemeta_label      ON simplemeta(label);
        CREATE INDEX IF NOT EXISTS idx_simplemeta_notes      ON simplemeta(notes);
        CREATE INDEX IF NOT EXISTS idx_simplemeta_ctime      ON simplemeta(ctime);
        CREATE INDEX IF NOT EXISTS idx_simplemeta_utime      ON simplemeta(utime);
        
        CREATE TABLE IF NOT EXISTS file_checked
        (
          id         TEXT   PRIMARY KEY COLLATE NOCASE,
          checked    TEXT   NOT NULL,
          damaged    INT    NOT NULL
        );

        CREATE INDEX IF NOT EXISTS idx_file_checked_checked ON file_checked(checked);
        """;

}
