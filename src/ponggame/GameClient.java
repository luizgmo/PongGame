package ponggame;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class GameClient {

    private GameFrame gameFrame;
    private GamePanel gamePanel;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String playerRole;
    private boolean running;

    // construtor que recebe os componentes principais da interface
    public GameClient(GameFrame gameFrame, GamePanel gamePanel) throws Exception {
        this.gameFrame = gameFrame;
        this.gamePanel = gamePanel;
        this.running = true;
        connectToServer();
    }

    // conecta no servidor usando ip e porta do arquivo config.xml
    private void connectToServer() throws Exception {
        System.out.println("[CLIENT] conectando ao servidor...");
        socket = new Socket(InetAddress.getByName(Config.getIp()), Config.getPorta());
        socket.setSoTimeout(30000); // define timeout para resposta

        // cria streams de comunicação
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());

        // recebe a função (P1 ou P2) do servidor
        playerRole = (String) in.readObject();
        System.out.println("[CLIENT] função atribuída: " + playerRole);

        // configura o painel do jogo para modo rede
        gamePanel.setNetworkMode(true);
        gamePanel.setPlayerRole(playerRole);
        gamePanel.setGameClient(this);

        // inicia o timer do jogo na interface gráfica
        SwingUtilities.invokeLater(() -> {
            gamePanel.getGameTimer().start();
        });

        // escuta as mensagens do servidor em outra thread
        new Thread(new ServerListener()).start();
        
    }

    // envia teclas pressionadas para o servidor
    public void sendKeyPress(int keyCode, boolean pressed) {
        if (out != null) {
            try {
                String message = String.format("KEY;%d;%b", keyCode, pressed);
                out.writeObject(message);
                out.flush();
                System.out.println("[CLIENT] enviado: " + message);
            } catch (IOException e) {
                System.err.println("[CLIENT] erro ao enviar tecla: " + e.getMessage());
                handleDisconnection();
            }
        }
    }

    // envia pedido de reinício
    public void sendRestartRequest() {
        if (out != null) {
            try {
                out.writeObject("RESTART_REQUEST");
                out.flush();
                System.out.println("[CLIENT] solicitação de reinício enviada");
            } catch (IOException e) {
                System.err.println("[CLIENT] erro ao enviar reinício: " + e.getMessage());
                handleDisconnection();
            }
        }
    }

    // envia pedido para sair do jogo
    public void sendQuitRequest() {
        if (out != null) {
            try {
                out.writeObject("QUIT_REQUEST");
                out.flush();
                System.out.println("[CLIENT] solicitação de saída enviada");
            } catch (IOException e) {
                System.err.println("[CLIENT] erro ao enviar saída: " + e.getMessage());
                handleDisconnection();
            }
        }
    }

    // trata perda de conexão com o servidor
    private void handleDisconnection() {
        running = false;
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(gameFrame,
                    "Conexão com o servidor perdida",
                    "Erro de Rede",
                    JOptionPane.ERROR_MESSAGE);
            gameFrame.showMenu();
        });
        disconnect();
    }

    // fecha conexões e streams
    public void disconnect() {
        running = false;
        try {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (socket != null) {
                socket.close();
            }
            System.out.println("[CLIENT] conexão fechada");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // thread interna que escuta o servidor continuamente
    private class ServerListener implements Runnable {

        @Override
        public void run() {
            try {
                System.out.println("[CLIENT] escutando servidor...");
                while (running && !socket.isClosed()) {
                    try {
                        Object message = in.readObject();
                        if (message == null) {
                            System.out.println("[CLIENT] conexão encerrada pelo servidor");
                            break;
                        }

                        final String msg = message.toString();
                        System.out.println("[CLIENT] recebido: " + msg);

                        SwingUtilities.invokeLater(() -> {
                            try {
                                if (msg.startsWith("GAME_STATE")) {
                                    processGameState(msg);
                                } else if (msg.equals("PONG")) {
                                    System.out.println("[CLIENT] conexão ativa confirmada");
                                } else if (msg.startsWith("GAME_OVER")) {
                                    processGameOver(msg);
                                } else if (msg.equals("RESTART_CONFIRMED")) {
                                    processRestartConfirmed();
                                } else if (msg.equals("QUIT_CONFIRMED")) {
                                    processQuitConfirmed();
                                } else if (msg.equals("WAITING_FOR_PLAYER")) {
                                    System.out.println("[CLIENT] aguardando segundo jogador...");
                                }
                            } catch (Exception e) {
                                System.err.println("[CLIENT] erro processando mensagem: " + e.getMessage());
                            }
                        });
                    } catch (java.net.SocketTimeoutException e) {
                        System.out.println("[CLIENT] timeout do socket - continuando...");
                        continue;
                    }
                }
            } catch (Exception e) {
                System.err.println("[CLIENT] erro na conexão: " + e.getMessage());
            } finally {
                if (running) {
                    handleDisconnection();
                }
            }
        }

        // processa estado do jogo recebido do servidor
        private void processGameState(String msg) {
            String[] parts = msg.split(";");
            if (parts.length >= 9) {
                gamePanel.updateGameStateFromNetwork(
                        Integer.parseInt(parts[1]), // ballX
                        Integer.parseInt(parts[2]), // ballY
                        Integer.parseInt(parts[3]), // player1Y
                        Integer.parseInt(parts[4]), // player2Y
                        Integer.parseInt(parts[5]), // player1Score
                        Integer.parseInt(parts[6]), // player2Score
                        Boolean.parseBoolean(parts[7]), // waitingForInput
                        Boolean.parseBoolean(parts[8]) // gameOver
                );
            }
        }

        // processa mensagem de fim de jogo
        private void processGameOver(String msg) {
            String[] parts = msg.split(";");
            if (parts.length >= 2) {
                gamePanel.endGame(parts[1]);
            }
        }

        // reinicia a interface local
        private void processRestartConfirmed() {
            System.out.println("[CLIENT] jogo reiniciado pelo servidor");
            gamePanel.resetGame();
        }

        // encerra o cliente
        private void processQuitConfirmed() {
            System.out.println("[CLIENT] saindo do jogo");
            disconnect();
            System.exit(0);
        }
    }
}
