# TimestampRemover

TimestampRemover is a Java application designed to clean up log files by removing timestamp patterns. The tool processes text files line by line, using regular expressions to identify and remove timestamps.

## Features

- Removes timestamp patterns from log files
- Uses multiple processing cycles for thorough timestamp removal
- Provides detailed progress tracking
- Offers targeted timestamp removal configuration
- Creates a detailed log of processing activities

## How It Works

The application processes files in multiple cycles (up to 3):
1. First cycle removes obvious timestamps
2. Second cycle removes any timestamps revealed after the first cleaning
3. Third cycle catches any remaining timestamp patterns

This multi-cycle approach ensures the thorough removal of nested or varied timestamp formats.

## Targeted Timestamp Removal Configuration

The application offers two operating modes that control how timestamps are detected and removed:

- **Start of line only mode (default)**: When enabled, the application only removes timestamps found at the beginning of lines. This is considered the safer option as it minimizes the risk of accidentally removing text that resembles timestamps but serves a different purpose in the log file.

- **Anywhere in line mode**: When disabled, the application searches for and removes timestamp patterns anywhere they appear within each line of text. This mode is more aggressive and provides thorough timestamp cleaning but carries a higher risk of removing non-timestamp data.

## How to Use

1. Launch the application
2. Configure the timestamp removal option (Start of line only is the default and safer option)
3. Click the "Select File" button to choose a file for processing
4. Monitor the processing progress in the status area
5. Once complete, the status will show a summary of modifications

> **Important:** The timestamp removal option must be configured *before* selecting a file. There is no button to restart processing with different settings after a file is selected.

## Requirements

- Java 17 or higher
- Compatible with Windows and macOS (including Apple Silicon)