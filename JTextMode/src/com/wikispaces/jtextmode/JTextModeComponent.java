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
	public byte[][] screenForegroundColor;

	/**
	 * Note that on a real DOS display, EITHER 8-15 would be valid background
	 * colors OR blinking could be enabled.
	 */
	public byte[][] screenBackgroundColor;

	/**
	 * The blinking bit for each character on the display.
	 */
	public boolean[][] screenBlinking;

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
		screenForegroundColor = new byte[columns][rows];
		screenBackgroundColor = new byte[columns][rows];
		screenBlinking = new boolean[columns][rows];

		for (int x = 0; x < columns; x++) {
			for (int y = 0; y < rows; y++) {
				charsDisplayed[x][y] = 32;
				screenForegroundColor[x][y] = 15;
				screenBackgroundColor[x][y] = 0;
				screenBlinking[x][y] = false;
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
				if (allBlinkingEnabled && screenBlinking[x][y]
						&& blinkOnThisFrame) {
					// Draw background color only
					frameGraphics.setColor(new Color(
							colors[screenBackgroundColor[x][y]][0],
							colors[screenBackgroundColor[x][y]][1],
							colors[screenBackgroundColor[x][y]][2]));
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
					frameGraphics.drawImage(
							codePage[screenForegroundColor[x][y]], x * 9,
							y * 16, (x + 1) * 9, (y + 1) * 16,
							pageSheetColumn * 9, pageSheetRow * 16,
							(pageSheetColumn + 1) * 9, (pageSheetRow + 1) * 16,
							new Color(colors[screenBackgroundColor[x][y]][0],
									colors[screenBackgroundColor[x][y]][1],
									colors[screenBackgroundColor[x][y]][2]),
							null);

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

	/**
	 * Sets a character on the text screen without moving the cursor.
	 * 
	 * @param x
	 * @param y
	 * @param character
	 *            return false if the x and y are out of bounds
	 */
	public boolean setCharAt(int x, int y, int character) {
		if (x < 0 || x >= columns) {
			return false;
		}
		if (y < 0 || y >= rows) {
			return false;
		}

		charsDisplayed[x][y] = character;

		return true;
	}

	/**
	 * Sets a character on the text screen without moving the cursor with all
	 * its metadata.
	 * 
	 * @param x
	 * @param y
	 * @param character
	 * @param color
	 * @param bgColor
	 * @param blink
	 *            return false if the x and y are out of bounds
	 */
	public boolean setCharAt(int x, int y, int character, int color,
			int bgColor, boolean blink) {
		if (x < 0 || x >= columns) {
			return false;
		}
		if (y < 0 || y >= rows) {
			return false;
		}

		charsDisplayed[x][y] = character;
		screenForegroundColor[x][y] = (byte) color;
		screenBackgroundColor[x][y] = (byte) bgColor;
		screenBlinking[x][y] = blink;

		return true;
	}

	public boolean setBlinkingAt(int x, int y, boolean blink) {
		if (x < 0 || x >= columns) {
			return false;
		}
		if (y < 0 || y >= rows) {
			return false;
		}

		screenBlinking[x][y] = blink;

		return true;
	}

	public boolean setBGColorAt(int x, int y, int bgColor) {
		if (x < 0 || x >= columns) {
			return false;
		}
		if (y < 0 || y >= rows) {
			return false;
		}

		screenBackgroundColor[x][y] = (byte) bgColor;

		return true;
	}

	public boolean setColorAt(int x, int y, int color) {
		if (x < 0 || x >= columns) {
			return false;
		}
		if (y < 0 || y >= rows) {
			return false;
		}

		screenForegroundColor[x][y] = (byte) color;

		return true;
	}

	public boolean setColorsAt(int x, int y, int color, int bgColor,
			boolean blink) {
		if (x < 0 || x >= columns) {
			return false;
		}
		if (y < 0 || y >= rows) {
			return false;
		}

		screenForegroundColor[x][y] = (byte) color;
		screenBackgroundColor[x][y] = (byte) bgColor;
		screenBlinking[x][y] = blink;

		return true;
	}

	/**
	 * Puts the cursor at the given character on the text screen.
	 * 
	 * @param x
	 * @param y
	 * @return false if the given cursor position is out of bounds; will set as
	 *         close as possible to given position anyway
	 */
	public boolean setCursorPos(int x, int y) {
		int finalX = x;
		int finalY = y;
		boolean goodSet = true;
		if (x >= columns) {
			goodSet = false;
			finalX = columns - 1;
		} else if (x < 0) {
			goodSet = false;
			finalX = 0;
		}
		if (y >= rows) {
			goodSet = false;
			finalY = rows - 1;
		} else if (y < 0) {
			goodSet = false;
			finalY = 0;
		}
		cursorX = finalX;
		cursorY = finalY;
		return goodSet;
	}

	/**
	 * Fills the screen with spaces written with the current draw colors and
	 * blinking status.
	 */
	public void clearScreen() {
		scrollScreen(rows);
	}

	private boolean doneReading = false;

	/**
	 * UNTESTED - UNFINISHED - Blocks and reads characters typed into the
	 * component, displaying them on screen and allowing basic editing (enter,
	 * backspace).
	 * 
	 * @return
	 */
	public String readLn() {
		doneReading = false;
		final StringBuilder readBuffer = new StringBuilder();
		addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
				readBuffer.append(e.getKeyChar());
				write(e.getKeyChar());
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

	public void setDrawColor(int i) {
		currDrawColor = (byte) i;
	}

	public void setDrawBGColor(int bgColor) {
		currDrawBGColor = (byte) bgColor;
	}

	public void setDrawBlinking(boolean blinking) {
		currDrawBlinking = blinking;
	}

	private void putAtCursor(int character) {
		charsDisplayed[cursorX][cursorY] = character;
		screenForegroundColor[cursorX][cursorY] = currDrawColor;
		screenBackgroundColor[cursorX][cursorY] = currDrawBGColor;
		screenBlinking[cursorX][cursorY] = currDrawBlinking;
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
					screenForegroundColor[x][y - 1] = screenForegroundColor[x][y];
					screenBackgroundColor[x][y - 1] = screenBackgroundColor[x][y];
					screenBlinking[x][y - 1] = screenBlinking[x][y];
				}
			}

			// Clear bottom row
			for (int x = 0; x < columns; x++) {
				charsDisplayed[x][rows - 1] = 32;
				screenForegroundColor[x][rows - 1] = currDrawColor;
				screenBackgroundColor[x][rows - 1] = currDrawBGColor;
				screenBlinking[x][rows - 1] = currDrawBlinking;
			}

			// Move cursor up
			cursorY--;
			if (cursorY < 0) {
				cursorY = 0;
			}
		}
	}
}
