package ponggame;

import javax.sound.sampled.*;
import java.io.File;

public class Sons {

    private Clip hitClip;
    private Clip golClip;

    // carrega os dois sons na memória quando o objeto é criado
    // sem carregar antes o jogo estava dando uma leve travada na primeira vez em que um som era reproduzido
    public Sons(String hitPath, String golPath) {
        try {
            hitClip = loadClip(hitPath);
            golClip = loadClip(golPath);
        } catch (Exception e) {
            System.err.println("[Sons] erro ao carregar sons: " + e.getMessage());
        }
    }

    // método que carrega e prepara o som
    private Clip loadClip(String path) throws Exception {
        File file = new File(path);
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
        Clip clip = AudioSystem.getClip();
        clip.open(audioInputStream);
        return clip;
    }

    // toca o som do início
    private void playClip(Clip clip) {
        if (clip == null) return;
        if (clip.isRunning()) {
            clip.stop();
        }
        clip.setFramePosition(0);
        clip.start();
    }

    // toca o som de batida
    public void hit() {
        playClip(hitClip);
    }

    // toca o som de gol
    public void gol() {
        playClip(golClip);
    }
}
