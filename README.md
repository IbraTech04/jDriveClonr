<div align="center">

# jDriveClonr

<div align="center">
<img src="src/main/resources/DriveClonrLogo.png" alt="jDriveClonr Logo" width="200"/>
</div>

> A Java port of DriveClonrPro with enhanced features and performance!


[![Java Version](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/technologies/downloads/)
[![JavaFX Version](https://img.shields.io/badge/JavaFX-21-blue.svg)](https://openjfx.io/)
</div>

## Overview

New year, new project! Introducing **jDriveClonr**, the Java port of the original Python-based DriveClonrPro! This project delivers a more efficient and user-friendly alternative to the original, with a focus on performance, ease of use, and features!

Even before implementing multithreading, jDriveClonr was already faster than the original DriveClonrPro! It also _finally_ supports the TreeView for selecting files and folders to clone, a long-awaited feature. jDriveClonr is designed to be modular and extensible, making it easier to add new features and improvements in the future.
## Features

- **Configurable Multithreading** - A long-awaited feature! Customize thread count via UI slider for optimal performance (1-10 threads)
- **TreeView Navigation** - At long last, intuitive selection of files and folders to clone! 
- **Shared Drive Support** - Full access to all your shared Google Drive content
- ~~**Google Photos Cloning** - Coming soon!~~ Google Photos cloning is no longer planned due to the changes in the Google Photos API.
- **Lazy-loading** - For a faster, lag-free UI experience
- **Improved Error Handling** - Better logging and error recovery
- **Modern JavaFX UI** - Native interface with hDPI scaling support
- **Document Format Options** - Export Google Workspace documents in various formats:
  - Google Docs → DOCX, PDF, TXT, HTML, MD
  - Google Sheets → XLSX, PDF, CSV, TSV, HTML
  - Google Slides → PPTX, PDF, TXT
  - Google Drawings → PNG, JPG, SVG, PDF
  - Google Jamboard → PDF

## Requirements and Build Instructions

### Google Drive API Credentials

> [!IMPORTANT]
> Without the `credentials.json` file, jDriveClonr **will not work**. Ensure that you have created a Google Drive API project and have the `credentials.json` file available.

This project does *not* come with the required Google Drive API credentials. You will need to create your own credentials to use this project.
Follow the instructions in the [Google Drive API documentation](https://developers.google.com/drive/api/v3/quickstart/java) to obtain your credentials.

> [!NOTE]
> jDriveClonr does not require the `.json` file to be in a specific directory. If it cannot find it, it will prompt you to select
> it from a file chooser dialog.

### System Requirements

- **Java:** JDK 21 (JDK 17+ may work but is untested)
- **JavaFX:** 21
- **Gradle** (or Gradle Wrapper)
  - If you don't have Gradle installed, you can use the Gradle Wrapper included in this project. Just run `./gradlew` (or `gradlew.bat` on Windows) instead of `gradle`.
  - This project uses Gradle 8.14.2, though *in theory* any Gradle version 8.0+ should work.
- **OS:** Windows, macOS, or Linux. Anything that's not a toaster and has an internet connection should work.
- **WiX Toolset** (for Windows builds only)
  - If you want to build the Windows installer, you will need to install the [WiX Toolset](https://wixtoolset.org/). 
  - Ensure that the WiX binaries are in your system's PATH.

### Building the Project

All dependencies and build tools are managed via Gradle, so you don't need to install anything else to build the project. The Gradle Wrapper is included in this project, so you can use `./gradlew` (or `gradlew.bat` on Windows) to run Gradle commands without needing a system-wide installation of Gradle.

To build the project, run the following command in the root directory:

```bash
gradle clean build
```

This will compile jDriveClonr and install all necessary dependencies.

### Running the Application

```bash
gradle run
```

*Again, if you do not have Gradle installed, simply substitute `gradle` with `./gradlew` (or `gradlew.bat` on Windows) in the above commands.*

## Performance Tuning

jDriveClonr now features a thread count slider in the configuration screen that allows you to customize the number of concurrent download threads (1-10).

> [!WARNING]
> **Best Practice:** It is recommended to use 4-5 threads for optimal performance. Using more than 5 threads will result in increased memory usage, and may result in rate-limiting by Google's API, among other unpredictable behavior. The application will display a warning when selecting more than 5 threads. 
jDriveClonr will attempt to automatically throttle its downloads to avoid rate-limiting, but this is mostly untested and may not work as expected.

## ❓ FAQ

<details>
<summary><b>Why is the jDriveClonr progress-bar unstable?</b></summary>
<p>jDriveClonr uses lazy-loading to improve performance and decrease memory usage. In simple terms, jDriveClonr is discovering the files as it clones them; it does not know how many files there are to clone until it's done cloning. This means that the progress bar will not be accurate until the cloning is complete. There is unfortunately no solution to this that maintains performance or speed :(</p>
</details>

<details>
<summary><b>What happened to the 'e' in Clon(e)r?</b></summary>
<p>`Clonr` looks cooler than `Cloner`. 'Nuff said.</p>
</details>

<details>
<summary><b>What can jDriveClonr clone that DriveClonrPro cannot?</b></summary>
<p>jDriveClonr shares a superset of features with DriveClonrPro. They both maintain the same base cloning abilities, 
including Google Workspace exporting, but jDriveClonr has additional features that are not present in DriveClonrPro:</p>

- 📁 jDriveClonr has a functional, lazily loaded TreeView for pruning and selecting files and folders to clone
- 🔄 jDriveClonr supports Shared Drives, which are not supported in DriveClonrPro
- 🔐 jDriveClonr fixes the issue of DriveClonrPro being unable to access certain shared files and folders
- ⚡ jDriveClonr is configurable multi-threaded, which allows for faster cloning
- 🖼️ jDriveClonr will support Google Photos cloning (coming soon)
- 💻 jDriveClonr uses JavaFX for the UI rather than Tkinter, providing a more modern UI with hDPI scaling support

</details>

<details>
<summary><b>What files can't be cloned with jDriveClonr?</b></summary>

Currently, jDriveClonr cannot clone:
- Google Forms
- Google Sites
- Google Maps/My Maps
- Google Keep
- File shortcuts

These files will likely never be supported due to the limitations of the Google Drive API, however we are actively looking into ways around this.

</details>

<details>
<summary><b>Why do you keep making DriveClonr clones?</b></summary>
<p>I do not know. If you have any recommendations for good rehab facilities, please let me know.</p>
</details>

## 🤝 Contributing

Contributions are welcome! <!-- Coming soon: Please see our [CONTRIBUTING.md](CONTRIBUTING.md) file for details on how to contribute to the project and our [TESTING.md](TESTING.md) file for testing guidelines. -->

### Reporting Issues

If you encounter a bug, have a feature request, or want to suggest documentation improvements, please use our issue templates:

- [Report a bug](https://github.com/username/jDriveClonr/issues/new?template=bug_report.md)
- [Request a feature](https://github.com/username/jDriveClonr/issues/new?template=feature_request.md)
- [Suggest documentation improvements](https://github.com/username/jDriveClonr/issues/new?template=documentation.md)
- [Report performance issues](https://github.com/username/jDriveClonr/issues/new?template=performance.md)
