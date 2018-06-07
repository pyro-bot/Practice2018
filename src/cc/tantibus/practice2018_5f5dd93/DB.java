package cc.tantibus.practice2018_5f5dd93;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * 0da - 07.06.2018.
 */
public enum DB {
    INSTANCE;
    private static final String DB_URL = "jdbc:h2:./db";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "root";
    private static final String INIT_TABLE = "" +
            "CREATE TABLE IF NOT EXISTS PUBLIC.IMG\n" +
            "(\n" +
            "    ID INT AUTO_INCREMENT PRIMARY KEY NOT NULL,\n" +
            "    CONTENT BLOB,\n" +
            "    Y INT NOT NULL,\n" +
            "    N INT NOT NULL" +
            ");";
    private static final String PUT_IMAGE = "INSERT INTO PUBLIC.IMG(CONTENT,Y,N) VALUES (?,0,0)";
    private static final String GET_FEVER_VOTES = "SELECT * FROM PUBLIC.IMG ORDER BY Y + N ASC LIMIT 1";


    private static final String CONTENT = "CONTENT";
    private static final String YES = "Y";
    private static final String NO = "N";

    private boolean contentExist = false;

    private Connection connection;
    private PreparedStatement getFewerVotesImageStatement;
    private ResultSet currentSet;


    public void init() throws SQLException {
        org.h2.Driver.load();

        connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);

        try (CallableStatement statement = connection.prepareCall(INIT_TABLE)) {
            statement.execute();
        }

        getFewerVotesImageStatement = connection.prepareStatement(GET_FEVER_VOTES, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);

        next();

    }

    Runnable putImage(BufferedImage image) throws IOException, SQLException {
        return putImage(Collections.singleton(image));
    }

    Runnable putImage(Collection<BufferedImage> images) throws IOException, SQLException {
        return () -> {
            try {
                CallableStatement putImageStatement;
                synchronized (this) {
                    putImageStatement = connection.prepareCall(PUT_IMAGE);
                }
                for (BufferedImage image : images) {
                    try {
                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        ImageIO.write(image, "png", os);
                        InputStream is = new ByteArrayInputStream(os.toByteArray());
                        putImageStatement.setBinaryStream(1, is);
                        putImageStatement.addBatch();

                    } catch (IOException | SQLException e) {
                        e.printStackTrace();
                    }
                }
                synchronized (this) {
                    putImageStatement.executeBatch();
                }
                putImageStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };
    }

    public BufferedImage getImage() throws IOException, SQLException {
        if (!contentExist) return null;
        InputStream content = currentSet.getBinaryStream(CONTENT);
        return ImageIO.read(content);

    }

    public void next(boolean good) throws SQLException {
        if (contentExist)
            if (good) {
                currentSet.updateInt(YES, currentSet.getInt(YES) + 1);
            } else {
                currentSet.updateInt(NO, currentSet.getInt(NO) + 1);
            }
        next();
    }


    private void next() throws SQLException {
        if (currentSet != null && contentExist) {
            currentSet.updateRow();
            connection.commit();
        }
        currentSet = getFewerVotesImageStatement.executeQuery();
        contentExist = currentSet.first();
    }


    public void close() {
        try {
            getFewerVotesImageStatement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
