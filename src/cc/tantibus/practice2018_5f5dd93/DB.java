package cc.tantibus.practice2018_5f5dd93;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import java.io.*;
import java.sql.*;

/**
 * 0da - 07.06.2018.
 */
public class DB implements Closeable {
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
    private static final String GET_FEVER_VOTES = "SELECT * FROM PUBLIC.IMG ORDER BY Y + N ASC";


    private static final String CONTENT = "CONTENT";
    private static final String YES = "Y";
    private static final String NO = "N";


    private Connection connection;
    private PreparedStatement putImageStatement;
    private PreparedStatement getFewerVotesImageStatement;
    private ResultSet currentSet;

    public DB() {
        org.h2.Driver.load();
        try {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage());
        }

        try (CallableStatement statement = connection.prepareCall(INIT_TABLE)) {
            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage());
        }

        try {
            putImageStatement = connection.prepareStatement(PUT_IMAGE);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage());
        }
        try {
            getFewerVotesImageStatement = connection.prepareStatement(GET_FEVER_VOTES, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage());
        }

        try {
            next();
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage());
        }

    }


    public void putImage(BufferedImage image) throws IOException, SQLException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(image, "png", os);
        InputStream is = new ByteArrayInputStream(os.toByteArray());

        putImageStatement.setBinaryStream(1, is);
        putImageStatement.execute();

    }

    public BufferedImage getImage() throws IOException, SQLException {
        InputStream content = currentSet.getBinaryStream(CONTENT);
        return ImageIO.read(content);

    }

    public void next(boolean good) throws SQLException {
        if (good) {
            currentSet.updateInt(YES, currentSet.getInt(YES) + 1);
        } else {
            currentSet.updateInt(NO, currentSet.getInt(NO) + 1);
        }
        next();
    }


    private void next() throws SQLException {
        if (currentSet != null) {
            currentSet.updateRow();
            connection.commit();
        }
        currentSet = getFewerVotesImageStatement.executeQuery();
        currentSet.first();
    }


    @Override
    public void close() throws IOException {
        try {
            putImageStatement.close();
            getFewerVotesImageStatement.close();
            connection.close();
        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        }
    }
}
