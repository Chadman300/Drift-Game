# Sound Effects Implementation Guide 🔊

This document outlines all the sound effect trigger points in the Drift Game codebase.
Each SFX note in the code is marked with `SFX NOTE:` comments.

## Quick Implementation Guide

To add sound effects, create a `SoundManager` class in a new `audio/` package:

```java
package audio;

public class SoundManager {
    // Singleton pattern recommended
    private static SoundManager instance;
    
    public static SoundManager getInstance() {
        if (instance == null) instance = new SoundManager();
        return instance;
    }
    
    // Sound playback methods
    public void playEngineLoop(double rpm, double throttle) { }
    public void playGearShift(boolean isUpshift) { }
    public void playBackfire(double intensity) { }
    public void playTireSqueal(double intensity) { }
    public void playDriftStart() { }
    public void playScoreCashIn(long points) { }
    // etc.
}
```

---

## Sound Categories & Trigger Locations

### 1. ENGINE SOUNDS (Continuous Loop)

**Location:** `physics/Engine.java`

| Sound | Trigger | Parameters |
|-------|---------|------------|
| Engine idle | `rpm` near `IDLE_RPM` | Pitch: low, Volume: medium |
| Engine rev | Continuous based on `rpm` | Pitch: 0.5 + 0.5 * rpmPercent |
| Rev limiter | `isRevLimiterActive() == true` | Crackling/popping overlay |
| Engine off | `rpm == 0` (if implemented) | Silence or wind noise |

**Implementation Notes:**
- Use a seamless loop that crossfades between RPM samples
- Pitch shift a single sample, or blend multiple samples at different RPMs
- Add subtle variation to prevent monotony

---

### 2. GEAR SHIFTING

**Location:** `game/GameLoop.java` (lines 200-210)

| Sound | Trigger | Notes |
|-------|---------|-------|
| Shift Up | `input.isShiftUpPressed()` | Higher pitch click/clunk |
| Shift Down | `input.isShiftDownPressed()` | Lower pitch, with rev-match blip |

**Implementation Notes:**
- Very short sounds (50-150ms)
- Optional: Add brief engine rev dip on upshift, rev blip on downshift

---

### 3. BACKFIRE / EXHAUST POPS

**Location:** `game/Car.java` (updateBackfire method) and `graphics/ParticleSystem.java`

| Sound | Trigger | Parameters |
|-------|---------|------------|
| Throttle lift-off pop | Quick throttle release at high RPM | Intensity: 0-1 |
| Rev limiter pop | `isRevLimiterActive()` | Intensity: 0.8 (fixed) |

**Implementation Notes:**
- Loud, sharp "pop" or "bang" sound
- Can layer multiple pops for realism
- Pitch/volume varies with `intensity` parameter
- Iconic drift car sound - make it punchy!

---

### 4. TIRE SOUNDS

**Location:** `graphics/ParticleSystem.java` (spawnTireSmoke method)

| Sound | Trigger | Parameters |
|-------|---------|------------|
| Tire screech/squeal | `spawnTireSmoke()` called | `intensity` 0-1 |
| Tire chirp | Brief wheelspin | Short screech |

**Implementation Notes:**
- Continuous while drifting, pitch based on slip angle
- Higher intensity = louder and higher pitched
- Can use `Tire.getSmokeIntensity()` directly

---

### 5. DRIFT SCORING

**Location:** `scoring/DriftScoring.java`

| Sound | Trigger | Notes |
|-------|---------|-------|
| Drift start | Transition to `isInDrift == true` | Subtle whoosh or tire init |
| Score cash-in | Drift ends, points added | "Cha-ching" or positive chime |
| Combo increase | `comboMultiplier` increases | Rising tone |
| Grade achieved | Grade improves (C→B→A→S) | Achievement sound |

**Implementation Notes:**
- Score cash-in should scale with points earned
- Grade sounds should get more impressive (C=subtle, S=epic)

---

### 6. COLLISION SOUNDS

**Location:** `graphics/ParticleSystem.java` (spawnSparks method)

| Sound | Trigger | Notes |
|-------|---------|-------|
| Metal scrape | `spawnSparks()` called | Harsh metallic sound |
| Impact | Collision detected | Thud/crunch |

**Implementation Notes:**
- Not yet fully implemented in physics
- Placeholder for when collision detection is added

---

### 7. UI / MENU SOUNDS

**Location:** `game/GameLoop.java`

| Sound | Trigger | Notes |
|-------|---------|-------|
| Pause | `input.isPausePressed()` | Menu open sound |
| Reset | `input.isResetPressed()` | Respawn/whoosh sound |

---

### 8. AMBIENT SOUNDS

**Location:** Not yet implemented - suggested addition

| Sound | Trigger | Notes |
|-------|---------|-------|
| Wind noise | Based on `speed` | Increases with speed |
| City ambience | Always playing | Subtle background |

---

## Sound File Recommendations

### Formats
- **WAV** for short effects (< 2 seconds)
- **OGG** for longer loops (engine, music)

### Suggested File Structure
```
resources/
└── audio/
    ├── engine/
    │   ├── idle.ogg
    │   ├── low_rpm.ogg
    │   ├── mid_rpm.ogg
    │   ├── high_rpm.ogg
    │   └── rev_limiter.ogg
    ├── effects/
    │   ├── backfire_pop.wav
    │   ├── gear_up.wav
    │   ├── gear_down.wav
    │   ├── tire_squeal.ogg
    │   └── collision.wav
    ├── scoring/
    │   ├── drift_start.wav
    │   ├── score_cashin.wav
    │   ├── combo_up.wav
    │   └── grade_s.wav
    └── ui/
        ├── pause.wav
        └── unpause.wav
```

---

## Java Sound Libraries

### Built-in (javax.sound)
- Simple but limited
- Good for basic WAV playback

### Recommended Libraries
1. **TinySound** - Lightweight, easy to use
2. **LibGDX Audio** - If using LibGDX
3. **OpenAL (LWJGL)** - More control, 3D audio support
4. **FMOD** - Professional quality, licensing required

---

## Example Integration

```java
// In GameLoop.java update():
if (input.isShiftUpPressed()) {
    car.shiftUp();
    SoundManager.getInstance().playGearShift(true); // true = upshift
}

// In Car.java updateBackfire():
if (shouldBackfire) {
    particles.spawnBackfire(exhaustX, exhaustY, rotation, intensity);
    SoundManager.getInstance().playBackfire(intensity);
}

// In ParticleSystem.java spawnTireSmoke():
public void spawnTireSmoke(double x, double y, double intensity, double carRotation) {
    // Existing particle code...
    SoundManager.getInstance().playTireSqueal(intensity);
}
```

---

## Volume Mixing Guidelines

| Category | Relative Volume |
|----------|----------------|
| Engine | 100% (base) |
| Tire squeal | 70-90% |
| Backfire | 80-100% |
| Gear shifts | 50-70% |
| UI sounds | 60-80% |
| Score sounds | 70-90% |
| Ambient | 30-50% |

---

*SFX system not yet implemented - this guide is for future development*
