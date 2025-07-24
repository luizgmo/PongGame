package ponggame;

import javax.swing.*;
import java.awt.*;

public class GameFrame extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private GamePanel gamePanel;

    // configura a janela principal do jogo
    public GameFrame() {
        this.setTitle("Pong");
        this.setSize(800, 650);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(false);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        MenuPanel menuPanel = new MenuPanel(this);
        gamePanel = new GamePanel();

        mainPanel.add(menuPanel, "Menu");
        mainPanel.add(gamePanel, "Game");

        this.add(mainPanel);
        cardLayout.show(mainPanel, "Menu");

        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    // troca para a tela do jogo
    public void startGame() {
        cardLayout.show(mainPanel, "Game");
        gamePanel.requestFocusInWindow();
    }

    // inicia o modo online em outra thread
    public void startNetworkGame() {
        new Thread(() -> {
            try {
                GameClient client = new GameClient(this, gamePanel);
                SwingUtilities.invokeLater(() -> {
                    cardLayout.show(mainPanel, "Game");
                    gamePanel.requestFocusInWindow();
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                            "Erro ao conectar ao servidor: " + e.getMessage(),
                            "Erro de Rede",
                            JOptionPane.ERROR_MESSAGE);
                });
                e.printStackTrace();
            }
        }).start();
    }

    // mostra o menu e reseta o jogo
    public void showMenu() {
        cardLayout.show(mainPanel, "Menu");
        gamePanel.resetGame();
    }
}
