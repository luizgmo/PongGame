package ponggame;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class GameServer {

    public static void main(String[] args) {
        try {
            // escreve no log que o servidor está iniciando
            ServerLogger.log("iniciando servidor na porta " + Config.getPorta());

            // cria socket do servidor com IP e porta do config.xml
            ServerSocket serverSocket = new ServerSocket(Config.getPorta(), 2, InetAddress.getByName(Config.getIp()));
            ServerLogger.log("servidor pronto em " + serverSocket.getInetAddress());

            // espera o primeiro jogador conectar
            ServerLogger.log("aguardando jogador 1...");
            Socket player1Socket = serverSocket.accept();
            ServerLogger.log("jogador 1 conectado: "
                    + player1Socket.getInetAddress().getHostAddress() + ":" + player1Socket.getPort());

            // cria streams para o jogador 1
            ObjectOutputStream out1 = new ObjectOutputStream(player1Socket.getOutputStream());
            ObjectInputStream in1 = new ObjectInputStream(player1Socket.getInputStream());
            out1.writeObject("P1");
            out1.flush();

            // envia mensagens de keep-alive para o jogador 1 enquanto espera o segundo
            new Thread(() -> {
                try {
                    while (true) {
                        Thread.sleep(5000);
                        out1.writeObject("WAITING_FOR_PLAYER");
                        out1.flush();
                        ServerLogger.log("enviado keep-alive para jogador 1");

                        if (player1Socket.isClosed()) {
                            ServerLogger.log("jogador 1 desconectou enquanto esperava");
                            break;
                        }
                    }
                } catch (Exception e) {
                    ServerLogger.log("jogador 1 desconectou: " + e.getMessage());
                }
            }).start();

            // espera o segundo jogador conectar
            ServerLogger.log("aguardando jogador 2...");
            Socket player2Socket = serverSocket.accept();
            ServerLogger.log("jogador 2 conectado: "
                    + player2Socket.getInetAddress().getHostAddress() + ":" + player2Socket.getPort());

            // cria streams para o jogador 2
            ObjectOutputStream out2 = new ObjectOutputStream(player2Socket.getOutputStream());
            ObjectInputStream in2 = new ObjectInputStream(player2Socket.getInputStream());
            out2.writeObject("P2");
            out2.flush();

            // inicia o coordenador do jogo em uma nova thread
            ServerLogger.log("iniciando partida entre "
                    + player1Socket.getPort() + " e " + player2Socket.getPort());

            new Thread(new GameCoordinator(in1, out1, in2, out2)).start();

        } catch (Exception e) {
            ServerLogger.logError("falha no servidor", e);
        }
    }
}
