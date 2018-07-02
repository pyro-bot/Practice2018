package cc.tantibus.practice2018_5f5dd93;

import javafx.application.Application;
import org.apache.commons.cli.*;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;


/**
 * 0da - 29.06.2018.
 */
public class Cmd {
    public static void main(String[] args) {

        Option tileWidthOption = Option.builder("tw")
                .longOpt("tile-width")
                .hasArg(true)
                .desc("width of cut tiles")
                .argName("int")
                .type(Number.class)
                .required(true)
                .build();

        Option tileHeightOption = Option.builder("th")
                .longOpt("tile-height")
                .hasArg(true)
                .desc("height of cut tiles")
                .argName("int")
                .type(Number.class)
                .required(true)
                .build();

        Option chunkWidthOption = Option.builder("cw")
                .longOpt("chunk-width")
                .hasArg(true)
                .desc("width of the territory processed at one time")
                .argName("int")
                .type(Number.class)
                .required(true)
                .build();

        Option chunkHeightOption = Option.builder("ch")
                .longOpt("chunk-height")
                .hasArg(true)
                .desc("height of the territory processed at one time")
                .argName("int")
                .type(Number.class)
                .required(true)
                .build();

        Option fileOption = Option.builder("f")
                .longOpt("file")
                .hasArg(true)
                .desc("path to image [this argument can be used more then one time]")
                .argName("path")
                .build();

        Option fileChooserOption = Option.builder("fc")
                .longOpt("file-chooser")
                .desc("open file chooser")
                .build();

        Option pixelArrayOption = Option.builder("p")
                .longOpt("pixel-array")
                .desc("save tiles as pixel array[r, g, b, a, r1, g1, b1, ...][unsigned byte]")
                .build();

        Option helpOption = Option.builder("h")
                .longOpt("help")
                .desc("print this message")
                .argName("path")
                .build();

        Options options = new Options();

        options.addOption(tileWidthOption);
        options.addOption(tileHeightOption);
        options.addOption(chunkWidthOption);
        options.addOption(chunkHeightOption);
        options.addOption(fileOption);
        options.addOption(fileChooserOption);
        options.addOption(pixelArrayOption);
        options.addOption(helpOption);


        DefaultParser defaultParser = new DefaultParser();
        HelpFormatter helpFormatter = new HelpFormatter();

        try {
            CommandLine commandLine = defaultParser.parse(options, args);
            if (commandLine.hasOption('h')) {
                helpFormatter.printHelp("ImageCutter", options);
                return;
            }

            if (commandLine.hasOption('p')) DB.INSTANCE.setSaveFormatToPixelArray(true);

            int tileWidth = ((Number) commandLine.getParsedOptionValue("tile-width")).intValue();
            int tileHeight = ((Number) commandLine.getParsedOptionValue("tile-height")).intValue();
            int chunkWidth = ((Number) commandLine.getParsedOptionValue("chunk-width")).intValue();
            int chunkHeight = ((Number) commandLine.getParsedOptionValue("chunk-height")).intValue();


            ArrayList<File> files = new ArrayList<>();
            if (commandLine.hasOption("fc")) {
                Application.launch(FileChooserDialog.class);
                files.addAll(FileChooserDialog.files);
            }


            String[] fs = commandLine.getOptionValues('f');
            if (fs != null) for (String path : fs) files.add(new File(path));

            System.out.println("===============================");
            for (File file : files) ImageLoader.load(tileWidth, tileHeight, chunkWidth, chunkHeight, file);

        } catch (ParseException e) {
            System.out.println(e.getMessage());
            helpFormatter.printHelp("ImageCutter", options);
        }


        try {
            DB.INSTANCE.init();
        } catch (SQLException e) {
            e.printStackTrace();
            return;
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
}
