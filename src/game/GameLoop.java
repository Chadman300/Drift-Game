package game;

import graphics.Camera;
import graphics.ParticleSystem;
import graphics.Renderer;
import input.InputHandler;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import scoring.DriftScoring;
import ui.ShopPanel;
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
    private Shop shop;
    private ShopPanel shopPanel;
    
    // Shop notification message
    private String shopMessage = "";
    private double shopMessageTimer = 0;
    
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
        setFocusTraversalKeysEnabled(false); // Allow TAB key to reach KeyListener
        
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
        
        // Create shop
        shop = new Shop();
        shop.setPlayerMoney(1000); // Starting money
        shopPanel = new ShopPanel(shop);
        
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
            
            // Request focus for keyboard input
            requestFocusInWindow();
            
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
        
        // Update shop message timer
        if (shopMessageTimer > 0) {
            shopMessageTimer -= dt;
            if (shopMessageTimer <= 0) {
                shopMessage = "";
            }
        }
        
        // Handle shop toggle (TAB key)
        if (input.isShopPressed()) {
            shopPanel.toggle();
            if (shopPanel.isVisible()) {
                gameState.pause();
            } else {
                gameState.resume();
                applyShopUpgrades(); // Apply upgrades when closing shop
            }
        }
        
        // Handle shop navigation when open
        if (shopPanel.isVisible()) {
            // ESC closes shop instead of pausing game
            if (input.isPausePressed()) {
                shopPanel.hide();
                gameState.resume();
                applyShopUpgrades();
                input.consumeOneShots();
                return;
            }
            if (input.isShopNextCategory()) {
                shopPanel.nextCategory();
            }
            if (input.isShopPrevCategory()) {
                shopPanel.prevCategory();
            }
            if (input.isShopSelectNext()) {
                shopPanel.selectNext();
            }
            if (input.isShopSelectPrev()) {
                shopPanel.selectPrev();
            }
            if (input.isShopConfirm()) {
                shopMessage = shopPanel.confirmSelection();
                shopMessageTimer = 2.0;
                applyShopUpgrades();
            }
            input.consumeOneShots();
            return; // Don't update game when shop is open
        }
        
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
        
        // Award money when drift ends (points get banked)
        int earnedMoney = scoring.getLastBankedPoints() / 10; // 10 points = $1
        if (earnedMoney > 0) {
            shop.addMoney(earnedMoney);
        }
        
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
        
        // Render game with shop panel (rendered at pixel art resolution)
        renderer.render(g, world, car, scoring, shopPanel);
        
        // Draw money display (only when shop is closed)
        if (!shopPanel.isVisible()) {
            g.setColor(new Color(100, 255, 100));
            g.setFont(new Font("Monospaced", Font.BOLD, 16));
            g.drawString("$" + String.format("%,d", shop.getPlayerMoney()), 10, 25);
            g.setFont(new Font("Monospaced", Font.PLAIN, 11));
            g.setColor(new Color(180, 180, 180));
            g.drawString("[TAB] Shop", 10, 40);
        }
        
        // Draw shop message
        if (!shopMessage.isEmpty()) {
            g.setFont(new Font("Monospaced", Font.BOLD, 18));
            FontMetrics fm = g.getFontMetrics();
            int msgWidth = fm.stringWidth(shopMessage);
            int msgX = (GameConstants.WINDOW_WIDTH - msgWidth) / 2;
            int msgY = GameConstants.WINDOW_HEIGHT - 100;
            
            // Background
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRoundRect(msgX - 15, msgY - 20, msgWidth + 30, 35, 10, 10);
            
            // Text
            g.setColor(shopMessage.contains("Not enough") ? new Color(255, 80, 80) : new Color(100, 255, 100));
            g.drawString(shopMessage, msgX, msgY);
        }
        
        // Draw pause overlay if paused (but not if shop is open)
        if (gameState.isPaused() && !shopPanel.isVisible()) {
            drawPauseOverlay(g);
        }
        
        // Draw FPS and debug info
        g.setColor(Color.GREEN);
        g.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g.drawString("FPS: " + fps, 10, GameConstants.WINDOW_HEIGHT - 10);
        
        // Debug: Show RPM info
        int actualRpm = (int) car.getPhysics().getEngine().getRpm();
        int gear = car.getPhysics().getEngine().getCurrentGear();
        boolean revLimiter = car.getPhysics().getEngine().isRevLimiterActive();
        String rpmText = "RPM: " + actualRpm + " | Gear: " + gear + (revLimiter ? " [REV LIMITER]" : "");
        g.drawString(rpmText, 100, GameConstants.WINDOW_HEIGHT - 10);
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
    
    /**
     * Apply shop upgrades to car physics
     */
    private void applyShopUpgrades() {
        double steering = shop.getModifier(ShopItem.Category.STEERING);
        double power = shop.getModifier(ShopItem.Category.ENGINE);
        double grip = shop.getModifier(ShopItem.Category.TIRES);
        double handling = shop.getModifier(ShopItem.Category.SUSPENSION);
        double weight = shop.getModifier(ShopItem.Category.WEIGHT);
        double tireDur = shop.getTireDurability();
        
        car.getPhysics().applyUpgrades(steering, power, grip, handling, weight, tireDur);
    }
}
