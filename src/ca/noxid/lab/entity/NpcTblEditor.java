package ca.noxid.lab.entity;

import ca.noxid.lab.EditorApp;
import ca.noxid.lab.Messages;
import ca.noxid.lab.gameinfo.GameInfo;
import ca.noxid.lab.rsrc.ResourceManager;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class NpcTblEditor extends JDialog implements ActionListener {

	private static final long serialVersionUID = -8397275955244640295L;
	private JList<EntityData> entList;
	private ArrayList<EntityData> dataCopy;
	private ArrayList<EntityData> filteredData;
	private EntityData currentEnt = null;
	private NpcTblClipboardData copiedAttributes = null;
	private GameInfo exeData;
	private JTextField searchField;
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
	private JCheckBox faceRightCheckbox = new JCheckBox("Flip sprite");
	
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
	private JComboBox<String> sizeList = new JComboBox<>();
	private JComboBox<String> hurtList = new JComboBox<>();
	private JComboBox<String> deathList = new JComboBox<>();
	private JComboBox<String> tilesetList = new JComboBox<>();
	private JButton pasteButton;
	
	private NpcPreviewPane previewPane;
	private JComboBox<String> npcSheetSelector;
	private EditorApp parentApp;
	private boolean isPopulating = false;

	public NpcTblEditor(Frame aFrame) {
		super(aFrame);
		this.parentApp = (EditorApp) aFrame;
		if (EditorApp.blazed)
			this.setCursor(ResourceManager.cursor);
		this.setDefaultCloseOperation(HIDE_ON_CLOSE);
		this.addComponents();
		this.setTitle(Messages.getString("NpcTblEditor.49")); //$NON-NLS-1$
		this.setModal(true);
	}
	
	private void addComponents() {
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
		
		JPanel leftPanel = new JPanel(new GridBagLayout());
		GridBagConstraints lc = new GridBagConstraints();
		lc.gridx = 0;
		lc.gridy = 0;
		lc.fill = GridBagConstraints.HORIZONTAL;
		lc.weightx = 1.0;
		
		searchField = new JTextField();
		searchField.setForeground(Color.GRAY);
		searchField.setText("Search");
		searchField.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				if (searchField.getText().equals("Search")) {
					searchField.setText("");
					searchField.setForeground(Color.WHITE);
				}
			}
			@Override
			public void focusLost(FocusEvent e) {
				if (searchField.getText().isEmpty()) {
					searchField.setForeground(Color.GRAY);
					searchField.setText("Search");
				}
			}
		});
		searchField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) { filterList(); }
			@Override
			public void removeUpdate(DocumentEvent e) { filterList(); }
			@Override
			public void changedUpdate(DocumentEvent e) { filterList(); }
		});
		lc.insets = new Insets(2, 2, 2, 2);
		leftPanel.add(searchField, lc);
		
		lc.gridy++;
		lc.fill = GridBagConstraints.BOTH;
		lc.weighty = 1.0;
		entList = new JList<>();
		entList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent eve) {
				if (!isPopulating && entList.getSelectedValue() != null) {
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
		jsp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		leftPanel.add(jsp, lc);

		lc.gridy++;
		lc.fill = GridBagConstraints.HORIZONTAL;
		lc.weighty = 0;
		JButton addButton = new JButton(new AbstractAction() {
			private static final long serialVersionUID = 320754668181434348L;
			@Override
			public void actionPerformed(ActionEvent eve) {
				CreateEntityDialog d = new CreateEntityDialog(parentApp);
				if (d.ent != null) {
					d.ent.setID(dataCopy.size());
					dataCopy.add(d.ent);
					filterList();
					entList.setSelectedValue(d.ent, true);
				}
			}
		});
		addButton.setText("Add New");
		leftPanel.add(addButton, lc);
		
		lc.gridy++;
		JButton deleteButton = new JButton(new AbstractAction() {
			private static final long serialVersionUID = 320754668181434348L;
			@Override
			public void actionPerformed(ActionEvent eve) {
				deleteSelectedEntities();
			}
		});
		deleteButton.setText("Delete Selected");
		leftPanel.add(deleteButton, lc);
		
		mainPanel.add(leftPanel);
		
		JPanel rightPanel = new JPanel(new GridBagLayout());
		mainPanel.add(rightPanel);
		
		//set up main panel
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 0.5;
		c.weighty = 0.5;
		JPanel pane;
		
		pane = buildMainEditorPane();
		c.gridwidth = 1;
		c.gridheight = 1;
		rightPanel.add(pane, c);
		
		c.gridx = 1;
		c.gridheight = 1;
		pane = buildPreviewAndSpritePane();
		rightPanel.add(pane, c);
		
		c.gridx = 0;
		c.gridy = 3;
		c.gridwidth = 2;
		c.insets = new Insets(2, 2, 2, 2);
		pane = buildFlagsPane();
		rightPanel.add(pane, c);		
		
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.EAST;
		c.weightx = 0;
		c.insets = new Insets(2, 2, 2, 2);
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 2, 0));
		
		JButton copyButton = new JButton(new AbstractAction() {
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
		copyButton.setText("Copy");
		buttonPanel.add(copyButton);
		
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
		buttonPanel.add(pasteButton);
		
		JButton saveButton = new JButton(new AbstractAction() {
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
		saveButton.setText(Messages.getString("NpcTblEditor.50")); //$NON-NLS-1$
		buttonPanel.add(saveButton);
		
		JButton cancelButton = new JButton(new AbstractAction() {
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
		cancelButton.setText(Messages.getString("NpcTblEditor.51")); //$NON-NLS-1$
		buttonPanel.add(cancelButton);
		
		rightPanel.add(buttonPanel, c);
		
		this.setContentPane(mainPanel);
		this.pack();
		this.setMinimumSize(this.getSize());
	}
	
	
private JPanel buildMainEditorPane() {
		JPanel retVal = new JPanel();
		retVal.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(2, 2, 2, 2);
		
		retVal.add(new JLabel("Name"), c);
		c.gridx = 1;
		c.gridwidth = 2;
		c.weightx = 1.0;
		retVal.add(nameLabel, c);
		
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.weightx = 0;
		retVal.add(new JLabel("Short Name"), c);
		c.gridx = 1;
		c.weightx = 0.5;
		short1Label.setPreferredSize(new Dimension(70, short1Label.getPreferredSize().height));
		retVal.add(short1Label, c);
		c.gridx = 2;
		short2Label.setPreferredSize(new Dimension(70, short2Label.getPreferredSize().height));
		retVal.add(short2Label, c);
		
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.weightx = 0;
		c.anchor = GridBagConstraints.NORTHWEST;
		retVal.add(new JLabel("Description"), c);
		c.gridx = 1;
		c.gridwidth = 2;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.BOTH;
		JScrollPane descScroll = new JScrollPane(descArea);
		descScroll.setPreferredSize(new Dimension(150, 60));
		retVal.add(descScroll, c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.WEST;
		
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.weightx = 0;
		retVal.add(new JLabel("Tileset"), c);
		c.gridx = 1;
		c.gridwidth = 2;
		c.weightx = 1.0;
		retVal.add(tilesetList, c);
		
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.weightx = 0;
		retVal.add(new JLabel("Hurt SFX"), c);
		c.gridx = 1;
		c.gridwidth = 2;
		c.weightx = 1.0;
		retVal.add(hurtList, c);
		
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.weightx = 0;
		retVal.add(new JLabel("Death SFX"), c);
		c.gridx = 1;
		c.gridwidth = 2;
		c.weightx = 1.0;
		retVal.add(deathList, c);
		
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.weightx = 0;
		retVal.add(new JLabel("Health"), c);
		c.gridx = 1;
		c.weightx = 0.33;
		hpField.setPreferredSize(new Dimension(50, hpField.getPreferredSize().height));
		retVal.add(hpField, c);
		c.gridx = 0;
		c.gridy++;
		c.weightx = 0;
		retVal.add(new JLabel("Experience"), c);
		c.gridx = 1;
		c.weightx = 0.33;
		xpField.setPreferredSize(new Dimension(50, xpField.getPreferredSize().height));
		retVal.add(xpField, c);
		c.gridx = 0;
		c.gridy++;
		c.weightx = 0;
		retVal.add(new JLabel("Damage"), c);
		c.gridx = 1;
		c.weightx = 0.33;
		dmgField.setPreferredSize(new Dimension(50, dmgField.getPreferredSize().height));
		retVal.add(dmgField, c);
		
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 3;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.BOTH;
		JPanel hitboxPane = new JPanel();
		hitboxPane.setBorder(BorderFactory.createTitledBorder("Hitbox"));
		hitboxPane.setLayout(new GridLayout(3, 3));
		hitboxPane.add(new JPanel());
		hitboxU.addActionListener(e -> updatePreview());
		hitboxPane.add(hitboxU);
		hitboxPane.add(new JPanel());
		hitboxL.addActionListener(e -> updatePreview());
		hitboxPane.add(hitboxL);
		JLabel hitboxCenter = new JLabel("Npc Center", SwingConstants.CENTER);
		hitboxPane.add(hitboxCenter);
		hitboxR.addActionListener(e -> updatePreview());
		hitboxPane.add(hitboxR);
		hitboxPane.add(new JPanel());
		hitboxD.addActionListener(e -> updatePreview());
		hitboxPane.add(hitboxD);
		hitboxPane.add(new JPanel());
		retVal.add(hitboxPane, c);
		
		c.gridy++;
		JPanel displayPane = new JPanel();
		displayPane.setBorder(BorderFactory.createTitledBorder("Display"));
		displayPane.setLayout(new GridLayout(3, 3));
		displayPane.add(new JPanel());
		spriteOffY.addActionListener(e -> updatePreview());
		displayPane.add(spriteOffY);
		displayPane.add(new JPanel());
		spriteOffX.addActionListener(e -> updatePreview());
		displayPane.add(spriteOffX);
		JLabel displayCenter = new JLabel("Hi", SwingConstants.CENTER);
		displayPane.add(displayCenter);
		widthField.addActionListener(e -> updatePreview());
		displayPane.add(widthField);
		displayPane.add(new JPanel());
		faceRightCheckbox.addActionListener(e -> updatePreview());
		displayPane.add(faceRightCheckbox);
		displayPane.add(new JPanel());
		retVal.add(displayPane, c);
		
		return retVal;
	}
	
	private JPanel buildPreviewAndSpritePane() {
		JPanel retVal = new JPanel();
		retVal.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 1.0;
		
		JPanel previewContainer = new JPanel();
		previewContainer.setBorder(BorderFactory.createTitledBorder("Preview"));
		previewContainer.setLayout(new BorderLayout());
		previewPane = new NpcPreviewPane(parentApp.getImageManager());
		previewPane.setScale(3.0);
		previewPane.setPreferredSize(new Dimension(200, 200));
		previewContainer.add(previewPane, BorderLayout.CENTER);
		retVal.add(previewContainer, c);
		
		c.gridy++;
		c.weighty = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		JPanel spriteSelectPanel = new JPanel(new GridBagLayout());
		GridBagConstraints sc = new GridBagConstraints();
		sc.gridx = 0;
		sc.gridy = 0;
		sc.anchor = GridBagConstraints.WEST;
		sc.insets = new Insets(2, 2, 2, 2);
		spriteSelectPanel.add(new JLabel("Sprite"), sc);
		sc.gridx = 1;
		sc.weightx = 1.0;
		sc.fill = GridBagConstraints.HORIZONTAL;
		npcSheetSelector = new JComboBox<>();
		npcSheetSelector.addActionListener(e -> loadNpcSheet());
		spriteSelectPanel.add(npcSheetSelector, sc);
		retVal.add(spriteSelectPanel, c);
		
		c.gridy++;
		c.fill = GridBagConstraints.BOTH;
		JPanel spriteLocPane = new JPanel();
		spriteLocPane.setBorder(BorderFactory.createTitledBorder("Sprite Location"));
		spriteLocPane.setLayout(new GridBagLayout());
		GridBagConstraints slc = new GridBagConstraints();
		slc.fill = GridBagConstraints.BOTH;
		slc.weightx = 1.0;
		slc.weighty = 1.0;
		slc.insets = new Insets(2, 2, 2, 2);
		
		slc.gridx = 0;
		slc.gridy = 0;
		spriteLocPane.add(new JPanel(), slc);
		slc.gridx = 1;
		JPanel topPanel = new JPanel(new BorderLayout(1, 0));
		topPanel.add(new JLabel("T"), BorderLayout.WEST);
		spriteTop.addActionListener(e -> updatePreview());
		topPanel.add(spriteTop, BorderLayout.CENTER);
		spriteLocPane.add(topPanel, slc);
		slc.gridx = 2;
		spriteLocPane.add(new JPanel(), slc);
		
		slc.gridx = 0;
		slc.gridy = 1;
		JPanel leftPanel = new JPanel(new BorderLayout(1, 0));
		leftPanel.add(new JLabel("L"), BorderLayout.WEST);
		spriteLeft.addActionListener(e -> updatePreview());
		leftPanel.add(spriteLeft, BorderLayout.CENTER);
		spriteLocPane.add(leftPanel, slc);
		slc.gridx = 1;
		spriteLocPane.add(new JPanel(), slc);
		slc.gridx = 2;
		JPanel rightPanel = new JPanel(new BorderLayout(1, 0));
		spriteRight.addActionListener(e -> updatePreview());
		rightPanel.add(spriteRight, BorderLayout.CENTER);
		rightPanel.add(new JLabel("R"), BorderLayout.EAST);
		spriteLocPane.add(rightPanel, slc);
		
		slc.gridx = 0;
		slc.gridy = 2;
		spriteLocPane.add(new JPanel(), slc);
		slc.gridx = 1;
		JPanel bottomPanel = new JPanel(new BorderLayout(1, 0));
		spriteBottom.addActionListener(e -> updatePreview());
		bottomPanel.add(spriteBottom, BorderLayout.CENTER);
		bottomPanel.add(new JLabel("B"), BorderLayout.EAST);
		spriteLocPane.add(bottomPanel, slc);
		slc.gridx = 2;
		spriteLocPane.add(new JPanel(), slc);
		
		retVal.add(spriteLocPane, c);
		return retVal;
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
	
public void populate(GameInfo inf) {
		isPopulating = true;
		//init list of entities
		copiedAttributes = null;
		if (pasteButton != null) {
			pasteButton.setEnabled(false);
		}
		dataCopy = new ArrayList<>();
		for (int i = 0; i < inf.getAllEntities().length; i++) {
			dataCopy.add(new EntityData(inf.getAllEntities()[i]));
		}
		filteredData = new ArrayList<>(dataCopy);
		entList.setListData(filteredData.toArray(new EntityData[filteredData.size()]));
		exeData = inf;
		
		sizeList.removeAllItems();
		for (String s : GameInfo.NpcSizeNames) {
			sizeList.addItem(s);
		}
		hurtList.removeAllItems();
		for (String s : GameInfo.sfxNames) {
			hurtList.addItem(s);
		}
		deathList.removeAllItems();
		for (String s : GameInfo.sfxNames) {
			deathList.addItem(s);
		}
		tilesetList.removeAllItems();
		for (String s : GameInfo.NpcSurfaceNames) {
			tilesetList.addItem(s);
		}
		
		if (npcSheetSelector != null) {
			npcSheetSelector.removeAllItems();
			String[] sheets = inf.getNpcSheets();
			for (String sheet : sheets) {
				npcSheetSelector.addItem(sheet);
			}
		}
		
		if (previewPane != null) {
			int entityRes = inf.getConfig().getEntityRes();
			previewPane.setEntityResolution(entityRes);
			previewPane.setScale(3.0);
		}
		
		if (!filteredData.isEmpty()) {
			entList.setSelectedIndex(0);
			setEntity(filteredData.get(0));
		}
		isPopulating = false;
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
		faceRightCheckbox.setSelected(false);
		
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
		if (ent.getSize() >= 0 && ent.getSize() < sizeList.getItemCount()) {
			sizeList.setSelectedIndex(ent.getSize());
		}
		if (ent.getHurt() >= 0 && ent.getHurt() < hurtList.getItemCount()) {
			hurtList.setSelectedIndex(ent.getHurt());
		}
		if (ent.getDeath() >= 0 && ent.getDeath() < deathList.getItemCount()) {
			deathList.setSelectedIndex(ent.getDeath());
		}
		if (ent.getTileset() >= 0 && ent.getTileset() < tilesetList.getItemCount()) {
			tilesetList.setSelectedIndex(ent.getTileset());
		}
		
		updatePreview();
	}
	
	private void updatePreview() {
		if (currentEnt == null || previewPane == null) return;
		try {
			Rectangle hr = new Rectangle(
				Integer.parseInt(hitboxL.getText()),
				Integer.parseInt(hitboxU.getText()),
				Integer.parseInt(hitboxR.getText()),
				Integer.parseInt(hitboxD.getText())
			);
			Rectangle dr = new Rectangle(
				Integer.parseInt(spriteOffX.getText()),
				Integer.parseInt(spriteOffY.getText()),
				Integer.parseInt(widthField.getText()),
				0
			);
			Rectangle fr = new Rectangle(
				Integer.parseInt(spriteLeft.getText()),
				Integer.parseInt(spriteTop.getText()),
				Integer.parseInt(spriteRight.getText()),
				Integer.parseInt(spriteBottom.getText())
			);
			currentEnt.setHit(hr);
			currentEnt.setDisplay(dr);
			currentEnt.setFramerect(fr);
			previewPane.setEntity(currentEnt);
			previewPane.setFaceRight(faceRightCheckbox.isSelected());
		} catch (NumberFormatException ignored) {}
	}
	
	private void loadNpcSheet() {
		if (exeData == null || npcSheetSelector == null) return;
		String sheetName = (String) npcSheetSelector.getSelectedItem();
		if (sheetName == null) return;
		ResourceManager iMan = parentApp.getImageManager();
		java.io.File npcFile = new java.io.File(exeData.getDataDirectory() + "/Npc/" + sheetName + exeData.getImgExtension());
		if (npcFile.exists()) {
			iMan.addImage(npcFile, 1);
			previewPane.setNpcSheet(iMan.getImg(npcFile));
		}
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
		filterList();
	}

	private void filterList() {
		if (dataCopy == null) return;
		String query = searchField.getText();
		if (query.equals("Search") || query.isEmpty()) {
			filteredData = new ArrayList<>(dataCopy);
		} else {
			filteredData = new ArrayList<>();
			String lowerQuery = query.toLowerCase();
			for (EntityData ent : dataCopy) {
				if (String.valueOf(ent.getID()).contains(lowerQuery) ||
					ent.getName().toLowerCase().contains(lowerQuery) ||
					ent.getShort1().toLowerCase().contains(lowerQuery) ||
					ent.getShort2().toLowerCase().contains(lowerQuery)) {
					filteredData.add(ent);
				}
			}
		}
		entList.setListData(filteredData.toArray(new EntityData[filteredData.size()]));
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
		dr.height = 0;
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
