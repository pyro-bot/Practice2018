package cc.tantibus.practice2018_5f5dd93;

import com.objectplanet.image.PngEncoder;

import java.awt.*;
import java.awt.image.BufferedImage;

import java.awt.image.DataBufferInt;
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

    private boolean pixelArray = false;

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

    public void setSaveFormatToPixelArray(boolean flag) {
        pixelArray = flag;
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
                        if (pixelArray) {
                            BufferedImage argb = convertToARGB(image);
                            DataBufferInt dataBuffer = (DataBufferInt) argb.getRaster().getDataBuffer();
                            int[] data = dataBuffer.getData();
                            byte[] bytes = new byte[data.length * 4];
                            for (int i = 0; i < data.length; i++) {
                                bytes[i] = (byte) data[i];
                                bytes[i + 0] = (byte) ((data[i] >> 16) & 0xff); // red
                                bytes[i + 1] = (byte) ((data[i] >> 8) & 0xff);  // green
                                bytes[i + 2] = (byte) ((data[i]) & 0xff);       // blue
                                bytes[i + 3] = (byte) ((data[i] >> 24) & 0xff); // alpha
                            }
                            putImageStatement.setBytes(1, bytes);
                        } else {
                            ByteArrayOutputStream os = new ByteArrayOutputStream();
                            pngEncoder.encode(image, os);
                            putImageStatement.setBytes(1, os.toByteArray());
                        }
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

    private BufferedImage convertToARGB(BufferedImage old) {
        int w = old.getWidth();
        int h = old.getHeight();
        BufferedImage argb = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = argb.createGraphics();
        g.drawImage(old, 0, 0, w, h, null);
        g.dispose();
        return argb;
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
