package graphics;

import util.GameConstants;
import util.Vector2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages particle effects for the game
 * Handles tire smoke, sparks, exhaust backfire, etc.
 * 
 * SFX NOTE: Each spawn method is a good place to trigger corresponding sound effects:
 * - spawnTireSmoke() -> tire screech/squeal sound (pitch based on intensity)
 * - spawnSparks() -> metal scraping sound
 * - spawnBackfire() -> loud pop/bang sound
 */
public class ParticleSystem {
    
    private List<Particle> particles;
    private List<TireMark> tireMarks;
    
    private static final int MAX_PARTICLES = 500;
    private static final int MAX_TIRE_MARKS = 1000;
    
    public ParticleSystem() {
        particles = new ArrayList<>();
        tireMarks = new ArrayList<>();
    }
    
    /**
     * Update all particles
     */
    public void update(double dt) {
        // Update particles
        synchronized(particles) {
            for (int i = particles.size() - 1; i >= 0; i--) {
                Particle p = particles.get(i);
                p.update(dt);
                if (p.isDead()) {
                    particles.remove(i);
                }
            }
        }
        
        // Fade old tire marks
        synchronized(tireMarks) {
            for (int i = tireMarks.size() - 1; i >= 0; i--) {
                TireMark mark = tireMarks.get(i);
                mark.update(dt);
                if (mark.isDead()) {
                    tireMarks.remove(i);
                }
            }
        }
    }
    
    /**
     * Spawn realistic tire smoke particles
     * Creates billowing smoke that rises and expands realistically
     * SFX NOTE: Play tire screech sound here, volume/pitch based on intensity
     */
    public void spawnTireSmoke(double x, double y, double intensity, double carRotation) {
        synchronized(particles) {
            if (particles.size() >= MAX_PARTICLES) return;
            
            // More particles for higher intensity, but cap for performance
            int count = (int)(intensity * 5) + 1;
            count = Math.min(count, 8);
            
            for (int i = 0; i < count; i++) {
                // Smoke rises and spreads outward from tire
                double spreadAngle = carRotation + Math.PI + (Math.random() - 0.5) * 1.5;
                double upwardBias = -15 - Math.random() * 10; // Negative Y = upward in screen coords
                double horizontalSpeed = 3 + Math.random() * 8;
                
                double vx = Math.cos(spreadAngle) * horizontalSpeed;
                double vy = Math.sin(spreadAngle) * horizontalSpeed + upwardBias * 0.3;
                
                // Longer lifetime for more realistic billowing
                double lifetime = 0.8 + Math.random() * 0.7;
                
                Particle p = new Particle(
                    x + (Math.random() - 0.5) * 1.5,
                    y + (Math.random() - 0.5) * 1.5,
                    vx, vy,
                    lifetime,
                    ParticleType.SMOKE
                );
                
                // Vary initial size for depth effect
                p.size = 1.5 + Math.random() * 2.5;
                // Smoke starts slightly transparent
                p.startAlpha = 0.6 + Math.random() * 0.3;
                particles.add(p);
            }
        }
    }
    
    /**
     * Add a tire mark segment
     */
    public void addTireMark(double x, double y, double rotation, double width) {
        synchronized(tireMarks) {
            if (tireMarks.size() >= MAX_TIRE_MARKS) {
                tireMarks.remove(0);
            }
            
            tireMarks.add(new TireMark(x, y, rotation, width));
        }
    }
    
    /**
     * Spawn sparks (for collisions)
     * SFX NOTE: Play metal scraping/impact sound here
     */
    public void spawnSparks(double x, double y, double dirX, double dirY) {
        synchronized(particles) {
            if (particles.size() >= MAX_PARTICLES) return;
            
            for (int i = 0; i < 10; i++) {
                double angle = Math.atan2(dirY, dirX) + (Math.random() - 0.5) * Math.PI;
                double speed = 20 + Math.random() * 30;
                
                Particle p = new Particle(
                    x, y,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed,
                    0.3 + Math.random() * 0.3,
                    ParticleType.SPARK
                );
                p.size = 1;
                particles.add(p);
            }
        }
    }
    
    /**
     * Spawn exhaust backfire effect
     * Creates a burst of flame and sparks from the exhaust when lifting off throttle at high RPM
     * SFX NOTE: Play loud pop/bang/crackle sound here - iconic drift car sound!
     * 
     * @param x exhaust position X
     * @param y exhaust position Y
     * @param carRotation car's current rotation
     * @param intensity 0-1 based on RPM and throttle lift speed
     */
    public void spawnBackfire(double x, double y, double carRotation, double intensity) {
        synchronized(particles) {
            if (particles.size() >= MAX_PARTICLES) return;
            
            // Direction behind the car (exhaust points backward)
            double exhaustAngle = carRotation + Math.PI;
            
            // Flame particles (orange/yellow core)
            int flameCount = (int)(3 + intensity * 5);
            for (int i = 0; i < flameCount; i++) {
                double angle = exhaustAngle + (Math.random() - 0.5) * 0.5;
                double speed = 15 + Math.random() * 25 * intensity;
                
                Particle p = new Particle(
                    x + (Math.random() - 0.5) * 0.5,
                    y + (Math.random() - 0.5) * 0.5,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed,
                    0.1 + Math.random() * 0.15,
                    ParticleType.BACKFIRE_FLAME
                );
                p.size = 2 + Math.random() * 3;
                particles.add(p);
            }
            
            // Spark particles (fly outward)
            int sparkCount = (int)(2 + intensity * 4);
            for (int i = 0; i < sparkCount; i++) {
                double angle = exhaustAngle + (Math.random() - 0.5) * 0.8;
                double speed = 25 + Math.random() * 35;
                
                Particle p = new Particle(
                    x, y,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed,
                    0.2 + Math.random() * 0.3,
                    ParticleType.BACKFIRE_SPARK
                );
                p.size = 1;
                particles.add(p);
            }
        }
    }
    
    public List<Particle> getParticles() { 
        synchronized(particles) {
            return new ArrayList<>(particles); 
        }
    }
    public List<TireMark> getTireMarks() { 
        synchronized(tireMarks) {
            return new ArrayList<>(tireMarks); 
        }
    }
    
    public void clear() {
        synchronized(particles) {
            particles.clear();
        }
        synchronized(tireMarks) {
            tireMarks.clear();
        }
    }
    
    /**
     * Particle types
     */
    public enum ParticleType {
        SMOKE,
        SPARK,
        DUST,
        BACKFIRE_FLAME,
        BACKFIRE_SPARK
    }
    
    /**
     * Individual particle
     */
    public static class Particle {
        public double x, y;
        public double vx, vy;
        public double lifetime;
        public double maxLifetime;
        public double size;
        public double startAlpha;
        public ParticleType type;
        
        public Particle(double x, double y, double vx, double vy, 
                        double lifetime, ParticleType type) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.lifetime = lifetime;
            this.maxLifetime = lifetime;
            this.type = type;
            this.size = 2;
            this.startAlpha = 1.0;
        }
        
        public void update(double dt) {
            x += vx * dt;
            y += vy * dt;
            
            // Different behavior per type
            switch (type) {
                case SMOKE:
                    // Smoke rises, slows down, and expands
                    vy -= dt * 5; // Gentle upward drift
                    vx *= 0.92;
                    vy *= 0.92;
                    size += dt * 4; // Expand over time
                    break;
                    
                case SPARK:
                case BACKFIRE_SPARK:
                    // Sparks affected by gravity, don't slow as much
                    vy += dt * 30; // Gravity
                    vx *= 0.98;
                    vy *= 0.98;
                    break;
                    
                case BACKFIRE_FLAME:
                    // Flames shrink quickly and slow down fast
                    vx *= 0.85;
                    vy *= 0.85;
                    size *= 0.92; // Shrink
                    break;
                    
                case DUST:
                default:
                    vx *= 0.95;
                    vy *= 0.95;
                    break;
            }
            
            lifetime -= dt;
        }
        
        public boolean isDead() {
            return lifetime <= 0 || (type == ParticleType.BACKFIRE_FLAME && size < 0.3);
        }
        
        public double getAlpha() {
            double baseAlpha = Math.max(0, lifetime / maxLifetime);
            return baseAlpha * startAlpha;
        }
    }
    
    /**
     * Tire mark on the ground
     */
    public static class TireMark {
        public double x, y;
        public double rotation;
        public double width;
        public double alpha;
        
        private static final double FADE_TIME = 10.0; // Seconds to fade
        
        public TireMark(double x, double y, double rotation, double width) {
            this.x = x;
            this.y = y;
            this.rotation = rotation;
            this.width = width;
            this.alpha = 1.0;
        }
        
        public void update(double dt) {
            alpha -= dt / FADE_TIME;
        }
        
        public boolean isDead() {
            return alpha <= 0;
        }
    }
}
