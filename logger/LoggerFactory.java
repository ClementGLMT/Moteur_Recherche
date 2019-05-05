package logger;

import java.io.IOException;

public class LoggerFactory {



    public static CompositeLogger getConsLogger(String pathAsString) throws IOException {
        return new CompositeLogger(new ConsoleLogger(), new ContextualLogger(new FileLogger(pathAsString)));
    }
    public static ContextualLogger getFileLogger(String pathAsString) throws IOException {
        return new ContextualLogger(new FileLogger(pathAsString));
    }
}
