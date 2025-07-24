package ponggame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MenuPanel extends JPanel {

    private JButton networkButton;
    private JButton exitButton;
    private GameFrame gameFrame;

    // monta a tela inicial com botões
    public MenuPanel(GameFrame gameFrame) {
        this.gameFrame = gameFrame;

        this.setLayout(new BorderLayout());
        this.setBackground(Color.BLACK);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(2, 1, 10, 10));
        buttonPanel.setBackground(Color.BLACK);

        networkButton = new JButton("Jogo em Rede");
        networkButton.setFont(new Font("Arial", Font.BOLD, 30));
        networkButton.setBackground(Color.WHITE);
        networkButton.setForeground(Color.BLACK);
        networkButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gameFrame.startNetworkGame();
            }
        });

        exitButton = new JButton("Sair");
        exitButton.setFont(new Font("Arial", Font.BOLD, 30));
        exitButton.setBackground(Color.WHITE);
        exitButton.setForeground(Color.BLACK);
        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        buttonPanel.add(networkButton);
        buttonPanel.add(exitButton);
        this.add(buttonPanel, BorderLayout.CENTER);
    }
}
