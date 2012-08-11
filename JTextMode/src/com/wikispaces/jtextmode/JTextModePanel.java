/**
 * 
 */
package com.wikispaces.jtextmode;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JComponent;


/**
 * 
 *
 */
public class JTextModePanel extends JComponent {
	public int rows;
	public int columns;
	
	public int cursorX;
	public int cursorY;
	
	public char[][] displayMemory;
	public byte[][] foregroundColor;
	public byte[][] backgroundColor;
	public boolean[][] blinking;
	
	// For drawing
	private static BufferedImage codePage;
	static {
		try {
			codePage = ImageIO.read(JTextModePanel.class.getResourceAsStream("CodePage.png"));
		} catch (IOException e) {
			e.printStackTrace();
			throw new MissingCodePageException();
		}
	}
	
	public JTextModePanel (int rows, int columns) throws IllegalArgumentException {
		if (rows <= 0 || columns <= 0) {
			throw new IllegalArgumentException("Rows and columns must be greater than 0");
		}
		
		this.rows = rows;
		this.columns = columns;
		
		cursorX = 0;
		cursorY = 0;
		
		displayMemory = new char[rows][columns];
		foregroundColor = new byte[rows][columns];
		backgroundColor = new byte[rows][columns];
		blinking = new boolean[rows][columns];
		
		for (int x = 0;x<rows;x++) {
			for (int y=0;y<columns;y++) {
				displayMemory[x][y] = ' ';
				foregroundColor[x][y] = 15;
				backgroundColor[x][y] = 0;
				blinking[x][y] = false;
			}
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		
	}
}
