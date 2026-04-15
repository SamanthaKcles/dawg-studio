package ca.noxid.uiComponents;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.NumberFormat;


public class FormattedUpdateTextField extends JFormattedTextField
		implements KeyListener {
	private static final long serialVersionUID = 5123472341660584854L;
	private static final Color bgCol = new Color(75, 110, 140);
	private static final Color defaultBg = new Color(69, 73, 74);
	boolean updated = false;
	//private static final InputVerifier verify = new Verifier();

	public FormattedUpdateTextField(NumberFormat f) {
		super(f);
		this.addKeyListener(this);
		//this.setInputVerifier(verify);
	}

	public static NumberFormat getNumberOnlyFormat(int minDigit, int maxDigit) {
		NumberFormat retVal = NumberFormat.getIntegerInstance();
		retVal.setMaximumIntegerDigits(maxDigit);
		retVal.setMinimumIntegerDigits(minDigit);
		retVal.setGroupingUsed(false);
		return retVal;
	}

	@Override
	public void keyPressed(KeyEvent arg0) {
		//ignore
	}

	@Override
	public void keyReleased(KeyEvent arg0) {
		//ignore
	}

	@Override
	public void keyTyped(KeyEvent eve) {
		char k = eve.getKeyChar();
		if (k == '\n') {
			this.setBackground(defaultBg);
			updated = false;
			this.fireActionPerformed();
		} else if (k >= ' ' || k == 8 || k == 22) { //character or backspace or paste
			this.setBackground(bgCol);
			updated = true;
		}
	}

	@Override
	public void processFocusEvent(FocusEvent eve) {
		super.processFocusEvent(eve);
		if (updated) {
			this.setBackground(defaultBg);
			updated = false;
			this.fireActionPerformed();
		}
	}
}
