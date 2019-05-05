package logger;

public class ConsoleLogger implements Logger {

    public void info(String category, String message) {
        System.out.println(message + "\n");
    }//end of info

    public void error(String category, String message) {
        System.err.println("ERROR : " + message + "\n");
    }//end of error

}//end of ConsoleLogger