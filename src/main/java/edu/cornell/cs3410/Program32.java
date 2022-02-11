package edu.cornell.cs3410;

import java.io.IOException;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.Font;
import java.awt.Window;

import javax.swing.JLabel;
import javax.swing.JOptionPane;

import com.cburch.logisim.proj.Project;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringUtil;
import com.cburch.logisim.gui.main.Frame;

import edu.cornell.cs3410.ProgramAssembler.Listing;

/** Represents a program ROM.
 */
class Program32 extends InstanceFactory {

	static final int NUM_ROWS = 5; // must be odd

	// size
	static final int CHIP_WIDTH = 240;
	static final int CHIP_DEPTH = 20 * NUM_ROWS + 20;

	static final int PC_WIDTH = 32;
    static final int INST_WIDTH = 32;

	static final int BOX_WIDTH = 196;
	static final int ACOL_WIDTH = 54;
	static final int ARROW_WIDTH = 20;

	static final int P_PC = 0;
	static final int P_OP = 1;
	static final int NUM_PINS = 2;
    
	public static final Attribute<Listing> CONTENTS_ATTR = new ContentsAttribute();
    
    public Program32() {
        super("RISC-VProgramROM", new SimpleStringGetter("RISC-V Program ROM"));
        setAttributes(new Attribute[] { CONTENTS_ATTR }, new Object[] { new Listing() });
        setOffsetBounds(Bounds.create(-1*CHIP_WIDTH, -1*CHIP_DEPTH/2, CHIP_WIDTH, CHIP_DEPTH));
        setPorts(new Port[] {
            new Port(10 - CHIP_WIDTH, CHIP_DEPTH / 2, Port.INPUT, PC_WIDTH),
            new Port(0, 0, Port.OUTPUT, INST_WIDTH)
        });
        setInstancePoker(ProgramPoker.class);
    }

    private Listing getCode(InstanceState state) {
        return state.getAttributeValue(CONTENTS_ATTR);
    }

	private Value val(InstanceState s, int pin) { return s.getPortValue(pin); }
	private int addr(InstanceState s, int pin) { return val(s, pin).toIntValue(); }

    @Override
    public void propagate(InstanceState state) {
        ProgramState programState = ProgramState.get(state, getCode(state));
        programState.update(val(state, P_PC));
        state.setPort(P_OP, programState.instr(), 9);
    }

    @Override
    public AttributeSet createAttributeSet() {
        return new ProgramAttributes();
    }

	private void drawBox(Graphics g, Bounds bds, Color color) {
		g.setColor(Color.WHITE);
		g.fillRect(bds.getX() + ARROW_WIDTH, bds.getY() + 5,
				ACOL_WIDTH, 20*NUM_ROWS+10);
		g.fillRect(bds.getX() + ARROW_WIDTH, bds.getY() + 5,
				BOX_WIDTH, 20*NUM_ROWS+10);
		g.setColor(color);
		g.drawRect(bds.getX() + ARROW_WIDTH, bds.getY() + 5,
				ACOL_WIDTH, 20*NUM_ROWS+10);
		g.drawRect(bds.getX() + ARROW_WIDTH, bds.getY() + 5,
				BOX_WIDTH, 20*NUM_ROWS+10);
		g.setColor(Color.BLACK);
	}

	private void drawArrow(Graphics g, Bounds bds, Color color) {
		int left = bds.getX()+ARROW_WIDTH-13;
		int c = bds.getY() + 20*NUM_ROWS/2 + 10;
		int[] xs = { left, left+8, left, left };
		int[] ys = { c-5, c, c+5, c-5 };
		g.setColor(color);
		g.fillPolygon(xs, ys, 4);
		g.setColor(Color.BLACK);
		g.drawPolyline(xs, ys, 4);
	}

    @Override
    public void paintInstance(InstancePainter painter) {
        Bounds bds = painter.getBounds();
        painter.drawRectangle(bds, "");
		Graphics g = painter.getGraphics();

		GraphicsUtil.drawText(g, "PC",
				bds.getX() + 2, bds.getY() + CHIP_DEPTH - 12,
				GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
		GraphicsUtil.drawText(g, "Op",
				bds.getX() + CHIP_WIDTH - 2, bds.getY() + CHIP_DEPTH/2,
				GraphicsUtil.H_RIGHT, GraphicsUtil.V_CENTER);
		painter.drawPort(P_PC /*, "PC", Direction.EAST */);
		painter.drawPort(P_OP /*, "Op", Direction.WEST */);

		// draw some rectangles 
		drawBox(g, bds, Color.GRAY);
        
		if (painter.getShowState()) {
			drawState(painter);
		}
	}

    @Override
    public void paintIcon(InstancePainter painter) {
        Graphics g = painter.getGraphics();
        Font old = g.getFont();
        g.setFont(old.deriveFont(9.0f));
        GraphicsUtil.drawCenteredText(g, "ASM", 10, 9);
        g.setFont(old);
        g.drawRect(0, 4, 19, 12);
        for (int dx = 2; dx < 20; dx += 5) {
            g.drawLine(dx, 2, dx, 4);
            g.drawLine(dx, 16, dx, 18);
        }
    }

	private void drawState(InstancePainter painter) {
        Font font = new Font("Monospaced", Font.PLAIN, 10);
		ProgramState state = ProgramState.get(painter, getCode(painter));
		if (state.code == null || state.code.isEmpty()) {
			return;
		}

		Graphics g = painter.getGraphics();
		Bounds bds = painter.getBounds();

		Color arrowcolor;
		if (state.isErrorPC()) arrowcolor = Color.RED;
		else if (state.isUndefinedPC()) arrowcolor = Color.GRAY;
		else if (state.haveCodeFor(state.pc)) arrowcolor = Color.BLUE;
		else arrowcolor = Color.BLUE; // Color.YELLOW;
		drawArrow(g, bds, arrowcolor);

		int j = -1;
		int pc = (state.isValidPC() ? state.pc : -1);
		for (int i = pc - (NUM_ROWS-1)/2; i <= pc + (NUM_ROWS-1)/2; i++) {
			j++;
			if (i < 0 || i > 0x3fffffff) continue;
			if (i == state.pc) g.setColor(Color.BLUE);
			else if (!state.haveCodeFor(i)) g.setColor(Color.GRAY);
			GraphicsUtil.drawText(g, font, StringUtil.toHexString(32, i*4),
					bds.getX() + ARROW_WIDTH + 2,
					bds.getY() + 20*j + 20/2 + 10,
					GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
			GraphicsUtil.drawText(g, font, state.decode(i), 
					bds.getX() + ARROW_WIDTH + ACOL_WIDTH + 1,
					bds.getY() + 20*j + 20/2 + 10,
					GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
			g.setColor(Color.BLACK);
		}
    }

    private static class ContentsAttribute extends Attribute<Listing> {
        public ContentsAttribute() {
            super("contents", new SimpleStringGetter("RISC-V Program Listing"));
        }
        public java.awt.Component getCellEditor(Window source, Listing value) {
            if(source instanceof Frame) {
                Project proj = ((Frame) source).getProject();
                Listing code = value;
                ProgramState state = code.getState();
                if (state != null) state.setProject(proj); // will set on Program/AutoProgram
            }
            ContentsCell cell = new ContentsCell(source, value);
            cell.mouseClicked(null); // this cannot be called in constructor
            return cell;
        }
        public String toDisplayString(Listing value) { return "(click to edit)"; }
        public String toStandardString(Listing value) {
            return value.getSource();
        }
        public Listing parse(String value) {
            try {
                return new Listing(value);
            } catch(IOException e) {
                // too bad this will be in back of the splash
                JOptionPane.showMessageDialog(null, "The contents of the Program chip could not be read: " +
                        e.getMessage(),
                        "Error loading RISC-V program." ,
                        JOptionPane.ERROR_MESSAGE);
                return new Listing();
            }
        }
    }
    private static class ContentsCell extends JLabel implements MouseListener {
        private static final long serialVersionUID = -2005158851998108994L;
        private Window source;
        private Listing code;

        ContentsCell(Window source, Listing code) {
            super("(click here to edit)");
            this.source = source;
            this.code = code;
            addMouseListener(this);
        }

        public void mouseClicked(MouseEvent e) {
            if (code == null) return;
			Project proj = source instanceof Frame ? ((Frame) source).getProject() : null;
			ProgramFrame32 frame = ProgramAttributes.getProgramFrame(code, proj);
            frame.setVisible(true);
            frame.toFront();
        }

        public void mousePressed(MouseEvent e) { }
        public void mouseReleased(MouseEvent e) { }
        public void mouseEntered(MouseEvent e) { }
        public void mouseExited(MouseEvent e) { }
    }

    public static class ProgramPoker extends InstancePoker {
        @Override
        public boolean init(InstanceState state, MouseEvent e) {
            return state.getInstance().getBounds().contains(e.getX(), e.getY());
        }
    }

}
