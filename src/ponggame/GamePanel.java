package ponggame;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class GamePanel extends JPanel implements KeyListener {

    // variáveis que armazenam o estado do jogo
    private int player1Y, player2Y;
    private int player1Score = 0, player2Score = 0;
    private int ballX, ballY;
    private boolean waitingForInput = true;
    private boolean gameOver = false;
    private boolean waitingForSecondPlayer = false;
    private String winnerMessage = "";
    private GameClient gameClient;
    private String playerRole;

    // recursos para otimizar o desenho
    private Image buffer;
    private Graphics bufferGraphics;
    private Timer gameTimer;

    private Sons sons;

    // construtor que inicializa os componentes
    public GamePanel() {
        setPreferredSize(new Dimension(GameConstants.WIDTH, GameConstants.HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        setDoubleBuffered(true);

        try {
            sons = new Sons("./sounds/hit.wav", "./sounds/gol.wav");
        } catch (Exception e) {
            System.err.println("erro ao carregar sons: " + e.getMessage());
        }

        // timer que repinta a tela com base no FPS definido
        gameTimer = new Timer(1000 / GameConstants.FPS, e -> repaint());

        resetGame();
    }

    // método que desenha todos os elementos do jogo
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // cria buffer se necessário
        if (buffer == null || buffer.getWidth(null) != getWidth() || buffer.getHeight(null) != getHeight()) {
            buffer = createImage(getWidth(), getHeight());
            bufferGraphics = buffer.getGraphics();
        }

        renderGame(bufferGraphics);
        g.drawImage(buffer, 0, 0, null);
    }

    // método que renderiza todos os componentes do jogo
    private void renderGame(Graphics g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, GameConstants.WIDTH, GameConstants.HEIGHT);

        // desenha linha tracejada no meio
        g.setColor(Color.GRAY);
        Graphics2D g2d = (Graphics2D) g;
        Stroke dashed = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{10}, 0);
        g2d.setStroke(dashed);
        g2d.drawLine(GameConstants.WIDTH / 2, 0, GameConstants.WIDTH / 2, GameConstants.HEIGHT);
        g2d.setStroke(new BasicStroke());

        // desenha as raquetes
        g.setColor(Color.WHITE);
        g.fillRect(30 - GameConstants.PADDLE_WIDTH, player1Y, GameConstants.PADDLE_WIDTH, GameConstants.PADDLE_HEIGHT);
        g.fillRect(GameConstants.WIDTH - 30 - GameConstants.PADDLE_WIDTH, player2Y, GameConstants.PADDLE_WIDTH, GameConstants.PADDLE_HEIGHT);

        // desenha a bola
        g.fillOval(ballX, ballY, GameConstants.BALL_SIZE, GameConstants.BALL_SIZE);

        // desenha o placar
        g.setFont(new Font("Arial", Font.BOLD, 36));
        g.drawString(String.valueOf(player1Score), GameConstants.WIDTH / 2 - 50, 50);
        g.drawString(String.valueOf(player2Score), GameConstants.WIDTH / 2 + 25, 50);

        // mostra mensagem enquanto espera o segundo jogador
        if (waitingForSecondPlayer) {
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            String message = "Aguardando segundo jogador se conectar...";
            g.drawString(message, GameConstants.WIDTH / 2 - g.getFontMetrics().stringWidth(message) / 2, GameConstants.HEIGHT / 2 + 30);
        } // mostra instruções de início
        else if (waitingForInput && !gameOver) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 20));
            String message = playerRole.equals("P1") ? "Pressione W ou S para começar" : "Pressione ↑ ou ↓ para começar";
            g.drawString(message, GameConstants.WIDTH / 2 - g.getFontMetrics().stringWidth(message) / 2, GameConstants.HEIGHT / 2 + 50);
        }

        // mostra mensagem de fim de jogo
        if (gameOver) {
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 40));
            g.drawString(winnerMessage, GameConstants.WIDTH / 2 - g.getFontMetrics().stringWidth(winnerMessage) / 2, GameConstants.HEIGHT / 2 - 50);

            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.drawString("Pressione 'R' para Jogar Novamente", GameConstants.WIDTH / 2 - 180, GameConstants.HEIGHT / 2 + 40);
            g.drawString("Pressione 'Q' para Sair do Jogo", GameConstants.WIDTH / 2 - 150, GameConstants.HEIGHT / 2 + 70);
        }
    }

    // define flag de espera por outro jogador
    public void setWaitingForSecondPlayer(boolean waiting) {
        this.waitingForSecondPlayer = waiting;
    }

    // finaliza o jogo e mostra quem venceu
    public void endGame(String winner) {
        gameOver = true;
        waitingForSecondPlayer = false;
        winnerMessage = playerRole.equals(winner) ? "Você Venceu!" : "Você Perdeu!";
        System.out.println("[CLIENT] Jogo terminado: " + winnerMessage);
    }

    // reinicia o estado do jogo
    public void resetGame() {
        player1Y = GameConstants.HEIGHT / 2 - GameConstants.PADDLE_HEIGHT / 2;
        player2Y = GameConstants.HEIGHT / 2 - GameConstants.PADDLE_HEIGHT / 2;
        player1Score = 0;
        player2Score = 0;
        ballX = GameConstants.WIDTH / 2 - GameConstants.BALL_SIZE / 2;
        ballY = GameConstants.HEIGHT / 2 - GameConstants.BALL_SIZE / 2;
        gameOver = false;
        waitingForInput = true;
        waitingForSecondPlayer = false;
    }

    // seta o cliente para se comunicar com o servidor
    public void setGameClient(GameClient client) {
        this.gameClient = client;
    }

    // ativa o modo online e exibe mensagem de espera
    public void setNetworkMode(boolean networkMode) {
        if (networkMode) {
            waitingForSecondPlayer = true;
        }
    }

    // atualiza o estado do jogo com informações recebidas do servidor
    public void updateGameStateFromNetwork(int ballX, int ballY, int player1Y, int player2Y,
            int player1Score, int player2Score, boolean waitingForInput, boolean gameOver) {
        this.ballX = ballX;
        this.ballY = ballY;
        this.player1Y = player1Y;
        this.player2Y = player2Y;
        this.player1Score = player1Score;
        this.player2Score = player2Score;
        this.waitingForInput = waitingForInput;
        this.gameOver = gameOver;
        this.waitingForSecondPlayer = false;

        if (!gameTimer.isRunning()) {
            gameTimer.start();
        }
    }

    public Timer getGameTimer() {
        return gameTimer;
    }

    public void setPlayerRole(String playerRole) {
        this.playerRole = playerRole;
    }

    // trata teclas pressionadas durante o jogo
    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver) {
            handleGameOverInput(e);
            return;
        }

        int keyCode = e.getKeyCode();

        if (gameClient != null) {
            if ((playerRole.equals("P1") && (keyCode == KeyEvent.VK_W || keyCode == KeyEvent.VK_S))
                    || (playerRole.equals("P2") && (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN))) {
                gameClient.sendKeyPress(keyCode, true);
            }
        }
    }

    // trata teclas soltas
    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();

        if (gameClient != null) {
            if ((playerRole.equals("P1") && (keyCode == KeyEvent.VK_W || keyCode == KeyEvent.VK_S))
                    || (playerRole.equals("P2") && (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN))) {
                gameClient.sendKeyPress(keyCode, false);
            }
        }
    }

    // trata comandos no fim do jogo
    private void handleGameOverInput(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_R) {
            if (gameClient != null) {
                gameClient.sendRestartRequest();
            }
        } else if (e.getKeyCode() == KeyEvent.VK_Q) {
            if (gameClient != null) {
                gameClient.sendQuitRequest();
            } else {
                System.exit(0);
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    // requisita o foco quando o painel for adicionado
    @Override
    public void addNotify() {
        super.addNotify();
        requestFocusInWindow();
    }
}
