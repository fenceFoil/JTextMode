/**
 * 
 */
package com.wikispaces.jtextmode;

import java.awt.Point;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author BJ
 *
 */
public interface Terminal {

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
			throws IOException;

	public void resetCodePage() throws MissingCodePageException;

	/**
	 * @param text
	 */
	public void writeLn(String... text);

	/**
	 * 
	 * @param chars
	 */
	public void writeLn(char... chars);

	public void writeLn();

	public void write(String... text);

	public void write(char... chars);

	/**
	 * Sets a character on the text screen without moving the cursor.
	 * 
	 * @param x
	 * @param y
	 * @param character
	 *            return false if the x and y are out of bounds
	 */
	public boolean setCharAt(int x, int y, int character);

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
			int bgColor, boolean blink);

	public boolean setBlinkingAt(int x, int y, boolean blink);

	public boolean setBGColorAt(int x, int y, int bgColor);

	public boolean setColorAt(int x, int y, int color);

	public boolean setColorsAt(int x, int y, int color, int bgColor,
			boolean blink);

	/**
	 * 
	 * @param x
	 * @param y
	 * @return the character at x, y, or -1 if coords our out of bounds
	 */
	public int getCharAt(int x, int y);

	/**
	 * 
	 * @param x
	 * @param y
	 * @return the color at x, y, or -1 if coords our out of bounds
	 */
	public int getColorAt(int x, int y);

	/**
	 * 
	 * @param x
	 * @param y
	 * @return the background color at x, y, or -1 if coords our out of bounds
	 */
	public int getBGColorAt(int x, int y);

	/**
	 * 
	 * @param x
	 * @param y
	 * @return 1 if the character is blinking at x, y<br>
	 *         0 if not<br>
	 *         -1 if coords are out of range
	 */
	public int getBlinkingAt(int x, int y);

	/**
	 * Puts the cursor at the given character on the text screen.
	 * 
	 * @param x
	 * @param y
	 * @return false if the given cursor position is out of bounds; will set as
	 *         close as possible to given position anyway
	 */
	public boolean setCursorPos(int x, int y);

	/**
	 * Fills the screen with spaces written with the current draw colors and
	 * blinking status.
	 */
	public void clearScreen();

	public void scrollScreen(int scrollRows);

	// TODO: Range check set color methods
	public void setDrawColor(int i);

	public void setDrawBGColor(int bgColor);

	public void setDrawBlinking(boolean blinking);

	public void setShowMouseCursor(boolean show);

	/**
	 * Returns the character used on the code page to draw the cursor.
	 * 
	 * @return
	 */
	public int getCursorChar();

	/**
	 * Sets the character used on the code page to draw the cursor. If your code
	 * page doesn't put the cusor at 95 (underscore), change the character used
	 * here to match your code page.
	 * 
	 * @param cursorChar
	 */
	public void setCursorChar(int cursorChar);

	/**
	 * 
	 * @return
	 */
	public int getBlankChar();

	/**
	 * Sets the character used on the code page to draw empty space. If your
	 * code page doesn't have a blank space at 32 (ASCII space), set this to
	 * match your code page.
	 * 
	 * @param blankChar
	 */
	public void setBlankChar(int blankChar);

	/**
	 * Returns text screen coords (rows and columns) for given pixel coordinates
	 * within this component.
	 * 
	 * @param x
	 * @param y
	 * @return null if the aspect ratio is locked and the given position is
	 *         outside of the display area.
	 */
	public Point getTextCoordsForComponentCoords(int x, int y);

	public boolean isLockAspect();

	public void setLockAspect(boolean lockAspect);

	public int getColumns();

	public int getRows();
}
