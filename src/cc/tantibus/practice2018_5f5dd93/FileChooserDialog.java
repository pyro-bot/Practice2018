package cc.tantibus.practice2018_5f5dd93;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 0da - 29.06.2018.
 */
public class FileChooserDialog extends Application {
    static List<File> files;

    @Override
    public void start(Stage primaryStage) {
        FileChooser chooser = new FileChooser();

        files = chooser.showOpenMultipleDialog(primaryStage);

        if (files == null) files = new ArrayList<>();

        primaryStage.close();

        Platform.exit();


    }
}
