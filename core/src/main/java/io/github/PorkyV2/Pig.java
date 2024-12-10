package io.github.PorkyV2;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

public class Pig {
    private final Animation<TextureRegion> runAnimation;
    private Animation<TextureRegion> rollAnimation;
    private final TextureRegion jumpFrame;
    private final TextureRegion bumpFrame;
    private final Rectangle hitbox;
    private final float height;
    private final float width;
    private float velocity;
    private float x;
    private float y;
    private float hitBoxOffsetX;

    private float ground = 63;
    private float animationTime = 0f;
    private float hitBoxOffsetY = 70;

    private float rollTimeElapsed = 0f; // Tracks how long the roll has lasted
    private final float maxRollTime = 0.5f; // Maximum duration of a roll in seconds

    private boolean isGrounded = true;
    private boolean isRolling;
    private boolean isGameOver;

    private float hitBoxWidth;
    private float hitBoxHeight;
    private float hitBoxY;
    private float hitBoxX;

    public Pig(float width, float height) {
        this.x = 0;
        this.y = ground;
        this.width = width;
        this.height = height;
        this.velocity = 0;
        this.hitbox = new Rectangle(hitBoxX, hitBoxY, hitBoxWidth, hitBoxHeight);
        this.isGameOver = false;
        this.hitBoxX = 0;
        this.hitBoxY = ground;
        this.hitBoxWidth = 60;
        this.hitBoxHeight = 50;

        // Running Animation
        TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("atlas/porky-atlas.atlas"));
        Array<TextureAtlas.AtlasRegion> runFrames = atlas.findRegions("run");
        runAnimation = new Animation<>(0.1f, runFrames, Animation.PlayMode.LOOP);

        // Roll Animation
        TextureAtlas rollAtlas = new TextureAtlas(Gdx.files.internal("atlas/porkyRoll-atlas.atlas"));
        Array<TextureAtlas.AtlasRegion> rollFrames = rollAtlas.findRegions("roll");
        rollAnimation = new Animation<>(0.1f, rollFrames, Animation.PlayMode.LOOP);

        // Jump Animation
        Texture jumpTexture = new Texture(Gdx.files.internal("porky_frames/jump.png"));
        jumpFrame = new TextureRegion(jumpTexture);

        Texture bumpTexture = new Texture(Gdx.files.internal("porky_frames/bump.png"));
        bumpFrame = new TextureRegion(bumpTexture);
    }

    public void update(float deltaTime) {
        animationTime += deltaTime;

        // Update rolling state
        if (isRolling) {
            rollTimeElapsed += deltaTime;
            if (rollTimeElapsed >= maxRollTime) {
                isRolling = false;
                rollTimeElapsed = 0f; // Reset roll timer
            }
        }

        // Apply velocity and gravity
        if (y > ground || velocity > 0) {
            float gravity = -1500;
            velocity += gravity * deltaTime;
            y += velocity * deltaTime;
        }

        // When at ground
        if (y <= ground) {
            y = ground;
            velocity = 0;
            isGrounded = true;
        }

        // Adjust hitbox size based on rolling state
        if (isRolling) {
            y = ground - 20;
            hitBoxWidth = 30; // Smaller hitbox width when rolling
            hitBoxHeight = 30; // Smaller hitbox height when rolling
            hitBoxOffsetX = 70;
        } else {
            hitBoxWidth = 60; // Default hitbox width
            hitBoxHeight = 50; // Default hitbox height
            hitBoxOffsetX = 50;
        }

        // Update hitbox
        hitbox.setPosition(x + hitBoxOffsetX, y + hitBoxOffsetY);
        hitbox.setSize(hitBoxWidth,hitBoxHeight);
    }

    public void jump() {
        if (y == ground) {                          // Allow jump only if on the ground
            velocity = 500;                         // Set upward velocity
            isGrounded = false;
        }
    }

    public void startRolling() {
        if (!isRolling) { // Only start rolling if not already rolling
            isRolling = true;
            rollTimeElapsed = 0f; // Reset the roll timer
        }
    }

    public TextureRegion getCurrentFrame() {
        TextureRegion currentFrame = null;

        if(!isGrounded){
            currentFrame = jumpFrame;
        }else if (isGameOver) {
            currentFrame = bumpFrame;
        }else if(isRolling){
            currentFrame = rollAnimation.getKeyFrame(animationTime);
        }else{
            currentFrame = runAnimation.getKeyFrame(animationTime); // Default animation;
        }
        return currentFrame;
    }

    // Getters and setters

    //Set the ground level
    public void setGround(float ground) {
        this.ground = ground; // Update the ground level dynamically
        y = ground; // Adjust y position to match new ground
        isGrounded = true;
    }

    public void setRolling(Boolean isRolling){
        this.isRolling = isRolling;
    }

    public void setGameOver(boolean isGameOver){
        this.isGameOver = isGameOver;
    }

    public void setX(float x){
        this.x = x;
    }

    public float getX() {
        return x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public Rectangle getHitbox(){
        return hitbox;
    }

    public float getHitBoxOffsetX(){
        return  hitBoxOffsetX;
    }

    public float getHitBoxOffsetY(){
        return  hitBoxOffsetY;
    }

    public float getHitBoxWidth(){
        return hitBoxWidth;
    }

    public float getHitBoxHeight(){
        return hitBoxHeight;
    }
}
