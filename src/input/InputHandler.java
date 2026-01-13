package input;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashSet;
import java.util.Set;

/**
 * Handles keyboard input for the game
 * Tracks pressed keys for smooth input handling
 */
public class InputHandler implements KeyListener {
    
    private Set<Integer> pressedKeys;
    private Set<Integer> justPressedKeys;
    private Set<Integer> justReleasedKeys;
    
    // Game inputs (processed values)
    private double throttle;
    private double brake;
    private double steering;
    private boolean handbrake;
    private boolean shiftUp;
    private boolean shiftDown;
    private boolean pause;
    private boolean reset;
    
    // Shop inputs
    private boolean shopToggle;
    private boolean shopNextCat;
    private boolean shopPrevCat;
    private boolean shopSelectNext;
    private boolean shopSelectPrev;
    private boolean shopConfirm;
    
    // Input smoothing
    private double throttleSmooth;
    private double brakeSmooth;
    private double steeringSmooth;
    
    public InputHandler() {
        pressedKeys = new HashSet<>();
        justPressedKeys = new HashSet<>();
        justReleasedKeys = new HashSet<>();
        
        throttle = 0;
        brake = 0;
        steering = 0;
        handbrake = false;
        shiftUp = false;
        shiftDown = false;
        pause = false;
        reset = false;
    }
    
    /**
     * Update input state - call once per frame
     * NOTE: Call consumeOneShots() at end of frame after checking shift/pause/reset
     */
    public void update(double dt) {
        // Clear just pressed/released sets (these are set during keyPressed/keyReleased)
        justPressedKeys.clear();
        justReleasedKeys.clear();
        
        // NOTE: One-shot inputs (shiftUp, shiftDown, pause, reset) are NOT reset here.
        // They persist until consumeOneShots() is called after they've been checked.
        // This fixes the bug where shift inputs were cleared before GameLoop could read them.
        
        // Process raw key states into game inputs
        processInputs(dt);
    }
    
    /**
     * Reset one-shot inputs after they've been consumed by the game loop.
     * Call this AFTER checking isShiftUpPressed(), isShiftDownPressed(), etc.
     * SFX NOTE: This is where gear shift sound effects should be triggered
     */
    public void consumeOneShots() {
        shiftUp = false;
        shiftDown = false;
        pause = false;
        reset = false;
        shopToggle = false;
        shopNextCat = false;
        shopPrevCat = false;
        shopSelectNext = false;
        shopSelectPrev = false;
        shopConfirm = false;
    }
    
    /**
     * Process key states into smooth game inputs
     */
    private void processInputs(double dt) {
        // Target values based on keys
        double targetThrottle = isKeyPressed(KeyEvent.VK_W) || isKeyPressed(KeyEvent.VK_UP) ? 1.0 : 0.0;
        double targetBrake = isKeyPressed(KeyEvent.VK_S) || isKeyPressed(KeyEvent.VK_DOWN) ? 1.0 : 0.0;
        
        double targetSteering = 0;
        if (isKeyPressed(KeyEvent.VK_A) || isKeyPressed(KeyEvent.VK_LEFT)) {
            targetSteering -= 1;
        }
        if (isKeyPressed(KeyEvent.VK_D) || isKeyPressed(KeyEvent.VK_RIGHT)) {
            targetSteering += 1;
        }
        
        // Smooth inputs for more natural feel
        double inputSpeed = 5.0; // How fast inputs respond
        double returnSpeed = 8.0; // How fast inputs return to neutral
        
        // Throttle smoothing
        if (targetThrottle > throttleSmooth) {
            throttleSmooth += inputSpeed * dt;
            if (throttleSmooth > targetThrottle) throttleSmooth = targetThrottle;
        } else {
            throttleSmooth -= returnSpeed * dt;
            if (throttleSmooth < targetThrottle) throttleSmooth = targetThrottle;
        }
        
        // Brake smoothing
        if (targetBrake > brakeSmooth) {
            brakeSmooth += inputSpeed * dt;
            if (brakeSmooth > targetBrake) brakeSmooth = targetBrake;
        } else {
            brakeSmooth -= returnSpeed * dt;
            if (brakeSmooth < targetBrake) brakeSmooth = targetBrake;
        }
        
        // Steering smoothing
        double steerSpeed = targetSteering != 0 ? inputSpeed : returnSpeed;
        if (targetSteering > steeringSmooth) {
            steeringSmooth += steerSpeed * dt;
            if (steeringSmooth > targetSteering) steeringSmooth = targetSteering;
        } else if (targetSteering < steeringSmooth) {
            steeringSmooth -= steerSpeed * dt;
            if (steeringSmooth < targetSteering) steeringSmooth = targetSteering;
        }
        
        // Clamp values
        throttle = Math.max(0, Math.min(1, throttleSmooth));
        brake = Math.max(0, Math.min(1, brakeSmooth));
        steering = Math.max(-1, Math.min(1, steeringSmooth));
        
        // Handbrake (instant, no smoothing)
        handbrake = isKeyPressed(KeyEvent.VK_SPACE);
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        
        if (!pressedKeys.contains(key)) {
            justPressedKeys.add(key);
            
            // Handle one-shot inputs
            if (key == KeyEvent.VK_E || key == KeyEvent.VK_SHIFT) {
                shiftUp = true;
            }
            if (key == KeyEvent.VK_Q || key == KeyEvent.VK_CONTROL) {
                shiftDown = true;
            }
            if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_P) {
                pause = true;
            }
            if (key == KeyEvent.VK_R) {
                reset = true;
            }
            // Shop controls
            if (key == KeyEvent.VK_TAB) {
                shopToggle = true;
            }
            if (key == KeyEvent.VK_E) {
                shopNextCat = true;
            }
            if (key == KeyEvent.VK_Q) {
                shopPrevCat = true;
            }
            if (key == KeyEvent.VK_W || key == KeyEvent.VK_UP) {
                shopSelectPrev = true;
            }
            if (key == KeyEvent.VK_S || key == KeyEvent.VK_DOWN) {
                shopSelectNext = true;
            }
            if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) {
                shopConfirm = true;
            }
        }
        
        pressedKeys.add(key);
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        pressedKeys.remove(key);
        justReleasedKeys.add(key);
    }
    
    @Override
    public void keyTyped(KeyEvent e) {
        // Not used
    }
    
    // Query methods
    public boolean isKeyPressed(int keyCode) {
        return pressedKeys.contains(keyCode);
    }
    
    public boolean isKeyJustPressed(int keyCode) {
        return justPressedKeys.contains(keyCode);
    }
    
    public boolean isKeyJustReleased(int keyCode) {
        return justReleasedKeys.contains(keyCode);
    }
    
    // Game input getters
    public double getThrottle() { return throttle; }
    public double getBrake() { return brake; }
    public double getSteering() { return steering; }
    public boolean isHandbrakePressed() { return handbrake; }
    public boolean isShiftUpPressed() { return shiftUp; }
    public boolean isShiftDownPressed() { return shiftDown; }
    public boolean isPausePressed() { return pause; }
    public boolean isResetPressed() { return reset; }
    
    // Shop input getters
    public boolean isShopPressed() { return shopToggle; }
    public boolean isShopNextCategory() { return shopNextCat; }
    public boolean isShopPrevCategory() { return shopPrevCat; }
    public boolean isShopSelectNext() { return shopSelectNext; }
    public boolean isShopSelectPrev() { return shopSelectPrev; }
    public boolean isShopConfirm() { return shopConfirm; }
}
