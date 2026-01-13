package scoring;

import util.GameConstants;
import util.MathUtils;

/**
 * Handles drift scoring system
 * Calculates points based on drift angle, speed, and duration
 * 
 * SFX NOTE: Key sound trigger points in this class:
 * - startDrift(): Play drift start sound (tire screech beginning)
 * - endDrift(): Play score cash-in sound (cha-ching or similar)
 * - combo increase: Play combo level-up sound
 * - grade changes (C->B->A->S): Play grade achievement sound
 */
public class DriftScoring {
    
    // Current drift
    private boolean isInDrift;
    private double currentDriftScore;
    private double currentDriftAngle;
    private double currentDriftTime;
    private double currentDriftMaxAngle;
    
    // Combo system
    private int comboMultiplier;
    private double comboTimer;
    private static final double COMBO_TIMEOUT = GameConstants.COMBO_TIMEOUT;
    
    // Total scores
    private long totalScore;
    private long sessionBestDrift;
    private long allTimeBestDrift;
    
    // Statistics
    private int totalDrifts;
    private double totalDriftDistance;
    private double longestDriftTime;
    private double highestDriftAngle;
    
    // For shop money - tracks last banked points (consumed each frame)
    private int lastBankedPoints;
    
    public DriftScoring() {
        reset();
    }
    
    public void reset() {
        isInDrift = false;
        currentDriftScore = 0;
        currentDriftAngle = 0;
        currentDriftTime = 0;
        currentDriftMaxAngle = 0;
        comboMultiplier = 1;
        comboTimer = 0;
        totalScore = 0;
        sessionBestDrift = 0;
        totalDrifts = 0;
        totalDriftDistance = 0;
        longestDriftTime = 0;
        highestDriftAngle = 0;
    }
    
    /**
     * Update scoring based on current drift state
     * @param isDrifting whether car is currently drifting
     * @param driftAngle current drift angle in degrees
     * @param speed current speed in m/s
     * @param dt delta time
     */
    public void update(boolean isDrifting, double driftAngle, double speed, double dt) {
        // Update combo timer
        if (comboTimer > 0) {
            comboTimer -= dt;
            if (comboTimer <= 0) {
                comboMultiplier = 1;
            }
        }
        
        if (isDrifting) {
            if (!isInDrift) {
                // Starting new drift
                startDrift();
            }
            
            // Update current drift
            currentDriftAngle = Math.abs(driftAngle);
            currentDriftTime += dt;
            currentDriftMaxAngle = Math.max(currentDriftMaxAngle, currentDriftAngle);
            
            // Calculate score for this frame
            double frameScore = calculateFrameScore(currentDriftAngle, speed, dt);
            currentDriftScore += frameScore;
            
            // Track distance
            totalDriftDistance += speed * dt;
            
        } else {
            if (isInDrift) {
                // Ending drift
                endDrift();
            }
        }
    }
    
    /**
     * Start a new drift
     */
    private void startDrift() {
        isInDrift = true;
        currentDriftScore = 0;
        currentDriftTime = 0;
        currentDriftMaxAngle = 0;
    }
    
    /**
     * End current drift and bank the score
     */
    private void endDrift() {
        if (currentDriftScore > 0) {
            // Apply combo multiplier
            long finalScore = (long)(currentDriftScore * comboMultiplier);
            
            // Track for money reward (consumed by GameLoop)
            lastBankedPoints = (int) finalScore;
            
            // Add to total
            totalScore += finalScore;
            totalDrifts++;
            
            // Update bests
            if (finalScore > sessionBestDrift) {
                sessionBestDrift = finalScore;
            }
            if (finalScore > allTimeBestDrift) {
                allTimeBestDrift = finalScore;
            }
            
            // Update statistics
            if (currentDriftTime > longestDriftTime) {
                longestDriftTime = currentDriftTime;
            }
            if (currentDriftMaxAngle > highestDriftAngle) {
                highestDriftAngle = currentDriftMaxAngle;
            }
            
            // Increase combo
            comboMultiplier = Math.min(comboMultiplier + 1, 10);
            comboTimer = COMBO_TIMEOUT;
        }
        
        isInDrift = false;
        currentDriftScore = 0;
        currentDriftTime = 0;
        currentDriftAngle = 0;
        currentDriftMaxAngle = 0;
    }
    
    /**
     * Calculate score for a single frame of drifting
     */
    private double calculateFrameScore(double angle, double speed, double dt) {
        // Base score from angle
        double angleScore = angle * GameConstants.DRIFT_SCORE_MULTIPLIER;
        
        // Bonus for extreme angles
        if (angle > 45) {
            angleScore *= GameConstants.DRIFT_ANGLE_BONUS;
        }
        
        // Speed bonus
        double speedBonus = 1.0 + speed * GameConstants.SPEED_BONUS_MULTIPLIER;
        
        // Time bonus (longer drifts score more per second)
        double timeBonus = 1.0 + currentDriftTime * 0.2;
        
        return angleScore * speedBonus * timeBonus * dt;
    }
    
    /**
     * Called when drift is interrupted (crash, spin out, etc)
     */
    public void cancelDrift() {
        // Don't bank the score
        isInDrift = false;
        currentDriftScore = 0;
        currentDriftTime = 0;
        currentDriftAngle = 0;
        currentDriftMaxAngle = 0;
        
        // Reset combo
        comboMultiplier = 1;
        comboTimer = 0;
    }
    
    // Getters
    public boolean isInDrift() { return isInDrift; }
    public double getCurrentDriftScore() { return currentDriftScore; }
    public double getCurrentDriftAngle() { return currentDriftAngle; }
    public double getCurrentDriftTime() { return currentDriftTime; }
    public int getComboMultiplier() { return comboMultiplier; }
    public double getComboTimer() { return comboTimer; }
    public double getComboTimerPercentage() { return comboTimer / COMBO_TIMEOUT; }
    public long getTotalScore() { return totalScore; }
    public long getSessionBestDrift() { return sessionBestDrift; }
    public long getAllTimeBestDrift() { return allTimeBestDrift; }
    public int getTotalDrifts() { return totalDrifts; }
    public double getTotalDriftDistance() { return totalDriftDistance; }
    public double getLongestDriftTime() { return longestDriftTime; }
    public double getHighestDriftAngle() { return highestDriftAngle; }
    
    /**
     * Get and consume the last banked points (for money rewards)
     * Returns 0 if no points were banked since last call
     */
    public int getLastBankedPoints() {
        int points = lastBankedPoints;
        lastBankedPoints = 0; // Consume
        return points;
    }
    
    /**
     * Get grade for current drift
     */
    public String getDriftGrade() {
        if (!isInDrift) return "";
        
        if (currentDriftAngle > 60 && currentDriftScore > 5000) return "INSANE!";
        if (currentDriftAngle > 50 && currentDriftScore > 3000) return "AMAZING!";
        if (currentDriftAngle > 40 && currentDriftScore > 2000) return "GREAT!";
        if (currentDriftAngle > 30 && currentDriftScore > 1000) return "GOOD!";
        if (currentDriftAngle > 20) return "NICE!";
        return "DRIFT!";
    }
}
