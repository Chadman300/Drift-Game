package graphics;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;

import game.Car;
import physics.Tire;
import physics.VehiclePhysics;
import scoring.DriftScoring;
import util.GameConstants;
import util.MathUtils;
import world.Building;
import world.CityWorld;
import world.Road;

/**
 * Main rendering system for the game
 * Handles all drawing operations with pixel art style
 */
public class Renderer {
    
    // Render buffer (low resolution for pixel art effect)
    private BufferedImage renderBuffer;
    private Graphics2D bufferGraphics;
    
    // References
    private Camera camera;
    private ParticleSystem particles;
    
    // Colors (retro palette)
    private Color roadColor = new Color(0x2D3436);
    private Color roadLineColor = new Color(0xDFE6E9);
    private Color sidewalkColor = new Color(0x636E72);
    private Color grassColor = new Color(0x00B894);
    
    // Car sprite colors
    private Color carBodyColor = new Color(0xE94560);
    private Color carWindowColor = new Color(0x1A1A2E);
    private Color carWheelColor = new Color(0x0D0D0D);
    private Color carHeadlightColor = new Color(0xFFE66D);
    private Color carTaillightColor = new Color(0xFF6B6B);
    
    public Renderer(Camera camera, ParticleSystem particles) {
        this.camera = camera;
        this.particles = particles;
        
        // Create low-res render buffer (will be recreated when screen size is set)
        createRenderBuffer();
    }
    
    /**
     * Create or recreate the render buffer based on current screen size
     */
    public void createRenderBuffer() {
        renderBuffer = new BufferedImage(
            GameConstants.RENDER_WIDTH,
            GameConstants.RENDER_HEIGHT,
            BufferedImage.TYPE_INT_RGB
        );
        bufferGraphics = renderBuffer.createGraphics();
        
        // Disable anti-aliasing for crisp pixels
        bufferGraphics.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_OFF
        );
        bufferGraphics.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        );
    }
    
    /**
     * Render the entire game frame
     */
    public void render(Graphics g, CityWorld world, Car car, DriftScoring scoring) {
        // Clear buffer
        bufferGraphics.setColor(new Color(0x1A1A2E)); // Dark background
        bufferGraphics.fillRect(0, 0, GameConstants.RENDER_WIDTH, GameConstants.RENDER_HEIGHT);
        
        // Get visible bounds for culling
        double[] bounds = camera.getVisibleBounds();
        
        // Draw world layers
        drawRoads(world, bounds);
        drawTireMarks();
        drawBuildings(world, bounds);
        drawParticles();
        drawCar(car);
        
        // Draw HUD on top
        drawHUD(car, scoring);
        
        // Scale up to window (pixel art effect)
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        );
        g2d.drawImage(renderBuffer, 0, 0, 
                      GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT, null);
    }
    
    /**
     * Draw all roads
     */
    private void drawRoads(CityWorld world, double[] bounds) {
        for (Road road : world.getRoads()) {
            double[] roadBounds = road.getBounds();
            
            // Culling check
            if (roadBounds[0] + roadBounds[2] < bounds[0] || roadBounds[0] > bounds[2] ||
                roadBounds[1] + roadBounds[3] < bounds[1] || roadBounds[1] > bounds[3]) {
                continue;
            }
            
            // Convert to screen coordinates
            int sx = camera.worldToScreenX(roadBounds[0]);
            int sy = camera.worldToScreenY(roadBounds[1]);
            int sw = camera.scaleToScreen(roadBounds[2]);
            int sh = camera.scaleToScreen(roadBounds[3]);
            
            // Draw road surface
            bufferGraphics.setColor(roadColor);
            bufferGraphics.fillRect(sx, sy, sw, sh);
            
            // Draw road markings
            bufferGraphics.setColor(roadLineColor);
            if (road.isVertical()) {
                int centerX = sx + sw / 2;
                // Dashed center line
                for (int y = sy; y < sy + sh; y += 8) {
                    bufferGraphics.drawLine(centerX, y, centerX, y + 4);
                }
            } else {
                int centerY = sy + sh / 2;
                for (int x = sx; x < sx + sw; x += 8) {
                    bufferGraphics.drawLine(x, centerY, x + 4, centerY);
                }
            }
        }
    }
    
    /**
     * Draw tire marks on the road
     */
    private void drawTireMarks() {
        bufferGraphics.setColor(new Color(0x1A1A1A));
        
        for (ParticleSystem.TireMark mark : particles.getTireMarks()) {
            if (!camera.isVisible(mark.x, mark.y, 5)) continue;
            
            int sx = camera.worldToScreenX(mark.x);
            int sy = camera.worldToScreenY(mark.y);
            int size = Math.max(1, camera.scaleToScreen(mark.width));
            
            // Set alpha
            int alpha = (int)(mark.alpha * 150);
            bufferGraphics.setColor(new Color(0x1A, 0x1A, 0x1A, alpha));
            bufferGraphics.fillOval(sx - size/2, sy - size/2, size, size);
        }
    }
    
    /**
     * Draw buildings
     */
    private void drawBuildings(CityWorld world, double[] bounds) {
        for (Building building : world.getBuildings()) {
            // Culling
            if (building.getX() + building.getWidth() < bounds[0] || 
                building.getX() > bounds[2] ||
                building.getY() + building.getHeight() < bounds[1] || 
                building.getY() > bounds[3]) {
                continue;
            }
            
            int sx = camera.worldToScreenX(building.getX());
            int sy = camera.worldToScreenY(building.getY());
            int sw = camera.scaleToScreen(building.getWidth());
            int sh = camera.scaleToScreen(building.getHeight());
            
            // Building shadow
            bufferGraphics.setColor(new Color(0, 0, 0, 80));
            bufferGraphics.fillRect(sx + 2, sy + 2, sw, sh);
            
            // Main building color
            int colorIdx = building.getColorIndex() % GameConstants.COLOR_PALETTE.length;
            Color buildingColor = new Color(GameConstants.COLOR_PALETTE[colorIdx]);
            bufferGraphics.setColor(buildingColor);
            bufferGraphics.fillRect(sx, sy, sw, sh);
            
            // Building outline
            bufferGraphics.setColor(buildingColor.darker());
            bufferGraphics.drawRect(sx, sy, sw - 1, sh - 1);
            
            // Windows (if building is large enough on screen)
            if (sw > 8 && sh > 8) {
                bufferGraphics.setColor(new Color(0xFFE66D, true)); // Yellow windows
                int windowSpacing = 4;
                for (int wx = sx + 2; wx < sx + sw - 2; wx += windowSpacing) {
                    for (int wy = sy + 2; wy < sy + sh - 2; wy += windowSpacing) {
                        if (Math.random() > 0.3) { // Some windows are lit
                            bufferGraphics.fillRect(wx, wy, 2, 2);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Draw particles (smoke, sparks, backfire)
     * SFX NOTE: Particle rendering is visual-only, sounds are triggered in ParticleSystem spawn methods
     */
    private void drawParticles() {
        for (ParticleSystem.Particle p : particles.getParticles()) {
            if (!camera.isVisible(p.x, p.y, p.size * 2)) continue;
            
            int sx = camera.worldToScreenX(p.x);
            int sy = camera.worldToScreenY(p.y);
            int size = Math.max(1, (int)p.size);
            
            int alpha = (int)(p.getAlpha() * 200);
            alpha = Math.min(255, Math.max(0, alpha));
            
            switch (p.type) {
                case SMOKE:
                    // Realistic gray-white smoke that fades
                    int smokeGray = 180 + (int)(Math.random() * 40);
                    bufferGraphics.setColor(new Color(smokeGray, smokeGray, smokeGray, alpha));
                    bufferGraphics.fillOval(sx - size/2, sy - size/2, size, size);
                    // Softer inner core
                    if (size > 2) {
                        int innerAlpha = alpha / 2;
                        bufferGraphics.setColor(new Color(220, 220, 220, innerAlpha));
                        bufferGraphics.fillOval(sx - size/4, sy - size/4, size/2, size/2);
                    }
                    break;
                    
                case SPARK:
                    // Bright yellow-orange sparks
                    bufferGraphics.setColor(new Color(255, 200, 50, alpha));
                    bufferGraphics.fillRect(sx, sy, 1, 1);
                    break;
                    
                case DUST:
                    bufferGraphics.setColor(new Color(139, 119, 101, alpha));
                    bufferGraphics.fillOval(sx - size/2, sy - size/2, size, size);
                    break;
                    
                case BACKFIRE_FLAME:
                    // Hot flame colors - orange/yellow core with red edges
                    float lifeRatio = (float)(p.lifetime / p.maxLifetime);
                    int r = 255;
                    int g = (int)(150 + lifeRatio * 100); // Yellower when fresh
                    int b = (int)(50 * lifeRatio);
                    bufferGraphics.setColor(new Color(r, g, b, Math.min(255, alpha + 55)));
                    bufferGraphics.fillOval(sx - size/2, sy - size/2, size, size);
                    // Bright white-yellow core
                    if (size > 1 && lifeRatio > 0.5) {
                        bufferGraphics.setColor(new Color(255, 255, 200, alpha));
                        bufferGraphics.fillOval(sx - size/4, sy - size/4, size/2, size/2);
                    }
                    break;
                    
                case BACKFIRE_SPARK:
                    // Bright orange-red sparks
                    bufferGraphics.setColor(new Color(255, 150, 50, alpha));
                    bufferGraphics.fillRect(sx, sy, 1, 1);
                    // Slight glow
                    if (p.lifetime > p.maxLifetime * 0.5) {
                        bufferGraphics.setColor(new Color(255, 200, 100, alpha / 3));
                        bufferGraphics.fillRect(sx - 1, sy - 1, 3, 3);
                    }
                    break;
            }
        }
    }
    
    /**
     * Draw the car with proper rotation
     */
    private void drawCar(Car car) {
        VehiclePhysics physics = car.getPhysics();
        
        int cx = camera.worldToScreenX(physics.getPosition().x);
        int cy = camera.worldToScreenY(physics.getPosition().y);
        
        // Car dimensions in pixels
        int carLength = camera.scaleToScreen(GameConstants.CAR_LENGTH);
        int carWidth = camera.scaleToScreen(GameConstants.CAR_WIDTH);
        
        // Save transform
        AffineTransform oldTransform = bufferGraphics.getTransform();
        
        // Rotate around car center
        bufferGraphics.translate(cx, cy);
        bufferGraphics.rotate(physics.getRotation());
        
        // Draw shadow
        bufferGraphics.setColor(new Color(0, 0, 0, 100));
        bufferGraphics.fillRect(-carLength/2 + 1, -carWidth/2 + 1, carLength, carWidth);
        
        // Draw wheels (they rotate with steering)
        drawWheels(physics, carLength, carWidth);
        
        // Draw car body
        bufferGraphics.setColor(carBodyColor);
        bufferGraphics.fillRect(-carLength/2, -carWidth/2, carLength, carWidth);
        
        // Car outline
        bufferGraphics.setColor(carBodyColor.darker());
        bufferGraphics.drawRect(-carLength/2, -carWidth/2, carLength - 1, carWidth - 1);
        
        // Windshield
        bufferGraphics.setColor(carWindowColor);
        int windowOffset = carLength / 4;
        bufferGraphics.fillRect(windowOffset - 2, -carWidth/2 + 1, carLength/4, carWidth - 2);
        
        // Headlights
        bufferGraphics.setColor(carHeadlightColor);
        bufferGraphics.fillRect(carLength/2 - 2, -carWidth/2 + 1, 2, 2);
        bufferGraphics.fillRect(carLength/2 - 2, carWidth/2 - 3, 2, 2);
        
        // Taillights
        bufferGraphics.setColor(carTaillightColor);
        bufferGraphics.fillRect(-carLength/2, -carWidth/2 + 1, 2, 2);
        bufferGraphics.fillRect(-carLength/2, carWidth/2 - 3, 2, 2);
        
        // Brake lights (brighter when braking)
        if (car.isBraking()) {
            bufferGraphics.setColor(new Color(255, 50, 50));
            bufferGraphics.fillRect(-carLength/2, -carWidth/2 + 1, 2, carWidth - 2);
        }
        
        // Restore transform
        bufferGraphics.setTransform(oldTransform);
    }
    
    /**
     * Draw the four wheels with steering angle
     */
    private void drawWheels(VehiclePhysics physics, int carLength, int carWidth) {
        int wheelLength = Math.max(2, carLength / 5);
        int wheelWidth = Math.max(1, carWidth / 5);
        
        double steeringAngle = physics.getSteeringAngle();
        
        Tire[] tires = physics.getTires();
        
        // Wheel positions relative to car center
        int[][] wheelPositions = {
            {carLength/3, -carWidth/2 - 1},      // Front left
            {carLength/3, carWidth/2 - wheelWidth + 1},   // Front right
            {-carLength/3, -carWidth/2 - 1},     // Rear left
            {-carLength/3, carWidth/2 - wheelWidth + 1}   // Rear right
        };
        
        for (int i = 0; i < 4; i++) {
            AffineTransform wheelTransform = bufferGraphics.getTransform();
            
            int wx = wheelPositions[i][0];
            int wy = wheelPositions[i][1];
            
            bufferGraphics.translate(wx, wy + wheelWidth/2);
            
            // Front wheels turn with steering
            if (i < 2) {
                bufferGraphics.rotate(steeringAngle);
            }
            
            // Wheel color (red tint if spinning)
            if (tires[i].isSpinning()) {
                bufferGraphics.setColor(new Color(100, 30, 30));
            } else if (tires[i].isLocked()) {
                bufferGraphics.setColor(new Color(80, 80, 80));
            } else {
                bufferGraphics.setColor(carWheelColor);
            }
            
            bufferGraphics.fillRect(-wheelLength/2, -wheelWidth/2, wheelLength, wheelWidth);
            
            bufferGraphics.setTransform(wheelTransform);
        }
    }
    
    /**
     * Draw the HUD (heads-up display)
     * Organized with a dark bar at bottom for gauges and a clean top area for scores
     */
    private void drawHUD(Car car, DriftScoring scoring) {
        VehiclePhysics physics = car.getPhysics();
        
        // ====== DRAW BLACK HUD BAR AT BOTTOM ======
        int hudBarHeight = GameConstants.HUD_BAR_HEIGHT;
        int hudBarY = GameConstants.RENDER_HEIGHT - hudBarHeight;
        
        // Semi-transparent black bar background
        bufferGraphics.setColor(new Color(0, 0, 0, 200));
        bufferGraphics.fillRect(0, hudBarY, GameConstants.RENDER_WIDTH, hudBarHeight);
        
        // Subtle top border for the HUD bar
        bufferGraphics.setColor(new Color(0x4ECDC4, true));
        bufferGraphics.drawLine(0, hudBarY, GameConstants.RENDER_WIDTH, hudBarY);
        
        // ====== LEFT SECTION: RPM GAUGE + GEAR ======
        int gaugeX = 50;
        int gaugeY = hudBarY + hudBarHeight/2 + 5;
        drawRPMGauge(physics.getEngine(), gaugeX, gaugeY);
        
        // Gear display - left of gauge
        bufferGraphics.setFont(new Font("Monospaced", Font.BOLD, 22));
        int gear = physics.getEngine().getCurrentGear();
        String gearText = gear == 0 ? "N" : (gear == -1 ? "R" : String.valueOf(gear));
        drawHUDText(gearText, gaugeX - 35, gaugeY + 8, new Color(0x4ECDC4));
        
        // ====== CENTER SECTION: SPEED ======
        bufferGraphics.setFont(new Font("Monospaced", Font.BOLD, 28));
        int speed = (int) physics.getSpeedMph();
        String speedText = String.format("%d", speed);
        int speedTextWidth = bufferGraphics.getFontMetrics().stringWidth(speedText);
        int speedX = GameConstants.RENDER_WIDTH/2 - speedTextWidth/2;
        drawHUDText(speedText, speedX, gaugeY + 5, Color.WHITE);
        bufferGraphics.setFont(new Font("Monospaced", Font.BOLD, 10));
        drawHUDText("MPH", speedX + speedTextWidth + 5, gaugeY + 3, Color.GRAY);
        
        // ====== RIGHT SECTION: TIRE INFO ======
        bufferGraphics.setFont(new Font("Monospaced", Font.PLAIN, 10));
        Tire[] tires = physics.getTires();
        double avgTemp = 0;
        double avgGrip = 0;
        for (Tire t : tires) {
            avgTemp += t.getTemperature();
            avgGrip += t.getGrip();
        }
        avgTemp /= 4;
        avgGrip /= 4;
        
        String tireTemp = String.format("TIRE: %.0f°C", avgTemp);
        String tireGrip = String.format("GRIP: %.0f%%", avgGrip * 100);
        drawHUDText(tireTemp, GameConstants.RENDER_WIDTH - 80, gaugeY - 5, Color.GRAY);
        drawHUDText(tireGrip, GameConstants.RENDER_WIDTH - 80, gaugeY + 8, 
                    avgGrip < 0.7 ? new Color(0xE94560) : Color.GRAY);
        
        // Handbrake indicator (in HUD bar)
        if (physics.isHandbrakeActive()) {
            bufferGraphics.setFont(new Font("Monospaced", Font.BOLD, 10));
            drawHUDText("[HB]", GameConstants.RENDER_WIDTH - 80, gaugeY + 20, new Color(0xFF6B6B));
        }
        
        // Wheelspin indicator (in HUD bar, center)
        bufferGraphics.setFont(new Font("Monospaced", Font.BOLD, 12));
        if (physics.isWheelspinning()) {
            drawHUDText("!! WHEELSPIN !!", GameConstants.RENDER_WIDTH/2 - 50, 
                        gaugeY + 18, new Color(0xE94560));
        }
        
        // ====== TOP AREA: SCORES AND DRIFT INFO ======
        
        // Total score - top right
        bufferGraphics.setFont(new Font("Monospaced", Font.BOLD, 14));
        String totalScore = String.format("SCORE: %,d", scoring.getTotalScore());
        drawHUDText(totalScore, GameConstants.RENDER_WIDTH - 120, 18, Color.WHITE);
        
        // Combo multiplier - below total score
        bufferGraphics.setFont(new Font("Monospaced", Font.BOLD, 16));
        if (scoring.getComboMultiplier() > 1) {
            String comboText = "x" + scoring.getComboMultiplier();
            drawHUDText(comboText, GameConstants.RENDER_WIDTH - 60, 38, new Color(0x4ECDC4));
            
            // Combo timer bar
            int barWidth = 50;
            int barX = GameConstants.RENDER_WIDTH - 60;
            int barY = 45;
            int filledWidth = (int)(barWidth * scoring.getComboTimerPercentage());
            
            bufferGraphics.setColor(new Color(0x2D3436));
            bufferGraphics.fillRect(barX, barY, barWidth, 4);
            bufferGraphics.setColor(new Color(0x4ECDC4));
            bufferGraphics.fillRect(barX, barY, filledWidth, 4);
        }
        
        // Drift score - top center (only when drifting)
        if (scoring.isInDrift()) {
            String driftScore = String.format("%,d", (int)scoring.getCurrentDriftScore());
            String grade = scoring.getDriftGrade();
            
            // Grade
            bufferGraphics.setFont(new Font("Monospaced", Font.BOLD, 20));
            int gradeWidth = bufferGraphics.getFontMetrics().stringWidth(grade);
            drawHUDText(grade, GameConstants.RENDER_WIDTH/2 - gradeWidth/2, 35, new Color(0xFFE66D));
            
            // Score
            bufferGraphics.setFont(new Font("Monospaced", Font.BOLD, 18));
            int scoreWidth = bufferGraphics.getFontMetrics().stringWidth(driftScore);
            drawHUDText(driftScore, GameConstants.RENDER_WIDTH/2 - scoreWidth/2, 58, Color.WHITE);
            
            // Drift angle
            bufferGraphics.setFont(new Font("Monospaced", Font.BOLD, 14));
            String angleText = String.format("%.0f°", Math.abs(scoring.getCurrentDriftAngle()));
            int angleWidth = bufferGraphics.getFontMetrics().stringWidth(angleText);
            drawHUDText(angleText, GameConstants.RENDER_WIDTH/2 - angleWidth/2, 75, new Color(0xE94560));
        }
        
        // Controls hint - top left (faded)
        bufferGraphics.setFont(new Font("Monospaced", Font.PLAIN, 8));
        drawHUDText("WASD: Drive  SPACE: Handbrake  E/Q: Shift  ESC: Exit", 
                   10, 12, new Color(80, 80, 80));
    }
    
    /**
     * Draw RPM gauge (tachometer style) with rev limiter bounce effect
     * The needle bounces back when hitting the rev limiter for visual feedback
     */
    private void drawRPMGauge(physics.Engine engine, int centerX, int centerY) {
        int radius = 35; // Slightly smaller to fit in HUD bar
        double rpmPercent = engine.getRpmPercentage();
        boolean revLimiter = engine.isRevLimiterActive();
        double bounce = engine.getRevLimiterBounce();
        int rpm = (int) engine.getRpm();
        
        // Apply bounce effect to displayed RPM percentage
        double displayPercent = rpmPercent + bounce;
        displayPercent = Math.max(0, Math.min(1.0, displayPercent));
        
        // Draw gauge background (semi-circle arc)
        bufferGraphics.setColor(new Color(0x1A1A2E));
        bufferGraphics.fillArc(centerX - radius, centerY - radius, 
                               radius * 2, radius * 2, 0, 180);
        
        // Draw tick marks and numbers
        bufferGraphics.setFont(new Font("Monospaced", Font.PLAIN, 6));
        for (int i = 0; i <= 8; i++) {
            double tickRpm = i * 1000.0 / 8000.0; // 0 to 8000 RPM
            double angle = Math.PI - (tickRpm * Math.PI); // 180 to 0 degrees
            
            int innerRadius = radius - 6;
            int outerRadius = radius - 2;
            
            int x1 = centerX + (int)(Math.cos(angle) * innerRadius);
            int y1 = centerY - (int)(Math.sin(angle) * innerRadius);
            int x2 = centerX + (int)(Math.cos(angle) * outerRadius);
            int y2 = centerY - (int)(Math.sin(angle) * outerRadius);
            
            // Red zone for high RPM (above 6000)
            if (i >= 6) {
                bufferGraphics.setColor(new Color(0xE94560));
            } else {
                bufferGraphics.setColor(Color.WHITE);
            }
            bufferGraphics.drawLine(x1, y1, x2, y2);
            
            // Draw numbers
            if (i % 2 == 0) {
                int numX = centerX + (int)(Math.cos(angle) * (radius - 12)) - 3;
                int numY = centerY - (int)(Math.sin(angle) * (radius - 12)) + 2;
                bufferGraphics.setColor(Color.GRAY);
                bufferGraphics.drawString(String.valueOf(i), numX, numY);
            }
        }
        
        // Draw needle with bounce effect
        double needleAngle = Math.PI - (displayPercent * Math.PI);
        int needleLength = radius - 10;
        int needleX = centerX + (int)(Math.cos(needleAngle) * needleLength);
        int needleY = centerY - (int)(Math.sin(needleAngle) * needleLength);
        
        // Needle color based on RPM - flashes when bouncing
        Color needleColor;
        if (revLimiter || bounce < -0.02) {
            // Flash between red and white when bouncing/at limiter
            needleColor = (System.currentTimeMillis() % 100 < 50) ? 
                          new Color(0xE94560) : new Color(0xFF8888);
        } else if (rpmPercent > 0.9) {
            needleColor = new Color(0xE94560); // Red at redline
        } else if (rpmPercent > 0.75) {
            needleColor = new Color(0xFCBF49); // Orange high RPM
        } else {
            needleColor = new Color(0x4ECDC4); // Cyan normal
        }
        
        bufferGraphics.setColor(needleColor);
        bufferGraphics.drawLine(centerX, centerY, needleX, needleY);
        bufferGraphics.drawLine(centerX + 1, centerY, needleX + 1, needleY);
        
        // Center dot
        bufferGraphics.setColor(Color.WHITE);
        bufferGraphics.fillOval(centerX - 2, centerY - 2, 4, 4);
        
        // RPM text below gauge
        bufferGraphics.setFont(new Font("Monospaced", Font.BOLD, 7));
        String rpmText = String.format("%d", rpm);
        int textWidth = bufferGraphics.getFontMetrics().stringWidth(rpmText);
        drawHUDText(rpmText, centerX - textWidth/2, centerY + 10, Color.WHITE);
        bufferGraphics.setFont(new Font("Monospaced", Font.PLAIN, 5));
        drawHUDText("RPM", centerX - 5, centerY + 16, Color.GRAY);
    }
    
    /**
     * Helper to draw outlined text
     */
    private void drawHUDText(String text, int x, int y, Color color) {
        // Shadow
        bufferGraphics.setColor(Color.BLACK);
        bufferGraphics.drawString(text, x + 1, y + 1);
        
        // Main text
        bufferGraphics.setColor(color);
        bufferGraphics.drawString(text, x, y);
    }
    
    public void dispose() {
        if (bufferGraphics != null) {
            bufferGraphics.dispose();
        }
    }
}
