# jDriveClonr

> A Java port of DriveClonrPro, with additional features and improvements.

New year, new project! Introducing jDriveClonr, the Java port of the original Python-based DriveClonrPro! This project is designed to be a more efficient and user-friendly alternative to the original DriveClonrPro, with a focus on performance and ease of use.

Immediately, before even implementing multithreading, jDriveClonr is already faster than the original DriveClonrPro! It also _finally_ supports the TreeView for selecting files and folders to clone, which was a long-awaited feature in the original DriveClonrPro. Additionally, jDriveClonr is designed to be more modular and extensible, making it easier to add new features and improvements in the future! 

## Features

- Multithreading support for faster cloning
- TreeView for selecting files and folders to clone
- 🆕 Google Photos Cloning Support! (Technically coming soon, but who's keeping track :P)
- 🆕 Lazy-loading for a faster, lag-free UI
- 🆕 Improved error handling and logging
- 🆕 JavaFX Native UI
- Pretty much everything else that was in DriveClonrPro, but better!

## Requirements and Build Instructions

### Prelude: Google Drive API Credentials 

> [!IMPORTANT]
> Without the `credentials.json` file, jDriveClonr **will not work**. Ensure that you have created a Google Drive API project and have the `credentials.json` on-hand.

This project does *not* come with the required Google Drive API credentials. You will need to create your own credentials in order to use this project.
These can be created by following the instructions in the [Google Drive API documentation](https://developers.google.com/drive/api/v3/quickstart/java).

> [!NOTE]
> jDriveClonr does not require the `.json` file to be in a specific directory. If it cannot find it, it will prompt you to select
> it from a file chooser dialog.

### Library and Framework Requirements

DriveClonr is developed using Java 21, JavaFX 21, and Maven 3.8.5. While it *should* compile under Java 17+, this is untested and 
not recommended.

### Building the Project

All requirements are listed in [pom.xml](pom.xml). To build the project, simply run the following command in the root directory of the project:


```bash
mvnw clean install
```

Or, if you have a system-wide installation of Maven, you can run:

```bash
mvn clean install
```

This will compile DriveClonr and install all necessary dependencies.

Then, you can run the project using the following command:

```bash
mvn javafx:run
```

*Or `mvnw javafx:run` if you are using the wrapper.*

## FAQ: 

> What happened to the `e` in Clon(e)r?

`Clonr` looks cooler than `Cloner`. 'Nuff said.

> What can jDriveClonr clone that DriveClonrPro cannot?

Great question! jDriveClonr shares a superset of features with DriveClonrPro. They both maintain the same base cloning abilities, 
including Google Workspace exporting, but jDriveClonr has additional features that are not present in DriveClonrPro. Namely:

- jDriveClonr has a functional, lazily loaded TreeView for pruning and selecting files and folders to clone.
- jDriveClonr supports Shared Drives, which are not supported in DriveClonrPro.
- jDriveClonr fixes the issue of DriveClonrPro being unable to access certain shared files and folders.
- jDriveClonr is *multithreaded*, which allows for faster cloning.

    > [!WARNING]
    > It is not recommended to run jDriveClonr with any more than 4-6 threads at a time. Any more and you risk 
    > running into rate-limiting and other unpredictable behavior.

- jDriveClonr supports Google Photos cloning, which was not supported in jDriveClonr 
    > [!NOTE]
    > This feature is not yet implemented, but is planned for the future. Stay tuned!

- jDriveClonr uses JavaFX for the UI, rather than Tkinter. This allows for a more modern UI which supports hDPI scaling 
    and is more responsive overall.

    > [!NOTE]
  > This was one of the main gripes I had with DriveClonrPro. It looked horribly blurry on hDPI displays.

> Why do you keep making DriveClonr clones? Why haven't you checked yourself into a rehab facility yet?

I do not know. If you have any recommendations for good rehab facilities, please let me know.