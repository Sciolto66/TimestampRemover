package nl.rowendu.unstamp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileProcessingTask extends Task<FileProcessingTask.ProcessingResult> {
  private static final Logger logger = LoggerFactory.getLogger(FileProcessingTask.class);

  private final File file;
  private final Pattern pattern;
  private final AtomicBoolean cancelled;

  public FileProcessingTask(File file, boolean startOfLineOnly, AtomicBoolean cancelled) {
    this.file = file;
    this.pattern = TimestampPatternUtil.getPattern(startOfLineOnly);
    this.cancelled = cancelled;

    logger.info("Using pattern: {}", pattern.pattern());
  }

  @Override
  protected ProcessingResult call() throws Exception {
    updateMessage("Analyzing file...");

    long totalLines = countTotalLines();
    if (cancelled.get()) return new ProcessingResult(false, 0, 0);

    return processFileContent(totalLines);
  }

  private long countTotalLines() throws IOException {
    try (var lines = Files.lines(file.toPath())) {
      long count = lines.count();
      logger.info("Total lines to process in {}: {}", file.getName(), count);
      return count;
    }
  }

  private ProcessingResult processFileContent(long totalLines) throws IOException {
    updateMessage("Processing file...");

    int maxCycles = 3;
    int currentCycle = 0;
    long totalProcessedLines = 0;
    long totalModifiedLines = 0;
    boolean madeChanges;
    File currentFile = file;

    do {
      currentCycle++;
      updateMessage(String.format("Processing cycle %d/%d...", currentCycle, maxCycles));

      File tempFile = new File(file.getParent(), file.getName() + ".tmp");
      ProcessCycleResult cycleResult = processCycle(currentFile, tempFile, totalLines, currentCycle, maxCycles);

      madeChanges = cycleResult.modifiedLines > 0;
      totalProcessedLines += cycleResult.processedLines;
      totalModifiedLines += cycleResult.modifiedLines;

      if (cancelled.get()) {
        cleanupTempFile(tempFile);
        updateProgress(1, 1);
        return new ProcessingResult(totalModifiedLines > 0, totalProcessedLines, totalModifiedLines);
      }

      if (madeChanges) {
        // Replace original with temp file
        if (currentCycle > 1) {
          Files.delete(currentFile.toPath());
        }
        Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        logger.info(
            "Cycle {} completed: processed {} lines, modified {} lines",
            currentCycle,
            cycleResult.processedLines,
            cycleResult.modifiedLines);
      } else {
        cleanupTempFile(tempFile);
        logger.info("Cycle {} completed. No changes made.", currentCycle);
      }
    } while (madeChanges && currentCycle < maxCycles && !cancelled.get());

    updateProgress(1, 1);

    logger.info(
        "Processing completed after {} cycles. Total modified lines: {}",
        currentCycle,
        totalModifiedLines);
    return new ProcessingResult(totalModifiedLines > 0, totalProcessedLines, totalModifiedLines);
  }

  private ProcessCycleResult processCycle(File inputFile, File outputFile, long totalLines,
                                         int currentCycle, int maxCycles) throws IOException {
    long modifiedLines = 0;
    long processedLines = 0;

    try (var reader = Files.newBufferedReader(inputFile.toPath());
         var writer = Files.newBufferedWriter(outputFile.toPath())) {

      String line;
      while ((line = reader.readLine()) != null) {
        if (cancelled.get()) {
          return new ProcessCycleResult(processedLines, modifiedLines);
        }

        LineProcessingResult result = processLine(line);
        writer.write(result.cleanedLine);
        writer.write(System.lineSeparator());

        if (result.wasModified) {
          modifiedLines++;
        }

        processedLines++;
        updateProgressBar(processedLines, totalLines, currentCycle, maxCycles, modifiedLines > 0);
      }
    }

    return new ProcessCycleResult(processedLines, modifiedLines);
  }

  private void updateProgressBar(long processedLines, long totalLines, int currentCycle,
                               int maxCycles, boolean madeChanges) {
    double cycleProgress = (double) processedLines / totalLines;
    double overallProgress = (currentCycle - 1 + cycleProgress) /
                           Math.min(currentCycle + (madeChanges ? 1 : 0), maxCycles);
    updateProgress(overallProgress, 1.0);
  }

  private void cleanupTempFile(File tempFile) {
    if (tempFile.exists()) {
      try {
        Files.delete(tempFile.toPath());
        logger.debug("Temporary file deleted: {}", tempFile.getAbsolutePath());
      } catch (IOException e) {
        logger.warn("Failed to delete temporary file: {}", tempFile.getAbsolutePath(), e);
      }
    }
  }

  private LineProcessingResult processLine(String line) {
    String cleanedLine = line;
    boolean wasModified = false;

    if (pattern.matcher(line).find()) {
      String modified = pattern.matcher(line).replaceAll("");
      if (!modified.equals(line)) {
        cleanedLine = modified;
        wasModified = true;
        logLineModification(line, cleanedLine);
      }
    }

    return new LineProcessingResult(cleanedLine, wasModified);
  }

  private void logLineModification(String original, String modified) {
    logger.debug("Line modified:");
    logger.debug("  Before: '{}'", original);
    logger.debug("  After:  '{}'", modified);
  }

  @Override
  protected void succeeded() {
    ProcessingResult result = getValue();
    Platform.runLater(() -> updateUIOnSuccess(result));
  }

  private void updateUIOnSuccess(ProcessingResult result) {
    if (result.filesChanged) {
      updateMessage(String.format(
          "Status: Completed - %s (%d/%d lines modified)",
          file.getName(), result.modifiedLines, result.processedLines));
    } else {
      updateMessage(String.format("Status: No changes needed - %s", file.getName()));
    }
  }

  @Override
  protected void failed() {
    Platform.runLater(() -> handleFailure(getException()));
  }

  private void handleFailure(Throwable e) {
    updateMessage("Status: Failed");
    logger.error("Task failed", e);
    showErrorDialog(e);
  }

  private void showErrorDialog(Throwable e) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle("Error");
    alert.setHeaderText(null);
    alert.setContentText("An error occurred: " + e.getMessage());
    alert.showAndWait();
  }

  @Override
  protected void cancelled() {
    Platform.runLater(() -> updateMessage("Status: Cancelled"));
  }

  public record ProcessingResult(boolean filesChanged, long processedLines, long modifiedLines) {}

  private record ProcessCycleResult(long processedLines, long modifiedLines) {}

  private record LineProcessingResult(String cleanedLine, boolean wasModified) {}
}