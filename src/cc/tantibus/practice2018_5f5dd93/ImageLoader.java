package cc.tantibus.practice2018_5f5dd93;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.concurrent.*;


/**
 * 0da - 07.06.2018.
 */
public final class ImageLoader extends Thread {

    private static Deque<ImageLoader> deque = new ArrayDeque<>();

    private static boolean imageInProcess = false;
    private static int savesCount = 0;

    private static synchronized void setImageInProcess(boolean imageInProcess) {
        ImageLoader.imageInProcess = imageInProcess;
    }

    private static synchronized void addSave(int count) { savesCount += count; }

    private static synchronized void saveDone(int count) { savesCount -= count; }

    public static int queue() { return deque.size(); }

    public static boolean image() { return imageInProcess; }

    public static int saves() { return savesCount; }

    private final ExecutorService service;

    private ImageInputStream stream;
    private ImageReadParam param;
    private ImageReader reader;

    private int width;
    private int height;

    private int chunkHeight;
    private int chunkWidth;

    private int sourceWidth;
    private int sourceHeight;

    static Runnable queueThread() {
        return () -> {
            while (deque.size() > 0) {
                ImageLoader temp = deque.poll();
                if (temp == null) continue;
                // TODO: 02.07.2018 Better memory management

                long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

                while (Runtime.getRuntime().maxMemory() < usedMemory * 2 || image()) {
                    usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                    try {
                        TimeUnit.SECONDS.sleep(5);
                        System.gc();
                        System.out.print('*');
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                setImageInProcess(true);
                temp.start();
            }
        };
    }

    private ImageLoader(File file, int width, int height, int chunkWidth, int chunkHeight) throws IOException {
        super("ImageLoader");
        this.service = Executors.newFixedThreadPool(5);
        this.width = width;
        this.height = height;
        this.chunkWidth = chunkWidth;
        this.chunkHeight = chunkHeight;

        /////////////////////////////////////////////////////////////////////////////////////
        this.stream = ImageIO.createImageInputStream(file);

        this.reader = ImageIO.getImageReaders(stream).next();
        this.reader.setInput(stream, false, true);

        this.sourceWidth = reader.getWidth(0);
        this.sourceHeight = reader.getHeight(0);
        this.param = reader.getDefaultReadParam();
        /////////////////////////////////////////////////////////////////////////////////////

    }


    public static void load(int width, int height, int chunkWidth, int chunkHeight, File file) {
        if (file.exists()) {
            try {
                deque.add(new ImageLoader(file, width, height, chunkWidth, chunkHeight));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else System.out.println("File does not exist: " + file.getAbsoluteFile());
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
                    cutImage(result, width, height);

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


        setImageInProcess(false);
        service.shutdown();
    }


    private void cutImage(BufferedImage source, int width, int height) {
        ArrayList<BufferedImage> result = new ArrayList<>();

        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();

        width = Math.min(width, sourceWidth);
        height = Math.min(height, sourceHeight);

        for (int i = 0; i < sourceWidth; i += width) {
            if (i + width > sourceWidth) {
                i = sourceWidth - width;
            }
            for (int j = 0; j < sourceHeight; j += height) {
                if (j + height > sourceHeight) {
                    j = sourceHeight - height;
                }
                result.add(source.getSubimage(i, j, width, height));

                if (result.size() >= 10) {
                    save(result);
                    result = new ArrayList<>();
                }
            }
        }

        save(result);
    }

    private void save(ArrayList<BufferedImage> list) {
        int size = list.size();
        addSave(size);
        Runnable runnable = DB.INSTANCE.putImage(list);
        CompletableFuture<Void> voidCompletableFuture = CompletableFuture.runAsync(runnable, service);
        voidCompletableFuture.thenAccept(v -> saveDone(size));

    }
}
