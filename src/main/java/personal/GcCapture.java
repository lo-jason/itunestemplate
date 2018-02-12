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
import javafx.scene.layout.Region;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
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
  private static String static_content;
  private Runnable draw;

  @Override
  public void start(Stage stage) {
    final Browser browser = new Browser("file");

    stage.setTitle("iTunes");
    scene = new Scene(browser, 800, 580);
    stage.setScene(scene);

    List<String> array = Arrays.asList(new String[10]);

    this.draw = new Runnable() {
      private AtomicInteger count = new AtomicInteger(0);

      @Override
      public void run() {
        int pos = count.getAndIncrement();
        if (pos >= array.size()) {
          stage.close();
          browser.close();
          return;
        }
        StringBuilder content = new StringBuilder(static_content);
        // replace CODE first as that will change length of content (value is before code in html)
        int i_code = content.indexOf(CODE);
        int i_value = content.indexOf(VALUE);

        content.replace(i_code, i_code + CODE.length(), "LDKFJLSKJFLSKDFJL" + pos);
        System.out.println("Printed out: " + pos);
        content.replace(i_value, i_value + VALUE.length(), "100");
        browser.loadContent(content.toString());
      }
    };

    stage.show();
    browser.startListener(this);
    triggerDraw();
  }


  public void triggerDraw() {
    draw.run();
  }

  public static void main(String[] args) throws IOException {
    if (args.length >= 1 && args[0].contains("help")) {
      System.out.println("Usage: java -jar GcCapture <html template> <csv file>");
      return;
    }


    File file;
    File parentFile;
    if (args.length >= 1) {
      file = new File(args[0]);
      parentFile = file.getParentFile();
    } else {
      file = new File(PPDG_TEMPLATE_DOCX_HTML);
      parentFile = file.getParentFile();
    }

    BufferedReader reader = new BufferedReader(new FileReader(file));
    StringBuilder sb = new StringBuilder();
    String line = "";
    while((line = reader.readLine()) != null) {
      sb.append(line);
    }

    int i;
    while ((i = sb.indexOf(PPDG_SUBFOLDER_html)) != -1) {
      sb.replace(i + 1, i + PPDG_SUBFOLDER_html.length(), new File(parentFile, PPDG_SUBFOLDER).toURI().toString());
    }
    static_content = sb.toString();
    launch(args);
  }

  /**
   * Parses out cols of valueColName from CSV, columns are in original order
   * @param csv
   * @param valueColName list of cols we want to read
   * @return
   * @throws IOException
   */
  private static List<List<String>> parseCSV(File csv, ArrayList<String> valueColName) throws IOException {
    ArrayList<String> listOfLists = new ArrayList<>();
    try {
      BufferedReader bufferedReader = new BufferedReader(new FileReader(csv));
      List<Integer> value_indices = new ArrayList<Integer>(valueColName.size());
      String line = bufferedReader.readLine();
      {
        String[] colNames = line.split(",");
        for (int csvColumn = 0; csvColumn < colNames.length; csvColumn++) {
          String colName = colNames[csvColumn];
          for (int i = 0; i < valueColName.size(); i++) {
            if (valueColName.equals(colName.toLowerCase())) {
              value_indices.set(i, csvColumn);
              break;
            }
          }
        }
      }
      while ((line = bufferedReader.readLine()) != null) {
        String[] data = line.split(",");

        ArrayList<String> values = new ArrayList<String>();
        for (Integer i : value_indices) {
          values.add(data[i]);
        }
        listOfLists.add(values);
      }
    }
    return dataMap;
  }
}
class Browser extends Region {

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
    this.tChangeListener = (observable, oldValue, newValue) -> {
      if (newValue == Worker.State.SUCCEEDED && started) {
        System.out.println("Take snapshot now render complete");
        Platform.runLater(() -> {
          browser.snapshot((snapImage) -> {
            ImageView snapView = new ImageView();
            snapView.setImage(snapImage.getImage());
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(snapImage.getImage(), null);
            try {
              System.out.println("Wrote image" + count.get());
              ImageIO.write(bufferedImage,
                "png",
                new File("/Users/jason.lo/Dropbox/temp/" + filePrefix + String.format("%05d", count.getAndIncrement())
                  + ".png"));
            } catch (IOException e) {
              e.printStackTrace();
            }

            /*if (debug) {
              StackPane snapLayout = new StackPane();
              snapLayout.getChildren().add(snapView);
              Scene snapScene =
                new Scene(snapLayout, snapImage.getImage().getWidth(), snapImage.getImage().getHeight());

              Stage snapStage = new Stage();
              snapStage.setTitle("Snapshot");
              snapStage.setScene(snapScene);

              snapStage.show();
            }*/
            // schedule drawing
            Platform.runLater(() -> capture.triggerDraw());
            return null;
          }, new SnapshotParameters(), null);
        });
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
