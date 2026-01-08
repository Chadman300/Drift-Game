package game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import graphics.Camera;
import graphics.ParticleSystem;
import graphics.Renderer;
import input.InputHandler;
import scoring.DriftScoring;
import util.GameConstants;
import world.CityWorld;

/**
 * Main game loop and coordination
 * Handles timing, updates, and rendering
 */
public class GameLoop extends JPanel implements Runnable {
    
    // Game components
    private CityWorld world;
    private Car car;
    private Camera camera;
    private ParticleSystem particles;
    private Renderer renderer;
    private InputHandler input;
    private DriftScoring scoring;
    private GameState gameState;
    
    // Game loop
    private Thread gameThread;
    private volatile boolean running;
    private double deltaTime;
    
    // Timing
    private long lastTime;
    private double accumulator;
    private static final double FIXED_TIMESTEP = GameConstants.DELTA_TIME;
    
    // FPS tracking
    private int fps;
    private int frameCount;
    private long fpsTimer;
    
    public GameLoop() {
        // Panel setup - size will be set by fullscreen
        setPreferredSize(new Dimension(GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT));
        setBackground(Color.BLACK);
        setDoubleBuffered(true);
        setFocusable(true);
        
        // Initialize components
        initializeGame();
        
        // Input handling
        input = new InputHandler();
        addKeyListener(input);
        
        // Handle ESC to exit
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    System.exit(0);
                }
            }
        });
    }
    
    /**
     * Initialize all game components
     */
    private void initializeGame() {
        // Create world
        world = new CityWorld();
        
        // Create particles system
        particles = new ParticleSystem();
        
        // Create car at center of world
        car = new Car(0, 0, particles);
        
        // Create camera
        camera = new Camera();
        camera.setPosition(0, 0);
        
        // Create renderer
        renderer = new Renderer(camera, particles);
        
        // Create scoring system
        scoring = new DriftScoring();
        
        // Create game state
        gameState = new GameState();
    }
    
    /**
     * Start the game loop
     */
    public void start() {
        if (gameThread == null || !running) {
            // Recreate render buffer with actual screen size
            renderer.createRenderBuffer();
            
            running = true;
            gameThread = new Thread(this);
            gameThread.start();
        }
    }
    
    /**
     * Stop the game loop
     */
    public void stop() {
        running = false;
        try {
            if (gameThread != null) {
                gameThread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void run() {
        lastTime = System.nanoTime();
        accumulator = 0;
        fpsTimer = System.currentTimeMillis();
        frameCount = 0;
        
        while (running) {
            long currentTime = System.nanoTime();
            double frameTime = (currentTime - lastTime) / 1_000_000_000.0;
            lastTime = currentTime;
            
            // Cap frame time to avoid spiral of death
            if (frameTime > 0.25) {
                frameTime = 0.25;
            }
            
            accumulator += frameTime;
            
            // Fixed timestep updates
            while (accumulator >= FIXED_TIMESTEP) {
                update(FIXED_TIMESTEP);
                accumulator -= FIXED_TIMESTEP;
            }
            
            // Render
            repaint();
            frameCount++;
            
            // FPS counter
            if (System.currentTimeMillis() - fpsTimer >= 1000) {
                fps = frameCount;
                frameCount = 0;
                fpsTimer = System.currentTimeMillis();
            }
            
            // Frame limiting
            try {
                long sleepTime = (long)((FIXED_TIMESTEP - frameTime) * 1000);
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Update game logic
     * SFX NOTE: Multiple sound effect triggers in this method - see comments
     */
    private void update(double dt) {
        // Update input (processes key states into smooth values)
        input.update(dt);
        
        // Handle pause
        if (input.isPausePressed()) {
            gameState.togglePause();
            // SFX NOTE: Play pause menu sound here
        }
        
        // Handle reset
        if (input.isResetPressed()) {
            resetGame();
            // SFX NOTE: Play reset/respawn sound here
        }
        
        // Only update if playing
        if (!gameState.isPlaying()) {
            input.consumeOneShots(); // Still consume inputs when paused
            return;
        }
        
        // Update game state
        gameState.update(dt);
        
        // Handle manual shifting - check BEFORE consuming one-shots
        if (input.isShiftUpPressed()) {
            car.shiftUp();
            // SFX NOTE: Play gear shift up sound here (higher pitch)
        }
        if (input.isShiftDownPressed()) {
            car.shiftDown();
            // SFX NOTE: Play gear shift down sound here (lower pitch, maybe with rev match blip)
        }
        
        // Consume one-shot inputs after they've been processed
        input.consumeOneShots();
        
        // Update car with input
        car.update(dt,
            input.getThrottle(),
            input.getBrake(),
            input.getSteering(),
            input.isHandbrakePressed()
        );
        
        // Update scoring
        scoring.update(
            car.isDrifting(),
            car.getDriftAngle(),
            car.getSpeed(),
            dt
        );
        
        // Update particles
        particles.update(dt);
        
        // Update camera with velocity look-ahead
        camera.update(dt,
            car.getPosition(),
            car.getRotation(),
            car.isDrifting(),
            car.getDriftAngle(),
            car.getPhysics().getVelocity()
        );
        
        // Camera shake on wheelspin
        if (car.getPhysics().isWheelspinning() && car.isAccelerating()) {
            camera.shake(0.5, 0.1);
        }
        
        // Track distance
        gameState.addDistance(car.getSpeed() * dt);
    }
    
    /**
     * Reset the game
     */
    private void resetGame() {
        car.reset(0, 0);
        camera.setPosition(0, 0);
        particles.clear();
        scoring.reset();
        gameState.startNewSession();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // Render game
        renderer.render(g, world, car, scoring);
        
        // Draw pause overlay if paused
        if (gameState.isPaused()) {
            drawPauseOverlay(g);
        }
        
        // Draw FPS (debug)
        g.setColor(Color.GREEN);
        g.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g.drawString("FPS: " + fps, 10, GameConstants.WINDOW_HEIGHT - 10);
    }
    
    /**
     * Draw pause screen overlay
     */
    private void drawPauseOverlay(Graphics g) {
        // Darken screen
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
        
        // Pause text
        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.BOLD, 48));
        String pauseText = "PAUSED";
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(pauseText);
        g.drawString(pauseText, 
            (GameConstants.WINDOW_WIDTH - textWidth) / 2,
            GameConstants.WINDOW_HEIGHT / 2);
        
        // Instructions
        g.setFont(new Font("Monospaced", Font.PLAIN, 20));
        String instructions = "Press ESC or P to resume";
        fm = g.getFontMetrics();
        textWidth = fm.stringWidth(instructions);
        g.drawString(instructions,
            (GameConstants.WINDOW_WIDTH - textWidth) / 2,
            GameConstants.WINDOW_HEIGHT / 2 + 40);
    }
    
    /**
     * Cleanup resources
     */
    public void dispose() {
        stop();
        if (renderer != null) {
            renderer.dispose();
        }
    }
}
