/**
 * 
 */
package com.wikispaces.jtextmode;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.io.IOException;
import java.util.LinkedList;

import javax.imageio.ImageIO;
import javax.swing.JComponent;

/**
 * 
 *
 */
@SuppressWarnings("serial")
public class JTextModeComponent extends JComponent {
	/**
	 * The size of the text screen in characters.
	 */
	private int rows;
	private int columns;

	/**
	 * The position of the cursor on the text screen.
	 */
	private int cursorX;
	private int cursorY;

	/**
	 * Whether the cursor is rendered
	 */
	private boolean cursorVisible;

	/**
	 * The characters rendered on the screen.
	 */
	public int[][] charsDisplayed;

	/**
	 * The colors that the characters are rendered in.
	 */
	public byte[][] foregroundColor;

	/**
	 * Note that on a real DOS display, EITHER 8-15 would be valid background
	 * colors OR blinking could be enabled.
	 */
	public byte[][] backgroundColor;

	/**
	 * The blinking bit for each character on the display.
	 */
	public boolean[][] blinking;

	/**
	 * If true, the cursor and blinking characters will be rendered on or
	 * invisible as the timer has it. Otherwise, such as when you redraw the
	 * screen occasionally, turn this off to have blinking characters always
	 * visible.
	 * */
	private boolean allBlinkingEnabled;

	// The metadata for characters drawn by the cursor
	private byte currDrawColor = 15;
	private byte currDrawBGColor = 0;
	private boolean currDrawBlinking = false;

	/**
	 * These are images of the characters to be drawn on the screen. Their
	 * required dimensions: CHARACTER: 9 px wide, 16 px high. IMAGE: 256
	 * characters arranged to be 32 wide by 8 high. More than 256 characters to
	 * still be arranged in 32 wide rows.<br>
	 * <br>
	 * One image is required in this array for each color text will be drawn in.
	 */
	private static Image codePage[];

	private static final int[][] colors = { { 0, 0, 0 }, { 0, 0, 127 },
			{ 0, 127, 0 }, { 0, 127, 127 }, { 127, 0, 0 }, { 127, 0, 127 },
			{ 127, 127, 0 }, { 127, 127, 127 }, { 99, 99, 99 }, { 0, 0, 255 },
			{ 0, 255, 0 }, { 0, 255, 255 }, { 255, 0, 0 }, { 255, 0, 255 },
			{ 255, 255, 0 }, { 255, 255, 255 } };

	public JTextModeComponent(int rows, int columns, boolean blinkingEnabled,
			boolean showCursor) throws IllegalArgumentException,
			MissingCodePageException {
		if (rows <= 0 || columns <= 0) {
			throw new IllegalArgumentException(
					"Rows and columns must be greater than 0");
		}

		// Load first code page, or font image
		codePage = new Image[16];
		try {
			codePage[0] = makeColorTransparent(
					ImageIO.read(JTextModeComponent.class
							.getResourceAsStream("CodePage437.png")),
					Color.white);
		} catch (IOException e1) {
			e1.printStackTrace();
			throw new MissingCodePageException();
		}

		// Tint it each of the 15 other foreground colors
		for (int i = 1; i < 16; i++) {
			BufferedImage rawCodePage;
			try {
				rawCodePage = ImageIO.read(JTextModeComponent.class
						.getResourceAsStream("CodePage437.png"));

				BufferedImage coloredCodePage = new BufferedImage(
						rawCodePage.getWidth(), rawCodePage.getHeight(),
						rawCodePage.getType());
				Graphics2D g2 = (Graphics2D) coloredCodePage.getGraphics();
				g2.setXORMode(new Color(colors[i][0], colors[i][1],
						colors[i][2], 0));
				g2.drawImage(makeColorTransparent(rawCodePage, Color.white), 0,
						0, null);
				g2.dispose();

				codePage[i] = makeColorTransparent(coloredCodePage, Color.black);
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
		cursorVisible = showCursor;

		charsDisplayed = new int[columns][rows];
		foregroundColor = new byte[columns][rows];
		backgroundColor = new byte[columns][rows];
		blinking = new boolean[columns][rows];

		for (int x = 0; x < columns; x++) {
			for (int y = 0; y < rows; y++) {
				charsDisplayed[x][y] = 32;
				foregroundColor[x][y] = 15;
				backgroundColor[x][y] = 0;
				blinking[x][y] = false;
			}
		}

		if (blinkingEnabled) {
			allBlinkingEnabled = true;
			Thread blinkThread = new Thread(new Runnable() {

				@Override
				public void run() {
					boolean lastRunState = false;
					while (true) {
						boolean currentState = false;
						if (System.currentTimeMillis() % 1000 < 500) {
							currentState = true;
						}
						if (currentState != lastRunState) {
							repaint();
							lastRunState = currentState;
						}
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							break;
						}
					}
				}

			});
			blinkThread.start();
		} else {
			allBlinkingEnabled = false;
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		// Create buffered image: a render of the text screen
		BufferedImage frameBuffer = new BufferedImage(columns * 9, rows * 16,
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D frameGraphics = frameBuffer.createGraphics();
		RenderingHints renderingHints = new RenderingHints(
				RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		frameGraphics.setRenderingHints(renderingHints);

		boolean blinkOnThisFrame = false;
		// Decide, if blinking, whether to draw or blank blinking characters
		if (allBlinkingEnabled && (System.currentTimeMillis() % 1000 < 500)) {
			blinkOnThisFrame = true;
		}

		// Place each individual character images into the frame
		for (int x = 0; x < columns; x++) {
			for (int y = 0; y < rows; y++) {
				if (allBlinkingEnabled && blinking[x][y] && blinkOnThisFrame) {
					// Draw background color only
					frameGraphics.setColor(new Color(
							colors[backgroundColor[x][y]][0],
							colors[backgroundColor[x][y]][1],
							colors[backgroundColor[x][y]][2]));
					frameGraphics.fillRect(x * 9, y * 16, 9, 16);
				} else {
					int charToDraw = charsDisplayed[x][y];

					// Handle the cursor
					if (cursorVisible && x == cursorX && y == cursorY) {
						if (blinkOnThisFrame) {
							// Cursor is a space
							charToDraw = 32;
						} else {
							// Cursor is an underscore
							charToDraw = 95;
						}
					}

					// Draw character
					int pageSheetColumn = charToDraw % 32;
					int pageSheetRow = charToDraw / 32;
					frameGraphics.drawImage(codePage[foregroundColor[x][y]],
							x * 9, y * 16, (x + 1) * 9, (y + 1) * 16,
							pageSheetColumn * 9, pageSheetRow * 16,
							(pageSheetColumn + 1) * 9, (pageSheetRow + 1) * 16,
							new Color(colors[backgroundColor[x][y]][0],
									colors[backgroundColor[x][y]][1],
									colors[backgroundColor[x][y]][2]), null);

				}
			}
		}

		// When finished with buffer, dispose of the Graphics used to edit it
		frameGraphics.dispose();

		// Draw and scale the framebuffer into the component
		g.drawImage(frameBuffer, 0, 0, getWidth(), getHeight(), null);
		g.dispose();
	}

	/**
	 * Takes an image and replaces the given color with the color
	 * "completely transparent."
	 * 
	 * Source: http://stackoverflow.com/questions/665406/how-to-make-a-color-
	 * transparent-in-a-bufferedimage-and-save-as-png
	 * 
	 * @param im
	 * @param color
	 * @return
	 */
	private static Image makeColorTransparent(BufferedImage im,
			final Color color) {
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

	/**
	 * @param text
	 */
	public void writeLn(String... text) {
		for (String currString : text) {
			char[] textChars = currString.toCharArray();
			for (int i = 0; i < textChars.length; i++) {
				putAtCursor(textChars[i]);
			}
			cursorMoveNewline();
		}

		if (text.length <= 0) {
			cursorMoveNewline();
		}
	}

	/**
	 * 
	 * @param chars
	 */
	public void writeLn(char... chars) {
		for (char currChar : chars) {
			putAtCursor(currChar);
			cursorMoveNewline();
		}

		if (chars.length <= 0) {
			cursorMoveNewline();
		}
	}

	public void writeLn() {
		cursorMoveNewline();
	}

	public void write(String... text) {
		for (String currString : text) {
			char[] textChars = currString.toCharArray();
			for (int i = 0; i < textChars.length; i++) {
				putAtCursor(textChars[i]);
			}
		}
	}

	public void write(char... chars) {
		for (char currChar : chars) {
			putAtCursor(currChar);
		}
	}
	
	public void clearScreen () {
		scrollScreen(rows);
	}

	private boolean doneReading = false;
	public String readLn() {
		doneReading = false;
		final StringBuilder readBuffer = new StringBuilder();
		addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
				readBuffer.append(e.getKeyChar());
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					doneReading = true;
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void keyPressed(KeyEvent e) {
				// TODO Auto-generated method stub

			}
		});
		while (!doneReading) {
			try {
				Thread.sleep(5);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
				break;
			}
		}
		return readBuffer.toString();
	}

	public void setDrawColor(byte color) {
		currDrawColor = color;
	}

	public void setDrawBGColor(byte bgColor) {
		currDrawBGColor = bgColor;
	}

	public void setDrawBlinking(boolean blinking) {
		currDrawBlinking = blinking;
	}

	private void putAtCursor(int character) {
		charsDisplayed[cursorX][cursorY] = character;
		foregroundColor[cursorX][cursorY] = currDrawColor;
		backgroundColor[cursorX][cursorY] = currDrawBGColor;
		blinking[cursorX][cursorY] = currDrawBlinking;
		incrementCursor();
	}

	private void incrementCursor() {
		cursorX++;
		if (cursorX >= columns) {
			cursorMoveNewline();
		}
	}

	private void cursorMoveNewline() {
		cursorX = 0;
		cursorY++;
		if (cursorY >= rows) {
			scrollScreen(1);
		}
	}

	private void scrollScreen(int scrollRows) {
		// Shift one row at a time
		for (int i = 0; i < scrollRows; i++) {
			// Shift every row on the screen up
			for (int y = 1; y < rows; y++) {
				for (int x = 0; x < columns; x++) {
					charsDisplayed[x][y - 1] = charsDisplayed[x][y];
					foregroundColor[x][y - 1] = foregroundColor[x][y];
					backgroundColor[x][y - 1] = backgroundColor[x][y];
					blinking[x][y - 1] = blinking[x][y];
				}
			}

			// Clear bottom row
			for (int x = 0; x < columns; x++) {
				charsDisplayed[x][rows - 1] = 32;
				foregroundColor[x][rows - 1] = currDrawColor;
				backgroundColor[x][rows - 1] = currDrawBGColor;
				blinking[x][rows - 1] = currDrawBlinking;
			}

			// Move cursor up
			cursorY--;
			if (cursorY < 0) {
				cursorY = 0;
			}
		}
	}
}
