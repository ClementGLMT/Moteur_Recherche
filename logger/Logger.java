package logger;

public interface Logger {

    void info(String category, String message);
    void error(String category, String message);

}//end of Logger
