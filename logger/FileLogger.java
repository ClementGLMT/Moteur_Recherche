package logger;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

public class FileLogger implements Logger {
    private final Path path;

    public FileLogger(String pathAsString) throws IOException {
        path = Paths.get(pathAsString).toAbsolutePath();
        File temp = new File("<your file name>");
        if (temp.exists()) {
            RandomAccessFile raf = new RandomAccessFile(temp, "rw");
            raf.setLength(0);
        }
    }

    public void info(String category, String message) {
        try {
            Files.write(path, (message + "\n\n").getBytes(), APPEND, CREATE);
        } catch (IOException e) {
            e.getMessage();
            e.getCause();
            throw new RuntimeException("Cannot write log message to file [" + path + "]", e);
        }

    }

    public void error(String category, String message) {
        try {
            Files.write(path, ("ERROR : " + message + "\n\n").getBytes(), APPEND, CREATE);
        } catch (IOException e) {
            throw new RuntimeException("Cannot write log message to file [" + path + "]", e);
        }
    }
}