package com.cburch.logisim.riscv;

import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.GraphicsUtil;

import java.awt.*;

public class CpuDrawSupport {

    public static Bounds getBounds(int x, int y, int width, int height, boolean scale) {
        if (scale)
            return Bounds.create(
                    AppPreferences.getScaled(x),
                    AppPreferences.getScaled(y),
                    AppPreferences.getScaled(width),
                    AppPreferences.getScaled(height));
        return Bounds.create(x, y, width, height);
    }
    public static int getBlockWidth(Graphics2D g2, boolean scale) {
        FontMetrics f = g2.getFontMetrics();
        int StrWidth = f.stringWidth("0x00000000") + (scale ? AppPreferences.getScaled(2) : 2);
        int blkPrefWidth = scale ? AppPreferences.getScaled(80) : 80;
        return Math.max(StrWidth, blkPrefWidth);
    }

    public static void drawHexReg(
            Graphics2D g, int x, int y, boolean scale, int value, String Name, boolean valid) {
        Graphics2D g2 = (Graphics2D) g.create();
        Bounds bds;
        if (scale) g2.setFont(AppPreferences.getScaledFont(g.getFont()));
        bds = getBounds(x, y, 0, 0, scale);
        g2.translate(bds.getX(), bds.getY());
        int blockWidth = getBlockWidth(g2, scale);
        if (scale) blockWidth = AppPreferences.getDownScaled(blockWidth);
        g2.setColor(Color.YELLOW);
        bds = getBounds(0, 0, blockWidth, 30, scale);
        g2.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
        g2.setColor(Color.BLUE);
        bds = getBounds(0, 0, blockWidth, 15, scale);
        g2.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
        g2.setColor(Color.YELLOW);
        bds = getBounds(blockWidth / 2, 6, 0, 0, scale);
        GraphicsUtil.drawCenteredText(g2, Name, bds.getX(), bds.getY());
        g2.setColor(Color.BLACK);
        bds = getBounds(0, 0, blockWidth, 30, scale);
        g2.drawRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
        g2.setColor(Color.WHITE);
        bds = getBounds(1, 16, blockWidth - 2, 13, scale);
        g2.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
        g2.setColor(Color.BLACK);
        g2.drawRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
        g2.setColor(Color.MAGENTA);
        bds = getBounds(blockWidth / 2, 21, 0, 0, scale);
        GraphicsUtil.drawCenteredText(
                g2, valid ? String.format("0x%08X", value) : "??????????", bds.getX(), bds.getY());
        g2.dispose();
    }

    public static void drawRegisters(
            Graphics2D graphics, int posX, int posY, boolean scale, rv32imData state) {
        int blockWidth = getBlockWidth(graphics, scale);
        int blockX = ((scale ? AppPreferences.getScaled(160) : 160) - blockWidth) / 2;
        if (scale) {
            blockWidth = AppPreferences.getDownScaled(blockWidth);
            blockX = AppPreferences.getDownScaled(blockX);
        }

        // Background yellow rectangle
        graphics.setColor(Color.YELLOW);
        Bounds bdsYellow = getBounds(posX, posY, 160, 495, scale);
        graphics.fillRect(bdsYellow.getX(), bdsYellow.getY(), bdsYellow.getWidth(), bdsYellow.getHeight());

        // Blue header
        graphics.setColor(Color.BLUE);
        Bounds bdsBlue = getBounds(posX, posY, 160, 15, scale);
        graphics.fillRect(bdsBlue.getX(), bdsBlue.getY(), bdsBlue.getWidth(), bdsBlue.getHeight());

        // Header text
        graphics.setColor(Color.YELLOW);
        Bounds bdsHeaderText = getBounds(posX + 80, posY + 6, 0, 0, scale);
        GraphicsUtil.drawCenteredText(graphics, "Registers", bdsHeaderText.getX(), bdsHeaderText.getY());

        // Outer rectangle
        graphics.setColor(Color.BLACK);
        graphics.drawRect(posX, posY, bdsYellow.getWidth(), bdsYellow.getHeight());

        // Draw each register
        for (int i = 0; i < 32; i++) {
            // Register name
            Bounds bdsRegName = getBounds(posX + 20, posY + 21 + i * 15, 0, 0, scale);
            GraphicsUtil.drawCenteredText(graphics, "x" + i, bdsRegName.getX(), bdsRegName.getY());

            // Register value rectangle
            graphics.setColor(i == 0 ? Color.BLUE : Color.WHITE);
            Bounds bdsRegValue = getBounds(posX + blockX, posY + 16 + i * 15, blockWidth, 13, scale);
            graphics.fillRect(bdsRegValue.getX(), bdsRegValue.getY(), bdsRegValue.getWidth(), bdsRegValue.getHeight());

            // Register value text
            graphics.setColor(Color.BLACK);
            graphics.drawRect(bdsRegValue.getX(), bdsRegValue.getY(), bdsRegValue.getWidth(), bdsRegValue.getHeight());
            graphics.setColor(i == 0 ? Color.WHITE : Color.BLUE);
            Bounds bdsRegValueText = getBounds(bdsRegValue.getX() + blockWidth / 2, posY + 21 + i * 15, 0, 0, scale);
            GraphicsUtil.drawCenteredText(graphics, String.valueOf(state.getX(i)), bdsRegValueText.getX(), bdsRegValueText.getY());

            // Register ABI name
            graphics.setColor(Color.DARK_GRAY);
            Bounds bdsAbiName = getBounds(posX + 140, posY + 21 + i * 15, 0, 0, scale);
            GraphicsUtil.drawCenteredText(graphics, IntegerRegisters.registerABINames[i], bdsAbiName.getX(), bdsAbiName.getY());

            // Reset to black for the next loop
            graphics.setColor(Color.BLACK);
        }
    }
}
