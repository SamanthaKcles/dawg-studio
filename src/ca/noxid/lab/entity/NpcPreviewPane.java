package ca.noxid.lab.entity;

import ca.noxid.lab.rsrc.ResourceManager;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class NpcPreviewPane extends JPanel {
	private EntityData currentEntity;
	private ResourceManager iMan;
	private BufferedImage npcSheet;
	private double scale = 2.0;
	private int entityResolution;
	private boolean faceRight = false;
	private Color gridColor1 = new Color(0x939393);
	private Color gridColor2 = new Color(0xb1b1b1);
	private int gridWidth = 64;
	private int gridHeight = 64;

	public NpcPreviewPane(ResourceManager iMan) {
		this.iMan = iMan;
		this.setPreferredSize(new Dimension(200, 200));
		this.setBackground(Color.BLACK);
	}

	public void setNpcSheet(BufferedImage img) {
		this.npcSheet = img;
		repaint();
	}
	
	public void setScale(double scale) {
		this.scale = scale;
		repaint();
	}
	
	public void setEntityResolution(int resolution) {
		this.entityResolution = resolution;
		repaint();
	}

	public void setEntity(EntityData ent) {
		this.currentEntity = ent;
		repaint();
	}
	
	public void setFaceRight(boolean faceRight) {
		this.faceRight = faceRight;
		repaint();
	}
	
	public void setGridColors(Color c1, Color c2) {
		this.gridColor1 = c1;
		this.gridColor2 = c2;
		repaint();
	}
	
	public void setGridSize(int width, int height) {
		this.gridWidth = width;
		this.gridHeight = height;
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		Graphics2D g2d = (Graphics2D) g;
		
		// A GRID BRO YUM !
		for (int y = 0; y < getHeight(); y += gridHeight) {
			for (int x = 0; x < getWidth(); x += gridWidth) {
				g2d.setColor(((x / gridWidth + y / gridHeight) % 2 == 0) ? gridColor1 : gridColor2);
				g2d.fillRect(x, y, gridWidth, gridHeight);
			}
		}
		
		if (currentEntity == null) return;
		int centerX = getWidth() / 2;
		int centerY = getHeight() / 2;

		Rectangle frameRect = currentEntity.getFramerect();
		BufferedImage srcImg = npcSheet;

		if (srcImg != null) {
			double divi = 1.0;
			double spriteScale = scale;
			if (entityResolution == 32) {
				divi = 0.5;
				spriteScale = scale * 0.5;
			}
			
			int srcX = (int)(frameRect.x / divi);
			int srcY = (int)(frameRect.y / divi);
			int srcW = (int)(frameRect.width / divi) - srcX;
			int srcH = (int)(frameRect.height / divi) - srcY;
			
			Rectangle display = currentEntity.getDisplay();
			int offsetX = faceRight ? display.width : display.x;
			int drawX = (int)(centerX - offsetX * scale);
			int drawY = (int)(centerY - display.y * scale);
			int drawW = (int)(srcW * spriteScale);
			int drawH = (int)(srcH * spriteScale);
			
			if (faceRight) {
				g2d.drawImage(srcImg, drawX + drawW, drawY, drawX, drawY + drawH,
						srcX, srcY, (int)(frameRect.width / divi), (int)(frameRect.height / divi), null);
			} else {
				g2d.drawImage(srcImg, drawX, drawY, drawX + drawW, drawY + drawH,
						srcX, srcY, (int)(frameRect.width / divi), (int)(frameRect.height / divi), null);
			}
		}

		g2d.setColor(Color.YELLOW);
		Rectangle display = currentEntity.getDisplay();
		int offsetX = faceRight ? display.width : display.x;
		int dispX = (int)(centerX - offsetX * scale);
		int dispY = (int)(centerY - display.y * scale);
		int dispW = (int)((display.width + display.x) * scale);
		int dispH = (int)((display.height + display.y) * scale);
		g2d.drawRect(dispX, dispY, dispW, dispH);

		g2d.setColor(Color.RED);
		Rectangle hit = currentEntity.getHit();
		int hitX = (int)(centerX - hit.x * scale);
		int hitY = (int)(centerY - hit.y * scale);
		int hitW = (int)((hit.width + hit.x) * scale);
		int hitH = (int)((hit.height + hit.y) * scale);
		g2d.drawRect(hitX, hitY, hitW, hitH);
	}
}
