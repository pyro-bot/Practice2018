package cc.tantibus.practice2018_5f5dd93;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;


/**
 * 0da - 07.06.2018.
 */
public final class ImageLoader extends Thread {

    private static int imageCount = 0;
    private static int chunkCount = 0;
    private static int savesCount = 0;

    private static synchronized void addCount(int i) { imageCount++; chunkCount += i; savesCount += i;}

    private static synchronized void imageDone() { imageCount--; }

    private static synchronized void chunkDone() { chunkCount--; }

    private static synchronized void saveDone() { savesCount--; }

    public static int image() { return imageCount; }

    public static int chunk() { return chunkCount; }

    public static int saves() { return savesCount; }

    private ImageInputStream stream;
    private ImageReadParam param;
    private ImageReader reader;

    private int width;
    private int height;

    private int chunkHeight;
    private int chunkWidth;

    private int sourceWidth;
    private int sourceHeight;

    private ImageLoader(String path, int width, int height, int chunkWidth, int chunkHeight) throws IOException {
        super("ImageLoader");

        this.stream = ImageIO.createImageInputStream(new File(path));

        this.reader = ImageIO.getImageReaders(stream).next();
        this.reader.setInput(stream, false, true);

        this.sourceWidth = reader.getWidth(0);
        this.sourceHeight = reader.getHeight(0);
        this.param = reader.getDefaultReadParam();

        this.width = width;
        this.height = height;
        this.chunkWidth = chunkWidth;
        this.chunkHeight = chunkHeight;

        int chunkXCount = sourceWidth / chunkWidth + (sourceWidth % chunkWidth == 0 ? 0 : 1);
        int chunkYCount = sourceHeight / chunkHeight + (sourceHeight % chunkHeight == 0 ? 0 : 1);
        addCount(chunkXCount * chunkYCount);
    }

    public static void load(int width, int height, int chunkWidth, int chunkHeight, String... paths) {
        for (String path : paths) {
            try {
                new ImageLoader(path, width, height, chunkWidth, chunkHeight).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {

        for (int x = 0; x < sourceWidth; x += chunkWidth) {
            int tempW = Math.min(chunkWidth, sourceWidth - x);
            for (int y = 0; y < sourceHeight; y += chunkHeight) {
                int tempH = Math.min(chunkHeight, sourceHeight - y);
                try {

                    param.setSourceRegion(new Rectangle(x, y, tempW, tempH));
                    BufferedImage result = reader.read(0, param);
                    save(cutImage(result, width, height));
                    chunkDone();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        imageDone();
    }


    private ArrayList<BufferedImage> cutImage(BufferedImage source, int width, int height) {
        ArrayList<BufferedImage> result = new ArrayList<>();

        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();

        for (int i = 0; i < sourceWidth; i += width) {
            if (i + width > sourceWidth) {
                i = sourceWidth - width;
            }
            for (int j = 0; j < sourceHeight; j += height) {
                if (j + height > sourceHeight) {
                    j = sourceHeight - height;
                }
                result.add(source.getSubimage(i, j, width, height));
            }
        }

        return result;
    }

    private void save(ArrayList<BufferedImage> list) {
        new Thread(() -> {
            try {
                Thread thread = DB.INSTANCE.putImage(list);
                thread.start();
                thread.join();
            } catch (IOException | SQLException | InterruptedException e) {
                e.printStackTrace();
            }

            saveDone();
        }).start();

    }
}
