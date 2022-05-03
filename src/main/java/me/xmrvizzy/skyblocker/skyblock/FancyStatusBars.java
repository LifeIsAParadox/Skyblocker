package me.xmrvizzy.skyblocker.skyblock;

import com.mojang.blaze3d.systems.RenderSystem;
import me.xmrvizzy.skyblocker.SkyblockerMod;
import me.xmrvizzy.skyblocker.config.SkyblockerConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class FancyStatusBars extends DrawableHelper {
    private static final Identifier BARS = new Identifier(SkyblockerMod.NAMESPACE, "textures/gui/bars.png");

    private final MinecraftClient client = MinecraftClient.getInstance();
    private final StatusBarTracker statusBarTracker = SkyblockerMod.getInstance().statusBarTracker;

    private final StatusBar[] bars = new StatusBar[]{
            new StatusBar(0, 16733525, 2),
            new StatusBar(1, 5636095, 2),
            new StatusBar(2, 12106180, 1),
            new StatusBar(3, 8453920, 1),
    };

    private int left;
    private int top;

    private int fill(int value, int max) {
        return (32 * value - 1) / max;
    }

    public boolean render(MatrixStack matrices, int scaledWidth, int scaledHeight) {
        var player = client.player;
        SkyblockerConfig.Bars barconf = SkyblockerConfig.get().general.bars;
        if (barconf == SkyblockerConfig.Bars.DISABLE || player == null)
            return false;
        if (barconf == SkyblockerConfig.Bars.OLD_BAR) {
            left = scaledWidth / 2 - 91;
            top = scaledHeight - 35;

            bars[0].update(statusBarTracker.getHealth());
            bars[1].update(statusBarTracker.getMana());
            int def = statusBarTracker.getDefense();
            bars[2].fill[0] = fill(def, def + 100);
            bars[2].text = def;
            bars[3].fill[0] = (int) (32 * player.experienceProgress);
            bars[3].text = player.experienceLevel;

            RenderSystem.setShaderTexture(0, BARS);
            for (var bar : bars)
                bar.draw(matrices);
            for (var bar : bars)
                bar.drawText(matrices);
        }

        if (barconf == SkyblockerConfig.Bars.NEW_BAR) {
            left = scaledWidth / 2 - 91;
            top = scaledHeight - 35;

            RenderSystem.setShaderTexture(0, BARS);

            drawTexture(matrices, left, top, 75, 10, 0, 0,43,9,256,256);
            drawTexture(matrices, left + 20, top, (54 * statusBarTracker.getHealth().value() - 1) / statusBarTracker.getHealth().max(), 10, 43, 0, 31,9,256,256);
            drawTexture(matrices, left + 20, top, (54 * statusBarTracker.getHealth().overflow() - 1) / statusBarTracker.getHealth().max(), 10, 43+30, 0, 31,9,256,256);
            drawTexture(matrices, left + 85, top, 15, 10, 0, 3*9,10,9,256, 256);
            drawTexture(matrices, left + 107, top, 75, 10, 0, 9,43,9,256, 256);
            drawTexture(matrices, left + 107 + 20, top, (54 * statusBarTracker.getMana().value() - 1) / statusBarTracker.getMana().max(), 10, 43, 9,31,9,256,256);
            drawTexture(matrices, left + 107 + 20, top, (54 * statusBarTracker.getMana().overflow() - 1) / statusBarTracker.getMana().max(), 10, 43 + 30, 9,31,9,256,256);

            TextRenderer textRenderer = client.textRenderer;
            String text = statusBarTracker.getHealth().value() + "/" + statusBarTracker.getHealth().max();
            textRenderer.draw(matrices, text,  left + ((float) (50 - text.length())/2), top - 8, 16733525);
            text = statusBarTracker.getMana().value() + "/" + statusBarTracker.getMana().max();
            textRenderer.draw(matrices, text,  left + 110 + ((float) (50 - text.length())/2), top - 8, 5636095);
            text = "" + player.experienceLevel;
            textRenderer.draw(matrices, text,  left + 60 + ((float) (50 - text.length())/2), top - 8, 8453920);
        }
        return true;
    }

    private class StatusBar {
        public final int[] fill;
        private final int offsetX;
        private final int v;
        private final int text_color;
        public Object text;

        private StatusBar(int i, int textColor, int fillNum) {
            this.offsetX = i * 46;
            this.v = i * 9;
            this.text_color = textColor;
            this.fill = new int[fillNum];
            this.fill[0] = 33;
            this.text = "";
        }

        public void update(StatusBarTracker.Resource resource) {
            int max = resource.max();
            int val = resource.value();
            this.fill[0] = fill(val, max);
            this.fill[1] = fill(resource.overflow(), max);
            this.text = val;
        }

        public void draw(MatrixStack matrices) {
            drawTexture(matrices, left + offsetX, top, 0, v, 43, 9);
            for (int i = 0; i < fill.length; i++)
                drawTexture(matrices, left + offsetX + 11, top, 43 + i * 31, v, fill[i], 9);
        }

        public void drawText(MatrixStack matrices) {
            TextRenderer textRenderer = client.textRenderer;
            String text = this.text.toString();
            int x = left + this.offsetX + 11 + (33 - textRenderer.getWidth(text)) / 2;
            int y = top - 3;

            final int[] offsets = new int[]{-1, 1};
            for (int i : offsets) {
                textRenderer.draw(matrices, text, (float) (x + i), (float) y, 0);
                textRenderer.draw(matrices, text, (float) x, (float) (y + i), 0);
            }
            textRenderer.draw(matrices, text, (float) x, (float) y, text_color);
        }
    }
}
