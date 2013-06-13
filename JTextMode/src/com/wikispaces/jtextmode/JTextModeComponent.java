/**
 * 
 */
package com.wikispaces.jtextmode;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;

import org.imgscalr.Scalr;

/**
 * 
 *
 */
@SuppressWarnings("serial")
public class JTextModeComponent extends JComponent implements Terminal {
	/**
	 * The size of the text screen in characters.
	 */
	private int rows;
	private int columns;

	/**
	 * Whether to ensure that the characters of this display are never
	 * distorted, stretched, or squished as the compnent is resized.
	 */
	private boolean lockAspect = false;

	/**
	 * The position of the cursor on the text screen.
	 */
	private int cursorX;
	private int cursorY;

	// Position of the mouse cursor on the text screen
	private int mouseCursorX = 0;
	private int mouseCursorY = 0;

	/**
	 * The character used to draw the cursor
	 * 
	 * Default is 95: ASCII underscore
	 */
	private int cursorChar = 95;

	/**
	 * The character used to draw blank space
	 * 
	 * Default is 32: ASCII space
	 */
	private int blankChar = 32;

	/**
	 * Whether the cursor is rendered
	 */
	private boolean cursorVisible;

	/**
	 * Whether the mouse cursor is rendered
	 */
	private boolean mouseCursorVisible;

	/**
	 * The characters rendered on the screen. Preferably alter this with
	 * JTextModeComponent's API.
	 */
	public int[][] charsDisplayed;

	/**
	 * The colors that the characters are rendered in. Preferably alter this
	 * with JTextModeComponent's API.
	 */
	public byte[][] screenForegroundColor;

	/**
	 * Note that on a real DOS display, EITHER 8-15 would be valid background
	 * colors OR blinking could be enabled. Preferably alter this with
	 * JTextModeComponent's API.
	 */
	public byte[][] screenBackgroundColor;

	/**
	 * The blinking bit for each character on the display. Preferably alter this
	 * with JTextModeComponent's API.
	 */
	public boolean[][] screenBlinking;

	/**
	 * If true, the cursor and blinking characters will be rendered on or
	 * invisible as the timer has it. Otherwise, such as when you only want to
	 * redraw the screen occasionally, turn this off to have blinking characters
	 * always visible.
	 * */
	private boolean allBlinkingEnabled;

	// The metadata for characters drawn by the cursor
	private byte currDrawColor = 15;
	private byte currDrawBGColor = 0;
	private boolean currDrawBlinking = false;

	// Info about the code page image being used
	private int charWidth;
	private int charHeight;
	private int pageWidthInChars;
	private int pageHeightInChars;

	/**
	 * These are images of the characters to be drawn on the screen. Their
	 * required dimensions: CHARACTER: 9 px wide, 16 px high. IMAGE: 256
	 * characters arranged to be 32 wide by 8 high. More than 256 characters to
	 * still be arranged in 32 wide rows.<br>
	 * <br>
	 * One image is required in this array for each color text will be drawn in.
	 */
	private Image codePage[];

	private static final Color[] colors = { new Color(0, 0, 0),
			new Color(0, 0, 127), new Color(0, 127, 0), new Color(0, 127, 127),
			new Color(127, 0, 0),
			new Color(127, 0, 127),
			new Color(127, 127, 0),
			new Color(127, 127, 127),
			// High intensity
			new Color(99, 99, 99), new Color(0, 0, 255), new Color(0, 255, 0),
			new Color(0, 255, 255), new Color(255, 0, 0),
			new Color(255, 0, 255), new Color(255, 255, 0),
			new Color(255, 255, 255) };

	private static final Color[] invertedColors = { new Color(127, 127, 127),
			new Color(127, 127, 0), new Color(127, 0, 127),
			new Color(127, 0, 0), new Color(0, 127, 127),
			new Color(0, 127, 0),
			new Color(0, 0, 127),
			new Color(0, 0, 0),
			// High intensity
			new Color(255, 255, 255), new Color(255, 255, 0),
			new Color(255, 0, 255), new Color(255, 0, 0),
			new Color(0, 255, 255), new Color(0, 255, 0), new Color(0, 0, 255),
			new Color(99, 99, 99) };

	public static void main(String[] args) {
		JTextModeComponent term = new JTextModeComponent(25, 80, true, true,
				true);
		JFrame frame = new JFrame();
		frame.add(term);
		frame.setSize(800, 600);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		Random rand = new Random();
		while (true) {

			term.setCharAt(rand.nextInt(term.getColumns()),
					rand.nextInt(term.getRows()), rand.nextInt(256), 15, 0,
					false);
			
			term.repaint();
			
			try {
				Thread.sleep(1000/30);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Creates a new JTextModeComponent with the settings given and the default
	 * code page.
	 * 
	 * @param rows
	 *            The width of the screen simulated. Larger = slightly slower.
	 * @param columns
	 *            The height of the screen simulated. Larger = slightly slower.
	 * @param blinkingEnabled
	 *            Whether the screen is re-rendered every 500ms to make cursors
	 *            and blinking characters blink. Consumes resources if true.
	 * @param showCursor
	 *            Whether the cursor is shown.
	 */
	public JTextModeComponent(int rows, int columns, boolean blinkingEnabled,
			boolean showCursor, boolean lockAspectRatio) {
		if (rows <= 0 || columns <= 0) {
			rows = 1;
			columns = 1;
		}

		// Load default code page
		try {
			setCodePage(null, 9, 16, 32, 8);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// Set up the "screen"
		this.rows = rows;
		this.columns = columns;

		setLockAspect(lockAspectRatio);

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
			blinkThread.setName("JTextMode Blink Thread");
			blinkThread.start();
		} else {
			allBlinkingEnabled = false;
		}
	}

	/**
	 * Loads a "code page" for this text screen: an image with 256 character
	 * images laid out in a grid used to draw the text on the screen. The
	 * characters should be black on an opaque white background with no
	 * anti-aliasing or shades of gray. Redraws the screen when done.
	 * 
	 * If imageStream is null, the default code page is loaded.
	 * 
	 * @param imageStream
	 *            A stream connected to the code page image or null.
	 * @param charWidth
	 *            The width of one character in pixels on the grid
	 * @param charHeight
	 *            The height of one character in pixels on the grid
	 * @param pageWidthInChars
	 *            The width of the grid in characters
	 * @param pageHeightInChars
	 *            The height of the grid in characters
	 * @throws IOException
	 */
	public void setCodePage(InputStream imageStream, int charWidth,
			int charHeight, int pageWidthInChars, int pageHeightInChars)
			throws IOException {
		// If no image stream is given, load default code page
		if (imageStream == null) {
			imageStream = JTextModeComponent.class
					.getResourceAsStream("CodePage437-9x16.png");
		}

		// Load code page image
		BufferedImage masterCopy = ImageIO.read(imageStream);

		// Load first code page, or font image in color 0
		codePage = new Image[16];
		codePage[0] = makeColorTransparent(deepCopyImage(masterCopy),
				Color.white);

		// Tint it each of the 15 other foreground colors
		for (int i = 1; i < 16; i++) {
			BufferedImage rawCodePage;
			rawCodePage = deepCopyImage(masterCopy);

			BufferedImage coloredCodePage = new BufferedImage(
					rawCodePage.getWidth(), rawCodePage.getHeight(),
					rawCodePage.getType());
			Graphics2D g2 = (Graphics2D) coloredCodePage.getGraphics();
			Color xorColor = colors[i];
			g2.setXORMode(new Color(xorColor.getRed(), xorColor.getGreen(),
					xorColor.getBlue(), 0));
			g2.drawImage(makeColorTransparent(rawCodePage, Color.white), 0, 0,
					null);
			g2.dispose();

			codePage[i] = makeColorTransparent(coloredCodePage, Color.black);
		}

		this.charWidth = charWidth;
		this.charHeight = charHeight;
		this.pageWidthInChars = pageWidthInChars;
		this.pageHeightInChars = pageHeightInChars;

		// Repaint everything, disposing of anything drawn with the last code
		// page
		repaint();
	}

	public void resetCodePage() throws MissingCodePageException {
		// Load default code page
		try {
			setCodePage(null, 9, 16, 32, 8);
		} catch (IOException e1) {
			e1.printStackTrace();
			throw new MissingCodePageException();
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		// Create buffered image: a render of the text screen
		BufferedImage frameBuffer = new BufferedImage(columns * charWidth, rows
				* charHeight, BufferedImage.TYPE_INT_ARGB);
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
					frameGraphics.setColor(colors[screenBackgroundColor[x][y]]);
					frameGraphics.fillRect(x * charWidth, y * charHeight,
							charWidth, charHeight);
				} else {
					int charToDraw = charsDisplayed[x][y];

					// Handle the cursor
					if (cursorVisible && x == cursorX && y == cursorY) {
						if (blinkOnThisFrame) {
							// Cursor is a space
							charToDraw = blankChar;
						} else {
							// Cursor is an underscore
							charToDraw = cursorChar;
						}
					}

					// Draw character
					int pageSheetColumn = charToDraw % pageWidthInChars;
					int pageSheetRow = charToDraw / pageWidthInChars;
					frameGraphics.drawImage(
							codePage[screenForegroundColor[x][y]], x
									* charWidth, y * charHeight, (x + 1)
									* charWidth, (y + 1) * charHeight,
							pageSheetColumn * charWidth, pageSheetRow
									* charHeight, (pageSheetColumn + 1)
									* charWidth, (pageSheetRow + 1)
									* charHeight,
							colors[screenBackgroundColor[x][y]], null);

				}
			}
		}

		// When finished with buffer, dispose of the Graphics used to edit it
		frameGraphics.dispose();

		// Draw and scale the framebuffer into the component
		if (lockAspect) {
			Rectangle scaledBufferRect = getAspectLockedScaledBufferSize();
			BufferedImage scaledImage = Scalr.resize(frameBuffer,
					Scalr.Method.SPEED, scaledBufferRect.width,
					scaledBufferRect.height);
			g.drawImage(scaledImage, scaledBufferRect.x, scaledBufferRect.y,
					null);
		} else {
			BufferedImage scaledImage = Scalr.resize(frameBuffer,
					Scalr.Method.SPEED, Scalr.Mode.FIT_EXACT, getWidth(),
					getHeight());
			// g.drawImage(frameBuffer, 0, 0, getWidth(), getHeight(), null);
			g.drawImage(scaledImage, 0, 0, null);
		}
		g.dispose();
	}

	private Rectangle getAspectLockedScaledBufferSize() {
		Rectangle scaledBufferRect = new Rectangle();

		int frameBufferHeight = rows * charHeight;
		int frameBufferWidth = columns * charWidth;
		double bufferRatio = (double) frameBufferHeight
				/ (double) frameBufferWidth;
		double componentRatio = (double) getHeight() / (double) getWidth();
		if (componentRatio < bufferRatio) {
			// Too wide
			// Center by x
			double resizeRatio = (double) getHeight()
					/ (double) frameBufferHeight;
			// Find excess width
			double excessWidth = (double) getWidth()
					- (resizeRatio * (double) frameBufferWidth);

			scaledBufferRect.setBounds((int) (excessWidth / 2), 0,
					(int) (resizeRatio * (double) frameBufferWidth),
					(int) (resizeRatio * (double) frameBufferHeight));
		} else if (componentRatio > bufferRatio) {
			// Too tall
			// Center by y
			double resizeRatio = (double) getWidth()
					/ (double) frameBufferWidth;
			// Find excess height
			double excessHeight = (double) getHeight()
					- (resizeRatio * (double) frameBufferHeight);

			scaledBufferRect.setBounds(0, (int) (excessHeight / 2),
					(int) (resizeRatio * (double) frameBufferWidth),
					(int) (resizeRatio * (double) frameBufferHeight));
		} else {
			// Just right
			scaledBufferRect.setBounds(0, 0, getWidth(), getHeight());
		}
		return scaledBufferRect;
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
	 * Makes a copy of a BufferedImage and its contents.
	 * 
	 * Source: http://stackoverflow.com/questions/3514158/how-do-you-clone-a-
	 * bufferedimage
	 * 
	 * @return
	 */
	private static BufferedImage deepCopyImage(BufferedImage image) {
		ColorModel colorModel = image.getColorModel();
		boolean isAlphaPremultiplied = colorModel.isAlphaPremultiplied();
		WritableRaster raster = image.copyData(null);
		return new BufferedImage(colorModel, raster, isAlphaPremultiplied, null);
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
	 * 
	 * @param x
	 * @param y
	 * @return the character at x, y, or -1 if coords our out of bounds
	 */
	public int getCharAt(int x, int y) {
		if (x < 0 || x >= columns) {
			return -1;
		}
		if (y < 0 || y >= rows) {
			return -1;
		}

		return charsDisplayed[x][y];
	}

	/**
	 * 
	 * @param x
	 * @param y
	 * @return the color at x, y, or -1 if coords our out of bounds
	 */
	public int getColorAt(int x, int y) {
		if (x < 0 || x >= columns) {
			return -1;
		}
		if (y < 0 || y >= rows) {
			return -1;
		}

		return screenForegroundColor[x][y];
	}

	/**
	 * 
	 * @param x
	 * @param y
	 * @return the background color at x, y, or -1 if coords our out of bounds
	 */
	public int getBGColorAt(int x, int y) {
		if (x < 0 || x >= columns) {
			return -1;
		}
		if (y < 0 || y >= rows) {
			return -1;
		}

		return screenBackgroundColor[x][y];
	}

	/**
	 * 
	 * @param x
	 * @param y
	 * @return 1 if the character is blinking at x, y<br>
	 *         0 if not<br>
	 *         -1 if coords are out of range
	 */
	public int getBlinkingAt(int x, int y) {
		if (x < 0 || x >= columns) {
			return -1;
		}
		if (y < 0 || y >= rows) {
			return -1;
		}

		if (screenBlinking[x][y]) {
			return 1;
		} else {
			return 0;
		}
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

	public void scrollScreen(int scrollRows) {
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
				charsDisplayed[x][rows - 1] = blankChar;
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

	// private boolean doneReading = false;
	// /**
	// * UNTESTED - UNFINISHED - Blocks and reads characters typed into the
	// * component, displaying them on screen and allowing basic editing (enter,
	// * backspace).
	// *
	// * @return
	// */
	// public String readLn() {
	// doneReading = false;
	// final StringBuilder readBuffer = new StringBuilder();
	// addKeyListener(new KeyListener() {
	//
	// @Override
	// public void keyTyped(KeyEvent e) {
	// readBuffer.append(e.getKeyChar());
	// write(e.getKeyChar());
	// if (e.getKeyCode() == KeyEvent.VK_ENTER) {
	// doneReading = true;
	// }
	// }
	//
	// @Override
	// public void keyReleased(KeyEvent e) {
	// // TODO Auto-generated method stub
	//
	// }
	//
	// @Override
	// public void keyPressed(KeyEvent e) {
	// // TODO Auto-generated method stub
	//
	// }
	// });
	// while (!doneReading) {
	// try {
	// Thread.sleep(5);
	// } catch (InterruptedException e1) {
	// e1.printStackTrace();
	// break;
	// }
	// }
	// return readBuffer.toString();
	// }

	// TODO: Range check set color methods
	public void setDrawColor(int i) {
		currDrawColor = (byte) i;
	}

	public void setDrawBGColor(int bgColor) {
		currDrawBGColor = (byte) bgColor;
	}

	public void setDrawBlinking(boolean blinking) {
		currDrawBlinking = blinking;
	}

	public void setShowMouseCursor(boolean show) {
		mouseCursorVisible = show;
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

	/**
	 * Returns the character used on the code page to draw the cursor.
	 * 
	 * @return
	 */
	public int getCursorChar() {
		return cursorChar;
	}

	/**
	 * Sets the character used on the code page to draw the cursor. If your code
	 * page doesn't put the cusor at 95 (underscore), change the character used
	 * here to match your code page.
	 * 
	 * @param cursorChar
	 */
	public void setCursorChar(int cursorChar) {
		this.cursorChar = cursorChar;
	}

	/**
	 * 
	 * @return
	 */
	public int getBlankChar() {
		return blankChar;
	}

	/**
	 * Sets the character used on the code page to draw empty space. If your
	 * code page doesn't have a blank space at 32 (ASCII space), set this to
	 * match your code page.
	 * 
	 * @param blankChar
	 */
	public void setBlankChar(int blankChar) {
		this.blankChar = blankChar;
	}

	/**
	 * Returns text screen coords (rows and columns) for given pixel coordinates
	 * within this component.
	 * 
	 * @param x
	 * @param y
	 * @return null if the aspect ratio is locked and the given position is
	 *         outside of the display area.
	 */
	public Point getTextCoordsForComponentCoords(int x, int y) {
		if (!lockAspect) {
			int textX = (int) (((double) x / (double) getWidth()) * (double) columns);
			int textY = (int) (((double) y / (double) getHeight()) * (double) rows);

			return new Point(textX, textY);
		} else {
			// If aspect is locked, calculate where the cursor appears
			Rectangle scaledScreenLocation = getAspectLockedScaledBufferSize();
			if (scaledScreenLocation.contains(x, y)) {
				int textX = (int) (((double) (x - scaledScreenLocation.x) / (double) (scaledScreenLocation.width)) * (double) columns);
				int textY = (int) (((double) (y - scaledScreenLocation.y) / (double) (scaledScreenLocation.height)) * (double) rows);

				return new Point(textX, textY);
			} else {
				return null;
			}
		}
	}

	public boolean isLockAspect() {
		return lockAspect;
	}

	public void setLockAspect(boolean lockAspect) {
		this.lockAspect = lockAspect;
	}

	public int getColumns() {
		return columns;
	}

	public int getRows() {
		return rows;
	}
}
