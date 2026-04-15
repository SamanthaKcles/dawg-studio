package ca.noxid.uiComponents;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class TabReorderHandler extends MouseAdapter {
	private final JTabbedPane tabbedPane;
	private int draggedTabIndex = -1;

	public interface TabMoveCallback {
		boolean canMove(int fromIndex, int toIndex);
		void moveTab(int fromIndex, int toIndex);
	}

	private final TabMoveCallback callback;

	public TabReorderHandler(JTabbedPane tabbedPane, TabMoveCallback callback) {
		this.tabbedPane = tabbedPane;
		this.callback = callback;
		tabbedPane.addMouseListener(this);
		tabbedPane.addMouseMotionListener(this);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (SwingUtilities.isLeftMouseButton(e)) {
			draggedTabIndex = tabbedPane.indexAtLocation(e.getX(), e.getY());
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (draggedTabIndex < 0) return;
		int targetIndex = tabbedPane.indexAtLocation(e.getX(), e.getY());
		if (targetIndex >= 0 && targetIndex != draggedTabIndex) {
			if (callback.canMove(draggedTabIndex, targetIndex)) {
				callback.moveTab(draggedTabIndex, targetIndex);
				draggedTabIndex = targetIndex;
			}
		}
		tabbedPane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		draggedTabIndex = -1;
		tabbedPane.setCursor(Cursor.getDefaultCursor());
	}

	public static void moveTabInPane(JTabbedPane pane, int from, int to) {
		String title = pane.getTitleAt(from);
		Icon icon = pane.getIconAt(from);
		Component comp = pane.getComponentAt(from);
		String tooltip = pane.getToolTipTextAt(from);
		pane.removeTabAt(from);
		pane.insertTab(title, icon, comp, tooltip, to);
		pane.setSelectedIndex(to);
	}
}
