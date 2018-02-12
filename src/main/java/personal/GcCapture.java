package personal;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;

public class GcCapture extends Application {
    //  public static final String IMAGES_PNG = "images/image1.png";
    //  public static final String COSTCO_ITUNES_GC_TEMPLATE_DOCX_HTML = "costco_itunes_gc_template.docx.html";
    public static final String PPDG_TEMPLATE_DOCX_HTML = "ppdg-template.html";
    public static final String PPDG_SUBFOLDER_html = "\"PayPal%20Digital%20Gifts_files";
    public static final String PPDG_SUBFOLDER = "PayPal Digital Gifts_files";
    public static final String VALUE = "{VALUE}";
    public static final String CODE = "{CODE}";

    private Scene scene;
    private Runnable draw;

    // set in from main
    private static String static_content;
    private static List<List<String>> gcToOutput;
    private static String filePrefix;

    @Override
    public void start(Stage stage) {
        final Browser browser = new Browser(filePrefix);

        stage.setTitle("iTunes");
        scene = new Scene(browser, 800, 610);
        stage.setScene(scene);

        this.draw = new Runnable() {
            private AtomicInteger count = new AtomicInteger(0);

            @Override
            public void run() {
                int pos = count.getAndIncrement();
                if (pos >= gcToOutput.size()) {
                    Timeline timeline = new Timeline(new KeyFrame(
                            Duration.millis(3000),
                            ae -> {stage.close();browser.close();}));
                    timeline.play();
//                    stage.close();
//                    browser.close();
                    return;
                }
                StringBuilder content = new StringBuilder(static_content);
                // replace CODE first as that will change length of content (value is before code in html)
                int i_code = content.indexOf(CODE);
                int i_value = content.indexOf(VALUE);

                String code = gcToOutput.get(pos).get(1);
                String value = formatCash(gcToOutput.get(pos).get(2));
                content.replace(i_code, i_code + CODE.length(), code);
                System.out.println("" + pos + " card " + code + " of value $" + value);
                content.replace(i_value, i_value + VALUE.length(), value);
                browser.loadContent(content.toString());
            }
        };

        stage.show();
        browser.startListener(this);
        Timeline timeline = new Timeline(new KeyFrame(
                Duration.millis(800),
                ae -> triggerDraw()));
        timeline.play();

    }

    private String formatCash(String s) {
        String s1 = s.trim();
        if (s1.startsWith("$")) {
            return s1.substring(1, s1.length());
        } else {
            return s1;
        }
    }

    public void triggerDraw() {
        Platform.runLater(() -> draw.run());
    }

    private static void printUsage() {
        System.out.println("Usage: java -jar gccapture-1.01-SNAPSHOT.jar <args> --csv=<csv>");
        System.out.println("Output is in current directory");
        System.out.println("");
        System.out.println("REQUIRED Arguments");
        System.out.println("--csv=<csv file>    csv file");
        System.out.println("Lower priority but will also work: java -jar gccapture <csv file>");
        System.out.println("");
        System.out.println("OPTIONAL Arguments");
        System.out.println("--help              this message");
        System.out.println("--html=<html>       if \"ppdg-template\" is not in execution directory");
        System.out.println("--value=<value>     Case-insensitive column name with value of GC, DEFAULT: Amount");
        System.out.println("--merchant=<value>  Case-insensitive column name with value of GC, DEFAULT: Merchant");
        System.out.println("                    Note: if your CSV is all iTunes can ignore this");
        System.out.println("--code=<value>      Case-insensitive column name with code of GC,  DEFAULT: Code");
        System.out.println("--prefix=<value>    <value>0001.png,  DEFAULT: card");
        System.exit(0);
    }

    public static void main(String[] args) {
        if (args.length >= 1 && args[0].contains("help")) {
            printUsage();
        }

        filePrefix = getArg(args, "file", "card");
        String csv = getArg(args, "csv", null);
        if (csv == null) {
            // try to get just plain CSV from args
            csv = getPlainCSV(args);
        }
        if (csv == null || !new File(csv).exists()) {
            System.out.println("*********CSV not found*********");
            printUsage();
        }

        StringBuilder sb = new StringBuilder();
        File file = new File(getArg(args, "html", PPDG_TEMPLATE_DOCX_HTML));
        File parentFile = file.getParentFile();

        try {
            CSVParse csvParse = new CSVParse();
            List<List<String>> lists = csvParse.parseCSV(new File(csv),
                    Arrays.asList(new String[] {
                            getArg(args, "merchant", "Merchant"),
                            getArg(args, "code", "Code"),
                            getArg(args, "value", "Amount")}));
            List<List<String>> filteredList = lists.stream()
                    .filter(e -> e.get(0) == null || e.get(0).toLowerCase().equals("itunes"))
                    .collect(Collectors.toList());

            gcToOutput = filteredList;

            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = "";
            while((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            printUsage();
        }


        int i;
        while ((i = sb.indexOf(PPDG_SUBFOLDER_html)) != -1) {
            sb.replace(i + 1, i + PPDG_SUBFOLDER_html.length(), new File(parentFile, PPDG_SUBFOLDER).toURI().toString());
        }
        static_content = sb.toString();
        launch(args);
    }

    private static String getArg(String[] args, String arg, String defaultValue) {
        for (String pair : args) {
            String[] split = pair.split("=", 2);
            if (split.length > 1) {
                if (split[0].toLowerCase().contains(arg)) {
                    return split[1];
                }
            }
        }
        return defaultValue;
    }

    private static String getPlainCSV(String[] args) {
        for (String arg : args) {
            if (!arg.contains("==") && new File(arg).exists()) {
                return arg;
            }
        }
        return null;
    }
}
class Browser extends StackPane {

    private final WebView browser = new WebView();
    private final WebEngine webEngine = browser.getEngine();
    private final AtomicInteger count = new AtomicInteger(1);
    private final String filePrefix;
    private ChangeListener<Worker.State> tChangeListener;
    private boolean started = false;

    public Browser(String filePrefix) {
        this.filePrefix = filePrefix;
        //add the web view to the scene
        getChildren().add(browser);
    }

    public void startListener(GcCapture capture) {
        final SnapshotParameters snapshotParameters = new SnapshotParameters();
        snapshotParameters.setTransform(javafx.scene.transform.Transform.scale(2, 2));
        this.tChangeListener = (observable, oldValue, newValue) -> {
            if (newValue == Worker.State.SUCCEEDED && started) {
//                System.out.println("Take snapshot now render complete");
                WritableImage writableImage = new WritableImage(1600, 1220);
                Timeline timeline = new Timeline(new KeyFrame(
                        Duration.millis(100),
                        ae -> {
                        browser.snapshot((snapImage) -> {
                        ImageView snapView = new ImageView();
                        snapView.setImage(snapImage.getImage());
                        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(snapImage.getImage(), null);
                        try {
                            int currentCount = count.getAndIncrement();
                            String fileName = String.format(filePrefix + "%04d.png", currentCount);
                            System.out.println("Wrote image" + currentCount + " to " + fileName);

                            ImageIO.write(bufferedImage,
                                    "png",
                                    new File(fileName));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

//                        if (true) {
//                            StackPane snapLayout = new StackPane();
//                            snapLayout.getChildren().add(snapView);
//                            Scene snapScene =
//                                    new Scene(snapLayout, snapImage.getImage().getWidth(), snapImage.getImage().getHeight());
//
//                            Stage snapStage = new Stage();
//                            snapStage.setTitle("Snapshot");
//                            snapStage.setScene(snapScene);
//
//                            snapStage.show();
//                        }

                        // schedule drawing
                        Platform.runLater(() -> capture.triggerDraw());
                        return null;
                    }, snapshotParameters, writableImage);
                }));
                timeline.play();
            }
        };
        webEngine.getLoadWorker().stateProperty().addListener(tChangeListener);
    }

    public void loadContent(String content) {
        this.started = true;
        webEngine.loadContent(content);
    }

    public void close() {
        webEngine.getLoadWorker().stateProperty().removeListener(tChangeListener);
        webEngine.load(null);
    }
}
