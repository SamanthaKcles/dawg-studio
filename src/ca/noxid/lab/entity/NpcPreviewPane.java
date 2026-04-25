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

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (currentEntity == null) return;

		Graphics2D g2d = (Graphics2D) g;
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

		g2d.setColor(Color.WHITE);
		g2d.fillOval(centerX - 2, centerY - 2, 4, 4);
	}
}
