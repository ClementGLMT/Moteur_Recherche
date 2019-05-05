package logger;

public class CompositeLogger implements Logger {

    private Logger logger1;
    private Logger logger2;

    public CompositeLogger(Logger logger1, Logger logger2) {
        this.logger1 = logger1;
        this.logger2 = logger2;
    }

    public void info(String category, String message) {
        logger1.info(category, message);
        logger2.info(category, message);
    }

    public void error(String category, String message) {
        logger1.error(category, message);
        logger2.error(category, message);
    }

}
