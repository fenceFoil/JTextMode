/**
 * 
 */
package com.wikispaces.jtextmode;

import java.awt.Graphics;

import javax.swing.JComponent;


/**
 * 
 *
 */
public class JTextModePanel extends JComponent {
	public int rows;
	public int columns;
	
	public char[][] displayMemory;
	public byte[][] foregroundColor;
	public byte[][] backgroundColor;
	public boolean[][] blinking;
	
	public JTextModePanel (int rows, int columns) throws IllegalArgumentException {
		if (rows <= 0 || columns <= 0) {
			throw new IllegalArgumentException("Rows and columns must be greater than 0");
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		
	}
}
