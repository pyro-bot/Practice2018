package cc.tantibus.practice2018_5f5dd93;

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

        System.out.println("===============================");

        for (int i = 4; i < args.length; i++) {
            System.out.println(i - 3 + ": " + args[i]);
            ImageLoader.load(tileWidth, tileHeight, chunkWidth, chunkHeight, args[i]);
        }

        int x = 0;
        int y = 0;
        int z = 0;
        while (ImageLoader.image() + ImageLoader.chunk() + ImageLoader.saves() > 0) {
            if (x != ImageLoader.image() || y != ImageLoader.chunk() || z != ImageLoader.saves()) {
                x = ImageLoader.image();
                y = ImageLoader.chunk();
                z = ImageLoader.saves();
                System.out.println("===============================");
                System.out.println("Image in process: " + x);
                System.out.println("Chunk in process: " + y);
                System.out.println("Tiles to save   : " + z);
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        DB.INSTANCE.close();
        System.out.println("=========== SUCCESS ===========");
    }

    private static void explain() {


        System.out.println("Params:");
        System.out.println("1: Tile width");
        System.out.println("2: Tile height");
        System.out.println("3: Chunk width");
        System.out.println("4: Chunk height");
        System.out.println("5..n: Image paths");

        System.exit(0);
    }
}
