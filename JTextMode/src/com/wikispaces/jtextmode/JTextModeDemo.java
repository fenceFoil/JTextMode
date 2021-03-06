package com.wikispaces.jtextmode;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

public class JTextModeDemo extends JFrame {

	private JPanel contentPane;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					JTextModeDemo frame = new JTextModeDemo();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 * @throws MissingCodePageException 
	 * @throws IllegalArgumentException 
	 */
	public JTextModeDemo() throws IllegalArgumentException, MissingCodePageException {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 495, 344);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);

		final JTextModeComponent c = new JTextModeComponent(20, 40, true, true, true);
		c.requestFocusInWindow();

		contentPane.add(c);

		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		c.setDrawColor(12);
		c.writeLn("Hello world! I am a very long string of text that should wrap around the edge of the screen.");
		c.writeLn();
		c.setDrawColor(14);
		c.setDrawBGColor(4);
		c.setDrawBlinking(true);
		c.writeLn("And scroll.");
		c.setCursorPos(20, 20);
		c.write("Yes or no? ");
		c.setCursorPos(0, 10);
		c.setDrawColor(15);
		c.setDrawBGColor(0);
		c.setDrawBlinking(false);
		c.write("C:\\>");
	}

}
