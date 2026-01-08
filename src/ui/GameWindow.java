package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import game.GameLoop;
import util.GameConstants;

/**
 * Main game window
 * Sets up the JFrame in fullscreen mode
 */
public class GameWindow extends JFrame {
    
    private GameLoop gameLoop;
    
    public GameWindow() {
        // Window setup for fullscreen
        setTitle("Drift City - Pixel Drift Racing");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setUndecorated(true);  // No window borders
        setResizable(false);
        
        // Get screen size and update constants
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        DisplayMode dm = gd.getDisplayMode();
        GameConstants.setScreenSize(dm.getWidth(), dm.getHeight());
        
        // Create game loop panel
        gameLoop = new GameLoop();
        add(gameLoop);
        
        // Set to fullscreen
        if (gd.isFullScreenSupported()) {
            gd.setFullScreenWindow(this);
        } else {
            // Fallback to maximized window
            setExtendedState(JFrame.MAXIMIZED_BOTH);
            pack();
        }
        
        // Handle window closing
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                gameLoop.dispose();
            }
        });
    }
    
    /**
     * Start the game
     */
    public void startGame() {
        setVisible(true);
        gameLoop.start();
    }
    
    /**
     * Create and show the game window
     */
    public static void launch() {
        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Use default look and feel
        }
        
        // Create window on EDT
        SwingUtilities.invokeLater(() -> {
            GameWindow window = new GameWindow();
            window.startGame();
        });
    }
}
