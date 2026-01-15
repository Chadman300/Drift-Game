package graphics;

import game.Car;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import physics.Tire;
import physics.VehiclePhysics;
import scoring.DriftScoring;
import util.GameConstants;
import world.Building;
import world.CityWorld;
import world.ParkingLot;
import world.Road;
import world.StreetLight;

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
        render(g, world, car, scoring, null);
    }
    
    /**
     * Render the entire game frame with optional shop overlay
     */
    public void render(Graphics g, CityWorld world, Car car, DriftScoring scoring, ui.ShopPanel shopPanel) {
        // Clear buffer
        bufferGraphics.setColor(new Color(0x1A1A2E)); // Dark background
        bufferGraphics.fillRect(0, 0, GameConstants.RENDER_WIDTH, GameConstants.RENDER_HEIGHT);
        
        // Get visible bounds for culling
        double[] bounds = camera.getVisibleBounds();
        
        // Draw world layers
        drawRoads(world, bounds);
        drawParkingLots(world, bounds);
        drawTireMarks();
        drawBuildings(world, bounds);
        drawStreetLights(world, bounds);
        drawParticles();
        drawCar(car);
        
        // Draw HUD on top
        drawHUD(car, scoring);
        
        // Draw minimap (top-right corner, only when shop is closed)
        if (shopPanel == null || !shopPanel.isVisible()) {
            drawMinimap(world, car);
        }
        
        // Draw shop panel if visible (at pixel art resolution)
        if (shopPanel != null && shopPanel.isVisible()) {
            shopPanel.render(bufferGraphics, GameConstants.RENDER_WIDTH, GameConstants.RENDER_HEIGHT);
        }
        
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
     * Draw all roads with improved visuals
     */
    private void drawRoads(CityWorld world, double[] bounds) {
        // Road edge color (darker than road)
        Color roadEdgeColor = new Color(0x1A1A1A);
        Color sidewalkColor = new Color(0x4A4A50);
        Color mainRoadColor = new Color(0x38383D);
        
        for (Road road : world.getRoads()) {
            double[] roadBounds = road.getBounds();
            
            // Culling check
            if (roadBounds[0] + roadBounds[2] < bounds[0] || roadBounds[0] > bounds[2] ||
                roadBounds[1] + roadBounds[3] < bounds[1] || roadBounds[1] > bounds[3]) {
                continue;
            }
            
            if (road.isCurved()) {
                drawCurvedRoad(road);
            } else {
                drawStraightRoad(road, roadBounds, roadEdgeColor, sidewalkColor, mainRoadColor);
            }
        }
    }
    
    /**
     * Draw a straight road with details
     */
    private void drawStraightRoad(Road road, double[] roadBounds, Color edgeColor, Color sidewalkColor, Color mainRoadColor) {
        int sx = camera.worldToScreenX(roadBounds[0]);
        int sy = camera.worldToScreenY(roadBounds[1]);
        int sw = camera.scaleToScreen(roadBounds[2]);
        int sh = camera.scaleToScreen(roadBounds[3]);
        
        // Sidewalk edge (slightly wider than road)
        int edgeSize = Math.max(2, camera.scaleToScreen(3));
        bufferGraphics.setColor(sidewalkColor);
        if (road.isVertical()) {
            bufferGraphics.fillRect(sx - edgeSize, sy, sw + edgeSize * 2, sh);
        } else {
            bufferGraphics.fillRect(sx, sy - edgeSize, sw, sh + edgeSize * 2);
        }
        
        // Road surface - use different color for highways
        if (road.getRoadType() == Road.RoadType.HIGHWAY) {
            bufferGraphics.setColor(mainRoadColor);
        } else {
            bufferGraphics.setColor(roadColor);
        }
        bufferGraphics.fillRect(sx, sy, sw, sh);
        
        // Road edge lines
        bufferGraphics.setColor(edgeColor);
        if (road.isVertical()) {
            bufferGraphics.drawLine(sx, sy, sx, sy + sh);
            bufferGraphics.drawLine(sx + sw - 1, sy, sx + sw - 1, sy + sh);
        } else {
            bufferGraphics.drawLine(sx, sy, sx + sw, sy);
            bufferGraphics.drawLine(sx, sy + sh - 1, sx + sw, sy + sh - 1);
        }
        
        // Draw road markings
        if (road.getRoadType() == Road.RoadType.HIGHWAY) {
            // Double yellow line for highway
            bufferGraphics.setColor(new Color(0xD4A017)); // Yellow
            if (road.isVertical()) {
                int centerX = sx + sw / 2;
                bufferGraphics.drawLine(centerX - 1, sy, centerX - 1, sy + sh);
                bufferGraphics.drawLine(centerX + 1, sy, centerX + 1, sy + sh);
            } else {
                int centerY = sy + sh / 2;
                bufferGraphics.drawLine(sx, centerY - 1, sx + sw, centerY - 1);
                bufferGraphics.drawLine(sx, centerY + 1, sx + sw, centerY + 1);
            }
        } else {
            // Dashed center line for regular roads
            bufferGraphics.setColor(roadLineColor);
            if (road.isVertical()) {
                int centerX = sx + sw / 2;
                for (int y = sy; y < sy + sh; y += 10) {
                    bufferGraphics.drawLine(centerX, y, centerX, y + 5);
                }
            } else {
                int centerY = sy + sh / 2;
                for (int x = sx; x < sx + sw; x += 10) {
                    bufferGraphics.drawLine(x, centerY, x + 5, centerY);
                }
            }
        }
        
        // Lane markers for highways
        if (road.getRoadType() == Road.RoadType.HIGHWAY && sw > 10) {
            bufferGraphics.setColor(roadLineColor);
            if (road.isVertical()) {
                int lane1 = sx + sw / 4;
                int lane2 = sx + 3 * sw / 4;
                for (int y = sy; y < sy + sh; y += 12) {
                    bufferGraphics.drawLine(lane1, y, lane1, y + 6);
                    bufferGraphics.drawLine(lane2, y, lane2, y + 6);
                }
            } else {
                int lane1 = sy + sh / 4;
                int lane2 = sy + 3 * sh / 4;
                for (int x = sx; x < sx + sw; x += 12) {
                    bufferGraphics.drawLine(x, lane1, x + 6, lane1);
                    bufferGraphics.drawLine(x, lane2, x + 6, lane2);
                }
            }
        }
    }
    
    /**
     * Draw a curved road
     */
    private void drawCurvedRoad(Road road) {
        java.util.List<double[]> points = road.getCurvePoints();
        if (points == null || points.size() < 2) return;
        
        int halfWidth = camera.scaleToScreen(road.getWidth() / 2);
        
        // Draw road segments along the curve
        for (int i = 0; i < points.size() - 1; i++) {
            double[] p1 = points.get(i);
            double[] p2 = points.get(i + 1);
            
            int x1 = camera.worldToScreenX(p1[0]);
            int y1 = camera.worldToScreenY(p1[1]);
            int x2 = camera.worldToScreenX(p2[0]);
            int y2 = camera.worldToScreenY(p2[1]);
            
            // Draw thick line for road
            java.awt.Graphics2D g2d = (java.awt.Graphics2D) bufferGraphics;
            java.awt.Stroke oldStroke = g2d.getStroke();
            
            g2d.setStroke(new java.awt.BasicStroke(halfWidth * 2, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
            g2d.setColor(roadColor);
            g2d.drawLine(x1, y1, x2, y2);
            
            // Center line
            g2d.setStroke(new java.awt.BasicStroke(1));
            g2d.setColor(roadLineColor);
            if (i % 2 == 0) { // Dashed
                g2d.drawLine(x1, y1, x2, y2);
            }
            
            g2d.setStroke(oldStroke);
        }
    }
    
    /**
     * Draw parking lots
     */
    private void drawParkingLots(CityWorld world, double[] bounds) {
        Color parkingColor = new Color(0x2D2D35);
        Color lineColor = new Color(0x555560);
        Color[] carColors = {
            new Color(0xE74C3C), // Red
            new Color(0x3498DB), // Blue
            new Color(0x2ECC71), // Green
            new Color(0xF1C40F), // Yellow
            new Color(0x9B59B6), // Purple
            new Color(0x1ABC9C)  // Teal
        };
        
        for (ParkingLot lot : world.getParkingLots()) {
            // Check bounds
            if (lot.getX() + lot.getWidth() < bounds[0] || lot.getX() > bounds[2] ||
                lot.getY() + lot.getHeight() < bounds[1] || lot.getY() > bounds[3]) {
                continue;
            }
            
            int lx = camera.worldToScreenX(lot.getX());
            int ly = camera.worldToScreenY(lot.getY());
            int lw = camera.scaleToScreen(lot.getWidth());
            int lh = camera.scaleToScreen(lot.getHeight());
            
            // Draw parking lot surface
            bufferGraphics.setColor(parkingColor);
            bufferGraphics.fillRect(lx, ly, lw, lh);
            
            // Draw parking spaces
            for (ParkingLot.ParkingSpace space : lot.getSpaces()) {
                int spx = camera.worldToScreenX(space.x);
                int spy = camera.worldToScreenY(space.y);
                int spw = camera.scaleToScreen(space.width);
                int sph = camera.scaleToScreen(space.height);
                
                // Parking line
                bufferGraphics.setColor(lineColor);
                bufferGraphics.drawRect(spx, spy, spw, sph);
                
                // Draw parked car if present
                if (space.hasCar && spw > 4 && sph > 6) {
                    int carMargin = 1;
                    bufferGraphics.setColor(carColors[space.carColor % carColors.length]);
                    bufferGraphics.fillRect(spx + carMargin, spy + carMargin, 
                                           spw - carMargin * 2, sph - carMargin * 2);
                    // Car roof
                    bufferGraphics.setColor(new Color(0x1A1A2E));
                    int roofMargin = spw / 4;
                    bufferGraphics.fillRect(spx + roofMargin, spy + sph/3, 
                                           spw - roofMargin * 2, sph/3);
                }
            }
        }
    }
    
    /**
     * Draw street lights
     */
    private void drawStreetLights(CityWorld world, double[] bounds) {
        Color poleColor = new Color(0x3D3D3D);
        Color lightOnColor = new Color(0xFFF59D);
        Color glowColor = new Color(255, 245, 157, 60);
        
        for (StreetLight light : world.getStreetLights()) {
            if (!camera.isVisible(light.getX(), light.getY(), 20)) continue;
            
            int sx = camera.worldToScreenX(light.getX());
            int sy = camera.worldToScreenY(light.getY());
            
            // Draw pole
            bufferGraphics.setColor(poleColor);
            bufferGraphics.fillRect(sx - 1, sy - 1, 3, 3);
            
            // Draw light glow if on
            if (light.isOn()) {
                bufferGraphics.setColor(glowColor);
                bufferGraphics.fillOval(sx - 6, sy - 6, 12, 12);
                bufferGraphics.setColor(lightOnColor);
                bufferGraphics.fillOval(sx - 2, sy - 2, 4, 4);
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
            
            // Building shadow (offset based on building height)
            int shadowOffset = Math.max(2, building.getFloors() / 3);
            bufferGraphics.setColor(new Color(0, 0, 0, 100));
            bufferGraphics.fillRect(sx + shadowOffset, sy + shadowOffset, sw, sh);
            
            // Main building color
            int colorIdx = building.getColorIndex() % GameConstants.COLOR_PALETTE.length;
            Color buildingColor = new Color(GameConstants.COLOR_PALETTE[colorIdx]);
            bufferGraphics.setColor(buildingColor);
            bufferGraphics.fillRect(sx, sy, sw, sh);
            
            // Building outline (thicker for taller buildings)
            bufferGraphics.setColor(buildingColor.darker().darker());
            bufferGraphics.drawRect(sx, sy, sw - 1, sh - 1);
            if (building.getFloors() > 8) {
                bufferGraphics.drawRect(sx + 1, sy + 1, sw - 3, sh - 3);
            }
            
            // Roof detail for tall buildings
            if (building.getFloors() > 10 && sw > 12 && sh > 12) {
                bufferGraphics.setColor(buildingColor.darker());
                int roofMargin = sw / 5;
                bufferGraphics.fillRect(sx + roofMargin, sy + roofMargin, 
                                       sw - roofMargin * 2, sh - roofMargin * 2);
            }
            
            // Windows (if building is large enough on screen)
            if (sw > 6 && sh > 6) {
                // Window color varies by building
                Color windowColor = (colorIdx % 2 == 0) ? 
                    new Color(0xFFE66D) : new Color(0x87CEEB); // Yellow or blue windows
                Color windowOff = new Color(0x1A1A2E);
                
                int windowSpacing = Math.max(3, sw / 6);
                int windowSize = Math.max(1, windowSpacing / 2);
                
                for (int wx = sx + 2; wx < sx + sw - windowSize; wx += windowSpacing) {
                    for (int wy = sy + 2; wy < sy + sh - windowSize; wy += windowSpacing) {
                        // Use building position to seed random for consistent windows
                        boolean isLit = ((wx + wy + (int)building.getX()) % 3) != 0;
                        bufferGraphics.setColor(isLit ? windowColor : windowOff);
                        bufferGraphics.fillRect(wx, wy, windowSize, windowSize);
                    }
                }
            }
            
            // AC unit or antenna on some tall buildings
            if (building.getFloors() > 12 && sw > 10) {
                bufferGraphics.setColor(new Color(0x555555));
                bufferGraphics.fillRect(sx + sw/2 - 1, sy + 2, 2, 3);
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
        
        // Exhaust pipe (small dark metal pipe at rear right of car)
        bufferGraphics.setColor(new Color(0x3D3D3D)); // Dark metal
        int exhaustX = -carLength/2 - 1; // Just barely sticking out back
        int exhaustY = carWidth/2 - 3; // Right side near taillight
        bufferGraphics.fillRect(exhaustX, exhaustY, 2, 2); // Small pipe
        bufferGraphics.setColor(new Color(0x1A1A1A)); // Dark hole
        bufferGraphics.fillRect(exhaustX, exhaustY, 1, 1); // Pipe opening
        
        // Draw flame shooting from exhaust when rev limiter is active (backfire)
        boolean revLimiterActive = physics.getEngine().isRevLimiterActive();
        double rpm = physics.getEngine().getRpm();
        
        if (revLimiterActive) {
            // Bright pixelated flame shooting out the back
            bufferGraphics.setColor(new Color(255, 255, 100)); // White-yellow core
            bufferGraphics.fillRect(exhaustX - 1, exhaustY, 2, 2); // Core at exhaust
            bufferGraphics.setColor(new Color(255, 200, 50)); // Yellow middle
            bufferGraphics.fillRect(exhaustX - 3, exhaustY, 2, 2); // Flame body
            bufferGraphics.setColor(new Color(255, 100, 30)); // Orange tip
            bufferGraphics.fillRect(exhaustX - 5, exhaustY, 2, 2); // Flame tip
        } else if (rpm > 6000) {
            // Small exhaust glow at high RPM
            bufferGraphics.setColor(new Color(255, 100, 50, 150)); // Dim orange
            bufferGraphics.fillRect(exhaustX - 1, exhaustY, 1, 1);
        }
        
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
        
        // Understeer indicator - front tires losing grip (pushing wide)
        if (physics.isUndersteering()) {
            drawHUDText("!! UNDERSTEER !!", GameConstants.RENDER_WIDTH/2 - 55, 
                        gaugeY + 5, new Color(0xFCBF49)); // Orange/yellow color
        }
        
        // Oversteer indicator - rear tires sliding out (drifting!)
        if (physics.isOversteering() && !physics.isDrifting()) {
            // Show when rear is sliding but not in full scored drift yet
            drawHUDText(">> OVERSTEER <<", GameConstants.RENDER_WIDTH/2 - 52, 
                        gaugeY + 5, new Color(0x4ECDC4)); // Cyan color
        }
        
        // Bogging indicator (engine struggling in too high a gear)
        if (physics.getEngine().isBogging()) {
            bufferGraphics.setFont(new Font("Monospaced", Font.BOLD, 12));
            // Flash red when bogging badly
            int bogAlpha = (int)(150 + 105 * physics.getEngine().getBogIntensity());
            drawHUDText("!! SHIFT DOWN !!", GameConstants.RENDER_WIDTH/2 - 55, 
                        gaugeY - 15, new Color(255, 100, 50, bogAlpha));
        }
        
        // Rev limiter indicator
        if (physics.getEngine().isRevLimiterActive()) {
            bufferGraphics.setFont(new Font("Monospaced", Font.BOLD, 12));
            drawHUDText("!! REV LIMIT !!", GameConstants.RENDER_WIDTH/2 - 50, 
                        gaugeY - 15, new Color(255, 50, 50));
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
        
        // Draw tick marks and numbers - show 0-8k RPM scale
        // The gauge goes from 0 to 8000 RPM for easy reading
        double gaugeMaxRpm = 8000.0;
        bufferGraphics.setFont(new Font("Monospaced", Font.PLAIN, 6));
        for (int i = 0; i <= 8; i++) {
            double tickPercent = i / 8.0; // 0 to 1
            double angle = Math.PI - (tickPercent * Math.PI); // 180 to 0 degrees
            
            int innerRadius = radius - 6;
            int outerRadius = radius - 2;
            
            int x1 = centerX + (int)(Math.cos(angle) * innerRadius);
            int y1 = centerY - (int)(Math.sin(angle) * innerRadius);
            int x2 = centerX + (int)(Math.cos(angle) * outerRadius);
            int y2 = centerY - (int)(Math.sin(angle) * outerRadius);
            
            // Red zone for high RPM (6k and above)
            if (i >= 6) {
                bufferGraphics.setColor(new Color(0xE94560));
            } else {
                bufferGraphics.setColor(Color.WHITE);
            }
            bufferGraphics.drawLine(x1, y1, x2, y2);
            
            // Draw numbers (in thousands) - 0, 2, 4, 6, 8
            if (i % 2 == 0) {
                int numX = centerX + (int)(Math.cos(angle) * (radius - 12)) - 3;
                int numY = centerY - (int)(Math.sin(angle) * (radius - 12)) + 2;
                bufferGraphics.setColor(Color.GRAY);
                bufferGraphics.drawString(String.valueOf(i), numX, numY);
            }
        }
        
        // Calculate needle position based on actual RPM (0-8000 scale)
        double needlePercent = rpm / gaugeMaxRpm;
        needlePercent = Math.max(0, Math.min(1.0, needlePercent + bounce));
        
        // Draw needle
        double needleAngle = Math.PI - (needlePercent * Math.PI);
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
    
    /**
     * Draw the minimap in the top-left corner, below controls
     * Shows a zoomed-in area around the player with same colors as main map
     */
    private void drawMinimap(CityWorld world, Car car) {
        // Minimap dimensions (smaller)
        int mapSize = 55;  // Smaller minimap
        int margin = 8;
        int mapX = margin;  // Left side
        int mapY = 20;      // Below controls text
        
        // Zoomed in view - show area around player (300 units radius)
        double viewRadius = 300.0;
        util.Vector2D carPos = car.getPosition();
        double scale = mapSize / (viewRadius * 2);
        
        // Draw background (same as game background)
        bufferGraphics.setColor(new Color(0x1A1A2E));
        bufferGraphics.fillRect(mapX - 2, mapY - 2, mapSize + 4, mapSize + 4);
        
        // Draw border
        bufferGraphics.setColor(new Color(0x4ECDC4));
        bufferGraphics.drawRect(mapX - 2, mapY - 2, mapSize + 4, mapSize + 4);
        
        // Set clip to minimap area
        bufferGraphics.setClip(mapX, mapY, mapSize, mapSize);
        
        // Draw roads on minimap (same color as main map roads)
        bufferGraphics.setColor(roadColor);
        for (Road road : world.getRoads()) {
            double[] roadBounds = road.getBounds();
            
            // Convert world coords to minimap coords (centered on player)
            int rx = mapX + mapSize/2 + (int)((roadBounds[0] - carPos.x) * scale);
            int ry = mapY + mapSize/2 + (int)((roadBounds[1] - carPos.y) * scale);
            int rw = Math.max(1, (int)(roadBounds[2] * scale));
            int rh = Math.max(1, (int)(roadBounds[3] * scale));
            
            // Only draw if visible in minimap
            if (rx + rw >= mapX && rx <= mapX + mapSize &&
                ry + rh >= mapY && ry <= mapY + mapSize) {
                bufferGraphics.fillRect(rx, ry, rw, rh);
                
                // Draw road lines (same as main map)
                bufferGraphics.setColor(roadLineColor);
                if (road.isVertical()) {
                    int centerX = rx + rw / 2;
                    for (int y = ry; y < ry + rh; y += 4) {
                        bufferGraphics.drawLine(centerX, y, centerX, y + 2);
                    }
                } else {
                    int centerY = ry + rh / 2;
                    for (int x = rx; x < rx + rw; x += 4) {
                        bufferGraphics.drawLine(x, centerY, x + 2, centerY);
                    }
                }
                bufferGraphics.setColor(roadColor);
            }
        }
        
        // Draw parking lots on minimap
        Color parkingColor = new Color(0x2D2D35);
        for (ParkingLot lot : world.getParkingLots()) {
            int px = mapX + mapSize/2 + (int)((lot.getX() - carPos.x) * scale);
            int py = mapY + mapSize/2 + (int)((lot.getY() - carPos.y) * scale);
            int pw = Math.max(1, (int)(lot.getWidth() * scale));
            int ph = Math.max(1, (int)(lot.getHeight() * scale));
            
            if (px + pw >= mapX && px <= mapX + mapSize &&
                py + ph >= mapY && py <= mapY + mapSize) {
                bufferGraphics.setColor(parkingColor);
                bufferGraphics.fillRect(px, py, pw, ph);
            }
        }
        
        // Draw buildings on minimap (same colors as main map)
        for (Building building : world.getBuildings()) {
            int bx = mapX + mapSize/2 + (int)((building.getX() - carPos.x) * scale);
            int by = mapY + mapSize/2 + (int)((building.getY() - carPos.y) * scale);
            int bw = Math.max(1, (int)(building.getWidth() * scale));
            int bh = Math.max(1, (int)(building.getHeight() * scale));
            
            // Only draw if visible in minimap
            if (bx + bw >= mapX && bx <= mapX + mapSize &&
                by + bh >= mapY && by <= mapY + mapSize) {
                
                // Use same color palette as main buildings
                int colorIdx = building.getColorIndex() % GameConstants.COLOR_PALETTE.length;
                Color buildingColor = new Color(GameConstants.COLOR_PALETTE[colorIdx]);
                bufferGraphics.setColor(buildingColor);
                bufferGraphics.fillRect(bx, by, bw, bh);
                
                // Add dark border like main map
                bufferGraphics.setColor(new Color(0x0D0D0D));
                bufferGraphics.drawRect(bx, by, bw, bh);
            }
        }
        
        // Reset clip
        bufferGraphics.setClip(null);
        
        // Draw player at center of minimap
        int playerX = mapX + mapSize / 2;
        int playerY = mapY + mapSize / 2;
        
        // Draw player direction indicator (small triangle)
        double rotation = car.getRotation();
        int arrowSize = 5;
        
        int[] xPoints = new int[3];
        int[] yPoints = new int[3];
        
        // Triangle pointing in direction of movement
        xPoints[0] = playerX + (int)(Math.cos(rotation) * arrowSize);
        yPoints[0] = playerY + (int)(Math.sin(rotation) * arrowSize);
        xPoints[1] = playerX + (int)(Math.cos(rotation + 2.5) * arrowSize);
        yPoints[1] = playerY + (int)(Math.sin(rotation + 2.5) * arrowSize);
        xPoints[2] = playerX + (int)(Math.cos(rotation - 2.5) * arrowSize);
        yPoints[2] = playerY + (int)(Math.sin(rotation - 2.5) * arrowSize);
        
        // Draw player marker (same red as car)
        bufferGraphics.setColor(carBodyColor);
        bufferGraphics.fillPolygon(xPoints, yPoints, 3);
        
        // White outline for visibility
        bufferGraphics.setColor(Color.WHITE);
        bufferGraphics.drawPolygon(xPoints, yPoints, 3);
    }
    
    public void dispose() {
        if (bufferGraphics != null) {
            bufferGraphics.dispose();
        }
    }
}
