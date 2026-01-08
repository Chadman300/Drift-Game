import ui.GameWindow;

/**
 * Main entry point for Drift City
 * A retro pixel-art drifting game with realistic physics
 * 
 * CONTROLS:
 * - W/Up Arrow: Accelerate
 * - S/Down Arrow: Brake/Reverse
 * - A/D or Left/Right: Steer
 * - Space: Handbrake
 * - E/Shift: Shift Up
 * - Q/Ctrl: Shift Down
 * - ESC/P: Pause
 * - R: Reset position
 * 
 * PROJECT STRUCTURE:
 * - game/       : Core game logic (Car, GameLoop, GameState)
 * - graphics/   : Rendering (Camera, Renderer, ParticleSystem)
 * - input/      : Input handling (InputHandler)
 * - physics/    : Vehicle physics (Engine, Tire, VehiclePhysics)
 * - scoring/    : Score system (DriftScoring)
 * - ui/         : User interface (GameWindow)
 * - util/       : Utilities (Vector2D, MathUtils, GameConstants)
 * - world/      : World generation (CityWorld, Building, Road)
 * 
 * @author Drift City Development
 * @version 1.0
 */
public class App {
    
    public static void main(String[] args) {
        System.out.println("Starting Drift City...");
        System.out.println("======================");
        System.out.println("Controls:");
        System.out.println("  WASD/Arrows - Drive");
        System.out.println("  Space - Handbrake (for drifting!)");
        System.out.println("  E/Q - Shift Up/Down");
        System.out.println("  ESC/P - Pause");
        System.out.println("  R - Reset");
        System.out.println("======================");
        
        // Launch the game
        GameWindow.launch();
    }
}
