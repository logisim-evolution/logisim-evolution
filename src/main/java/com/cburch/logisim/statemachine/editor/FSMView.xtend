package com.cburch.logisim.statemachine.editor

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.MenuItem
import java.awt.Panel
import java.awt.Point
import java.awt.PopupMenu
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.awt.event.MouseWheelEvent
import java.time.ZonedDateTime
import java.util.List
import javax.swing.JPanel
import com.cburch.logisim.statemachine.editor.view.FSMSelectionZone
import com.cburch.logisim.statemachine.editor.view.Zone
import com.cburch.logisim.statemachine.fSMDSL.FSM
import com.cburch.logisim.statemachine.fSMDSL.FSMElement
import com.cburch.logisim.statemachine.fSMDSL.LayoutInfo
import com.cburch.logisim.std.fsm.IFSMEditor
import com.cburch.logisim.statemachine.editor.view.FSMSelectionZone.AreaType

// CanvasPanel is the class upon which we actually draw. It listens
// for mouse events and calls the appropriate method of the current
// command.
class FSMView extends JPanel implements MouseListener, MouseMotionListener {
	/** 
	 */
	static final long serialVersionUID = 0
	FSMEditorController controller
	// the drawing: shapes in order
	final IFSMEditor editor
	double scale = 1.0
	Point unscaledPos
	Point scaledPos = new Point(0, 0)
	FSMPopupMenu popupMenu 

	// Constructor just needs to set up the CanvasPanel as a listener.
	new(IFSMEditor parent) {
		super()
		editor = parent
		var FSM fsm = editor.getContent().getFsm()
		controller = new FSMEditorController(this, fsm) // make
		addMouseListener(this)
		addMouseMotionListener(this)
		setPreferredSize(new Dimension(1000, 1000))
		addMouseWheelListener(new MouseAdapter() {
			override void mouseWheelMoved(MouseWheelEvent e) {
				var double delta = 0.05f * e.getPreciseWheelRotation()
				scale += delta
				System::out.println('''Scale=«scale»'''.toString)
				revalidate()
				repaint()
			}
		})
		this.popupMenu = new FSMPopupMenu(this)
		
		setBackground(Color::white)
	}

	override void repaint() {
		super.repaint
	}
	override void paint(Graphics page) {
		try {
			var LayoutInfo l = getController().getFSM().getLayout()
			setPreferredSize(new Dimension(Math::max(500, l.getWidth()), Math::max(500, l.getHeight())))
		} catch (Exception e) {
			println("layout issue")			
		}
		super.paint(page) // execute the paint method of JPanel
		var Graphics2D g = page as Graphics2D
		g.scale(scale, scale)
		controller.draw(page as Graphics2D)
	}

	def private void showMouseCursor(Graphics page, Graphics2D g) {
		var int x = (scaledPos.x) as int
		var int y = (scaledPos.y) as int
		var String label = '''[«x»,«y»]'''.toString
		var int sw = page.getFontMetrics().stringWidth(label)
		g.setColor(Color::blue)
		g.drawString(label, x - sw / 2, y)
		page.drawOval(x - 10, y - 10, 20, 20)
		g.setColor(Color::black)
	}

	def void showContextMenu(AreaType type) {
		popupMenu.showPopupMenu(unscaledPos,type)
	}




	def private void updatePosition(MouseEvent e) {
		scaledPos = new Point((e.getX() / scale) as int, (e.getY() / scale) as int)
		unscaledPos = new Point(e.getX(), e.getY())
	}

	// When the mouse is clicked, call the executeClick method of the
	// current command.
	override void mouseClicked(MouseEvent e) {
		updatePosition(e)
		var FSMEditorController ctrl = this.getController()
		if (e.getButton() === MouseEvent::BUTTON1) {
			if (e.getClickCount() === 2) {
				ctrl.executeDoubleClick(scaledPos)
			} else {
				ctrl.executeLeftClick(scaledPos)
			}
		} else {
			ctrl.executeRightClick()
		}
	}

	override void mousePressed(MouseEvent event) {
		updatePosition(event)
		getController().executePress(scaledPos)
		repaint()
	}

	override void mouseDragged(MouseEvent event) {
		updatePosition(event)
		getController().executeDragged(scaledPos)
		repaint()
	}

	override void mouseReleased(MouseEvent event) {
		updatePosition(event)
		getController().executeRelease(scaledPos)
		repaint()
	}

	override void mouseEntered(MouseEvent event) {
	}

	override void mouseExited(MouseEvent event) {
	}


	def getScaledPosition() {
		scaledPos
	}
	
	override void mouseMoved(MouseEvent event) {
		updatePosition(event)
		controller.executeMove(scaledPos)
	}

	def FSMEditorController getController() {
		return controller
	}

	def void setScale(double d) {
		scale = d
	}

}
