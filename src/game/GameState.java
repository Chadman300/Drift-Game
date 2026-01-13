package game;

import util.GameConstants;

/**
 * Manages game state (playing, paused, menu, etc.)
 */
public class GameState {
    
    public enum State {
        MENU,
        PLAYING,
        PAUSED,
        GAME_OVER
    }
    
    private State currentState;
    private double gameTime;        // Total time playing
    private double sessionTime;     // Time in current session
    private boolean isPaused;
    
    // Session statistics
    private int sessionDrifts;
    private double sessionDistance;
    
    public GameState() {
        this.currentState = State.PLAYING; // Start directly in game for now
        this.gameTime = 0;
        this.sessionTime = 0;
        this.isPaused = false;
        this.sessionDrifts = 0;
        this.sessionDistance = 0;
    }
    
    /**
     * Update game state
     */
    public void update(double dt) {
        if (currentState == State.PLAYING && !isPaused) {
            gameTime += dt;
            sessionTime += dt;
        }
    }
    
    /**
     * Toggle pause
     */
    public void togglePause() {
        if (currentState == State.PLAYING) {
            isPaused = !isPaused;
            currentState = isPaused ? State.PAUSED : State.PLAYING;
        } else if (currentState == State.PAUSED) {
            isPaused = false;
            currentState = State.PLAYING;
        }
    }
    
    /**
     * Start new session
     */
    public void startNewSession() {
        currentState = State.PLAYING;
        sessionTime = 0;
        sessionDrifts = 0;
        sessionDistance = 0;
        isPaused = false;
    }
    
    /**
     * End current session
     */
    public void endSession() {
        currentState = State.GAME_OVER;
    }
    
    /**
     * Go to menu
     */
    public void goToMenu() {
        currentState = State.MENU;
        isPaused = false;
    }
    
    // Track statistics
    public void addDrift() { sessionDrifts++; }
    public void addDistance(double dist) { sessionDistance += dist; }
    
    // Getters
    public State getCurrentState() { return currentState; }
    public double getGameTime() { return gameTime; }
    public double getSessionTime() { return sessionTime; }
    public boolean isPaused() { return isPaused; }
    public int getSessionDrifts() { return sessionDrifts; }
    public double getSessionDistance() { return sessionDistance; }
    
    public boolean isPlaying() { return currentState == State.PLAYING && !isPaused; }
    
    /**
     * Pause the game (for shop, menus, etc.)
     */
    public void pause() {
        if (currentState == State.PLAYING) {
            isPaused = true;
            currentState = State.PAUSED;
        }
    }
    
    /**
     * Resume the game from pause
     */
    public void resume() {
        if (currentState == State.PAUSED) {
            isPaused = false;
            currentState = State.PLAYING;
        }
    }
}
