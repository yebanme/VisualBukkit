package com.gmail.visualbukkit;

import com.gmail.visualbukkit.blocks.BlockRegistry;
import com.gmail.visualbukkit.project.ProjectManager;
import com.gmail.visualbukkit.project.UndoManager;
import com.gmail.visualbukkit.ui.ActionMenuItem;
import com.gmail.visualbukkit.ui.LogWindow;
import com.gmail.visualbukkit.ui.SettingsManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import net.arikia.dev.drpc.DiscordEventHandlers;
import net.arikia.dev.drpc.DiscordRPC;
import net.arikia.dev.drpc.DiscordRichPresence;
import org.controlsfx.control.Notifications;
import org.controlsfx.control.action.Action;
import org.json.JSONException;
import org.json.JSONObject;
import org.kordamp.ikonli.fontawesome5.FontAwesomeBrands;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class VisualBukkitApp extends Application {

    private static final String version = VisualBukkitLauncher.class.getPackage().getSpecificationVersion();
    private static final Logger logger = Logger.getLogger("Visual Bukkit");
    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle("lang.gui");
    private static final Set<VisualBukkitExtension> extensions = new HashSet<>();
    private static final Path dataDirectory = Paths.get(System.getProperty("user.home"), "VisualBukkit6_beta"); // todo: remove beta
    private static final Path dataFile = dataDirectory.resolve("data.json");
    private static JSONObject data = new JSONObject();

    private static final BorderPane rootPane = new BorderPane();
    private static LogWindow logWindow;
    private static Stage primaryStage;

    @Override
    public void start(Stage primaryStage) throws IOException {
        VisualBukkitApp.primaryStage = primaryStage;

        Files.createDirectories(dataDirectory);
        FileHandler logFileHandler = new FileHandler(dataDirectory.resolve("visualbukkit.log").toString(), 10 * 1024 * 1024, 1);
        logFileHandler.setFormatter(new SimpleFormatter());
        logFileHandler.setLevel(Level.ALL);
        logger.addHandler(logFileHandler);
        logger.addHandler((logWindow = new LogWindow()).getHandler());
        logger.info("Visual Bukkit v" + version);

        DiscordRPC.discordInitialize("799336716027691059", new DiscordEventHandlers(), true);
        DiscordRPC.discordUpdatePresence(new DiscordRichPresence.Builder("Developing plugins").setStartTimestamps(System.currentTimeMillis()).build());

        if (Files.exists(dataFile)) {
            try {
                data = new JSONObject(Files.readString(dataFile));
            } catch (IOException | JSONException e) {
                logger.log(Level.SEVERE, "Failed to load data file", e);
            }
        }

        primaryStage.setScene(new Scene(rootPane, 750, 750));
        primaryStage.setMaximized(true);
        primaryStage.setTitle("Visual Bukkit");

        rootPane.setTop(new MenuBar(
                new Menu(localizedText("menu.file"), null,
                        new Menu(localizedText("menu.project"), null,
                            new ActionMenuItem(localizedText("menu.new"), e -> ProjectManager.promptCreate(true)),
                            new ActionMenuItem(localizedText("menu.open"), e -> ProjectManager.promptOpen(true)),
                            new ActionMenuItem(localizedText("menu.import"), e -> ProjectManager.promptImport()),
                            new ActionMenuItem(localizedText("menu.export"), e -> ProjectManager.promptExport()),
                            new ActionMenuItem(localizedText("menu.rename"), e -> ProjectManager.promptRename()),
                            new ActionMenuItem(localizedText("menu.delete"), e -> ProjectManager.promptDelete()),
                            new ActionMenuItem(localizedText("menu.save"), e -> {
                                try {
                                    ProjectManager.current().save();
                                    displayInfo(localizedText("notification.saved_project"));
                                } catch (IOException ex) {
                                    displayException(ex);
                                }
                            })),
                        new Menu(localizedText("menu.folder"), null,
                                new ActionMenuItem(localizedText("menu.project"), e -> openURI(ProjectManager.current().getDirectory().toUri())),
                                new ActionMenuItem(localizedText("menu.extensions"), e -> openURI(dataDirectory.resolve("extensions").toUri())),
                                new ActionMenuItem(localizedText("menu.themes"), e -> openURI(dataDirectory.resolve("themes").toUri()))),
                        new ActionMenuItem(localizedText("menu.log"), e -> logWindow.show()),
                        new ActionMenuItem(localizedText("menu.restart"), e -> {}),
                        new ActionMenuItem(localizedText("menu.exit"), e -> Platform.exit())),
                new Menu(localizedText("menu.edit"), null,
                        new ActionMenuItem(localizedText("menu.undo"), e -> UndoManager.current().undo()),
                        new ActionMenuItem(localizedText("menu.redo"), e -> UndoManager.current().redo())),
                new Menu(localizedText("menu.settings"), null,
                        SettingsManager.createThemesMenu(),
                        SettingsManager.createFontSizeMenu()),
                new Menu(localizedText("menu.help"), null,
                        new ActionMenuItem("Github", FontAwesomeBrands.GITHUB, e -> openURI(URI.create("https://github.com/OfficialDonut/VisualBukkit"))),
                        new ActionMenuItem("Spigot", FontAwesomeSolid.FAUCET, e -> openURI(URI.create("https://www.spigotmc.org/resources/visual-bukkit-create-plugins.76474/"))),
                        new ActionMenuItem("Discord", FontAwesomeBrands.DISCORD, e -> openURI(URI.create("https://discord.gg/ugkvGpu"))))));

        primaryStage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            Node focusOwner = primaryStage.getScene().getFocusOwner();
            if (e.isShortcutDown() && e.getCode() == KeyCode.Z && !(focusOwner instanceof TextField) && !(focusOwner instanceof TextArea)) {
                if (e.isShiftDown()) {
                    UndoManager.current().redo();
                } else {
                    UndoManager.current().undo();
                }
            }
            if (e.isShortcutDown() && e.getCode() == KeyCode.S) {
                try {
                    ProjectManager.current().save();
                    displayInfo(localizedText("notification.saved_project"));
                } catch (IOException ex) {
                    displayException(ex);
                }
            }
        });

        logger.info("Loading extensions");
        Path extensionsDir = dataDirectory.resolve("extensions");
        Files.createDirectories(extensionsDir);
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(extensionsDir, "*.jar")) {
            for (Path path : dirStream) {
                try (JarFile jarFile = new JarFile(path.toFile())) {
                    String mainClassName = jarFile.getManifest().getMainAttributes().getValue("main-class");
                    if (mainClassName != null) {
                        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{path.toUri().toURL()})) {
                            Class<?> mainClass = Class.forName(mainClassName, true, classLoader);
                            if (VisualBukkitExtension.class.isAssignableFrom(mainClass)) {
                                VisualBukkitExtension extension = (VisualBukkitExtension) mainClass.getConstructor().newInstance();
                                extensions.add(extension);
                                logger.info("Loaded extension: " + extension.getName() + " " + extension.getVersion());
                            }
                        }
                    }
                } catch (Throwable e) {
                    logger.log(Level.WARNING, "Failed to load extension", e);
                }
            }
        }

        primaryStage.show();
        Thread.setDefaultUncaughtExceptionHandler((thread, e) -> displayException(e));
        logger.info("Loading initial project");
        Platform.runLater(() -> {
            ProjectManager.openInitial();
            rootPane.requestFocus();
            try {
                VisualBukkitGrpcServer.getInstance().start();
            } catch (Exception e) {
                displayException(e);
            }
        });
    }

    @Override
    public void stop() {
        logger.info("Shutting down");
        BlockRegistry.save();
        if (ProjectManager.current() != null) {
            try {
                ProjectManager.current().save();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to save project", e);
            }
        }
        try {
            Files.createDirectories(dataDirectory);
            Files.writeString(dataFile, data.toString(2));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save data file", e);
        }
        DiscordRPC.discordShutdown();
        VisualBukkitGrpcServer.getInstance().stop();
    }

    public static String localizedText(String key) {
        try {
            return resourceBundle.getString(key);
        } catch (MissingResourceException e) {
            logger.log(Level.WARNING, "Failed to get text for key: {0}", key);
            return key;
        }
    }

    public static void displayInfo(String message) {
        createNotification(localizedText("notification.info_title"), message).showInformation();
    }

    public static void displayError(String message) {
        createNotification(localizedText("notification.error_title"), message).showError();
    }

    public static void displayException(Throwable e) {
        if (Platform.isFxApplicationThread()) {
            logger.log(Level.SEVERE, "An unexpected error occurred", e);
            createNotification(localizedText("notification.error_title"), localizedText("notification.unexpected_error"))
                    .action(new Action(localizedText("button.show_error"), action -> logWindow.show()))
                    .showError();
        } else {
            Platform.runLater(() -> displayException(e));
        }
    }

    private static Notifications createNotification(String title, String text) {
        return Notifications.create()
                .owner(VisualBukkitApp.primaryStage)
                .darkStyle()
                .title(title)
                .text(text)
                .hideAfter(Duration.seconds(5));
    }

    public static void openURI(URI uri) {
        try {
            Desktop.getDesktop().browse(uri);
        } catch (Exception e) {
            displayException(e);
        }
    }

    public static Logger getLogger() {
        return logger;
    }

    public static Set<VisualBukkitExtension> getExtensions() {
        return extensions;
    }

    public static Path getDataDirectory() {
        return dataDirectory;
    }

    public static JSONObject getData() {
        return data;
    }

    public static BorderPane getRootPane() {
        return rootPane;
    }

    public static LogWindow getLogWindow() {
        return logWindow;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }
}
