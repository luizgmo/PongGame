package ponggame;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Random;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GameCoordinator implements Runnable {

    private ObjectInputStream inPlayer1, inPlayer2;
    private ObjectOutputStream outPlayer1, outPlayer2;

    // variáveis do estado do jogo
    private int player1Y, player2Y;
    private int player1Score = 0, player2Score = 0;
    private int ballX, ballY;
    private int ballXSpeed, ballYSpeed;
    private boolean gameRunning = false;
    private boolean p1Up, p1Down, p2Up, p2Down;
    private boolean waitingForInput = true;
    private boolean gameOver = false;
    private boolean gameStarted = false;

    private Sons sons;

    // construtor recebe os streams dos dois jogadores
    public GameCoordinator(ObjectInputStream in1, ObjectOutputStream out1,
            ObjectInputStream in2, ObjectOutputStream out2) {
        this.inPlayer1 = in1;
        this.outPlayer1 = out1;
        this.inPlayer2 = in2;
        this.outPlayer2 = out2;

        try {
            // carrega os sons uma única vez
            this.sons = new Sons("./sounds/hit.wav", "./sounds/gol.wav");
        } catch (Exception e) {
            System.err.println("[SERVER] erro ao carregar sons: " + e.getMessage());
        }

        resetGame();
        sendGameStateToClients();
    }

    // reinicia o jogo com valores iniciais
    private void resetGame() {
        player1Y = GameConstants.HEIGHT / 2 - GameConstants.PADDLE_HEIGHT / 2;
        player2Y = GameConstants.HEIGHT / 2 - GameConstants.PADDLE_HEIGHT / 2;
        player1Score = 0;
        player2Score = 0;
        gameOver = false;
        gameStarted = false;
        resetBall();
        gameRunning = true;
    }

    // posiciona a bola no centro com velocidade aleatória
    private void resetBall() {
        ballX = GameConstants.WIDTH / 2 - GameConstants.BALL_SIZE / 2;
        ballY = GameConstants.HEIGHT / 2 - GameConstants.BALL_SIZE / 2;
        Random rand = new Random();
        ballXSpeed = rand.nextBoolean() ? GameConstants.BALL_BASE_SPEED : -GameConstants.BALL_BASE_SPEED;
        ballYSpeed = rand.nextBoolean() ? GameConstants.BALL_BASE_SPEED / 2 : -GameConstants.BALL_BASE_SPEED / 2;
        waitingForInput = true;
        System.out.println("[SERVER] bola resetada - aguardando input dos jogadores");
    }

    // inicia as threads do jogo
    @Override
    public void run() {
        // thread do jogador 1
        new Thread(() -> {
            try {
                while (gameRunning) {
                    Object message = inPlayer1.readObject();
                    if (message instanceof String) {
                        handleClientMessage("P1", (String) message);
                    }
                }
            } catch (Exception e) {
                System.out.println("[SERVER] jogador 1 desconectado: " + e.getMessage());
                gameRunning = false;
            }
        }).start();

        // thread do jogador 2
        new Thread(() -> {
            try {
                while (gameRunning) {
                    Object message = inPlayer2.readObject();
                    if (message instanceof String) {
                        handleClientMessage("P2", (String) message);
                    }
                }
            } catch (Exception e) {
                System.out.println("[SERVER] jogador 2 desconectado: " + e.getMessage());
                gameRunning = false;
            }
        }).start();

        // timer que atualiza o jogo conforme FPS
        new Timer(1000 / GameConstants.FPS, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (gameRunning && !gameOver) {
                    updatePaddles();

                    if (!waitingForInput && gameStarted) {
                        updateGameState();
                    }

                    sendGameStateToClients();
                    checkWinCondition();
                }
            }
        }).start();

        System.out.println("[SERVER] GameCoordinator iniciado - aguardando jogadores pressionarem teclas");
    }

    // processa mensagens recebidas de cada jogador
    private synchronized void handleClientMessage(String player, String message) {
        try {
            System.out.println("[SERVER] mensagem de " + player + ": " + message);

            if (message.equals("PING")) {
                sendPong(player);
                return;
            }

            String[] parts = message.split(";");
            if (parts.length < 1) {
                return;
            }

            String messageType = parts[0];

            if (messageType.equals("KEY") && parts.length >= 3) {
                int keyCode = Integer.parseInt(parts[1]);
                boolean pressed = Boolean.parseBoolean(parts[2]);

                handleKeyInput(player, keyCode, pressed);

                // inicia o jogo quando uma tecla válida for pressionada
                if (waitingForInput && pressed && !gameOver) {
                    if ((player.equals("P1") && (keyCode == KeyEvent.VK_W || keyCode == KeyEvent.VK_S))
                            || (player.equals("P2") && (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN))) {
                        waitingForInput = false;
                        gameStarted = true;
                        System.out.println("[SERVER] jogo iniciado por " + player + " com tecla " + keyCode);
                    }
                }
            } else if (messageType.equals("RESTART_REQUEST")) {
                handleRestartRequest(player);
            } else if (messageType.equals("QUIT_REQUEST")) {
                handleQuitRequest(player);
            }

        } catch (Exception e) {
            System.out.println("[SERVER] erro processando mensagem: " + e.getMessage());
        }
    }

    // atualiza variáveis de movimentação
    private void handleKeyInput(String player, int keyCode, boolean pressed) {
        if (player.equals("P1")) {
            if (keyCode == KeyEvent.VK_W) {
                p1Up = pressed;
            } else if (keyCode == KeyEvent.VK_S) {
                p1Down = pressed;
            }
        } else if (player.equals("P2")) {
            if (keyCode == KeyEvent.VK_UP) {
                p2Up = pressed;
            } else if (keyCode == KeyEvent.VK_DOWN) {
                p2Down = pressed;
            }
        }
    }

    // reinicia o jogo quando pedido
    private void handleRestartRequest(String player) throws IOException {
        if (gameOver) {
            System.out.println("[SERVER] reinício solicitado por " + player);
            resetGame();
            sendRestartConfirmation();
        }
    }

    // finaliza o jogo quando pedido
    private void handleQuitRequest(String player) throws IOException {
        System.out.println("[SERVER] saída solicitada por " + player);
        gameRunning = false;
        sendQuitConfirmation();
    }

    // movimenta as raquetes conforme as teclas
    private void updatePaddles() {
        if (p1Up) {
            player1Y = Math.max(0, player1Y - GameConstants.PADDLE_SPEED);
        }
        if (p1Down) {
            player1Y = Math.min(GameConstants.HEIGHT - GameConstants.PADDLE_HEIGHT, player1Y + GameConstants.PADDLE_SPEED);
        }
        if (p2Up) {
            player2Y = Math.max(0, player2Y - GameConstants.PADDLE_SPEED);
        }
        if (p2Down) {
            player2Y = Math.min(GameConstants.HEIGHT - GameConstants.PADDLE_HEIGHT, player2Y + GameConstants.PADDLE_SPEED);
        }
    }

    // responde com "PONG" ao ping do cliente
    private void sendPong(String player) throws IOException {
        if (player.equals("P1")) {
            outPlayer1.writeObject("PONG");
            outPlayer1.flush();
        } else {
            outPlayer2.writeObject("PONG");
            outPlayer2.flush();
        }
    }

    // atualiza posição da bola e detecta colisões
    private synchronized void updateGameState() {
        try {
            ballX += ballXSpeed;
            ballY += ballYSpeed;

            if (ballY <= 0) {
                ballY = 0;
                ballYSpeed = Math.abs(ballYSpeed);
                sons.hit();
            }

            if (ballY >= GameConstants.HEIGHT - GameConstants.BALL_SIZE) {
                ballY = GameConstants.HEIGHT - GameConstants.BALL_SIZE;
                ballYSpeed = -Math.abs(ballYSpeed);
                sons.hit();
            }

            int paddle1X = 30 - GameConstants.PADDLE_WIDTH;
            int paddle2X = GameConstants.WIDTH - 30 - GameConstants.PADDLE_WIDTH;

            // colisão com raquete do jogador 1
            if (ballX <= paddle1X + GameConstants.PADDLE_WIDTH
                    && ballY + GameConstants.BALL_SIZE >= player1Y
                    && ballY <= player1Y + GameConstants.PADDLE_HEIGHT
                    && ballXSpeed < 0) {
                ballXSpeed = Math.abs(ballXSpeed);
                ballX = paddle1X + GameConstants.PADDLE_WIDTH;
                increaseBallSpeed();
                sons.hit();
            }

            // colisão com raquete do jogador 2
            if (ballX + GameConstants.BALL_SIZE >= paddle2X
                    && ballY + GameConstants.BALL_SIZE >= player2Y
                    && ballY <= player2Y + GameConstants.PADDLE_HEIGHT
                    && ballXSpeed > 0) {
                ballXSpeed = -Math.abs(ballXSpeed);
                ballX = paddle2X - GameConstants.BALL_SIZE;
                increaseBallSpeed();
                sons.hit();
            }

            // ponto do jogador 2
            if (ballX <= 0) {
                player2Score++;
                sons.gol();
                resetBall();
            } // ponto do jogador 1
            else if (ballX >= GameConstants.WIDTH - GameConstants.BALL_SIZE) {
                player1Score++;
                sons.gol();
                resetBall();
            }

        } catch (Exception e) {
            System.err.println("[SERVER] erro ao atualizar jogo: " + e.getMessage());
        }
    }

    // aumenta a velocidade da bola gradualmente
    private void increaseBallSpeed() {
        if (Math.abs(ballXSpeed) < 10) {
            ballXSpeed += (ballXSpeed > 0 ? 0.5 : -0.5);
        }
        if (Math.abs(ballYSpeed) < 8) {
            ballYSpeed += (ballYSpeed > 0 ? 0.3 : -0.3);
        }
    }

    // envia estado do jogo para os dois jogadores
    private synchronized void sendGameStateToClients() {
        String gameState = String.format("GAME_STATE;%d;%d;%d;%d;%d;%d;%b;%b",
                ballX, ballY, player1Y, player2Y, player1Score, player2Score,
                waitingForInput, gameOver);
        try {
            outPlayer1.writeObject(gameState);
            outPlayer1.flush();
            outPlayer2.writeObject(gameState);
            outPlayer2.flush();
        } catch (IOException e) {
            System.err.println("[SERVER] erro ao enviar estado do jogo: " + e.getMessage());
        }
    }

    // verifica se algum jogador venceu
    private synchronized void checkWinCondition() {
        if (!gameOver && (player1Score >= GameConstants.WINNING_SCORE || player2Score >= GameConstants.WINNING_SCORE)) {
            gameOver = true;
            gameStarted = false;
            String winner = (player1Score >= GameConstants.WINNING_SCORE) ? "P1" : "P2";
            System.out.println("[SERVER] jogo finalizado. vencedor: " + winner);

            try {
                outPlayer1.writeObject("GAME_OVER;" + winner);
                outPlayer1.flush();
                outPlayer2.writeObject("GAME_OVER;" + winner);
                outPlayer2.flush();
            } catch (IOException e) {
                System.err.println("[SERVER] erro ao enviar GAME_OVER: " + e.getMessage());
            }
        }
    }

    private void sendRestartConfirmation() throws IOException {
        outPlayer1.writeObject("RESTART_CONFIRMED");
        outPlayer1.flush();
        outPlayer2.writeObject("RESTART_CONFIRMED");
        outPlayer2.flush();
    }

    private void sendQuitConfirmation() throws IOException {
        outPlayer1.writeObject("QUIT_CONFIRMED");
        outPlayer1.flush();
        outPlayer2.writeObject("QUIT_CONFIRMED");
        outPlayer2.flush();
    }
}
