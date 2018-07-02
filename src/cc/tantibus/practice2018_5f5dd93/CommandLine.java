package cc.tantibus.practice2018_5f5dd93;

import javafx.application.Application;

import java.io.File;
import java.sql.SQLException;

/**
 * 0da - 29.06.2018.
 */
public class CommandLine {
    public static void main(String[] args) {
        if (args.length < 5) explain();
        int tileWidth;
        int tileHeight;
        int chunkWidth;
        int chunkHeight;

        try {
            tileWidth = Integer.parseInt(args[0]);
            tileHeight = Integer.parseInt(args[1]);
            chunkWidth = Integer.parseInt(args[2]);
            chunkHeight = Integer.parseInt(args[3]);
        } catch (Exception e) {
            e.printStackTrace();
            explain();
            return;
        }


        try {
            DB.INSTANCE.init();
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        System.out.println("\n===============================");

        if (args[4].equals("-file_chooser")) {
            Application.launch(FileChooserDialog.class);
            for (File file : FileChooserDialog.files) {
                ImageLoader.load(tileWidth, tileHeight, chunkWidth, chunkHeight, file.getPath());
            }

        } else {
            for (int i = 4; i < args.length; i++) {
                ImageLoader.load(tileWidth, tileHeight, chunkWidth, chunkHeight, args[i]);
            }
        }

        new Thread(ImageLoader.queueThread()).start();

        boolean x = true;
        int z = 0;
        int q = 0;

        while (ImageLoader.queue() + ImageLoader.saves() > 0 || ImageLoader.image()) {
            if (q != ImageLoader.queue() || x != ImageLoader.image() || z != ImageLoader.saves()) {
                q = ImageLoader.queue();
                x = ImageLoader.image();
                z = ImageLoader.saves();
                System.out.println("\n===============================");
                System.out.println("Image in queue:   " + q);
                System.out.println("Image in process: " + x);
                System.out.println("Tiles to save   : " + z);
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        DB.INSTANCE.close();
        System.out.println("\n=========== SUCCESS ===========");
    }

    private static void explain() {
        System.out.println("===============================");
        System.out.println("Params:");
        System.out.println("1: Tile width");
        System.out.println("2: Tile height");
        System.out.println("3: Chunk width");
        System.out.println("4: Chunk height");
        System.out.println("5: -file_chooser");
        System.out.println("    or          ");
        System.out.println("5..n: Image paths");
        System.out.println("===============================");
        System.exit(0);
    }
}
