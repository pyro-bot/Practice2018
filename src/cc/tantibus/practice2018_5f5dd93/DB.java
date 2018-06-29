package cc.tantibus.practice2018_5f5dd93;

import com.objectplanet.image.PngEncoder;

import java.awt.image.BufferedImage;

import java.io.*;
import java.sql.*;
import java.util.Collection;

/**
 * 0da - 07.06.2018.
 */
public enum DB implements Closeable {
    INSTANCE;
    private static final String DB_URL = "jdbc:sqlite:./db.sqlite3";
    private static final String INIT_TABLE = "" +
            "CREATE TABLE IF NOT EXISTS IMG\n" +
            "(\n" +
            "  ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,\n" +
            "  CONTENT BLOB NOT NULL,\n" +
            "  VALUE BOOLEAN\n" +
            ");";
    private static final String PUT_IMAGE = "INSERT INTO IMG(CONTENT) VALUES (?)";


    private Connection connection;

    public void init() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        connection = DriverManager.getConnection(DB_URL);

        try (Statement statement = connection.createStatement()) {
            statement.execute(INIT_TABLE);
        }

        connection.setAutoCommit(false);
    }

    Runnable putImage(Collection<BufferedImage> images) {
        return () -> {
            try {
                PreparedStatement putImageStatement = connection.prepareStatement(PUT_IMAGE);
                PngEncoder pngEncoder = new PngEncoder();
                for (BufferedImage image : images) {
                    try {
                        /*
                        if (ImageIO.getUseCache()) ImageIO.setUseCache(false);
                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        ImageIO.write(image, "png", os);
                        putImageStatement.setBytes(1, os.toByteArray());
                        */

                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        pngEncoder.encode(image, os);
                        putImageStatement.setBytes(1, os.toByteArray());

                        putImageStatement.addBatch();

                    } catch (SQLException | IOException e) {
                        e.printStackTrace();
                    }
                }
                putImageStatement.executeBatch();
                synchronized (DB.INSTANCE) {
                    connection.commit();
                }
                putImageStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };
    }


    @Override
    public void close() {
        synchronized (DB.INSTANCE) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
