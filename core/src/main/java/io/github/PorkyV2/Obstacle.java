package io.github.PorkyV2;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

public class Obstacle {
    private final Rectangle obstacleHitbox;
    private Texture texture;
    private float x;
    private float y;
    private String terrainType;
    private float width = 50;
    private float height = 50;

    public Obstacle(float startX, float startY,String terrainType ,Texture obsTexture) {
        this.x = startX;
        this.y = startY;
        this.width = 50;
        this.height = 50;
        this.terrainType = terrainType;
        this.texture = obsTexture;
        this.obstacleHitbox = new Rectangle(x,y, width, height);
    }

    // New method to set position for reusing obstacles from the pool
    public void setPosition(float startX, float startY) {
        this.x = startX;
        this.y = startY;
        obstacleHitbox.setPosition(startX, startY);  // Update hitbox position as well
    }

    public void update(float deltaTime) {
        int hitboxOffsetX = 30;
        int hitboxOffsetY = 30;
        float speed = 400;                                      // Speed at which the obstacle moves
        x -= speed * deltaTime;                                     // Move obstacle to the left
        obstacleHitbox.setPosition(x + hitboxOffsetX,y + hitboxOffsetY);
    }

    public void render(SpriteBatch batch) {
        // Display Obstacle
        batch.draw(texture, x, y, 120, 120);
    }

    public void dispose() {
        texture.dispose();
    }

    public float getWidth() {
        return texture.getWidth() / 7f;
    }

    public Rectangle getHitbox() {
        return obstacleHitbox;
    }

    public boolean isOffScreen() {
        return x + getWidth() < 0; // Check if the obstacle has moved off-screen
    }

    public void setTerrainType(String terrainType){
        this.terrainType = terrainType;
    }

    public String getTerrainType() {
        return terrainType;
    }


    public void setTexture(Texture obsTextture){
        this.texture = obsTextture;
    }
}
