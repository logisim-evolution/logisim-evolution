package com.cburch.draw.tools;

import com.cburch.draw.canvas.Canvas;

public abstract class Action {
    public abstract void execute(Canvas canvas, int mx, int my, int mods, boolean ctrlPressed, int dx, int dy);
    public abstract void execute(Canvas canvas, int mx, int my, int mods, boolean ctrlPressed, boolean shiftPressed,int dx, int dy);

}
