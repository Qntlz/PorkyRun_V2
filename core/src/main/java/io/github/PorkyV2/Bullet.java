package io.github.PorkyV2;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

public class Bullet {
    private float x, y;
    private float targetX, targetY;
    private float velocityX, velocityY;
    private Rectangle hitbox;
    private Texture texture;
    private float hitBoxWidth = 20;
    private float hitBoxHeight = 20;

    public Bullet(float startX, float startY, float targetX, float targetY, Texture texture) {
        this.x = startX;
        this.y = startY;
        this.targetX = targetX;
        this.targetY = targetY;
        this.texture = texture;

        // Calculate velocity vector
        float deltaX = targetX - startX;
        float deltaY = targetY - startY;
        float length = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        this.velocityX = deltaX / length;
        this.velocityY = deltaY / length;

        this.hitbox = new Rectangle(x, y, hitBoxWidth, hitBoxHeight);
    }

    public void update(float deltaTime, float speed) {
        x += velocityX * speed * deltaTime;
        y += velocityY * speed * deltaTime;
        hitbox.setPosition(x, y);
    }

    public void render(SpriteBatch batch) {
        batch.draw(texture, x, y, 50,50);
    }

    public Rectangle getHitbox() {
        return hitbox;
    }

    public boolean isOffScreen() {
        return x < 0 || y < 0 || x > 800 || y > 500; // Adjust screen boundaries
    }
}
