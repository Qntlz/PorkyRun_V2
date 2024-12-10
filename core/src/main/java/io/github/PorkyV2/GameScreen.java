package io.github.PorkyV2;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.viewport.StretchViewport;

import static com.badlogic.gdx.math.MathUtils.random;

public class GameScreen implements Screen {
    private static final float FIXED_TIMESTEP = 1 / 60f;
    private float obstacleSpawnTimer = 0;
    private float accumulator = 0f;
    private SpriteBatch batch;
    private BitmapFont font;
    private Pig myPorky;
    private StretchViewport viewport;
    private TextureRegion bgFrameBufferTextureRegion;
    private FrameBuffer bgFrameBuffer;
    private ShapeRenderer showHitbox;
    private Texture hayBaleTexture;
    private BitmapFont gameOverFont;
    private BitmapFont restartFont;
    private GlyphLayout gameOverLayout;
    private BitmapFont scoreFont;
    private Texture farmBgTexture;  // Initial background
    private Texture forestBgTexture; // New background for score >= 5
    private Texture currentBgTexture; // Background currently being displayed
    private Texture treeStumpTexture;

    private boolean isGameOver = false;
    private boolean restartBuffered = false;
    private boolean jumpBuffered = false;

    private Pool<Obstacle> obstaclePool;
    private Array<Obstacle> obstacles;

    private Array<Texture> cloudTextures;
    private Pool<Clouds> cloudPool;
    private Array<Clouds> activeClouds;

    private float obstacleSpawnInterval = 3f; // Initial interval for spawning obstacles
    private float maxInterval = 5f; // Maximum interval (initial difficulty)

    private Animation<TextureRegion> glareAnimation;
    private Texture bulletTexture;
    private Array<Bullet> bullets;
    private float glareAnimationTime = 0f;
    private float glareSpawnTimer = 0f;
    private boolean isGlareActive = false;
    private float glareX, glareY; // Position of the glare

    private int score = 0;

    @Override
    public void show() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        showHitbox = new ShapeRenderer();
        myPorky = new Pig(170, 170);
        gameOverLayout = new GlyphLayout();

        bgFrameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, 800, 500, false);
        viewport = new StretchViewport(800, 500);

        loadBg();
        loadFont();
        loadClouds();
        loadObstacles();
        loadHunter();
        createCloudPool();
        createObstaclePool();
    }

    @Override
    public void render(float delta) {

        draw();

        manageControls();

        accumulator += delta;                                   // Accumulate delta time
        while (accumulator >= FIXED_TIMESTEP) {
            if (!isGameOver) {                                  // Only update game if not over
                logic();
            } else if (restartBuffered) {
                restartGame();                                  // Restart if 'R' is pressed
            }
            accumulator -= FIXED_TIMESTEP;
        }
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        showHitbox.dispose();
        farmBgTexture.dispose();
        forestBgTexture.dispose();
        disposeObstacles();
        disposeClouds();
    }

    private void draw() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        drawBg();
        drawClouds();
        drawScore();
        drawObstacles();
        drawHunter();
        drawPorky();
        drawGameOverMessage();
        batch.end();

        showHitbox.begin(ShapeRenderer.ShapeType.Line);
        drawPorkyHitBox();
        drawObstacleHitbox();
        drawBulletHitbox();
        showHitbox.end();
    }

    private void logic() {
        myPorky.update(FIXED_TIMESTEP);
        manageDifficulty();
        // Handle jump input
        if (jumpBuffered) {
            myPorky.jump();
            jumpBuffered = false; // Consume the jump input
        }

        spawnObstacles();
        updateObstacles();
        checkCollisions();


        // Change the background when the score reaches 5
        if (score >= 3 && currentBgTexture != forestBgTexture) {
            currentBgTexture = forestBgTexture;
            activeClouds.clear();
            obstacles.clear();
            renderBackground();                                     // Render the new background to the framebuffer

            myPorky.setGround(-20);                                 // Lower the ground level for forest terrain
        }

        if (currentBgTexture == forestBgTexture) {
            spawnHunter();
            updateHunter();

        }
    }

    private void loadBg() {
        // Initial background
        farmBgTexture = new Texture("farm.png");
        forestBgTexture = new Texture("forest.png");
        currentBgTexture = farmBgTexture; // Start with farm background
        renderBackground();
    }

    private void loadFont() {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/LuckiestGuy-Regular.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter gameOverFontParam = new FreeTypeFontGenerator.FreeTypeFontParameter();
        FreeTypeFontGenerator.FreeTypeFontParameter restartFontParam = new FreeTypeFontGenerator.FreeTypeFontParameter();
        FreeTypeFontGenerator.FreeTypeFontParameter scoreFontParam = new FreeTypeFontGenerator.FreeTypeFontParameter();

        // Game-Over Text Configuration
        gameOverFontParam.size = 80;
        gameOverFontParam.color = Color.RED;
        gameOverFont = generator.generateFont(gameOverFontParam);

        // Restart Text Configuration
        restartFontParam.size = 40;
        restartFontParam.color = Color.YELLOW;
        restartFont = generator.generateFont(restartFontParam);

        // Score Text Configuration
        scoreFontParam.size = 40;
        scoreFontParam.color = Color.ORANGE;  // Set the color of the score
        scoreFont = generator.generateFont(scoreFontParam);


        generator.dispose();
    }

    private void loadClouds() {
        cloudTextures = new Array<>();
        activeClouds = new Array<>();
        cloudTextures.add(new Texture("clouds/1.png"));
        cloudTextures.add(new Texture("clouds/2.png"));
        cloudTextures.add(new Texture("clouds/3.png"));
    }

    private void loadObstacles() {
        // Load Obstacles
        obstacles = new Array<>();
        hayBaleTexture = new Texture("obstacles/hay-bale.png");
        treeStumpTexture = new Texture("obstacles/tree-stump.png");
    }

    private void loadHunter() {
        TextureAtlas glareAtlas = new TextureAtlas(Gdx.files.internal("atlas/glare-atlas.atlas"));
        glareAnimation = new Animation<>(0.1f, glareAtlas.getRegions(), Animation.PlayMode.NORMAL);

        bulletTexture = new Texture(Gdx.files.internal("obstacles/bullet.png"));
        bullets = new Array<>();

    }

    private void drawBg() {
        // Draw background
        batch.draw(bgFrameBufferTextureRegion, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
    }

    private void drawPorky() {
        TextureRegion currentFrame = myPorky.getCurrentFrame();
        batch.draw(currentFrame, myPorky.getX(), myPorky.getY(), myPorky.getWidth(), myPorky.getHeight());
    }

    private void drawPorkyHitBox() {
        // Draw hitbox using ShapeRenderer
        showHitbox.setColor(0, 1, 0, 1);                                    // Green color for the hitbox
        showHitbox.rect(myPorky.getX() + myPorky.getHitBoxOffsetX(), myPorky.getY() + myPorky.getHitBoxOffsetY(),
            myPorky.getHitBoxWidth(), myPorky.getHitBoxHeight());
    }

    private void drawClouds() {

        // First Condition: // Only generate clouds if the background is not "forest" !currentBgTexture.toString().contains("forest.png")
        // Second Condition: Determine if a new cloud should spawn (you can adjust spawn frequency here)
        if (currentBgTexture != forestBgTexture && random.nextFloat() < 0.0020f) {                  // 0.10% chance per frame to spawn a new cloud
            Clouds cloud = cloudPool.obtain();                                                                   // Get a cloud from the pool
            Texture randomTexture = cloudTextures.random();                                                     // Randomly select a cloud texture
            cloud.setTexture(randomTexture);                                                                    // Set the texture to the cloud

            // Set cloud position and speed
            cloud.setPosition(800, random.nextInt(30) + 170);   // Clouds start at right side, random Y-position
            cloud.setSpeed(random.nextInt(20) + 30);                  // Random speed

            activeClouds.add(cloud);
        }

        // Update and remove clouds that are off-screen
        for (int i = activeClouds.size - 1; i >= 0; i--) {
            Clouds cloud = activeClouds.get(i);
            cloud.update(FIXED_TIMESTEP);

            // For forest bg condition currentBgTexture.toString().contains("forest.png")
            if (cloud.isOffScreen()) {
                cloudPool.free(cloud);  // Return the cloud to the pool
                activeClouds.removeIndex(i);  // Remove cloud from the active list
            }
        }

        for (Clouds cloud : activeClouds) {
            cloud.render(batch);
        }
    }

    private void drawObstacles() {
        // Draw each obstacle
        for (Obstacle obstacle : obstacles) {
            obstacle.render(batch);
        }
    }

    private void drawObstacleHitbox() {
        for (Obstacle obstacle : obstacles) {
            Rectangle obstacleHitbox = obstacle.getHitbox();

            // Change hitbox color if it intersects with Pig's hitbox
            if (myPorky.getHitbox().overlaps(obstacleHitbox)) {
                showHitbox.setColor(1, 0, 1, 1); // Magenta for collision
            } else {
                showHitbox.setColor(1, 0, 0, 1); // Red for normal hitboxes
            }
            showHitbox.rect(obstacleHitbox.x, obstacleHitbox.y, obstacleHitbox.width, obstacleHitbox.height);
        }
    }

    private void drawBulletHitbox() {
        showHitbox.setColor(0, 0, 1, 1); // Blue color for bullet hitbox
        for (Bullet bullet : bullets) {
            Rectangle bulletHitbox = bullet.getHitbox();
            showHitbox.rect(bulletHitbox.x + 10, bulletHitbox.y + 15, bulletHitbox.width, bulletHitbox.height);
        }
    }

    private void drawHunter() {
        // Render glare
        if (isGlareActive) {
            TextureRegion glareFrame = glareAnimation.getKeyFrame(glareAnimationTime);
            batch.draw(glareFrame, glareX, glareY, 50, 50); // Adjust size as needed
        }

        // Render bullets
        for (Bullet bullet : bullets) {
            bullet.render(batch);
        }

    }

    private void drawGameOverMessage() {
        if (isGameOver) {
            String gameOverText = "GAME OVER!";
            String restartText = "Press R to Restart";
            gameOverLayout.setText(gameOverFont, gameOverText);
            gameOverLayout.setText(restartFont, restartText);
            gameOverFont.draw(batch, gameOverText, 320, 370);
            restartFont.draw(batch, restartText, 200, 70);
        }
    }

    private void drawScore() {
        String scoreText = "Score: " + score;  // The score display text
        scoreFont.draw(batch, scoreText, 10, viewport.getWorldHeight() - 20);  // Draw in the top-left corner
    }

    private void createCloudPool() {
        cloudPool = new Pool<>() {
            @Override
            protected Clouds newObject() {
                return new Clouds(); // Correct reference
            }
        };
    }

    private void createObstaclePool() {
        // Initialize Obstacle Pool (To Prevent Garbage Collection)
        obstaclePool = new Pool<>() {
            @Override
            protected Obstacle newObject() {
                return new Obstacle(800, 80, "farm", hayBaleTexture);      // X:800 Y:80 Obstacle spawn location
            }
        };
    }

    private void spawnObstacles() {
        // Spawn obstacles at intervals

        obstacleSpawnTimer += FIXED_TIMESTEP;         // Time interval between obstacle spawns
        if (obstacleSpawnTimer >= obstacleSpawnInterval) {
            Obstacle obstacle = obstaclePool.obtain();

            if (currentBgTexture == farmBgTexture) {
                // Farm terrain
                obstacle.setPosition(800, 80); // Adjust for farm obstacle position
                obstacle.setTexture(hayBaleTexture); // Set farm texture
                obstacle.setTerrainType("farm");
            } else if (currentBgTexture == forestBgTexture) {
                // Forest terrain
                obstacle.setPosition(800, 0); // Adjust for forest obstacle position
                obstacle.setTexture(treeStumpTexture); // Set forest texture
                obstacle.setTerrainType("forest");
            }

            obstacles.add(obstacle);
            obstacleSpawnTimer = 0;
            float minInterval = 2f; // Minimum interval
            float maxInterval = 5f; // Maximum interval
            // Minimum interval (difficulty scaling)
            obstacleSpawnInterval = random(minInterval, maxInterval); // Randomize next interval
        }
        //Gdx.app.log("Obstacle Timer", "Spawn Timer: " + obstacleSpawnTimer + ", Interval: " + obstacleSpawnInterval);
        //Gdx.app.log("Random Interval", "Generated Interval: " + obstacleSpawnInterval);
        MathUtils.random.setSeed(System.nanoTime());
    }

    private void spawnHunter() {
        // Spawn sniper glare
        glareSpawnTimer += FIXED_TIMESTEP;
        if (!isGlareActive && glareSpawnTimer >= 10f) { // Adjust interval as needed
            isGlareActive = true;
            glareSpawnTimer = 0f;

            // Randomize glare position
            glareX = random(500, 600); // Spawn towards the right side
            glareY = random(100, 130); // Random vertical position
        }

        // Handle glare animation
        if (isGlareActive) {
            glareAnimationTime += FIXED_TIMESTEP;
            if (glareAnimation.isAnimationFinished(glareAnimationTime)) {
                isGlareActive = false;
                glareAnimationTime = 0f;

                // Spawn a bullet
                Bullet bullet = new Bullet(glareX, glareY, myPorky.getX() + myPorky.getHitBoxOffsetX(),
                    myPorky.getY() + myPorky.getHitBoxOffsetY() + 10, bulletTexture);
                bullets.add(bullet);
            }
        }

    }

    private void updateObstacles() {
        // Update and remove obstacles
        for (int i = obstacles.size - 1; i >= 0; i--) {
            Obstacle obstacle = obstacles.get(i);
            obstacle.update(FIXED_TIMESTEP);

            if (obstacle.isOffScreen()) {
                obstacles.removeIndex(i);
                obstaclePool.free(obstacle);                       // Return to pool
                score++;
            }
        }
    }

    private void updateHunter() {
        // Update bullets
        float bulletSpeed = 500f;
        for (int i = bullets.size - 1; i >= 0; i--) {
            Bullet bullet = bullets.get(i);
            bullet.update(FIXED_TIMESTEP, bulletSpeed);

            // Check collision with Porky
            if (bullet.getHitbox().overlaps(myPorky.getHitbox())) {
                isGameOver = true;
                restartBuffered = false;
                bullets.clear(); // Clear bullets
                break;
            }

            // Remove bullet if it goes off-screen
            if (bullet.isOffScreen()) {
                bullets.removeIndex(i);
                score++;
            }
        }
    }

    private void renderBackground() {
        Sprite bgSprite = new Sprite(currentBgTexture);
        bgSprite.setSize(viewport.getWorldWidth(), viewport.getWorldHeight());

        bgFrameBuffer.begin();
        Gdx.gl.glClearColor(1, 1, 1, 1); // Clear the framebuffer
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        bgSprite.draw(batch);
        batch.end();

        bgFrameBuffer.end();
        bgFrameBufferTextureRegion = new TextureRegion(bgFrameBuffer.getColorBufferTexture());
        bgFrameBufferTextureRegion.flip(false, true); // Flip vertically for proper rendering
    }

    private void restartGame() {
        float ground = 63;
        isGameOver = false;
        score = 0;

        // Reset Porky Position, Obstacles && Other Elements
        myPorky.setGameOver(false);
        myPorky.setGround(ground);
        myPorky.setX(0);
        myPorky.setY(ground);
        obstacles.clear();
        activeClouds.clear();

        // Reset the background to default
        currentBgTexture = farmBgTexture;
        renderBackground();
    }

    private void manageDifficulty(){
        if (score % 10 == 0 && maxInterval > 1.5f) { // Increase difficulty every 10 points
            maxInterval -= 0.1f; // Gradually reduce maximum interval
        }

    }

    private void manageControls() {
        // Jump
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            jumpBuffered = true;
        }

        // Rolling
        if (Gdx.input.isKeyPressed(Input.Keys.DPAD_DOWN)) {
            myPorky.startRolling();
        }


        // Restart Game
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            restartBuffered = true;
        }


        // Go back to the main menu
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            ((Main) Gdx.app.getApplicationListener()).setScreen(new FirstScreen());
        }
    }

    private void disposeClouds() {
        for (Texture texture : cloudTextures) {
            texture.dispose();
        }
    }

    private void disposeObstacles() {
        for (Obstacle obstacle : obstacles) {
            obstacle.dispose();
        }
    }

    private void checkCollisions() {
        for (Obstacle obstacle : obstacles) {
            if (myPorky.getHitbox().overlaps(obstacle.getHitbox())) {
                isGameOver = true; // Handle collision (e.g., game over)
                myPorky.setGameOver(true);
                restartBuffered = false;
                break; // Exit loop once a collision is detected
            }
        }
    }
}


