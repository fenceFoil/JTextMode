/**
 * 
 */
package com.wikispaces.jtextmode;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
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

	public int[][] displayMemory;
	public byte[][] foregroundColor;
	public byte[][] backgroundColor;
	public boolean[][] blinking;

	// For drawing, in each foreground color
	private static BufferedImage codePage[];

	private static final int[][] colors = { { 0, 0, 0 }, { 0, 0, 127 },
			{ 0, 127, 0 }, { 0, 127, 127 }, { 127, 0, 0 }, { 127, 0, 127 },
			{ 127, 127, 0 }, { 127, 127, 127 }, { 99, 99, 99 }, { 0, 0, 255 },
			{ 0, 255, 0 }, { 0, 255, 255 }, { 255, 0, 0 }, { 255, 0, 255 },
			{ 255, 255, 0 }, { 255, 255, 255 } };

	public JTextModePanel(int rows, int columns)
			throws IllegalArgumentException, MissingCodePageException {
		if (rows <= 0 || columns <= 0) {
			throw new IllegalArgumentException(
					"Rows and columns must be greater than 0");
		}

		// Load code page, or font image
		codePage = new BufferedImage[16];
		// Tint it each of the 15 other foreground colors
		for (int i = 0; i < 16; i++) {
			try {
				codePage[i] = colorImage(ImageIO.read(JTextModePanel.class
						.getResourceAsStream("CodePage.png")), colors[i][0],
						colors[i][1], colors[i][2]);
			} catch (IOException e) {
				e.printStackTrace();
				throw new MissingCodePageException();
			}
		}

		// Set up the "screen"
		this.rows = rows;
		this.columns = columns;

		cursorX = 0;
		cursorY = 0;

		displayMemory = new int[columns][rows];
		foregroundColor = new byte[columns][rows];
		backgroundColor = new byte[columns][rows];
		blinking = new boolean[columns][rows];

		for (int x = 0; x < columns; x++) {
			for (int y = 0; y < rows; y++) {
				displayMemory[x][y] = 32;
				foregroundColor[x][y] = 15;
				backgroundColor[x][y] = 0;
				blinking[x][y] = false;
			}
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		// Create buffered image: a render of the text screen
		BufferedImage frameBuffer = new BufferedImage(rows * 9, columns * 16,
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D frameGraphics = frameBuffer.createGraphics();

		// Place each individual character image into the frame
		for (int x = 0; x < columns; x++) {
			for (int y = 0; y < rows; y++) {
				int pageSheetColumn = displayMemory[x][y] % 32;
				int pageSheetRow = displayMemory[x][y] / 32;
				frameGraphics.drawImage(codePage[foregroundColor[x][y]], x * 9, y * 16,
						(x + 1) * 9, (y + 1) * 16, pageSheetColumn * 9,
						pageSheetRow * 16, (pageSheetColumn + 1) * 9,
						(pageSheetRow + 1) * 16, new Color(
								colors[backgroundColor[x][y]][0],
								colors[backgroundColor[x][y]][1],
								colors[backgroundColor[x][y]][2]), null);
			}
		}

		// When finished with buffer, dispose of the Graphics used to edit it
		frameGraphics.dispose();
		
		// Draw and scale the framebuffer into the component
		g.drawImage(frameBuffer, 0, 0, getWidth(), getHeight(), null);
		g.dispose();
	}

	/**
	 * Adaped from
	 * http://stackoverflow.com/questions/4248104/applying-a-tint-to-
	 * an-image-in-java
	 * 
	 * @param srcImg
	 * @param red
	 * @param green
	 * @param blue
	 * @return
	 */
	public BufferedImage colorImage(BufferedImage srcImg, int red, int green,
			int blue) {
		BufferedImage img = new BufferedImage(srcImg.getWidth(),
				srcImg.getHeight(), BufferedImage.TRANSLUCENT);
		Graphics2D graphics = img.createGraphics();
		Color newColor = new Color(red, green, blue, 0 /*
														 * alpha needs to be
														 * zero
														 */);
		graphics.setXORMode(newColor);
		graphics.drawImage(srcImg, null, 0, 0);
		graphics.dispose();
		return img;
	}
}
