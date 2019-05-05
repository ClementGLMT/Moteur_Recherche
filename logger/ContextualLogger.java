package logger;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ContextualLogger implements Logger {

    private Logger delegateLogger;

    public ContextualLogger(Logger delegateLogger) {
        this.delegateLogger = delegateLogger;
    }

    public void info(String category, String message) {
        SimpleDateFormat logFormat = new SimpleDateFormat("YYYY-MM-DD HH:SS.sss");
        delegateLogger.info(category, "\nDATE : " + logFormat.format(new Date()) + "\nCATEGORY : " + category + "\nLEVEL : INFO" + "\nMESSAGE :\n" + message + "\n");
    }

    public void error(String category, String message) {
        SimpleDateFormat logFormat = new SimpleDateFormat("YYYY-MM-DD HH:SS.sss");
        delegateLogger.error(category, "\nDATE : " + logFormat.format(new Date()) + "\nCATEGORY : " + category + "\nLEVEL : ERROR" + "\nMESSAGE :\n" + message + "\n");
    }
}
