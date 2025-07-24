package ponggame;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ServerLogger {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    // escreve mensagem normal com hora
    public static void log(String message) {
        System.out.println("[" + sdf.format(new Date()) + "] " + message);
    }

    // escreve erro com stack trace
    public static void logError(String message, Exception e) {
        System.err.println("[" + sdf.format(new Date()) + "] ERRO: " + message);
        e.printStackTrace();
    }
}
