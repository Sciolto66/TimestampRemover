package nl.rowendu.unstamp;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimestampRemover extends Application {
  private static final Logger logger = LoggerFactory.getLogger(TimestampRemover.class);

  private final ProgressBar progressBar = new ProgressBar(0);
  private final Label statusLabel = new Label("Status: Ready");
  private final AtomicBoolean taskCancelled = new AtomicBoolean(false);
  private CheckBox startOfLineOnlyCheckbox;
  private Button cancelButton;
  private FileProcessingTask currentTask;

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) {
    VBox root = setupMainLayout();
    setupMainWindow(primaryStage, root);
    primaryStage.show();
  }

  private VBox setupMainLayout() {
    Button openButton = new Button("Open File");
    cancelButton = new Button("Cancel");
    cancelButton.setDisable(true);
    cancelButton.setOnAction(e -> cancelCurrentTask());

    startOfLineOnlyCheckbox = new CheckBox("Match only at start of line");
    startOfLineOnlyCheckbox.setSelected(true);

    HBox buttonBar = new HBox(10);
    buttonBar.setAlignment(Pos.CENTER_LEFT);
    buttonBar.getChildren().addAll(openButton, cancelButton);

    VBox root = new VBox(10);
    root.setPadding(new Insets(20));
    root.getChildren().addAll(buttonBar, startOfLineOnlyCheckbox, progressBar, statusLabel);

    setupFileChooser(openButton);

    return root;
  }

  private void setupMainWindow(Stage primaryStage, VBox root) {
    Scene scene = new Scene(root, 600, 180);
    primaryStage.setScene(scene);
    primaryStage.setTitle("Timestamp Remover");
  }

  private void setupFileChooser(Button openButton) {
    FileChooser fileChooser = createFileChooser();
    openButton.setOnAction(event -> handleFileSelection(fileChooser));
  }

  private FileChooser createFileChooser() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Open Log File");
    fileChooser
        .getExtensionFilters()
        .addAll(
            new FileChooser.ExtensionFilter("Log Files", "*.txt", "*.log"),
            new FileChooser.ExtensionFilter("All Files", "*.*"));
    return fileChooser;
  }

  private void handleFileSelection(FileChooser fileChooser) {
    File selectedFile = fileChooser.showOpenDialog(null);
    if (selectedFile != null) {
      logger.info("Processing file: {}", selectedFile.getAbsolutePath());
      processFile(selectedFile);
    }
  }

  private void cancelCurrentTask() {
    if (currentTask != null) {
      taskCancelled.set(true);
      currentTask.cancel();
      logger.info("Task cancellation requested by user");
      statusLabel.textProperty().unbind();
      statusLabel.setText("Status: Cancelled by user");
      cancelButton.setDisable(true);
    }
  }

  private void processFile(File file) {
    taskCancelled.set(false);
    boolean startOfLineOnly = startOfLineOnlyCheckbox.isSelected();

    currentTask = new FileProcessingTask(file, startOfLineOnly, taskCancelled);
    bindTaskProperties(currentTask);

    cancelButton.setDisable(false);
    startProcessingThread(currentTask);
  }

  private void bindTaskProperties(FileProcessingTask task) {
    progressBar.progressProperty().bind(task.progressProperty());
    statusLabel.textProperty().bind(task.messageProperty());
  }

  private void startProcessingThread(FileProcessingTask task) {
    Thread processingThread = new Thread(task);
    processingThread.setDaemon(true);
    processingThread.start();
  }
}