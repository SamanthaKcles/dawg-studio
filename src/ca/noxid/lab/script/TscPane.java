package ca.noxid.lab.script;

import ca.noxid.lab.BlConfig;
import ca.noxid.lab.Changeable;
import ca.noxid.lab.EditorApp;
import ca.noxid.lab.Messages;
import ca.noxid.lab.gameinfo.GameInfo;
import ca.noxid.lab.mapdata.Mapdata;
import ca.noxid.lab.rsrc.ResourceManager;
import ca.noxid.uiComponents.BgList;
import ca.noxid.uiComponents.LinkLabel;
import ca.noxid.uiComponents.UpdateTextField;
import com.carrotlord.string.StrTools;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.*;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.List;

public class TscPane extends JTextPane implements ActionListener, Changeable {
	private static final long serialVersionUID = 6530249265060832403L;
	private static final Hashtable<String, SimpleAttributeSet> styles = initStyles(); //maps styles for text formatting
	public static final String STYLE_EVENT = "eveNum"; //$NON-NLS-1$
	public static final String STYLE_SBEVENT = "sbEvent";
	public static final String STYLE_SBFLAGS = "sbFlag";
	public static final String STYLE_TAG = "tag"; //$NON-NLS-1$
	public static final String STYLE_NUM = "number"; //$NON-NLS-1$
	public static final String STYLE_FLAG = "flag";
	public static final String STYLE_SPACER = "spacer"; //$NON-NLS-1$
	public static final String STYLE_TXT = "text"; //$NON-NLS-1$
	public static final String STYLE_OVER = "overflow"; //$NON-NLS-1$
	public static final String STYLE_STRING = "string";
	public static final String STYLE_COMMENT = "comment"; //$NON-NLS-1$
	public static final String STYLE_UNKNOWN = "unknown";
	private static final String SET_KEY = "set key"; //$NON-NLS-1$
	private static final String SET_VALUE = "set value"; //$NON-NLS-1$
	private static Vector<TscCommand> commandInf = getCommands();
	private static List<String> musicList = getMusicList();
	private static List<String> sfxList = getSfxList();
	private static List<String> equipList = getEquipList();
	private static Set<String> eventEnders = loadEventEnders();
	private static Vector<String> def1 = new Vector<>();
	private static Vector<String> def2 = new Vector<>();
	private final JTextArea comLabel = new JTextArea(Messages.getString("TscPane.9"), 2, 18); //$NON-NLS-1$
	private final JTextArea descLabel = new JTextArea(Messages.getString("TscPane.10"), 4, 18); //$NON-NLS-1$
	private static JPanel commandPanel = null;
	private static JPanel defPanel = null;
	//private JTextPane scriptArea; //this
	private static JList<String> commandList;
	private static JPanel commandListExtras;
	private static TscPane lastFocus;
	private GameInfo exeDat;
	private ResourceManager rm;
	private File scriptFile;
	private File srcFile;
	private int mapNum;

	private boolean changed;
	private boolean justSaved = false;
	private boolean saveSource = true;
	private UndoManager undoManager = new UndoManager();
	private int lastTagPos = -1;
	private boolean updatingFromExtras = false;

	private EditorApp.LoadMapAction loadmap = null;

	public static JPanel getDefPanel() {
		return defPanel;
	}

	public static JPanel getComPanel() {
		return commandPanel;
	}

	public TscPane(GameInfo inf, int num, EditorApp p, ResourceManager iMan) {
		exeDat = inf;
		rm = iMan;
		mapNum = num;
		if (EditorApp.blazed) {
			this.setCursor(ResourceManager.cursor);
		}
		saveSource = exeDat.getConfig().getUseScriptSource();
		scriptFile = exeDat.getScriptFile(num);
		srcFile = exeDat.getScriptSource(num);
		//EditorApp.TabOrganizer t = p.new TabOrganizer();
		//init
		initActions(iMan);
		//fill
		loadFile(srcFile, scriptFile);
		//this.setText(parseScript(srcFile.exists() ? srcFile : scriptFile));
		//style

		loadmap = p.new LoadMapAction();

		highlightDoc(this.getStyledDocument(), 0, -1);
		undoManager.discardAllEdits();
	}

	public TscPane(GameInfo inf, Mapdata mapdat, EditorApp p,
			ResourceManager iMan) {
		rm = iMan;
		exeDat = inf;
		if (EditorApp.blazed) {
			this.setCursor(ResourceManager.cursor);
		}
		File dir = exeDat.getDataDirectory();
		saveSource = exeDat.getConfig().getUseScriptSource();
		scriptFile = new File(dir + "/Stage/" + mapdat.getFile() + ".tsc"); //$NON-NLS-1$ //$NON-NLS-2$
		srcFile = new File(dir + "/Stage/ScriptSource/" + mapdat.getFile() + ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
		//init
		initActions(iMan);

		loadmap = p.new LoadMapAction();
		loadFile(srcFile, scriptFile);
		//style
		highlightDoc(this.getStyledDocument(), 0, -1);
		undoManager.discardAllEdits();
	}

	public TscPane(GameInfo inf, File tscFile, ResourceManager iMan) {
		exeDat = inf;
		rm = iMan;
		saveSource = exeDat.getConfig().getUseScriptSource();
		scriptFile = tscFile;
		srcFile = new File(tscFile.getParent() + "/ScriptSource/"  //$NON-NLS-1$
				+ tscFile.getName().replace(".tsc", ".txt")); //$NON-NLS-1$ //$NON-NLS-2$
		initActions(iMan);
		//fill
		loadFile(srcFile, scriptFile);
		//this.setText(parseScript(srcFile.exists() ? srcFile : scriptFile));
		//style
		highlightDoc(this.getStyledDocument(), 0, -1);
		undoManager.discardAllEdits();
	}

	private void loadFile(File srcFile, File scriptFile) {
		String encoding = exeDat.getConfig().getEncoding();
		String tabText;
		//fill
		if (srcFile.exists() && saveSource) {
			if (srcFile.lastModified() > scriptFile.lastModified()) {
				int choice = JOptionPane.showOptionDialog(lastFocus,
						"ScriptSource is more recent than the TSC file",
						"Warning",
						JOptionPane.YES_NO_CANCEL_OPTION,
						JOptionPane.WARNING_MESSAGE,
						null,
						new Object[] {"Use ScriptSource", "Use TSC", "Compare Files"}, "Use TSC");

				switch (choice) {
				case JOptionPane.YES_OPTION: //use scriptsource
					tabText = parseScript(srcFile, encoding);
					break;
				case JOptionPane.NO_OPTION: //use tsc
					tabText = parseScript(scriptFile, encoding);
					break;
				default: //compare
					CompareScriptDialog csd = new CompareScriptDialog(srcFile, scriptFile);
					tabText = parseScript(csd.getSelection(), encoding);
				}
			} else {
				//source file is newer
				tabText = parseScript(srcFile, encoding);
			}
		} else {
			// no source file
			tabText = parseScript(scriptFile, encoding);
		}
		this.setText(tabText);
	}

	private class CompareScriptDialog extends JDialog {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private File selection;

		public File getSelection() {
			return selection;
		}

		CompareScriptDialog(final File srcFile, final File scriptFile) {
			super();
			selection = scriptFile;
			JSplitPane disp = new JSplitPane();

			JPanel left = new JPanel();
			left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
			String opt1 = parseScript(srcFile, exeDat.getConfig().getEncoding());
			JTextPane sourcePane = new JTextPane();
			sourcePane.setText(opt1);
			sourcePane.setEditable(false);
			highlightDoc(sourcePane.getStyledDocument(), 0, -1);
			JScrollPane jsp = new JScrollPane(sourcePane);
			jsp.setPreferredSize(new Dimension(320, 480));
			left.add(jsp);
			JButton button = new JButton(new AbstractAction() {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent arg0) {
					selection = srcFile;
					dispose();
				}

			});
			button.setText("Use ScriptSource version");
			left.add(button);

			String opt2 = parseScript(scriptFile, exeDat.getConfig().getEncoding());
			JPanel right = new JPanel();
			right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
			sourcePane = new JTextPane();
			sourcePane.setText(opt2);
			sourcePane.setEditable(false);
			highlightDoc(sourcePane.getStyledDocument(), 0, -1);
			jsp = new JScrollPane(sourcePane);
			right.add(jsp);
			jsp.setPreferredSize(new Dimension(320, 480));
			button = new JButton(new AbstractAction() {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent arg0) {
					selection = scriptFile;
					dispose();
				}

			});
			button.setText("Use TSC version");
			right.add(button);

			disp.setLeftComponent(left);
			disp.setRightComponent(right);

			this.setContentPane(disp);
			this.pack();

			this.setModal(true);
			this.setVisible(true);
		}
	}

	private void initActions(ResourceManager iMan) {

		if (defPanel == null) {
			defPanel = createDefinePanel();
		}
		if (commandPanel == null) {
			commandPanel = createCommandPanel(iMan);
		}
		this.addKeyListener(new java.awt.event.KeyAdapter() {
			public void keyReleased(java.awt.event.KeyEvent evt) {
				textBoxKeyReleased(evt);
			}
		});
		undoManager.setLimit(180);
		this.getDocument().addUndoableEditListener(new UndoableEditListener() {
			@Override
			public void undoableEditHappened(UndoableEditEvent e) {
				UndoableEdit edit = e.getEdit();
				if (edit instanceof AbstractDocument.DefaultDocumentEvent) {
					AbstractDocument.DefaultDocumentEvent docEvent = (AbstractDocument.DefaultDocumentEvent) edit;
					if (docEvent.getType() != javax.swing.event.DocumentEvent.EventType.CHANGE) {
						undoManager.addEdit(edit);
					}
				}
			}
		});
		this.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "tsc-undo");
		this.getActionMap().put("tsc-undo", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					if (undoManager.canUndo()) undoManager.undo();
				} catch (CannotUndoException ignored) {}
			}
		});
		this.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "tsc-redo");
		this.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "tsc-redo");
		this.getActionMap().put("tsc-redo", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					if (undoManager.canRedo()) undoManager.redo();
				} catch (CannotRedoException ignored) {}
			}
		});

		lastFocus = this;
		this.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				if (e.isControlDown()) {
					int delta = -e.getWheelRotation();
					updateFontSize(delta);
					e.consume();
				} else {
					getParent().dispatchEvent(e);
				}
			}
		});
		this.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent arg0) {
				lastFocus = TscPane.this;
			}

			@Override
			public void focusLost(FocusEvent arg0) {
				// nothing				
			}
		});

		this.addCaretListener(new CaretListener() {
			@Override
			public void caretUpdate(CaretEvent eve) {
				if (commandList.isVisible()) {
					JTextPane area = (JTextPane) eve.getSource();
					int cPos = eve.getDot();
					//weed out carriage return characters
					String txt = area.getText();
					if (txt == null) {
						return;
					}
					txt = txt.replace("\r", ""); //$NON-NLS-1$ //$NON-NLS-2$
					String searchTxt = txt.substring(0, cPos);
					int tagPos = searchTxt.lastIndexOf('<');
					if (tagPos < 0) {
						lastTagPos = -1;
						return;
					}
					if ((txt.length() - tagPos >= 4)) {
						String tag = txt.substring(tagPos, tagPos + 4);
						int index = commandList.getNextMatch(tag, 0, Position.Bias.Forward);
						commandList.setSelectedIndex(index);
						lastTagPos = tagPos;
						setCommandExtras(index, txt.substring(tagPos, txt.length()));
					}
				}
			}
		});
	}

	private void setCommandExtras(int commandIndex, String cmd) {

		commandListExtras.removeAll();
		TscCommand selCom;
		if (commandInf != null && commandInf.size() != 0 && commandIndex >= 0) {
			selCom = commandInf.elementAt(commandIndex);
		} else {
			commandListExtras.revalidate();
			return;
		}

		int commandSize = 4;
		for (int i = 0; i < selCom.numParam; i++) {
			if (selCom.paramLen[i] >= 0) {
				commandSize += selCom.paramLen[i];
			}
			if (selCom.paramSep && i < selCom.numParam - 1)
				commandSize++;
		}
		int paramStart = 4;
		if (cmd.length() >= commandSize) {
			for (int i = 0; i < selCom.numParam; i++) {
				int paramLen = selCom.paramLen[i];
				final int paramOffset = paramStart;
				String arg;
				if (paramLen < 0) {
					int end = cmd.indexOf('$', paramStart);
					if (end < 0) end = cmd.length();
					else end++;
					arg = cmd.substring(paramStart, end);
					paramStart = end;
				} else {
					if (paramStart + paramLen > cmd.length()) break;
					arg = cmd.substring(paramStart, paramStart + paramLen);
					paramStart += paramLen;
				}
				if (selCom.paramSep && i < selCom.numParam - 1)
					paramStart++;
				addCommandExtra(selCom.CE_param[i], arg, paramOffset, paramLen);
			}
		}
		commandListExtras.revalidate();
	}

	@SuppressWarnings("serial")
	private static final Map<Character, String> extraNameMap = new HashMap<Character, String>() {{
		put('a', "Weapon");
		put('A', "Ammo");
		put('d', "Direction");
		put('e', "Event");
		put('E', "Equip");
		put('f', "Face");
		put('F', "Flag");
		put('g', "Graphic");
		put('l', "Illustration");
		put('i', "Item");
		put('m', "Map");
		put('u', "Music");
		put('N', "NPC Number");
		put('n', "NPC Type");
		put('s', "Sound");
		put('t', "Tile");
		put('x', "X Coord");
		put('y', "Y Coord");
		put('#', "Number");
		put('.', "Ticks");
		put('$', "String");
	}};

	private void updateArgInScript(int paramOffset, int paramLen, String newValue) {
		if (lastTagPos < 0 || updatingFromExtras) return;
		updatingFromExtras = true;
		try {
			StyledDocument doc = getStyledDocument();
			int docPos = lastTagPos + paramOffset;
			int startLine = 0;
			if (paramLen > 0) {
				String padded;
				try {
					int num = Integer.parseInt(newValue.trim());
					padded = String.format("%0" + paramLen + "d", num);
					if (padded.length() > paramLen) padded = padded.substring(padded.length() - paramLen);
				} catch (NumberFormatException e) {
					padded = newValue.trim();
					while (padded.length() < paramLen) padded = "0" + padded;
					if (padded.length() > paramLen) padded = padded.substring(padded.length() - paramLen);
				}
				String text = doc.getText(0, docPos);
				for (int i = 0; i < text.length(); i++) {
					if (text.charAt(i) == '\n') startLine++;
				}
				doc.remove(docPos, paramLen);
				doc.insertString(docPos, padded, null);
			} else {
				// variable-length string param terminated by '$'
				String rawText = doc.getText(0, doc.getLength());
				for (int i = 0; i < docPos; i++) {
					if (rawText.charAt(i) == '\n') startLine++;
				}
				int end = rawText.indexOf('$', docPos);
				int removeLen = (end < 0 ? rawText.length() : end + 1) - docPos;
				doc.remove(docPos, removeLen);
				doc.insertString(docPos, newValue + "$", null);
			}
			highlightDoc(doc, startLine - 5, startLine + 5);
			markChanged();
		} catch (BadLocationException e) {
			e.printStackTrace();
		} finally {
			updatingFromExtras = false;
		}
	}

	private void showFacePicker(BufferedImage faceSheet, int faceSize, int paramOffset, int paramLen) {
		Window owner = SwingUtilities.getWindowAncestor(this);
		JDialog picker = new JDialog(owner instanceof Frame ? (Frame) owner : null, "Select Face", true);
		int tileSize = exeDat.getConfig().getTileSize();
		int sourceFaceSize = (tileSize > 16) ? faceSize * (tileSize / 16) : faceSize;
		BufferedImage displaySheet = faceSheet;
		int displaySize = sourceFaceSize;
		if (tileSize == 16) {
			displaySize = sourceFaceSize * 2;
			displaySheet = new BufferedImage(faceSheet.getWidth() * 2, faceSheet.getHeight() * 2, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = displaySheet.createGraphics();
			g.drawImage(faceSheet, 0, 0, faceSheet.getWidth() * 2, faceSheet.getHeight() * 2, null);
			g.dispose();
		}
		int cols = faceSheet.getWidth() / sourceFaceSize;
		final BufferedImage finalDisplaySheet = displaySheet;
		final int finalDisplaySize = displaySize;
		JPanel sheetPanel = new JPanel() {
			private static final long serialVersionUID = 1L;
			@Override public Dimension getPreferredSize() { return new Dimension(finalDisplaySheet.getWidth(), finalDisplaySheet.getHeight()); }
			@Override protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				g.drawImage(finalDisplaySheet, 0, 0, null);
				g.setColor(new Color(200, 200, 200, 120));
				int rows = finalDisplaySheet.getHeight() / finalDisplaySize;
				for (int r = 0; r <= rows; r++) g.drawLine(0, r * finalDisplaySize, finalDisplaySheet.getWidth(), r * finalDisplaySize);
				for (int c = 0; c <= cols; c++) g.drawLine(c * finalDisplaySize, 0, c * finalDisplaySize, finalDisplaySheet.getHeight());
			}
		};
		sheetPanel.addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
				int newFace = (e.getY() / finalDisplaySize) * cols + (e.getX() / finalDisplaySize);
				updateArgInScript(paramOffset, paramLen, String.valueOf(newFace));
				picker.dispose();
			}
		});
		sheetPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		JScrollPane scroll = new JScrollPane(sheetPanel);
		int side = finalDisplaySheet.getWidth();
		int vbarWidth = scroll.getVerticalScrollBar().getPreferredSize().width;
		scroll.setPreferredSize(new Dimension(side + vbarWidth, side));
		scroll.getVerticalScrollBar().setUnitIncrement(finalDisplaySize * 2);
		scroll.getHorizontalScrollBar().setUnitIncrement(finalDisplaySize * 2);
		picker.add(scroll);
		picker.pack();
		Point mousePos = MouseInfo.getPointerInfo().getLocation();
		picker.setLocation(mousePos.x - picker.getWidth(), mousePos.y);
		picker.setVisible(true);
	}

	private void showItemPicker(BufferedImage itemSheet, int itemW, int itemH, int paramOffset, int paramLen) {
		Window owner = SwingUtilities.getWindowAncestor(this);
		JDialog picker = new JDialog(owner instanceof Frame ? (Frame) owner : null, "Select Item", true);
		int tileSize = exeDat.getConfig().getTileSize();
		int sourceItemW = (tileSize > 16) ? itemW * (tileSize / 16) : itemW;
		int sourceItemH = (tileSize > 16) ? itemH * (tileSize / 16) : itemH;
		BufferedImage displaySheet = itemSheet;
		int displayW = sourceItemW;
		int displayH = sourceItemH;
		if (tileSize == 16) {
			displayW = sourceItemW * 2;
			displayH = sourceItemH * 2;
			displaySheet = new BufferedImage(itemSheet.getWidth() * 2, itemSheet.getHeight() * 2, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = displaySheet.createGraphics();
			g.drawImage(itemSheet, 0, 0, itemSheet.getWidth() * 2, itemSheet.getHeight() * 2, null);
			g.dispose();
		}
		int cols = itemSheet.getWidth() / sourceItemW;
		final BufferedImage finalDisplaySheet = displaySheet;
		final int finalDisplayW = displayW;
		final int finalDisplayH = displayH;
		JPanel sheetPanel = new JPanel() {
			private static final long serialVersionUID = 1L;
			@Override public Dimension getPreferredSize() { return new Dimension(finalDisplaySheet.getWidth(), finalDisplaySheet.getHeight()); }
			@Override protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				g.drawImage(finalDisplaySheet, 0, 0, null);
				g.setColor(new Color(200, 200, 200, 120));
				int rows = finalDisplaySheet.getHeight() / finalDisplayH;
				for (int r = 0; r <= rows; r++) g.drawLine(0, r * finalDisplayH, finalDisplaySheet.getWidth(), r * finalDisplayH);
				for (int c = 0; c <= cols; c++) g.drawLine(c * finalDisplayW, 0, c * finalDisplayW, finalDisplaySheet.getHeight());
			}
		};
		sheetPanel.addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
				int newItem = (e.getY() / finalDisplayH) * cols + (e.getX() / finalDisplayW);
				updateArgInScript(paramOffset, paramLen, String.valueOf(newItem));
				picker.dispose();
			}
		});
		sheetPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		JScrollPane scroll = new JScrollPane(sheetPanel);
		int vbarWidth = scroll.getVerticalScrollBar().getPreferredSize().width;
		scroll.setPreferredSize(new Dimension(finalDisplaySheet.getWidth() + vbarWidth, Math.min(400, finalDisplaySheet.getHeight())));
		scroll.getVerticalScrollBar().setUnitIncrement(finalDisplayH * 2);
		scroll.getHorizontalScrollBar().setUnitIncrement(finalDisplayW * 2);
		picker.add(scroll);
		picker.pack();
		Point mousePos = MouseInfo.getPointerInfo().getLocation();
		picker.setLocation(mousePos.x - picker.getWidth(), mousePos.y);
		picker.setVisible(true);
	}

	private void showGraphicPicker(BufferedImage graphicSheet, int graphicSize, int paramOffset, int paramLen) {
		Window owner = SwingUtilities.getWindowAncestor(this);
		JDialog picker = new JDialog(owner instanceof Frame ? (Frame) owner : null, "Select Graphic", true);
		int tileSize = exeDat.getConfig().getTileSize();
		int sourceGraphicSize = (tileSize > 16) ? graphicSize * (tileSize / 16) : graphicSize;
		BufferedImage displaySheet = graphicSheet;
		int displaySize = sourceGraphicSize;
		if (tileSize == 16) {
			displaySize = sourceGraphicSize * 2;
			displaySheet = new BufferedImage(graphicSheet.getWidth() * 2, graphicSheet.getHeight() * 2, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = displaySheet.createGraphics();
			g.drawImage(graphicSheet, 0, 0, graphicSheet.getWidth() * 2, graphicSheet.getHeight() * 2, null);
			g.dispose();
		}
		int cols = graphicSheet.getWidth() / sourceGraphicSize;
		final BufferedImage finalDisplaySheet = displaySheet;
		final int finalDisplaySize = displaySize;
		JPanel sheetPanel = new JPanel() {
			private static final long serialVersionUID = 1L;
			@Override public Dimension getPreferredSize() { return new Dimension(finalDisplaySheet.getWidth(), finalDisplaySheet.getHeight()); }
			@Override protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				g.drawImage(finalDisplaySheet, 0, 0, null);
				g.setColor(new Color(200, 200, 200, 120));
				int rows = finalDisplaySheet.getHeight() / finalDisplaySize;
				for (int r = 0; r <= rows; r++) g.drawLine(0, r * finalDisplaySize, finalDisplaySheet.getWidth(), r * finalDisplaySize);
				for (int c = 0; c <= cols; c++) g.drawLine(c * finalDisplaySize, 0, c * finalDisplaySize, finalDisplaySheet.getHeight());
			}
		};
		sheetPanel.addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
				int newGraphic = (e.getY() / finalDisplaySize) * cols + (e.getX() / finalDisplaySize);
				updateArgInScript(paramOffset, paramLen, String.valueOf(newGraphic));
				picker.dispose();
			}
		});
		sheetPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		JScrollPane scroll = new JScrollPane(sheetPanel);
		int vbarWidth = scroll.getVerticalScrollBar().getPreferredSize().width;
		int hbarHeight = scroll.getHorizontalScrollBar().getPreferredSize().height;
		scroll.setPreferredSize(new Dimension(finalDisplaySheet.getWidth() + vbarWidth, Math.min(400, finalDisplaySheet.getHeight() + hbarHeight)));
		scroll.getVerticalScrollBar().setUnitIncrement(finalDisplaySize * 2);
		scroll.getHorizontalScrollBar().setUnitIncrement(finalDisplaySize * 2);
		picker.add(scroll);
		picker.pack();
		Point mousePos = MouseInfo.getPointerInfo().getLocation();
		picker.setLocation(mousePos.x - picker.getWidth(), mousePos.y);
		picker.setVisible(true);
	}

	private void showListPicker(String title, java.util.List<String> items, int selectedIndex, int paramOffset, int paramLen) {
		Window owner = SwingUtilities.getWindowAncestor(this);
		JDialog picker = new JDialog(owner instanceof Frame ? (Frame) owner : null, title, true);
		DefaultListModel<String> model = new DefaultListModel<>();
		for (String item : items) model.addElement(item);
		JList<String> list = new JList<>(model);
		list.setSelectedIndex(Math.max(0, Math.min(selectedIndex, items.size() - 1)));
		list.addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 1) {
					updateArgInScript(paramOffset, paramLen, String.valueOf(list.getSelectedIndex()));
					picker.dispose();
				}
			}
		});
		JScrollPane scroll = new JScrollPane(list);
		scroll.setPreferredSize(new Dimension(300, 400));
		picker.add(scroll);
		picker.pack();
		Point mousePos = MouseInfo.getPointerInfo().getLocation();
		picker.setLocation(mousePos.x - picker.getWidth(), mousePos.y);
		picker.setVisible(true);
	}

	private void showEquipPicker(java.util.List<String> items, int selectedIndex, int paramOffset, int paramLen) {
		Window owner = SwingUtilities.getWindowAncestor(this);
		JDialog picker = new JDialog(owner instanceof Frame ? (Frame) owner : null, "Select Equip", true);
		DefaultListModel<String> model = new DefaultListModel<>();
		for (String item : items) model.addElement(item);
		JList<String> list = new JList<>(model);
		list.setSelectedIndex(Math.max(0, Math.min(selectedIndex, items.size() - 1)));
		list.addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 1) {
					int idx = list.getSelectedIndex();
					int val = (idx == 0) ? 0 : (1 << (idx - 1));
					updateArgInScript(paramOffset, paramLen, String.valueOf(val));
					picker.dispose();
				}
			}
		});
		JScrollPane scroll = new JScrollPane(list);
		scroll.setPreferredSize(new Dimension(300, 400));
		picker.add(scroll);
		picker.pack();
		Point mousePos = MouseInfo.getPointerInfo().getLocation();
		picker.setLocation(mousePos.x - picker.getWidth(), mousePos.y);
		picker.setVisible(true);
	}

	private void addCommandExtra(char extraType, String arg, int paramOffset, int paramLen) {
		int argNum = StrTools.ascii2Num_CS(arg);
		JPanel jp = new JPanel();
		jp.add(new JLabel(extraNameMap.get(extraType) + ": "));
		if (extraType == 'g') {
			if (argNum > 1000) {
				extraType = 'i';
				argNum -= 1000;
			} else {
				extraType = 'a';
			}
		}

		final int fArgNum = argNum;

		switch (extraType) {
		case 'A': case 'd': case 'F': case 'l': case 'N':
		case 'x': case 'y': case '#': case '.': {
			JTextField tf = new JTextField(arg.trim(), 6);
			tf.addActionListener(ae -> updateArgInScript(paramOffset, paramLen, tf.getText()));
			jp.add(tf);
			break;
		}
		case '$': {
			String display = arg.endsWith("$") ? arg.substring(0, arg.length() - 1) : arg;
			JTextField tf = new JTextField(display, 10);
			tf.addActionListener(ae -> updateArgInScript(paramOffset, paramLen, tf.getText()));
			jp.add(tf);
			break;
		}
		case 'e': {
			JTextField tf = new JTextField(arg.trim(), 5);
			tf.addActionListener(ae -> updateArgInScript(paramOffset, paramLen, tf.getText()));
			jp.add(tf);
			jp.add(new LinkLabel(">", new EventLink(arg)));
			break;
		}
		case 'm': {
			try {
				Mapdata[] allMaps = exeDat.getMapdata();
				java.util.List<String> mapList = new ArrayList<>();
				for (int i = 0; i < allMaps.length; i++) {
					mapList.add(i + " - " + allMaps[i].getMapname());
				}
				JLabel mapLabel = new JLabel(mapList.get(Math.max(0, Math.min(fArgNum, allMaps.length - 1))));
				mapLabel.setForeground(new Color(255, 150, 215));
				mapLabel.setOpaque(true);
				mapLabel.setBackground(new Color(60, 60, 80));
				mapLabel.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createLineBorder(new Color(255, 150, 215), 1),
					BorderFactory.createEmptyBorder(2, 4, 2, 4)));
				mapLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				mapLabel.setToolTipText("Select Maps");
				mapLabel.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						showListPicker("Select Map", mapList, fArgNum, paramOffset, paramLen);
					}
				});
				jp.add(mapLabel);
			} catch (Exception e) {
				JTextField tf = new JTextField(arg.trim(), 4);
				tf.addActionListener(ae -> updateArgInScript(paramOffset, paramLen, tf.getText()));
				jp.add(tf);
			}
			break;
		}
		case 'n': {
			JPanel npcPanel = new JPanel();
			npcPanel.setLayout(new BoxLayout(npcPanel, BoxLayout.Y_AXIS));
			JTextField tf = new JTextField(arg.trim(), 4);
			tf.addActionListener(ae -> updateArgInScript(paramOffset, paramLen, tf.getText()));
			npcPanel.add(tf);
			try {
				JLabel npcName = new JLabel(exeDat.getAllEntities()[fArgNum].getName());
				npcName.setFont(npcName.getFont().deriveFont(10f));
				npcPanel.add(npcName);
			} catch (Exception ignored) {}
			jp.add(npcPanel);
			break;
		}
		case 'u': {
			java.util.List<String> formattedMusic = new ArrayList<>();
			for (int i = 0; i < musicList.size(); i++) formattedMusic.add(i + ": " + musicList.get(i).trim());
			JLabel musicLabel = new JLabel(formattedMusic.get(Math.max(0, Math.min(fArgNum, musicList.size() - 1))));
			musicLabel.setForeground(new Color(255, 150, 215));
			musicLabel.setOpaque(true);
			musicLabel.setBackground(new Color(60, 60, 80));
			musicLabel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(new Color(255, 150, 215), 1),
				BorderFactory.createEmptyBorder(2, 4, 2, 4)));
			musicLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			musicLabel.setToolTipText("Select Song");
			musicLabel.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					showListPicker("Select Song", formattedMusic, fArgNum, paramOffset, paramLen);
				}
			});
			jp.add(musicLabel);
			break;
		}
		case 's': {
			java.util.List<String> formattedSfx = new ArrayList<>();
			for (int i = 0; i < sfxList.size(); i++) formattedSfx.add(i + ": " + sfxList.get(i).trim());
			JLabel sfxLabel = new JLabel(formattedSfx.get(Math.max(0, Math.min(fArgNum, sfxList.size() - 1))));
			sfxLabel.setForeground(new Color(255, 150, 215));
			sfxLabel.setOpaque(true);
			sfxLabel.setBackground(new Color(60, 60, 80));
			sfxLabel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(new Color(255, 150, 215), 1),
				BorderFactory.createEmptyBorder(2, 4, 2, 4)));
			sfxLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			sfxLabel.setToolTipText("Select Sound Effect");
			sfxLabel.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					showListPicker("Select SFX", formattedSfx, fArgNum, paramOffset, paramLen);
				}
			});
			jp.add(sfxLabel);
			break;
		}
		case 'E': {
			if (equipList == null || equipList.isEmpty()) {
				JTextField tf = new JTextField(arg.trim(), 6);
				tf.addActionListener(ae -> updateArgInScript(paramOffset, paramLen, tf.getText()));
				jp.add(tf);
				break;
			}
			java.util.List<String> formattedEquip = new ArrayList<>();
			formattedEquip.add("0 - None");
			for (int i = 0; i < equipList.size() && i < 16; i++) {
				formattedEquip.add((1 << i) + " - " + equipList.get(i).trim());
			}
			int eSelIdx = 0;
			for (int i = 0; i < 16; i++) {
				if (fArgNum == (1 << i)) { eSelIdx = i + 1; break; }
			}
			final int equipSelIdx = eSelIdx;
			JLabel equipLabel = new JLabel(formattedEquip.get(eSelIdx));
			equipLabel.setForeground(new Color(255, 150, 215));
			equipLabel.setOpaque(true);
			equipLabel.setBackground(new Color(60, 60, 80));
			equipLabel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(new Color(255, 150, 215), 1),
				BorderFactory.createEmptyBorder(2, 4, 2, 4)));
			equipLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			equipLabel.setToolTipText("Select Equip");
			equipLabel.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					showEquipPicker(formattedEquip, equipSelIdx, paramOffset, paramLen);
				}
			});
			jp.add(equipLabel);
			break;
		}
		case 't': {
			JTextField tf = new JTextField(arg.trim(), 4);
			tf.addActionListener(ae -> updateArgInScript(paramOffset, paramLen, tf.getText()));
			jp.add(tf);
			Mapdata md = null;
			try {
				md = exeDat.getMapdata(mapNum);
			} catch(Exception e) {
				break;
			}
			BlConfig conf = exeDat.getConfig();
			BufferedImage tileImg = rm.getImg(new File(exeDat.getDataDirectory() + "/Stage/Prt" +  //$NON-NLS-1$
					md.getTileset() + exeDat.getImgExtension()));
			int setWidth = conf.getTilesetWidth();
			if (setWidth <= 0) setWidth = tileImg.getWidth() / conf.getTileSize();
			int srcScale = conf.getTileSize();
			int sourceX = (fArgNum % setWidth) * srcScale;
			int sourceY = (fArgNum / setWidth) * srcScale;
			try {
				JLabel label = new JLabel(new ImageIcon(tileImg.getSubimage(sourceX, sourceY, srcScale, srcScale)));
				label.setBackground(Color.black);
				jp.add(label);
			} catch (RasterFormatException ignored) {}
			break;
		}
		case 'a': {
			try {
				int tileSize = exeDat.getConfig().getTileSize();
				int graphicSize = (tileSize > 16) ? 16 * (tileSize / 16) : 16;
				BufferedImage armsSheet = rm.getImg(exeDat.getArmsImageFile());
				BufferedImage subImg = armsSheet.getSubimage(graphicSize * fArgNum, 0, graphicSize, graphicSize);
				if (tileSize == 16) {
					BufferedImage scaled = new BufferedImage(graphicSize * 2, graphicSize * 2, BufferedImage.TYPE_INT_ARGB);
					Graphics2D g = scaled.createGraphics();
					g.drawImage(subImg, 0, 0, graphicSize * 2, graphicSize * 2, null);
					g.dispose();
					subImg = scaled;
				}
				JPanel graphicPanel = new JPanel();
				graphicPanel.setLayout(new BoxLayout(graphicPanel, BoxLayout.Y_AXIS));
				JLabel graphicLabel = new JLabel(new ImageIcon(subImg));
				graphicLabel.setBackground(Color.black);
				graphicLabel.setOpaque(true);
				graphicLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
				graphicPanel.add(graphicLabel);
				JButton selectGraphicBtn = new JButton("Select");
				selectGraphicBtn.setFont(selectGraphicBtn.getFont().deriveFont(10f));
				selectGraphicBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
				selectGraphicBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				selectGraphicBtn.addActionListener(ae -> showGraphicPicker(armsSheet, 16, paramOffset, paramLen));
				graphicPanel.add(selectGraphicBtn);
				jp.add(graphicPanel);
			} catch (Exception ignored) {}
			break;
		}
		case 'f': {
			try {
				int faceNum = fArgNum % 1000;
				int tileSize = exeDat.getConfig().getTileSize();
				int faceSize = (tileSize > 16) ? 48 * (tileSize / 16) : 48;
				BufferedImage faceSheet = rm.getImg(exeDat.getFaceFile());
				BufferedImage subImg = faceSheet.getSubimage(
						faceSize * (faceNum % 6), faceSize * (faceNum / 6), faceSize, faceSize);
				if (tileSize == 16) {
					BufferedImage scaled = new BufferedImage(faceSize * 2, faceSize * 2, BufferedImage.TYPE_INT_ARGB);
					Graphics2D g = scaled.createGraphics();
					g.drawImage(subImg, 0, 0, faceSize * 2, faceSize * 2, null);
					g.dispose();
					subImg = scaled;
				}
				JPanel facePanel = new JPanel();
				facePanel.setLayout(new BoxLayout(facePanel, BoxLayout.Y_AXIS));
				JLabel faceLabel = new JLabel(new ImageIcon(subImg));
				faceLabel.setBackground(Color.black);
				faceLabel.setOpaque(true);
				faceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
				facePanel.add(faceLabel);
				JButton selectFaceBtn = new JButton("Select");
				selectFaceBtn.setFont(selectFaceBtn.getFont().deriveFont(10f));
				selectFaceBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
				selectFaceBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				selectFaceBtn.addActionListener(ae -> showFacePicker(faceSheet, 48, paramOffset, paramLen));
				facePanel.add(selectFaceBtn);
				jp.add(facePanel);
			} catch (Exception ignored) {}
			break;
		}
		case 'i': {
			try {
				int tileSize = exeDat.getConfig().getTileSize();
				int itemW = (tileSize > 16) ? 32 * (tileSize / 16) : 32;
				int itemH = (tileSize > 16) ? 16 * (tileSize / 16) : 16;
				BufferedImage itemSheet = rm.getImg(exeDat.getItemImageFile());
				BufferedImage subImg = itemSheet.getSubimage(
						itemW * (fArgNum % 8), itemH * (fArgNum / 8), itemW, itemH);
				if (tileSize == 16) {
					BufferedImage scaled = new BufferedImage(itemW * 2, itemH * 2, BufferedImage.TYPE_INT_ARGB);
					Graphics2D g = scaled.createGraphics();
					g.drawImage(subImg, 0, 0, itemW * 2, itemH * 2, null);
					g.dispose();
					subImg = scaled;
				}
				JPanel itemPanel = new JPanel();
				itemPanel.setLayout(new BoxLayout(itemPanel, BoxLayout.Y_AXIS));
				JLabel itemLabel = new JLabel(new ImageIcon(subImg));
				itemLabel.setBackground(Color.black);
				itemLabel.setOpaque(true);
				itemLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
				itemPanel.add(itemLabel);
				JButton selectItemBtn = new JButton("Select");
				selectItemBtn.setFont(selectItemBtn.getFont().deriveFont(10f));
				selectItemBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
				selectItemBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				selectItemBtn.addActionListener(ae -> showItemPicker(itemSheet, 32, 16, paramOffset, paramLen));
				itemPanel.add(selectItemBtn);
				jp.add(itemPanel);
			} catch (Exception ignored) {}
			break;
		}
		}
		commandListExtras.add(jp);
	}

	private class EventLink extends MouseAdapter {
		String eve;

		EventLink(String event) {
			eve = event;
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			String text = getText().replace("\r", "");
			int eventLoc = text.indexOf("#" + eve);
			try {
				TscPane.this.setCaretPosition(eventLoc);
				TscPane.this.setSelectionStart(eventLoc);
				TscPane.this.setSelectionEnd(eventLoc + 5);
			} catch (IllegalArgumentException ignored) {

			}
		}
	}

	private class MapLink extends MouseAdapter {
		int map;

		MapLink(int i) {
			map = i;
		}

		@Override
		public void mouseClicked(MouseEvent eve) {
			loadmap.loadMap(map);
		}
	}

	private JPanel createDefinePanel() {
		JPanel retVal = new JPanel(new GridLayout(0, 2));
		//retVal.setMinimumSize(new Dimension(160, 200));
		Iterator<String> keyIt = def1.iterator();
		Iterator<String> defIt = def2.iterator();
		retVal.add(new JLabel(Messages.getString("TscPane.13"))); //$NON-NLS-1$
		//retVal.add(new JLabel("->"));
		retVal.add(new JLabel(Messages.getString("TscPane.14"))); //$NON-NLS-1$
		//fieldVec = new Vector<JTextField>();
		int objNum = 2;
		while (keyIt.hasNext()) {
			String key = keyIt.next();
			JTextField keyField = new UpdateTextField(key);
			keyField.addActionListener(this);
			keyField.setActionCommand(SET_KEY);
			keyField.setName(String.valueOf(objNum));
			objNum++;
			JTextField valueField = new UpdateTextField(defIt.next());
			valueField.addActionListener(this);
			valueField.setActionCommand(SET_VALUE);
			valueField.setName(String.valueOf(objNum));
			objNum++;
			retVal.add(keyField);
			//retVal.add(new JLabel("->"));
			retVal.add(valueField);
		}
		JTextField lastKey = new UpdateTextField();
		lastKey.setActionCommand(SET_KEY);
		lastKey.setName(String.valueOf(objNum));
		lastKey.addActionListener(this);
		objNum++;
		retVal.add(lastKey);
		JTextField lastField = new UpdateTextField();
		lastField.setActionCommand(SET_VALUE);
		lastField.setEnabled(false);
		lastField.setName(String.valueOf(objNum));
		lastField.addActionListener(this);
		retVal.add(lastField);
		return retVal;
	}

	public static void initDefines(File dataDir) {
		//HashMap<String, String> retVal = new HashMap<String, String>();
		//load defines.txt
		def1 = new Vector<>();
		def2 = new Vector<>();
		File defFile = new File(dataDir + File.separator + "tsc_def.txt"); //$NON-NLS-1$
		try {
			if (!defFile.exists()) {
				defFile.createNewFile();
			}
			Scanner sc = new Scanner(defFile);
			while (sc.hasNextLine()) {
				String next = sc.nextLine();
				if (next.isEmpty()) {
					continue;
				}
				if (next.charAt(0) == '/') //if this is a comment line
				{
					continue;
				}
				int pos = 0;
				String firstWord = "", secondWord; //$NON-NLS-1$ //$NON-NLS-2$
				char nextChar = next.charAt(pos);
				while (nextChar != '=' && pos < next.length()) {
					firstWord += nextChar;
					pos++;
					nextChar = next.charAt(pos);
				}
				if (firstWord.isEmpty() || pos >= next.length()) {
					continue;
				}

				secondWord = next.substring(++pos);
				def1.add(firstWord);
				def2.add(secondWord);
				//retVal.put(firstWord, secondWord);
			}
			sc.close();
		} catch (IOException err) {
			err.printStackTrace();
		}
	}

	/* I decided these deserved their own window
	private JPanel createTutorialPanel() {
		JPanel retVal = new JPanel();
		return retVal;
	}
	*/

	private JPanel createCommandPanel(ResourceManager iMan) {
		JPanel retVal = new JPanel();
		retVal.setLayout(new BoxLayout(retVal, BoxLayout.Y_AXIS));
		//rightPanel.setPreferredSize(new Dimension(140, 400));
		//setup the right side even more
		Vector<String> listData = new Vector<>();
		for (TscCommand newCommand : commandInf) {
			listData.add(newCommand.commandCode + " - " + newCommand.name); //$NON-NLS-1$
		}
		Font f = new Font(Font.MONOSPACED, 0, 11);
		
		commandList = new BgList<>(listData, iMan.getImg(ResourceManager.rsrcBgWhite)); //$NON-NLS-1$
		ListMan lm = new ListMan();
		commandList.addListSelectionListener(lm);
		commandList.addMouseListener(lm);
		commandList.addKeyListener(new KeyListener() {

			@Override
			public void keyPressed(KeyEvent eve) {
				if (eve.getKeyCode() >= KeyEvent.VK_A &&
						eve.getKeyCode() <= KeyEvent.VK_Z) {
					char c = (char) eve.getKeyCode();
					int index = commandList.getNextMatch("<" + c, 0, Position.Bias.Forward); //$NON-NLS-1$
					if (index != -1) {
						commandList.setSelectedIndex(index);
						commandList.scrollRectToVisible(
								commandList.getCellBounds(index, index));
					}
				}
			}

			@Override
			public void keyReleased(KeyEvent arg0) {
			}

			@Override
			public void keyTyped(KeyEvent eve) {

			}

		});
		//commandList.setPreferredSize(new Dimension(100, 380));
		commandList.setFont(f);
		JScrollPane listScroll = new JScrollPane(commandList);
		listScroll.setPreferredSize(new Dimension(160, 375));
		comLabel.setLineWrap(true);
		comLabel.setWrapStyleWord(true);
		comLabel.setEditable(false);
		//comLabel.setPreferredSize(new Dimension(160, 80));
		comLabel.setFont(f);
		descLabel.setWrapStyleWord(true);
		descLabel.setLineWrap(true);
		descLabel.setEditable(false);
		//descLabel.setPreferredSize(new Dimension(160, 100));
		descLabel.setFont(f);
		JPanel labelHolder = new JPanel();
		labelHolder.setLayout(new BoxLayout(labelHolder, BoxLayout.Y_AXIS));
		labelHolder.add(comLabel);
		labelHolder.add(descLabel);
		JScrollPane labelScroll = new JScrollPane(labelHolder);
		labelScroll.setPreferredSize(new Dimension(160, 130));
		labelScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

		retVal.add(labelScroll);
		//command list extras, used to show stuff like facepics
		commandListExtras = new JPanel();
		commandListExtras.setLayout(new BoxLayout(commandListExtras, BoxLayout.Y_AXIS));
		retVal.add(commandListExtras);
		retVal.add(listScroll);
		//retVal.setMaximumSize(new Dimension(200, 2000));

		return retVal;
	}
	//tsc zoom
	private static int currentFontSize = 12;

	private void updateFontSize(int delta) {
		int newSize = Math.max(6, Math.min(40, currentFontSize + delta));
		if (newSize == currentFontSize) return;
		currentFontSize = newSize;
		for (SimpleAttributeSet style : styles.values()) {
			StyleConstants.setFontSize(style, newSize);
		}
		highlightDoc(this.getStyledDocument(), 0, -1);
	}

	private static Hashtable<String, SimpleAttributeSet> initStyles() {
		Hashtable<String, SimpleAttributeSet> retVal = new Hashtable<>();
		SimpleAttributeSet newStyle;
		String fontFamily = "Monospaced";

		//event numbers
		newStyle = new SimpleAttributeSet();
		StyleConstants.setFontFamily(newStyle, fontFamily); //$NON-NLS-1$
		StyleConstants.setFontSize(newStyle, 12);
		StyleConstants.setForeground(newStyle, new Color(255, 205, 120));
		StyleConstants.setBold(newStyle, true);
		StyleConstants.setItalic(newStyle, false);
		retVal.put(STYLE_EVENT, newStyle);
		//speech bubble
		newStyle = new SimpleAttributeSet();
		StyleConstants.setFontFamily(newStyle, fontFamily); //$NON-NLS-1$
		StyleConstants.setFontSize(newStyle, 12);
		StyleConstants.setForeground(newStyle, new Color(255, 200, 100));
		StyleConstants.setBold(newStyle, true);
		StyleConstants.setItalic(newStyle, false);
		retVal.put(STYLE_SBEVENT, newStyle);
		//tsc tags
		newStyle = new SimpleAttributeSet();
		StyleConstants.setFontFamily(newStyle, fontFamily); //$NON-NLS-1$
		StyleConstants.setFontSize(newStyle, 12);
		StyleConstants.setForeground(newStyle, new Color(105, 220, 250));
		StyleConstants.setBold(newStyle, false);
		StyleConstants.setItalic(newStyle, false);
		retVal.put(STYLE_TAG, newStyle);
		//numbers
		newStyle = new SimpleAttributeSet();
		StyleConstants.setFontFamily(newStyle, fontFamily); //$NON-NLS-1$
		StyleConstants.setFontSize(newStyle, 12);
		StyleConstants.setForeground(newStyle, new Color(255, 150, 215));
		StyleConstants.setBold(newStyle, false);
		StyleConstants.setItalic(newStyle, false);
		retVal.put(STYLE_NUM, newStyle);
		//flag numbers
		newStyle = new SimpleAttributeSet();
		StyleConstants.setFontFamily(newStyle, fontFamily);
		StyleConstants.setFontSize(newStyle, 12);
		StyleConstants.setForeground(newStyle, new Color(195, 170, 255));
		StyleConstants.setBold(newStyle, false);
		StyleConstants.setItalic(newStyle, false);
		retVal.put(STYLE_FLAG, newStyle);
		//number spacer
		newStyle = new SimpleAttributeSet();
		StyleConstants.setFontFamily(newStyle, fontFamily); //$NON-NLS-1$
		StyleConstants.setFontSize(newStyle, 12);
		StyleConstants.setForeground(newStyle, new Color(218, 188, 154));
		StyleConstants.setBold(newStyle, false);
		StyleConstants.setItalic(newStyle, false);
		retVal.put(STYLE_SPACER, newStyle);
		//text
		newStyle = new SimpleAttributeSet();
		StyleConstants.setFontFamily(newStyle, fontFamily); //$NON-NLS-1$
		StyleConstants.setFontSize(newStyle, 12);
		StyleConstants.setForeground(newStyle, new Color(250, 250, 250));
		StyleConstants.setBold(newStyle, false);
		StyleConstants.setItalic(newStyle, false);
		retVal.put(STYLE_TXT, newStyle);
		//speech bubble flags
		newStyle = new SimpleAttributeSet();
		StyleConstants.setFontFamily(newStyle, fontFamily); //$NON-NLS-1$
		StyleConstants.setFontSize(newStyle, 12);
		StyleConstants.setForeground(newStyle, new Color(255, 120, 120));
		StyleConstants.setBold(newStyle, false);
		StyleConstants.setItalic(newStyle, false);
		retVal.put(STYLE_SBFLAGS, newStyle);
		//overlimit text
		newStyle = new SimpleAttributeSet();
		StyleConstants.setFontFamily(newStyle, fontFamily); //$NON-NLS-1$
		StyleConstants.setFontSize(newStyle, 12);
		StyleConstants.setBackground(newStyle, new Color(70, 50, 50));
		StyleConstants.setForeground(newStyle, new Color(255, 60, 60));
		StyleConstants.setBold(newStyle, false);
		StyleConstants.setItalic(newStyle, false);
		retVal.put(STYLE_OVER, newStyle);
		//string params
		newStyle = new SimpleAttributeSet();
		StyleConstants.setFontFamily(newStyle, fontFamily);
		StyleConstants.setFontSize(newStyle, 12);
		StyleConstants.setForeground(newStyle, new Color(220, 190, 80));
		StyleConstants.setBold(newStyle, false);
		StyleConstants.setItalic(newStyle, false);
		retVal.put(STYLE_STRING, newStyle);
		//inaccessible commands
		newStyle = new SimpleAttributeSet();
		StyleConstants.setFontFamily(newStyle, fontFamily); //$NON-NLS-1$
		StyleConstants.setFontSize(newStyle, 12);
		StyleConstants.setForeground(newStyle, new Color(60, 160, 30));
		StyleConstants.setBold(newStyle, false);
		StyleConstants.setItalic(newStyle, true);
		retVal.put(STYLE_COMMENT, newStyle);
		//unrecognized commands
		newStyle = new SimpleAttributeSet();
		StyleConstants.setFontFamily(newStyle, fontFamily);
		StyleConstants.setFontSize(newStyle, 12);
		StyleConstants.setBackground(newStyle, new Color(70, 50, 50));
		StyleConstants.setForeground(newStyle, new Color(255, 60, 60));
		StyleConstants.setBold(newStyle, false);
		StyleConstants.setItalic(newStyle, false);
		retVal.put(STYLE_UNKNOWN, newStyle);

		return retVal;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		int index, nComponent;
		String ac = e.getActionCommand();
		Component src = (Component) e.getSource();
		if (ac.equals(SET_KEY)) {
			JTextField tf = (JTextField) src;
			String value = tf.getText();
			index = Integer.parseInt(src.getName());
			nComponent = defPanel.getComponentCount();
			if (index == nComponent - 2) {
				if (value.equals(""))  //$NON-NLS-1$
				{
					return;
				}
				def1.add(value);
				def2.add(""); //$NON-NLS-1$
				defPanel.getComponent(nComponent - 1).setEnabled(true);
				defPanel.getComponent(nComponent - 1).requestFocus();
				JTextField newKey = new UpdateTextField();
				newKey.setActionCommand(SET_KEY);
				newKey.setName(String.valueOf(nComponent));
				newKey.addActionListener(this);
				JTextField newVal = new UpdateTextField();
				newVal.setActionCommand(SET_VALUE);
				newVal.setEnabled(false);
				newVal.setName(String.valueOf(nComponent + 1));
				newVal.addActionListener(this);
				defPanel.add(newKey);
				defPanel.add(newVal);
				defPanel.getParent().validate();
				//System.out.println("setkey");
			} else {
				index = Integer.parseInt(src.getName());
				int keyIndex = index / 2 - 1;
				if (value.equals("")) { //$NON-NLS-1$
					//remove the row
					defPanel.remove(index);
					defPanel.remove(index);
					numerateComponents(defPanel);
					defPanel.getParent().validate();
					def1.remove(keyIndex);
					def2.remove(keyIndex);
				} else {
					def1.set(keyIndex, value);
				}
				
				//System.out.println("setkey notlast");
			}
		} else if (ac.equals(SET_VALUE)) {
			JTextField tf = (JTextField) src;
			String value = tf.getText();
			index = Integer.parseInt(src.getName());
			int keyIndex = index / 2 - 1;
			def2.set(keyIndex, value);
		}
	}

	private void numerateComponents(Container c) {
		Component[] children = c.getComponents();
		for (int i = 0; i < children.length; i++) {
			children[i].setName(String.valueOf(i));
		}
	}


	public static String parseScript(File scriptFile, String encoding) {
		FileChannel inChan;
		ByteBuffer dataBuf = null;
		int fileSize = 0;

		try {
			FileInputStream inFile = new FileInputStream(scriptFile);
			inChan = inFile.getChannel();
			fileSize = (int) inChan.size();
			dataBuf = ByteBuffer.allocate(fileSize);
			inChan.read(dataBuf);
			inChan.close();
			inFile.close();
		} catch (FileNotFoundException e) {
			System.err.println(Messages.getString("TscPane.31") + scriptFile); //$NON-NLS-1$
			//e.printStackTrace();
		} catch (IOException e) {
			StrTools.msgBox(Messages.getString("TscPane.32") + scriptFile); //$NON-NLS-1$
			//e.printStackTrace();
		}

		byte[] datArray = null;
		if (fileSize > 0) {
			int cypher = dataBuf.get(fileSize / 2);
			datArray = dataBuf.array();
			if (scriptFile.getName().endsWith(".tsc")) { //$NON-NLS-1$
				for (int i = 0; i < fileSize; i++) {
					if (i != fileSize / 2) {
						datArray[i] -= cypher;
					}
				}
			}
		}

		//now read the input as a text
		String finalText = Messages.getString("TscPane.34"); //$NON-NLS-1$
		if (datArray != null)
		//saveRawCharData(datArray, scriptFile);
		{
			try {
				finalText = new String(datArray, encoding);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return finalText;
	}

	public void save() {
		if (EditorApp.EDITOR_MODE == 2) {
			this.markUnchanged();
			return;
		}
		String text = this.getText();
		System.out.println(text);
		System.out.println("가");
		List<String> warnings = validateScript(text);
		if (!warnings.isEmpty()) {
			String fileName = scriptFile != null ? scriptFile.getName() : "?";
			StringBuilder msg = new StringBuilder();
			for (String w : warnings) {
				msg.append("Warning! ").append(fileName).append(": ").append(w).append("\n");
			}
			Toolkit.getDefaultToolkit().beep();
			JOptionPane pane = new JOptionPane(msg.toString(), JOptionPane.WARNING_MESSAGE,
					JOptionPane.YES_NO_OPTION, null, new Object[]{"Save Anyway", "Don't Save"}, "Don't Save");
			JDialog dialog = pane.createDialog(null, "Script Warnings");
			dialog.setModalityType(java.awt.Dialog.ModalityType.APPLICATION_MODAL);
			dialog.pack();
			Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
			dialog.setLocation((screen.width - dialog.getWidth()) / 2, (screen.height - dialog.getHeight()) / 2);
			dialog.setVisible(true);
			if (!"Save Anyway".equals(pane.getValue())) return;
		}
		try {
			//save source
			if (saveSource) {
				if (!srcFile.exists()) {
					srcFile.getParentFile().mkdirs();
					srcFile.createNewFile();
				}
				BufferedWriter out = new BufferedWriter(
						new OutputStreamWriter(new FileOutputStream(srcFile),
								exeDat.getConfig().getEncoding()));
				out.write(text);
				out.close();
			}

			//save script
			if (scriptFile.getName().endsWith(".txt")) {
				// save as normal txt
				BufferedWriter scriptOut = new BufferedWriter(
						new OutputStreamWriter(new FileOutputStream(scriptFile),
								exeDat.getConfig().getEncoding()));
				scriptOut.write(text);
				scriptOut.close();
			} else {
				//replace things
				for (int i = 0; i < def1.size(); i++) {
					text = text.replace(def1.get(i), def2.get(i));
				}
				//strip comments
				String strippedScript = ""; //$NON-NLS-1$
				TscLexer lex = new TscLexer();
				lex.reset(new StringReader(text), 0, -1, 0);
				TscToken t;
				int line = 0;
				while ((t = lex.getNextToken()) != null) {
					if (line != t.getLineNumber()) {
						strippedScript += "\r\n"; //$NON-NLS-1$
					}
					if (!t.getDescription().equals(STYLE_COMMENT)) {
						String content = t.getContents();
						if (t.getDescription().equals(STYLE_EVENT)) {
							strippedScript += content.substring(0,
									(content.length() >= 5) ? 5 : content.length());
						} else {
							strippedScript += content;
						}
					}
					line = t.getLineNumber();
				}
				byte[] stripArr = strippedScript.getBytes(exeDat.getConfig().getEncoding());
				int fileSize = stripArr.length;
				if (fileSize > 0) {
					int cypher = stripArr[fileSize / 2];
					if (scriptFile.getName().endsWith(".tsc")) { //$NON-NLS-1$
						for (int i = 0; i < fileSize; i++) {
							if (i != fileSize / 2) {
								stripArr[i] += cypher;
							}
						}
					}
				}
				FileOutputStream oStream = new FileOutputStream(scriptFile);
				FileChannel output = oStream.getChannel();
				ByteBuffer b = ByteBuffer.wrap(stripArr);
				output.write(b);
				output.close();
				oStream.close();
			}
		} catch (IOException err) {
			StrTools.msgBox("Error saving .TSC file (check read-only?)");
			err.printStackTrace();
		}
		markUnchanged();
		justSaved = true;
	}

	/**
	 * Saves a TSC file with the given contents, properly encrypted.
	 *
	 * @param text contents to write
	 * @param dest destination file
	 */
	public static void SaveTsc(String text, File dest) {
		try {
			//save script
			//replace things
			for (int i = 0; i < def1.size(); i++) {
				text = text.replace(def1.get(i), def2.get(i));
			}
			//strip comments
			String strippedScript = ""; //$NON-NLS-1$
			TscLexer lex = new TscLexer();
			lex.reset(new StringReader(text), 0, -1, 0);
			TscToken t;
			int line = 0;
			while ((t = lex.getNextToken()) != null) {
				if (line != t.getLineNumber()) {
					strippedScript += "\r\n"; //$NON-NLS-1$
				}
				if (!t.getDescription().equals(STYLE_COMMENT)) {
					String content = t.getContents();
					if (t.getDescription().equals(STYLE_EVENT)) {
						strippedScript += content.substring(0,
								(content.length() >= 5) ? 5 : content.length());
					} else {
						strippedScript += content;
					}
				}
				line = t.getLineNumber();
			}
			byte[] stripArr = strippedScript.getBytes();
			int fileSize = stripArr.length;
			if (fileSize > 0) {
				int cypher = stripArr[fileSize / 2];
				if (dest.getName().endsWith(".tsc")) { //$NON-NLS-1$
					for (int i = 0; i < fileSize; i++) {
						if (i != fileSize / 2) {
							stripArr[i] += cypher;
						}
					}
				}
			}
			FileOutputStream oStream = new FileOutputStream(dest);
			FileChannel output = oStream.getChannel();
			ByteBuffer b = ByteBuffer.wrap(stripArr);
			output.write(b);
			output.close();
			oStream.close();
		} catch (IOException err) {
			err.printStackTrace();
		}
	}

	private void highlightDoc(StyledDocument doc, int first, int last) {
		if (first < 0)
			first = 0;
		if (last < first)
			last = Integer.MAX_VALUE;
		TscLexer lexer = new TscLexer();
		try {
			lexer.reset(new StringReader(doc.getText(0, doc.getLength())), first, -1, 0);
			TscToken t;
			while ((t = lexer.getNextToken()) != null) {
				doc.setCharacterAttributes(t.getCharBegin(),
						t.getCharEnd() - t.getCharBegin(),
						styles.get(t.getDescription()), true);
				if (t.getLineNumber() > last) break;
			}
		} catch (BadLocationException | IOException e) {
			e.printStackTrace();
		}
	}

	private static Vector<TscCommand> getCommands() {
		boolean advanced = true;
		BufferedReader commandFile;
		StreamTokenizer tokenizer;
		Vector<TscCommand> retVal = new Vector<>();
		try {
			commandFile = new BufferedReader(new FileReader(getJarLocationFile("tsc_list.txt"))); //$NON-NLS-1$
			tokenizer = new StreamTokenizer(commandFile);
		} catch (FileNotFoundException e) {
			StrTools.msgBox(Messages.getString("TscPane.11")); //$NON-NLS-1$
			return retVal;
		}

		//search for the correct header
		tokenizer.wordChars(0x20, 0x7E);
		try {
			tokenizer.nextToken();
			while (tokenizer.ttype != StreamTokenizer.TT_EOF) {  //$NON-NLS-1$
				tokenizer.nextToken();
				if (tokenizer.sval != null) {
					if ("[BL_TSC]".equals(tokenizer.sval)) { //$NON-NLS-1$
						break;
					} else if ("[CE_TSC]".equals(tokenizer.sval)) //$NON-NLS-1$
						{
							advanced = false;
							break;
						}
					}
			}
			if (tokenizer.sval == null) {
				StrTools.msgBox(Messages.getString("TscPane.15")); //$NON-NLS-1$
				throw new IOException("tokenizer"); //$NON-NLS-1$
			}
			//read how many commands
			tokenizer.nextToken();
			int numCommand = (int) tokenizer.nval;

			tokenizer.resetSyntax();
			tokenizer.whitespaceChars(0, 0x20);
			tokenizer.wordChars(0x20, 0x7E);

			retVal = new Vector<>();
			for (int i = 0; i < numCommand; i++) {
				TscCommand newCommand = new TscCommand();
				//read command code
				tokenizer.nextToken();
				newCommand.commandCode = tokenizer.sval;
				//read how many parameters it has
				tokenizer.parseNumbers();
				tokenizer.nextToken();
				newCommand.numParam = (int) tokenizer.nval;
				tokenizer.resetSyntax();
				tokenizer.whitespaceChars(0, 0x20);
				tokenizer.wordChars(0x20, 0x7E);
				//read the CE parameters
				newCommand.CE_param = new char[6];
				tokenizer.nextToken();
				String paramStr = tokenizer.sval;
				for (int j = 0; j < 6; j++) {
					newCommand.CE_param[j] = j < paramStr.length() ? paramStr.charAt(j) : '-';
				}
				//read short name
				tokenizer.nextToken();
				newCommand.name = tokenizer.sval;
				//read description
				tokenizer.nextToken();
				newCommand.description = tokenizer.sval;
				if (advanced) {
					tokenizer.parseNumbers();
					//read end event
					tokenizer.nextToken();
					newCommand.endsEvent = tokenizer.nval > 0;
					// read clear msgbox
					tokenizer.nextToken();
					newCommand.clearsMsg = tokenizer.nval > 0;
					// read parameter seperator
					tokenizer.nextToken();
					newCommand.paramSep = tokenizer.nval > 0;
					// read parameter length
					int[] paramLen = new int[6];
					for (int j = 0; j < newCommand.numParam; j++) {
						tokenizer.nextToken();
						paramLen[j] = (int) tokenizer.nval;
					}
					newCommand.paramLen = paramLen;
				} else {
					if (newCommand.commandCode.equals("<END") ||  //$NON-NLS-1$
							newCommand.commandCode.equals("<TRA") ||  //$NON-NLS-1$
							newCommand.commandCode.equals("<EVE") ||  //$NON-NLS-1$
							newCommand.commandCode.equals("<LDP") ||  //$NON-NLS-1$
							newCommand.commandCode.equals("<INI") || //$NON-NLS-1$
							newCommand.commandCode.equals("<ESC")) //$NON-NLS-1$
					{
						newCommand.endsEvent = true;
					}
					if (newCommand.commandCode.equals("<MSG") ||  //$NON-NLS-1$
							newCommand.commandCode.equals("<MS2") ||  //$NON-NLS-1$
							newCommand.commandCode.equals("<MS3") ||  //$NON-NLS-1$
							newCommand.commandCode.equals("<CLR"))  //$NON-NLS-1$
					{
						newCommand.clearsMsg = true;
					}
					newCommand.paramSep = true;
					int[] paramLen = new int[] {4, 4, 4, 4, 4, 4};
					for (int j = 0; j < newCommand.numParam; j++) {
						if (newCommand.CE_param[j] == '$') {
							paramLen[j] = -1;
						}
					}
					newCommand.paramLen = paramLen;
				}
				tokenizer.resetSyntax();
				tokenizer.whitespaceChars(0, 0x20);
				tokenizer.wordChars(0x20, 0x7E);

				retVal.add(newCommand);
				//result.add(newCommand.commandCode + " - " + newCommand.name);
			}
			TscLexer.initMap(retVal);
			commandFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return retVal;
	}

	private static ArrayList<String> getMusicList() {
		ArrayList<String> rv = new ArrayList<>();
		try {
			Scanner sc = new Scanner(getJarLocationFile("musiclist.txt"));
			while (sc.hasNext()) {
				sc.nextInt();
				String sfxName = sc.nextLine();
				rv.add(sfxName);
			}
			sc.close();
		} catch (FileNotFoundException err) {
			StrTools.msgBox("Could not find musiclist.txt");
		}
		return rv;
	}

	private static ArrayList<String> getSfxList() {
		ArrayList<String> rv = new ArrayList<>();
		try {
			Scanner sc = new Scanner(getJarLocationFile("sfxList.txt"));
			while (sc.hasNext()) {
				sc.nextInt();
				sc.next(); //disregard hyphen
				String sfxName = sc.nextLine();
				rv.add(sfxName);
			}
			sc.close();
		} catch (FileNotFoundException err) {
			StrTools.msgBox("Could not find sfxList.txt");
		}
		return rv;
	}

	private static ArrayList<String> getEquipList() {
		ArrayList<String> rv = new ArrayList<>();
		try {
			Scanner sc = new Scanner(getJarLocationFile("equipList.txt"));
			while (sc.hasNext()) {
				String sfxName = sc.nextLine();
				rv.add(sfxName);
			}
			sc.close();
		} catch (FileNotFoundException err) {
			StrTools.msgBox("Could not find equipList.txt");
		}
		return rv;
	}

	private static Set<String> loadEventEnders() {
		Set<String> rv = new HashSet<>();
		File f = getJarLocationFile("endlist.txt");
		if (!f.exists()) {
			rv.add("<END"); rv.add("<EVE"); rv.add("<TRA");
			try (java.io.PrintWriter pw = new java.io.PrintWriter(f)) {
				pw.println("<END"); pw.println("<EVE"); pw.println("<TRA");
			} catch (IOException ignored) {}
			return rv;
		}
		try (Scanner sc = new Scanner(f)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine().trim();
				if (!line.isEmpty()) rv.add(line);
			}
		} catch (IOException ignored) {}
		return rv;
	}

	private List<String> validateScript(String text) {
		List<String> warnings = new ArrayList<>();
		Set<String> knownCmds = new HashSet<>();
		if (commandInf != null) {
			for (TscCommand tc : commandInf) knownCmds.add(tc.commandCode);
		}
		String[] lines = text.replace("\r", "").split("\n", -1);
		String currentEvent = null;
		boolean ended = false, hasCmd = false;
		String unknownCmd = null;
		for (String rawLine : lines) {
			String line = rawLine.trim();
			if (line.isEmpty()) continue;
			if (line.charAt(0) == '#') {
				if (currentEvent != null) {
					if (hasCmd && !ended)
						warnings.add("Event #" + currentEvent + " doesn't end!");
					if (unknownCmd != null)
						warnings.add("Event #" + currentEvent + " '" + unknownCmd + "' is not a recognized command!");
				}
				currentEvent = line.length() >= 5 ? line.substring(1, 5) : line.substring(1);
				ended = false; hasCmd = false; unknownCmd = null;
				continue;
			}
			if (currentEvent == null || ended) continue;
			if (line.startsWith("//")) continue;
			int i = 0;
			while (i < line.length()) {
				if (i + 1 < line.length() && line.charAt(i) == '/' && line.charAt(i + 1) == '/') break;
				if (line.charAt(i) == '<' && i + 4 <= line.length()) {
					String cmd = line.substring(i, i + 4);
					hasCmd = true;
					if (knownCmds.contains(cmd)) {
						if (eventEnders.contains(cmd)) ended = true;
					} else if (unknownCmd == null) {
						unknownCmd = cmd;
					}
					i += 4;
				} else {
					i++;
				}
			}
		}
		if (currentEvent != null) {
			if (hasCmd && !ended) warnings.add("Event #" + currentEvent + " doesn't end!");
			if (unknownCmd != null) warnings.add("Event #" + currentEvent + " '" + unknownCmd + "' is not a recognized command!");
		}
		return warnings;
	}

	private static File getJarLocationFile(String filename) {
		try {
			String jarPath = TscPane.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
			File jarFile = new File(jarPath);
			File jarDir = jarFile.isDirectory() ? jarFile : jarFile.getParentFile();
			return new File(jarDir, filename);
		} catch (Exception e) {
			return new File(filename);
		}
	}

	protected void textBoxKeyReleased(KeyEvent evt) {
		JTextPane area = (JTextPane) evt.getSource();
		if (evt.getKeyCode() == KeyEvent.VK_CONTROL
				|| evt.getKeyCode() == KeyEvent.VK_SHIFT
				|| evt.getKeyCode() == KeyEvent.VK_ALT) {
			return;//do not mark changed if we just used the save shortcut
		}
		if (evt.getKeyCode() == KeyEvent.VK_S) {
			if (justSaved) {
				justSaved = false;
				return;
			}
		}
		int cPos = area.getCaretPosition();
		//calculate line #
		Scanner sc;
		try {
			sc = new Scanner(area.getText(0, area.getDocument().getLength()));
		} catch (BadLocationException e1) {
			e1.printStackTrace();
			return;
		}
		int character = 0, startLine = 0;
		//String oldLine;
		String newLine; //$NON-NLS-1$
		while (character < cPos) {
			//oldLine = newLine;
			newLine = sc.nextLine();
			character += newLine.length() + 1;
			startLine++;
			if (!sc.hasNextLine()) {
				break;
			}
		}
		sc.close();
		//colour it
		highlightDoc((StyledDocument) area.getDocument(), startLine - 50, startLine + 50);
		markChanged();
	}

	public void insertStringAtCursor(String s) {
		//insert command
		try {
			int cPos = this.getCaretPosition();
			this.getDocument().insertString(cPos, s, null);

			//rehilight
			//calculate line #
			Scanner sc;
			try {
				sc = new Scanner(this.getText(0, this.getDocument().getLength()));
			} catch (BadLocationException e1) {
				e1.printStackTrace();
				return;
			}
			int character = 0, startLine = 0;
			//String oldLine;
			String newLine; //$NON-NLS-1$
			while (character < cPos) {
				//oldLine = newLine;
				newLine = sc.nextLine();
				character += newLine.length() + 1;
				startLine++;
				if (!sc.hasNextLine()) {
					break;
				}
			}
			sc.close();
			//colour it
			highlightDoc(this.getStyledDocument(), startLine - 50, startLine + 50);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	class ListMan extends MouseAdapter implements ListSelectionListener {

		@Override
		public void mouseClicked(MouseEvent eve) {
			if (eve.getClickCount() == 2) {
				@SuppressWarnings("unchecked")
				JList<String> src = (JList<String>) eve.getSource();
				int selection = src.getSelectedIndex();
				if (selection != -1) {
					TscCommand selCom;
					if (commandInf != null && commandInf.size() != 0) {
						selCom = commandInf.elementAt(selection);
					} else {
						return;
					}
					String comStr = selCom.commandCode;
					char[] paramLetters = {'W', 'X', 'Y', 'Z', 'Q', 'R'};
					for (int i = 0; i < selCom.numParam; i++) {
						for (int j = 0; j < selCom.paramLen[i]; j++) {
							comStr += paramLetters[i];
						}
						if (i != selCom.numParam - 1 && selCom.paramSep) {
							comStr += ':';
						}
					}
					lastFocus.insertStringAtCursor(comStr);
				}
			}
		}

		public void valueChanged(ListSelectionEvent e) {
			if (!e.getValueIsAdjusting()) {
				@SuppressWarnings("unchecked")
				JList<String> src = (JList<String>) e.getSource();
				int selection = src.getSelectedIndex();
				if (selection != -1) {
					TscCommand selCom;
					if (commandInf != null && commandInf.size() != 0) {
						selCom = commandInf.elementAt(selection);
					} else {
						return;
					}
					String comStr = Messages.getString(
							"TscPane.43") + selCom.name + "\n" + selCom.commandCode; //$NON-NLS-1$ //$NON-NLS-2$
					char[] paramLetters = {'W', 'X', 'Y', 'Z', 'Q', 'R'};
					for (int i = 0; i < selCom.numParam; i++) {
						for (int j = 0; j < selCom.paramLen[i]; j++) {
							comStr += paramLetters[i];
						}

						if (i != selCom.numParam - 1 && selCom.paramSep) {
							comStr += ':';
						}
					}
					comLabel.setText(comStr);
					descLabel.setText(Messages.getString("TscPane.45") + selCom.description); //$NON-NLS-1$
				}
			}
		}
	}

	@Override
	public boolean isModified() {
		return changed;
	}

	@Override
	public void markUnchanged() {
		if (changed) {
			changed = false;
			this.firePropertyChange(PROPERTY_EDITED, true, false);
		}
	}

	@Override
	public void markChanged() {
		if (!changed) {
			changed = true;
			this.firePropertyChange(PROPERTY_EDITED, false, true);
		}
	}
}
