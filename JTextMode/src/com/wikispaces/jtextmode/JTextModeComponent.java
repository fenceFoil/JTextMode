/**
 * 
 */
package com.wikispaces.jtextmode;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JOptionPane;

/**
 * 
 *
 */
public class JTextModeComponent extends JComponent {
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

	public JTextModeComponent(int rows, int columns)
			throws IllegalArgumentException, MissingCodePageException {
		if (rows <= 0 || columns <= 0) {
			throw new IllegalArgumentException(
					"Rows and columns must be greater than 0");
		}

		// Load code page, or font image
		codePage = new BufferedImage[16];
		// Tint it each of the 15 other foreground colors
		try {
			codePage[0] = ImageIO.read(JTextModeComponent.class
					.getResourceAsStream("CodePage437.png"));

			JOptionPane.showConfirmDialog(null, new ImageIcon(codePage[0]));

			BufferedImage b = codePage[0];
			Graphics2D g = (Graphics2D) b.getGraphics();
			g.drawOval(0, 0, 100, 200);
			g.dispose();

			JOptionPane.showConfirmDialog(null, new ImageIcon(b));

			BufferedImage b2 = new BufferedImage(b.getWidth(), b.getHeight(),
					b.getType());
			Graphics2D g2 = (Graphics2D) b2.getGraphics();
			// g2.drawImage(b, 0, 0, Color.white, null);
			g2.setXORMode(new Color(0, 255, 0, 0));
			g2.drawImage(b, 0, 0, null);
			g2.dispose();

			JOptionPane.showConfirmDialog(null, new ImageIcon(b2));

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		for (int i = 1; i < 16; i++) {
			ColorTintFilter filter = new ColorTintFilter(new Color(
					colors[i][0], colors[i][1], colors[i][2]), 0f);
			codePage[i] = filter.filter(codePage[0], new BufferedImage(
					codePage[0].getWidth(), codePage[0].getHeight(),
					BufferedImage.TYPE_INT_ARGB));
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
				displayMemory[x][y] = 33;
				foregroundColor[x][y] = 0;
				backgroundColor[x][y] = 15;
				blinking[x][y] = false;
			}
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		// Create buffered image: a render of the text screen
		BufferedImage frameBuffer = new BufferedImage(columns * 9, rows * 16,
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D frameGraphics = frameBuffer.createGraphics();

		// Place each individual character images into the frame
		for (int x = 0; x < columns; x++) {
			for (int y = 0; y < rows; y++) {
				int pageSheetColumn = displayMemory[x][y] % 32;
				int pageSheetRow = displayMemory[x][y] / 32;
				frameGraphics.drawImage(codePage[foregroundColor[x][y]], x * 9,
						y * 16, (x + 1) * 9, (y + 1) * 16, pageSheetColumn * 9,
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

	public static Image makeColorTransparent(BufferedImage im, final Color color) {
		ImageFilter filter = new RGBImageFilter() {

			// the color we are looking for... Alpha bits are set to opaque
			public int markerRGB = color.getRGB() | 0xFF000000;

			public final int filterRGB(int x, int y, int rgb) {
				if ((rgb | 0xFF000000) == markerRGB) {
					// Mark the alpha bits as zero - transparent
					return 0x00FFFFFF & rgb;
				} else {
					// nothing to do
					return rgb;
				}
			}
		};

		ImageProducer ip = new FilteredImageSource(im.getSource(), filter);
		return Toolkit.getDefaultToolkit().createImage(ip);
	}
}
