# 🏓 PongGame em Java (Multiplayer via Socket)

Este é um jogo de Pong multiplayer desenvolvido em Java utilizando sockets para comunicação em rede. Dois jogadores se conectam ao servidor e jogam em tempo real com sincronização dos movimentos.

---

## 📦 Requisitos

- Java 8 ou superior
- Arquivo `config.xml` configurado corretamente
- Duas instâncias rodando o cliente (para os dois jogadores)

---

## 🚀 Como Executar

- Execute a classe `GameServer` para iniciar o servidor
- Os jogadores (dois) devem executar a classe `Main` para cada um iniciar uma instância
- Assim que os dois jogadores estiverem conectados, basta um pressionar alguma de suas teclas para iniciar o jogo

## 🕹️ Controles

| Jogador | Movimento | Teclas de fim de jogo   |
| ------- | --------- | ----------------------- |
| P1      | W / S     | R = reiniciar, Q = sair |
| P2      | ↑ / ↓     | R = reiniciar, Q = sair |

## 🎮 Recursos do Jogo

- Comunicação em rede usando Socket, ObjectInputStream e ObjectOutputStream
- Interface feita com Java Swing
- Sincronização de estado do jogo em tempo real
- Sons integrados (batida e gol)
- Sistema de vitória, reinício e saída multiplayer

## 📡 Observações

- O servidor aceita exatamente dois jogadores
- O jogo foi projetado para funcionar em LAN (mesma rede)
- Para jogar pela internet, seria necessário fazer redirecionamento de portas
