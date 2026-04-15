package ca.noxid.lab.entity;

import ca.noxid.lab.EditorApp;
import ca.noxid.lab.Messages;
import ca.noxid.lab.gameinfo.GameInfo;
import ca.noxid.lab.rsrc.ResourceManager;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class NpcTblEditor extends JDialog implements ActionListener {

	private static final long serialVersionUID = -8397275955244640295L;
	private JList<EntityData> entList;
	private ArrayList<EntityData> dataCopy;
	private EntityData currentEnt = null;
	private NpcTblClipboardData copiedAttributes = null;
	private GameInfo exeData;
	//button for close, save
	
	//info box
	private JLabel numLabel = new JLabel("####"); //$NON-NLS-1$
	private JTextField nameLabel = new JTextField("~~~~~~~~"); //$NON-NLS-1$
	private JTextField short1Label = new JTextField("~~~~"); //$NON-NLS-1$
	private JTextField short2Label = new JTextField("~~~~"); //$NON-NLS-1$
	private JTextArea descArea = new JTextArea("TEXTTEXTTEXTTEXTTEXT"); //$NON-NLS-1$
	
	//hitbox thing
	private JTextField hitboxL = new JTextField();
	private JTextField hitboxU = new JTextField();
	private JTextField hitboxR = new JTextField();
	private JTextField hitboxD = new JTextField();
	
	//display box
	private JTextField spriteOffX = new JTextField();
	private JTextField spriteOffY = new JTextField();
	private JTextField widthField = new JTextField();
	private JTextField heightField = new JTextField();
	
	//sprite rect
	private JTextField spriteLeft = new JTextField();
	private JTextField spriteTop = new JTextField();
	private JTextField spriteRight = new JTextField();
	private JTextField spriteBottom = new JTextField();
	
	//flags
	private JCheckBox[] flagCheckArray = new JCheckBox[EntityData.flagNames.length];
	
	//stats
	private JTextField hpField = new JTextField(6);
	private JTextField xpField = new JTextField(6);
	private JTextField dmgField = new JTextField(6);
	private JComboBox<String> sizeList = new JComboBox<>(GameInfo.NpcSizeNames);
	private JComboBox<String> hurtList = new JComboBox<>(GameInfo.sfxNames);
	private JComboBox<String> deathList = new JComboBox<>(GameInfo.sfxNames);
	private JComboBox<String> tilesetList = new JComboBox<>(GameInfo.NpcSurfaceNames);
	private JButton pasteButton;
	
	//that box panel
	//private JPanel previewPane = new JPanel();

	public NpcTblEditor(Frame aFrame) {
		super(aFrame);
		if (EditorApp.blazed)
			this.setCursor(ResourceManager.cursor);
		this.setDefaultCloseOperation(HIDE_ON_CLOSE);
		this.addComponents();
		this.setTitle(Messages.getString("NpcTblEditor.49")); //$NON-NLS-1$
		this.setModal(true);
	}
	
	private void addComponents() {
		GridBagConstraints c;
		entList = new JList<>();
		//entList.setMaximumSize(new Dimension(100, 3333));
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
		//c.fill = GridBagConstraints.BOTH;
		//c.weightx = 0.3;
		//c.weighty = 1.0;
		entList.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent eve) {
				if (entList.getSelectedValue() != null) {
					persistChanges();
					setEntity(entList.getSelectedValue());
				}
			}
			
		});
		entList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		entList.getInputMap(JComponent.WHEN_FOCUSED).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteSelected");
		entList.getActionMap().put("deleteSelected", new AbstractAction() {
			private static final long serialVersionUID = 7629384756102938475L;
			@Override
			public void actionPerformed(ActionEvent e) {
				deleteSelectedEntities();
			}
		});
		JScrollPane jsp = new JScrollPane(entList);
		jsp.setMinimumSize(new Dimension(130, 120));
		jsp.setPreferredSize(new Dimension(130, 120));
		jsp.setMaximumSize(new Dimension(130, 9999));
		jsp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		mainPanel.add(jsp);
		//c.gridx++;
		//c.weightx = 0.5;
		//c.gridwidth = 5;
		JPanel rightPanel = new JPanel(new GridBagLayout());
		//rightPanel.setBorder(BorderFactory.createTitledBorder("This is a title"));
		mainPanel.add(rightPanel);
		
		//set up main panel
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 0.5;
		c.weighty = 0.5;
		JPanel pane;
		
		pane = buildInfoPane();
		c.gridwidth = GridBagConstraints.REMAINDER;
		rightPanel.add(pane, c);
		
		pane = buildHitboxPane();
		c.gridy++;
		c.gridwidth = 2;
		rightPanel.add(pane, c);
		
		pane = buildDisplayPane();
		c.gridx += 2;
		rightPanel.add(pane, c);
		
		pane = buildSpritePane();
		c.gridx += 2;
		rightPanel.add(pane, c);
		
		pane = buildStatsPane();
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		rightPanel.add(pane, c);
		
		/*
		pane = previewPane;
		pane.setBorder(new LineBorder(Color.red));
		//c.gridy++;
		c.gridx += 3;
		c.gridwidth = 1;
		rightPanel.add(pane, c);
		*/
		
		pane = buildFlagsPane();
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		rightPanel.add(pane, c);		
		
		//TODO add buttons for save, close, cancel
		//save&close, cancel? probs.
		c.gridy++;
		c.gridx = 2;
		c.fill = 0;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.EAST;
		JButton button;
		button = new JButton(new AbstractAction() {
			private static final long serialVersionUID = 8192459460809148679L;

			@Override
			public void actionPerformed(ActionEvent eve) {
				int result = JOptionPane.showConfirmDialog(NpcTblEditor.this,
						"Are you sure you want to save?", "Warning!",
						JOptionPane.YES_NO_OPTION);
				if (result != JOptionPane.YES_OPTION) return;
				persistChanges();
				exeData.setEntities(dataCopy); 
				exeData.saveNpcTbl();
				setVisible(false);
			}
		});
		button.setText(Messages.getString("NpcTblEditor.50")); //$NON-NLS-1$
		rightPanel.add(button);
		
		c.gridx++;
		button = new JButton(new AbstractAction() {
			private static final long serialVersionUID = 1371948856887331870L;

			@Override
			public void actionPerformed(ActionEvent eve) {
				persistChanges();
				if (currentEnt != null) {
					copiedAttributes = new NpcTblClipboardData(currentEnt);
					String data = copiedAttributes.serialize();
					Toolkit.getDefaultToolkit().getSystemClipboard()
							.setContents(new StringSelection(data), null);
					if (pasteButton != null) {
						pasteButton.setEnabled(true);
					}
				}
			}
		});
		button.setText("Copy");
		rightPanel.add(button);

		c.gridx++;
		pasteButton = new JButton(new AbstractAction() {
			private static final long serialVersionUID = -4576863892371145178L;

			@Override
			public void actionPerformed(ActionEvent eve) {
				if (currentEnt == null) return;
				try {
					String clip = (String) Toolkit.getDefaultToolkit()
							.getSystemClipboard().getData(DataFlavor.stringFlavor);
					NpcTblClipboardData parsed = NpcTblClipboardData.deserialize(clip);
					if (parsed != null) {
						copiedAttributes = parsed;
					}
				} catch (Exception ignored) {
				}
				if (copiedAttributes == null) return;
				copiedAttributes.applyTo(currentEnt);
				setEntity(currentEnt);
			}
		});
		pasteButton.setText("Paste");
		pasteButton.setEnabled(false);
		rightPanel.add(pasteButton);

		c.gridx++;
		button = new JButton(new AbstractAction() {
			private static final long serialVersionUID = 320754668181434348L;
			
			@Override
			public void actionPerformed(ActionEvent eve) {
				int result = JOptionPane.showConfirmDialog(NpcTblEditor.this,
						"Are you sure you want to close without saving?", "Warning!",
						JOptionPane.YES_NO_OPTION);
				if (result != JOptionPane.YES_OPTION) return;
				setVisible(false);
			}
		});
		button.setText(Messages.getString("NpcTblEditor.51")); //$NON-NLS-1$
		rightPanel.add(button);
		
		c.gridx++;
		button = new JButton(new AbstractAction() {
			private static final long serialVersionUID = 320754668181434348L;
			
			@Override
			public void actionPerformed(ActionEvent eve) {
				deleteSelectedEntities();
			}
		});
		button.setText("Delete Selected");
		rightPanel.add(button);
		
		c.gridx++;
		button = new JButton(new AbstractAction() {
			private static final long serialVersionUID = 320754668181434348L;
			
			@Override
			public void actionPerformed(ActionEvent eve) {
				CreateEntityDialog d = new CreateEntityDialog((Frame) NpcTblEditor.this.getParent());
				if (d.ent != null) {
					d.ent.setID(dataCopy.size());
					dataCopy.add(d.ent);
					entList.setListData(dataCopy.toArray(new EntityData[dataCopy.size()]));
					entList.setSelectedValue(d.ent, true);
				}
			}
		});
		button.setText("Add New");
		rightPanel.add(button);
		
		this.setContentPane(mainPanel);
		this.pack();
		this.setMinimumSize(this.getSize());
	}
	
	private JPanel buildInfoPane() {

		JPanel retVal = new JPanel();
		retVal.setBorder(BorderFactory.createTitledBorder(Messages.getString("NpcTblEditor.0"))); //$NON-NLS-1$
		retVal.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		retVal.add(new JLabel(Messages.getString("NpcTblEditor.54")), c); //$NON-NLS-1$
		c.gridy++;
		retVal.add(new JLabel(Messages.getString("NpcTblEditor.55")), c); //$NON-NLS-1$
		descArea.setEditable(true);
		c.gridy++;
		c.gridwidth = 4;
		retVal.add(descArea, c);
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridx++;
		retVal.add(this.numLabel, c);
		c.gridy++;
		retVal.add(this.nameLabel, c);
		c.gridx++;
		c.gridy = 0;
		retVal.add(new JLabel(Messages.getString("NpcTblEditor.56")), c); //$NON-NLS-1$
		c.gridy++;
		retVal.add(new JLabel(Messages.getString("NpcTblEditor.57")), c); //$NON-NLS-1$
		c.gridy = 0;
		c.gridx++;
		retVal.add(this.short1Label, c);
		c.gridy++;
		retVal.add(this.short2Label, c);
		return retVal;
	}
	
	private JPanel buildHitboxPane() {
		JPanel retVal = new JPanel();
		retVal.setBorder(BorderFactory.createTitledBorder(Messages.getString("NpcTblEditor.58"))); //$NON-NLS-1$
		retVal.setLayout(new GridLayout(3, 3));
		retVal.add(new JPanel());
		retVal.add(this.hitboxU);
		retVal.add(new JPanel());
		retVal.add(this.hitboxL);
		retVal.add(new JPanel());
		retVal.add(this.hitboxR);
		retVal.add(new JPanel());
		retVal.add(this.hitboxD);
		return retVal;
	}
	
	private JPanel buildDisplayPane() {
		JPanel retVal = new JPanel();
		retVal.setBorder(BorderFactory.createTitledBorder(Messages.getString("NpcTblEditor.59"))); //$NON-NLS-1$

		retVal.setLayout(new GridLayout(2, 0, 4, 2));
		retVal.add(new JLabel(Messages.getString("NpcTblEditor.60"))); //$NON-NLS-1$
		retVal.add(this.spriteOffY);
		retVal.add(new JLabel(Messages.getString("NpcTblEditor.61"))); //$NON-NLS-1$
		retVal.add(this.spriteOffX);
		retVal.add(new JLabel(Messages.getString("NpcTblEditor.62"))); //$NON-NLS-1$
		retVal.add(this.widthField);
		retVal.add(new JLabel(Messages.getString("NpcTblEditor.63"))); //$NON-NLS-1$
		retVal.add(this.heightField);
		return retVal;
	}
	
	private JPanel buildSpritePane() {
		JPanel retVal = new JPanel();
		retVal.setBorder(BorderFactory.createTitledBorder("Sprite Location"));
		retVal.setLayout(new GridLayout(4, 2, 4, 2));
		retVal.add(new JLabel("Left"));
		retVal.add(this.spriteLeft);
		retVal.add(new JLabel("Top"));
		retVal.add(this.spriteTop);
		retVal.add(new JLabel("Right"));
		retVal.add(this.spriteRight);
		retVal.add(new JLabel("Bottom"));
		retVal.add(this.spriteBottom);
		return retVal;
	}
	
	private JPanel buildFlagsPane() {
		JPanel retVal = new JPanel();
		retVal.setBorder(BorderFactory.createTitledBorder(Messages.getString("NpcTblEditor.64"))); //$NON-NLS-1$
		retVal.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		for (int i = 0; i < flagCheckArray.length; i++) {
			flagCheckArray[i] = new JCheckBox(EntityData.flagNames[i]);
			flagCheckArray[i].addActionListener(this);
			retVal.add(flagCheckArray[i], c);
			c.gridx++;
			if (c.gridx > 3) {
				c.gridx = 0;
				c.gridy++;
			}
		}
		return retVal;
	}
	
	private JPanel buildStatsPane() {
		JPanel retVal = new JPanel();
		retVal.setBorder(BorderFactory.createTitledBorder(Messages.getString("NpcTblEditor.65"))); //$NON-NLS-1$
		retVal.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(2, 1, 2, 1);
		ArrayList<Component> comps = new ArrayList<>();
		comps.add(new JLabel(Messages.getString("NpcTblEditor.66"))); //$NON-NLS-1$
		comps.add(this.hpField);
		comps.add(new JLabel(Messages.getString("NpcTblEditor.67"))); //$NON-NLS-1$
		comps.add(this.tilesetList);
		comps.add(new JLabel(Messages.getString("NpcTblEditor.68"))); //$NON-NLS-1$
		comps.add(this.xpField);
		comps.add(new JLabel(Messages.getString("NpcTblEditor.69"))); //$NON-NLS-1$
		comps.add(this.sizeList);
		comps.add(new JLabel(Messages.getString("NpcTblEditor.70"))); //$NON-NLS-1$
		comps.add(this.dmgField);
		comps.add(new JLabel(Messages.getString("NpcTblEditor.71"))); //$NON-NLS-1$
		comps.add(this.hurtList);
		comps.add(new JPanel());
		comps.add(new JPanel());
		comps.add(new JLabel(Messages.getString("NpcTblEditor.72"))); //$NON-NLS-1$
		comps.add(this.deathList);
		
		for (Component com : comps) {
			retVal.add(com, c);
			c.gridx++;
			if (c.gridx > 3) {
				c.gridx = 0;
				c.gridy++;
			}
		}
		return retVal;
	}
	
	public void populate(GameInfo inf) {
		//init list of entities
		copiedAttributes = null;
		if (pasteButton != null) {
			pasteButton.setEnabled(false);
		}
		dataCopy = new ArrayList<>();
		for (int i = 0; i < inf.getAllEntities().length; i++) {
			dataCopy.add(new EntityData(inf.getAllEntities()[i]));
		}
		entList.setListData(dataCopy.toArray(new EntityData[dataCopy.size()]));
		entList.setSelectedIndex(0);
		exeData = inf;
	}
	
	private void setEntity(EntityData ent) {
		currentEnt = ent;
		//info
		numLabel.setText(String.valueOf(ent.getID()));
		nameLabel.setText(ent.getName());
		short1Label.setText(ent.getShort1());
		short2Label.setText(ent.getShort2());
		descArea.setText(ent.getDesc());
		//TODO set up category tree
		//noxid this idea is poop sorry
		
		
		//hitbox
		Rectangle hitRect = ent.getHit();
		hitboxL.setText(String.valueOf(hitRect.x));
		hitboxU.setText(String.valueOf(hitRect.y));
		hitboxR.setText(String.valueOf(hitRect.width));
		hitboxD.setText(String.valueOf(hitRect.height));
		
		//displayBox
		Rectangle dispRect = ent.getDisplay();
		spriteOffX.setText(String.valueOf(dispRect.x));
		spriteOffY.setText(String.valueOf(dispRect.y));
		widthField.setText(String.valueOf(dispRect.width));
		heightField.setText(String.valueOf(dispRect.height));
		
		Rectangle frameRect = ent.getFramerect();
		spriteLeft.setText(String.valueOf(frameRect.x / 2));
		spriteTop.setText(String.valueOf(frameRect.y / 2));
		spriteRight.setText(String.valueOf(frameRect.width / 2));
		spriteBottom.setText(String.valueOf(frameRect.height / 2));
		
		//flags
		int flags = ent.getFlags();
		for (int i = 0; i < flagCheckArray.length; i++) {
			if ((flags & 1 << i) != 0) {
				flagCheckArray[i].setSelected(true);
			} else {
				flagCheckArray[i].setSelected(false);
			}
		}
		
		//stats
		hpField.setText(String.valueOf(ent.getHP()));
		xpField.setText(String.valueOf(ent.getXP()));
		dmgField.setText(String.valueOf(ent.getDmg()));
		sizeList.setSelectedIndex(ent.getSize());
		hurtList.setSelectedIndex(ent.getHurt());
		deathList.setSelectedIndex(ent.getDeath());
		tilesetList.setSelectedIndex(ent.getTileset());
	}
	
	@Override
	public void actionPerformed(ActionEvent eve) {
		Object src = eve.getSource();
		for (int i = 0; i < flagCheckArray.length; i++) {
			if (src == flagCheckArray[i]) {
				int flag = currentEnt.getFlags();
				flag ^= 1 << i;
				currentEnt.setFlags(flag);
			}
		}
	}
	
	private void deleteSelectedEntities() {
		List<EntityData> selVal = entList.getSelectedValuesList();
		if (selVal.isEmpty()) return;
		dataCopy.removeAll(selVal);
		int i = 0;
		for (EntityData e : dataCopy) {
			e.setID(i);
			i++;
		}
		currentEnt = null;
		entList.setListData(dataCopy.toArray(new EntityData[dataCopy.size()]));
	}

	private void persistChanges() {
		if (currentEnt == null) return;
		Rectangle hr = currentEnt.getHit();
		Rectangle dr = currentEnt.getDisplay();

		hr.x = Integer.parseInt(hitboxL.getText());
		currentEnt.setHit(hr);
		hr.y = Integer.parseInt(hitboxU.getText());
		currentEnt.setHit(hr);
		hr.width = Integer.parseInt(hitboxR.getText());
		currentEnt.setHit(hr);	
		hr.height = Integer.parseInt(hitboxD.getText());
		currentEnt.setHit(hr);
		dr.x = Integer.parseInt(spriteOffX.getText());
		currentEnt.setDisplay(dr);
		dr.y = Integer.parseInt(spriteOffY.getText());
		currentEnt.setDisplay(dr);
		dr.width = Integer.parseInt(widthField.getText());
		currentEnt.setDisplay(dr);
		dr.height = Integer.parseInt(heightField.getText());
		currentEnt.setDisplay(dr);
		Rectangle fr = currentEnt.getFramerect();
		fr.x = Integer.parseInt(spriteLeft.getText()) * 2;
		fr.y = Integer.parseInt(spriteTop.getText()) * 2;
		fr.width = Integer.parseInt(spriteRight.getText()) * 2;
		fr.height = Integer.parseInt(spriteBottom.getText()) * 2;
		currentEnt.setFramerect(fr);
		currentEnt.setName(nameLabel.getText());
		currentEnt.setShort1(short1Label.getText());
		currentEnt.setShort2(short2Label.getText());
		currentEnt.setDesc(descArea.getText());
		int hp = Integer.parseInt(hpField.getText());
		currentEnt.setHP(hp);
		int xp = Integer.parseInt(xpField.getText());
		currentEnt.setXP(xp);
		int dmg = Integer.parseInt(dmgField.getText());
		currentEnt.setDmg(dmg);
		currentEnt.setSize(sizeList.getSelectedIndex());
		currentEnt.setHurt(hurtList.getSelectedIndex());
		currentEnt.setDeath(deathList.getSelectedIndex());
		currentEnt.setTileset(tilesetList.getSelectedIndex());
	}

	private static class NpcTblClipboardData {
		private static final String PREFIX = "BLNPC|";
		private final Rectangle hitbox;
		private final Rectangle display;
		private final int flags;
		private final int hp;
		private final int xp;
		private final int dmg;
		private final int size;
		private final int hurt;
		private final int death;
		private final int tileset;

		private NpcTblClipboardData(EntityData ent) {
			hitbox = ent.getHit();
			display = ent.getDisplay();
			flags = ent.getFlags();
			hp = ent.getHP();
			xp = ent.getXP();
			dmg = ent.getDmg();
			size = ent.getSize();
			hurt = ent.getHurt();
			death = ent.getDeath();
			tileset = ent.getTileset();
		}

		private NpcTblClipboardData(Rectangle hitbox, Rectangle display, int flags,
				int hp, int xp, int dmg, int size, int hurt, int death, int tileset) {
			this.hitbox = hitbox;
			this.display = display;
			this.flags = flags;
			this.hp = hp;
			this.xp = xp;
			this.dmg = dmg;
			this.size = size;
			this.hurt = hurt;
			this.death = death;
			this.tileset = tileset;
		}

		private String serialize() {
			return PREFIX +
					hitbox.x + "," + hitbox.y + "," + hitbox.width + "," + hitbox.height + "|" +
					display.x + "," + display.y + "," + display.width + "," + display.height + "|" +
					flags + "|" + hp + "|" + xp + "|" + dmg + "|" +
					size + "|" + hurt + "|" + death + "|" + tileset;
		}

		private static NpcTblClipboardData deserialize(String s) {
			if (s == null || !s.startsWith(PREFIX)) return null;
			try {
				String[] parts = s.substring(PREFIX.length()).split("\\|");
				if (parts.length != 10) return null;
				String[] h = parts[0].split(",");
				String[] d = parts[1].split(",");
				if (h.length != 4 || d.length != 4) return null;
				return new NpcTblClipboardData(
						new Rectangle(Integer.parseInt(h[0]), Integer.parseInt(h[1]),
								Integer.parseInt(h[2]), Integer.parseInt(h[3])),
						new Rectangle(Integer.parseInt(d[0]), Integer.parseInt(d[1]),
								Integer.parseInt(d[2]), Integer.parseInt(d[3])),
						Integer.parseInt(parts[2]), Integer.parseInt(parts[3]),
						Integer.parseInt(parts[4]), Integer.parseInt(parts[5]),
						Integer.parseInt(parts[6]), Integer.parseInt(parts[7]),
						Integer.parseInt(parts[8]), Integer.parseInt(parts[9]));
			} catch (NumberFormatException e) {
				return null;
			}
		}

		private void applyTo(EntityData ent) {
			ent.setHit(new Rectangle(hitbox));
			ent.setDisplay(new Rectangle(display));
			ent.setFlags(flags);
			ent.setHP(hp);
			ent.setXP(xp);
			ent.setDmg(dmg);
			ent.setSize(size);
			ent.setHurt(hurt);
			ent.setDeath(death);
			ent.setTileset(tileset);
		}
	}
}
