package io.github.PorkyV2;

import com.badlogic.gdx.tools.texturepacker.TexturePacker;

public class MyTexturePacker {
    public static void main(String[] args) {
        TexturePacker.Settings settings = new TexturePacker.Settings();
        settings.maxWidth = 2048;  // Adjust as necessary
        settings.maxHeight = 2048;
        settings.paddingX = 2;    // Padding between images
        settings.paddingY = 2;
        TexturePacker.process(settings, "assets/glare", "assets/atlas", "glare-atlas");
    }
}
