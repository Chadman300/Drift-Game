package world;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import util.GameConstants;

/**
 * Represents the city environment
 * Generates procedural city blocks with roads, parking lots, and street lights
 */
public class CityWorld {
    
    // World elements
    private List<Building> buildings;
    private List<Road> roads;
    private List<ParkingLot> parkingLots;
    private List<StreetLight> streetLights;
    
    // World bounds
    private double worldSize;
    private int blockSize;
    private int roadWidth;
    
    // Random seed for procedural generation
    private long seed;
    private Random random;
    
    public CityWorld() {
        this(System.currentTimeMillis());
    }
    
    public CityWorld(long seed) {
        this.seed = seed;
        this.random = new Random(seed);
        this.worldSize = GameConstants.WORLD_SIZE;
        this.blockSize = GameConstants.BLOCK_SIZE;
        this.roadWidth = GameConstants.ROAD_WIDTH;
        
        this.buildings = new ArrayList<>();
        this.roads = new ArrayList<>();
        this.parkingLots = new ArrayList<>();
        this.streetLights = new ArrayList<>();
        this.roundabouts = new ArrayList<>();
        
        generateCity();
    }
    
    // Roundabouts list
    private List<Roundabout> roundabouts;
    
    /**
     * Generate the city layout with variety
     */
    private void generateCity() {
        int numBlocks = (int)(worldSize / blockSize);
        
        // First, decide which intersections will be roundabouts
        boolean[][] isRoundabout = new boolean[numBlocks + 1][numBlocks + 1];
        for (int i = 1; i < numBlocks; i++) {
            for (int j = 1; j < numBlocks; j++) {
                // Place roundabouts at some intersections (not at edges)
                if (random.nextDouble() < 0.15 && !hasAdjacentRoundabout(isRoundabout, i, j)) {
                    isRoundabout[i][j] = true;
                    double rx = i * blockSize - worldSize / 2;
                    double ry = j * blockSize - worldSize / 2;
                    double outerR = roadWidth * 2.0;
                    double innerR = roadWidth * 0.8;
                    roundabouts.add(new Roundabout(rx, ry, outerR, innerR, 4));
                }
            }
        }
        
        // Generate roads - roads connect TO the roundabout outer edge
        for (int i = 0; i <= numBlocks; i++) {
            boolean isMainRoad = (i % 3 == 0);
            double width = isMainRoad ? roadWidth * 1.3 : roadWidth;
            
            // Vertical roads
            for (int seg = 0; seg < numBlocks; seg++) {
                double x = i * blockSize - worldSize / 2;
                double y1 = seg * blockSize - worldSize / 2;
                double y2 = (seg + 1) * blockSize - worldSize / 2;
                
                // Check roundabouts at both ends of this segment
                boolean startIsRound = i >= 0 && i <= numBlocks && seg >= 0 && seg <= numBlocks && isRoundabout[i][seg];
                boolean endIsRound = i >= 0 && i <= numBlocks && seg + 1 >= 0 && seg + 1 <= numBlocks && isRoundabout[i][seg + 1];
                
                // Extend road TO roundabout edge (not away from it)
                double outerR = roadWidth * 2.0;
                if (startIsRound) y1 += outerR;
                if (endIsRound) y2 -= outerR;
                
                if (y2 > y1 + 1) {
                    Road vRoad = new Road(x, y1, x, y2, width, true);
                    if (isMainRoad) vRoad.setRoadType(Road.RoadType.HIGHWAY);
                    roads.add(vRoad);
                }
            }
            
            // Horizontal roads
            for (int seg = 0; seg < numBlocks; seg++) {
                double y = i * blockSize - worldSize / 2;
                double x1 = seg * blockSize - worldSize / 2;
                double x2 = (seg + 1) * blockSize - worldSize / 2;
                
                boolean startIsRound = seg >= 0 && seg <= numBlocks && i >= 0 && i <= numBlocks && isRoundabout[seg][i];
                boolean endIsRound = seg + 1 >= 0 && seg + 1 <= numBlocks && i >= 0 && i <= numBlocks && isRoundabout[seg + 1][i];
                
                double outerR = roadWidth * 2.0;
                if (startIsRound) x1 += outerR;
                if (endIsRound) x2 -= outerR;
                
                if (x2 > x1 + 1) {
                    Road hRoad = new Road(x1, y, x2, y, width, false);
                    if (isMainRoad) hRoad.setRoadType(Road.RoadType.HIGHWAY);
                    roads.add(hRoad);
                }
            }
        }
        
        // Generate curved connector roads in some blocks
        generateCurvedRoads(numBlocks, isRoundabout);
        
        // Generate buildings and parking lots in each block
        for (int bx = 0; bx < numBlocks; bx++) {
            for (int by = 0; by < numBlocks; by++) {
                generateBlock(bx, by, isRoundabout);
            }
        }
        
        // Add street lights
        generateStreetLights(numBlocks, isRoundabout);
    }
    
    private boolean hasAdjacentRoundabout(boolean[][] isRoundabout, int i, int j) {
        int[][] neighbors = {{-1,0},{1,0},{0,-1},{0,1},{-1,-1},{1,1},{-1,1},{1,-1}};
        for (int[] n : neighbors) {
            int ni = i + n[0];
            int nj = j + n[1];
            if (ni >= 0 && ni < isRoundabout.length && nj >= 0 && nj < isRoundabout[0].length) {
                if (isRoundabout[ni][nj]) return true;
            }
        }
        return false;
    }
    
    /**
     * Generate curved roads inside some blocks
     */
    private void generateCurvedRoads(int numBlocks, boolean[][] isRoundabout) {
        // Add curved roads inside blocks (not at intersections)
        for (int bx = 0; bx < numBlocks; bx++) {
            for (int by = 0; by < numBlocks; by++) {
                if (random.nextDouble() < 0.12) {
                    // Create a curved road within this block
                    double blockCenterX = (bx + 0.5) * blockSize - worldSize / 2;
                    double blockCenterY = (by + 0.5) * blockSize - worldSize / 2;
                    
                    // Random curve configuration
                    double radius = blockSize * 0.3;
                    int curveType = random.nextInt(4);
                    double startAngle, endAngle;
                    double cx, cy;
                    
                    switch (curveType) {
                        case 0: // Top-left corner curve
                            cx = blockCenterX - blockSize * 0.2;
                            cy = blockCenterY - blockSize * 0.2;
                            startAngle = 0;
                            endAngle = Math.PI / 2;
                            break;
                        case 1: // Top-right corner curve
                            cx = blockCenterX + blockSize * 0.2;
                            cy = blockCenterY - blockSize * 0.2;
                            startAngle = Math.PI / 2;
                            endAngle = Math.PI;
                            break;
                        case 2: // Bottom-right corner curve
                            cx = blockCenterX + blockSize * 0.2;
                            cy = blockCenterY + blockSize * 0.2;
                            startAngle = Math.PI;
                            endAngle = Math.PI * 1.5;
                            break;
                        default: // Bottom-left corner curve
                            cx = blockCenterX - blockSize * 0.2;
                            cy = blockCenterY + blockSize * 0.2;
                            startAngle = Math.PI * 1.5;
                            endAngle = Math.PI * 2;
                            break;
                    }
                    
                    Road curved = new Road(cx, cy, radius, startAngle, endAngle, roadWidth * 0.8);
                    curved.setRoadType(Road.RoadType.SIDE_STREET);
                    roads.add(curved);
                }
            }
        }
    }
    
    /**
     * Generate street lights at proper positions
     */
    private void generateStreetLights(int numBlocks, boolean[][] isRoundabout) {
        double offset = roadWidth / 2 + 3;
        
        for (int i = 0; i <= numBlocks; i++) {
            for (int j = 0; j <= numBlocks; j++) {
                double x = i * blockSize - worldSize / 2;
                double y = j * blockSize - worldSize / 2;
                
                if (isRoundabout[Math.min(i, numBlocks - 1)][Math.min(j, numBlocks - 1)]) {
                    // Lights around roundabout
                    double rOffset = roadWidth * 2.2;
                    for (int a = 0; a < 8; a++) {
                        double angle = a * Math.PI / 4;
                        streetLights.add(new StreetLight(
                            x + Math.cos(angle) * rOffset,
                            y + Math.sin(angle) * rOffset,
                            angle + Math.PI, random.nextInt(2)
                        ));
                    }
                } else {
                    // Four corners of intersection
                    streetLights.add(new StreetLight(x - offset, y - offset, Math.PI * 0.25, 0));
                    streetLights.add(new StreetLight(x + offset, y - offset, Math.PI * 0.75, 1));
                    streetLights.add(new StreetLight(x - offset, y + offset, -Math.PI * 0.25, 0));
                    streetLights.add(new StreetLight(x + offset, y + offset, -Math.PI * 0.75, 1));
                }
            }
        }
    }
    
    /**
     * Generate buildings for a single city block
     */
    private void generateBlock(int blockX, int blockY, boolean[][] isRoundabout) {
        double margin = roadWidth * 0.8;
        double startX = blockX * blockSize - worldSize / 2 + margin;
        double startY = blockY * blockSize - worldSize / 2 + margin;
        double availableSize = blockSize - margin * 2;
        
        if (availableSize < 20) return;
        
        // Check if adjacent to roundabout - need extra margin
        boolean nearRoundabout = false;
        int[][] corners = {{blockX, blockY}, {blockX+1, blockY}, {blockX, blockY+1}, {blockX+1, blockY+1}};
        for (int[] c : corners) {
            if (c[0] < isRoundabout.length && c[1] < isRoundabout[0].length && isRoundabout[c[0]][c[1]]) {
                nearRoundabout = true;
            }
        }
        if (nearRoundabout) {
            margin += roadWidth * 0.5;
            startX = blockX * blockSize - worldSize / 2 + margin;
            startY = blockY * blockSize - worldSize / 2 + margin;
            availableSize = blockSize - margin * 2;
        }
        
        double distFromCenter = Math.sqrt(
            Math.pow(startX + availableSize/2, 2) + 
            Math.pow(startY + availableSize/2, 2)
        );
        
        boolean isCentral = distFromCenter < worldSize * 0.3;
        double roll = random.nextDouble();
        
        if (roll < 0.10) {
            generateParkingLot(startX, startY, availableSize);
        } else if (roll < 0.15) {
            return; // Empty lot
        } else if (isCentral && roll < 0.35) {
            generateSkyscraper(startX, startY, availableSize);
        } else if (roll < 0.45) {
            generateCircularBuilding(startX, startY, availableSize);
        } else if (roll < 0.55) {
            generateQuadBuildings(startX, startY, availableSize);
        } else if (roll < 0.70) {
            generateSmallBuildings(startX, startY, availableSize);
        } else if (roll < 0.80) {
            generateShapedBuilding(startX, startY, availableSize);
        } else if (roll < 0.90) {
            generateOctagonBuilding(startX, startY, availableSize);
        } else {
            generateMixedBuildings(startX, startY, availableSize);
        }
    }
    
    /**
     * Generate a circular/tower building
     */
    private void generateCircularBuilding(double x, double y, double size) {
        double centerX = x + size / 2;
        double centerY = y + size / 2;
        double radius = size * 0.4;
        int height = 8 + random.nextInt(15);
        int colorIndex = random.nextInt(GameConstants.COLOR_PALETTE.length);
        
        buildings.add(new Building(centerX, centerY, radius * 2, radius * 2, height, colorIndex, 
                                   Building.BuildingShape.CIRCLE, 0));
        
        // Maybe add smaller circular buildings around it
        if (random.nextDouble() < 0.4) {
            double smallRadius = radius * 0.35;
            double offset = radius + smallRadius + 3;
            int numSmall = 2 + random.nextInt(2);
            for (int i = 0; i < numSmall; i++) {
                double angle = (2 * Math.PI * i) / numSmall + random.nextDouble() * 0.5;
                double sx = centerX + Math.cos(angle) * offset;
                double sy = centerY + Math.sin(angle) * offset;
                if (sx > x && sx < x + size && sy > y && sy < y + size) {
                    buildings.add(new Building(sx, sy, smallRadius * 2, smallRadius * 2, 
                                               height / 2, colorIndex, Building.BuildingShape.CIRCLE, 0));
                }
            }
        }
    }
    
    /**
     * Generate octagonal building
     */
    private void generateOctagonBuilding(double x, double y, double size) {
        double margin = 6;
        double buildingSize = size - margin * 2;
        int height = 6 + random.nextInt(12);
        int colorIndex = random.nextInt(GameConstants.COLOR_PALETTE.length);
        double rotation = random.nextDouble() * Math.PI / 8; // Slight random rotation
        
        buildings.add(new Building(x + margin, y + margin, buildingSize, buildingSize, 
                                   height, colorIndex, Building.BuildingShape.OCTAGON, rotation));
    }
    
    /**
     * Generate a parking lot in a block
     */
    private void generateParkingLot(double x, double y, double size) {
        double margin = 5;
        ParkingLot lot = new ParkingLot(
            x + margin, y + margin, 
            size - margin * 2, size - margin * 2, 
            random
        );
        parkingLots.add(lot);
        
        // Maybe add a small building next to parking
        if (random.nextDouble() < 0.4) {
            double bSize = size * 0.25;
            int height = 2 + random.nextInt(3);
            int colorIndex = random.nextInt(GameConstants.COLOR_PALETTE.length);
            buildings.add(new Building(x + margin, y + margin, bSize, bSize, height, colorIndex));
        }
    }
    
    /**
     * Generate a tall downtown building
     */
    private void generateSkyscraper(double x, double y, double size) {
        double margin = 8;
        double buildingSize = size - margin * 2;
        int height = 12 + random.nextInt(18); // Tall buildings
        int colorIndex = random.nextInt(GameConstants.COLOR_PALETTE.length);
        
        // Main tower
        buildings.add(new Building(
            x + margin, y + margin,
            buildingSize, buildingSize,
            height, colorIndex
        ));
        
        // Maybe add a smaller attached structure
        if (random.nextDouble() < 0.5) {
            double smallSize = buildingSize * 0.3;
            buildings.add(new Building(
                x + margin + buildingSize - smallSize, 
                y + margin + buildingSize - smallSize,
                smallSize, smallSize,
                height / 2, colorIndex
            ));
        }
    }
    
    /**
     * Generate L-shaped or U-shaped buildings
     */
    private void generateShapedBuilding(double x, double y, double size) {
        double margin = 4;
        int height = 4 + random.nextInt(8);
        int colorIndex = random.nextInt(GameConstants.COLOR_PALETTE.length);
        double thickness = size * 0.3;
        
        if (random.nextBoolean()) {
            // L-shape
            buildings.add(new Building(x + margin, y + margin, size - margin * 2, thickness, height, colorIndex));
            buildings.add(new Building(x + margin, y + margin + thickness, thickness, size - margin * 2 - thickness, height, colorIndex));
        } else {
            // U-shape
            buildings.add(new Building(x + margin, y + margin, thickness, size - margin * 2, height, colorIndex));
            buildings.add(new Building(x + size - margin - thickness, y + margin, thickness, size - margin * 2, height, colorIndex));
            buildings.add(new Building(x + margin, y + margin, size - margin * 2, thickness, height, colorIndex));
        }
    }
    
    private void generateQuadBuildings(double x, double y, double size) {
        double margin = 3;
        double gap = 8;
        double buildingSize = (size - gap) / 2 - margin;
        
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                double bx = x + margin + i * (buildingSize + gap);
                double by = y + margin + j * (buildingSize + gap);
                int height = 3 + random.nextInt(10);
                int colorIndex = random.nextInt(GameConstants.COLOR_PALETTE.length);
                
                // Slight size variation
                double sizeVar = buildingSize * (0.8 + random.nextDouble() * 0.2);
                buildings.add(new Building(bx, by, sizeVar, sizeVar, height, colorIndex));
            }
        }
    }
    
    private void generateSmallBuildings(double x, double y, double size) {
        double margin = 2;
        double gap = 5;
        int count = 3;
        double buildingSize = (size - gap * (count - 1)) / count - margin;
        
        for (int i = 0; i < count; i++) {
            for (int j = 0; j < count; j++) {
                if (random.nextDouble() < 0.15) continue; // Some gaps
                
                double bx = x + margin + i * (buildingSize + gap);
                double by = y + margin + j * (buildingSize + gap);
                int height = 2 + random.nextInt(6);
                int colorIndex = random.nextInt(GameConstants.COLOR_PALETTE.length);
                
                // Varying sizes
                double w = buildingSize * (0.7 + random.nextDouble() * 0.3);
                double h = buildingSize * (0.7 + random.nextDouble() * 0.3);
                
                buildings.add(new Building(bx, by, w, h, height, colorIndex));
            }
        }
    }
    
    private void generateMixedBuildings(double x, double y, double size) {
        double margin = 3;
        double largeSize = size * 0.55 - margin * 2;
        
        // One large building
        int height = 8 + random.nextInt(12);
        int colorIndex = random.nextInt(GameConstants.COLOR_PALETTE.length);
        buildings.add(new Building(x + margin, y + margin, largeSize, largeSize, height, colorIndex));
        
        // Small buildings on the sides
        double smallSize = size * 0.35 - margin;
        for (int i = 0; i < 2; i++) {
            double bx = x + size * 0.60 + margin;
            double by = y + margin + i * (smallSize + margin * 2);
            height = 2 + random.nextInt(5);
            colorIndex = random.nextInt(GameConstants.COLOR_PALETTE.length);
            
            double w = smallSize * (0.8 + random.nextDouble() * 0.2);
            double h = smallSize * (0.7 + random.nextDouble() * 0.3);
            buildings.add(new Building(bx, by, w, h, height, colorIndex));
        }
    }
    
    /**
     * Check if a point is on a road or roundabout
     */
    public boolean isOnRoad(double x, double y) {
        // Check roundabouts first
        for (Roundabout r : roundabouts) {
            if (r.containsPoint(x, y)) return true;
        }
        for (Road road : roads) {
            if (road.containsPoint(x, y)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check collision with buildings or roundabout islands
     */
    public Building checkBuildingCollision(double x, double y, double radius) {
        // Check roundabout center islands
        for (Roundabout r : roundabouts) {
            if (r.collidesWithIsland(x, y, radius)) {
                // Return a fake building for collision response
                return new Building(r.getX() - r.getInnerRadius(), r.getY() - r.getInnerRadius(),
                                    r.getInnerRadius() * 2, r.getInnerRadius() * 2, 1, 0);
            }
        }
        for (Building building : buildings) {
            if (building.intersectsCircle(x, y, radius)) {
                return building;
            }
        }
        return null;
    }
    
    /**
     * Get surface type at position
     */
    public SurfaceType getSurfaceAt(double x, double y) {
        if (isOnRoad(x, y)) {
            return SurfaceType.ASPHALT;
        }
        for (ParkingLot lot : parkingLots) {
            if (x >= lot.getX() && x <= lot.getX() + lot.getWidth() &&
                y >= lot.getY() && y <= lot.getY() + lot.getHeight()) {
                return SurfaceType.CONCRETE;
            }
        }
        return SurfaceType.CONCRETE;
    }
    
    // Getters
    public List<Building> getBuildings() { return buildings; }
    public List<Road> getRoads() { return roads; }
    public List<ParkingLot> getParkingLots() { return parkingLots; }
    public List<StreetLight> getStreetLights() { return streetLights; }
    public List<Roundabout> getRoundabouts() { return roundabouts; }
    public double getWorldSize() { return worldSize; }
    
    /**
     * Get nearby buildings for rendering optimization
     */
    public List<Building> getNearbyBuildings(double x, double y, double radius) {
        List<Building> nearby = new ArrayList<>();
        for (Building b : buildings) {
            double dx = b.getCenterX() - x;
            double dy = b.getCenterY() - y;
            if (dx * dx + dy * dy < radius * radius) {
                nearby.add(b);
            }
        }
        return nearby;
    }
    
    /**
     * Surface types affect tire grip
     */
    public enum SurfaceType {
        ASPHALT(1.0),
        CONCRETE(0.9),
        GRAVEL(0.6),
        GRASS(0.4);
        
        public final double gripMultiplier;
        
        SurfaceType(double grip) {
            this.gripMultiplier = grip;
        }
    }
}
