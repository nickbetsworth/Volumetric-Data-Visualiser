import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/*
 * Name: Nicholas Betsworth
 * All of the code found within this file is my own work 
 * (besides some of the base code we were provided with by Mark Jones)
 */

public class UserInterface extends JFrame {
	private final VolumeData vd;
	
	private static final int DEFAULT_IMAGE_WIDTH = 256;
	private static final int DEFAULT_IMAGE_HEIGHT = 256;
	
	// Declare our BufferedImages to store our images internally
	private BufferedImage imageX;
	private BufferedImage imageY;
	private BufferedImage imageZ;
	private BufferedImage imageRotated;
	
	// Declare all of our swing components
	private JLabel title;
	
	private JLabel imageXOut;
	private JLabel imageYOut;
	private JLabel imageZOut;
	private JLabel imageRotatedOut;
	
	private JSlider sliderX;
	private JSlider sliderY;
	private JSlider sliderZ;
	
	private JSlider sliderPitch;
	private JSlider sliderYaw;
	private JSlider sliderRoll;
	
	private JComboBox<VolumeData.Interpolation> interpMode;
	
	private JTextField inputWidth;
	private JTextField inputHeight;
	private JCheckBox inputEqualize;
	private JButton resetButton;
	private JButton updateButton;
	
	private JSlider mipThreshold;
	
	public UserInterface(VolumeData vd) {
		this.vd = vd;
		vd.setImageData(vd.resizeData(256, 256, 256));
		//Use this for testing sampling methods (gives us rougher images)
		//vd.setImageData(vd.resizeData(64, 64, 64));
		
		imageX 			= new BufferedImage(DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT, BufferedImage.TYPE_3BYTE_BGR);
		imageY 			= new BufferedImage(DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT, BufferedImage.TYPE_3BYTE_BGR);
		imageZ 			= new BufferedImage(DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT, BufferedImage.TYPE_3BYTE_BGR);
		imageRotated 	= new BufferedImage(DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT, BufferedImage.TYPE_3BYTE_BGR);
		
		JPanel mainPanel = new JPanel(new GridBagLayout());
		
		title = new JLabel("Volume Data Visualisation");
		title.setFont(new Font(null, Font.BOLD, 60));
		title.setHorizontalAlignment(JLabel.CENTER);
		
		imageXOut 		= new JLabel(new ImageIcon(imageX));
		imageYOut 		= new JLabel(new ImageIcon(imageY));
		imageZOut 		= new JLabel(new ImageIcon(imageZ));
		imageRotatedOut = new JLabel(new ImageIcon(imageRotated));
		
		sliderX = new JSlider(0, vd.getDataWidth() - 1);
		sliderY = new JSlider(0, vd.getDataDepth() - 1);
		sliderZ = new JSlider(0, vd.getDataHeight() - 1);
		
		sliderPitch = new JSlider(JSlider.HORIZONTAL, -180, 180, 0);
		sliderYaw 	= new JSlider(JSlider.HORIZONTAL, -180, 180, 0);
		sliderRoll 	= new JSlider(JSlider.HORIZONTAL, -180, 180, 0);
		
		JPanel settingsPanel = new JPanel(new GridLayout(6, 2, 0, 5));
		interpMode = new JComboBox<VolumeData.Interpolation>(VolumeData.Interpolation.values());
		inputWidth = new JTextField();
		inputWidth.setText(String.valueOf(DEFAULT_IMAGE_WIDTH));
		inputHeight = new JTextField();
		inputHeight.setText(String.valueOf(DEFAULT_IMAGE_HEIGHT));
		inputEqualize = new JCheckBox();
		resetButton = new JButton("Reset");
		updateButton = new JButton("Update");
		//Threshold is set to max by default
		mipThreshold = new JSlider(vd.getMinValue(), vd.getMaxValue(), vd.getMaxValue());
		//Make the slider smaller so it doesn't make the third sliced image get more space than the other 2
		mipThreshold.setPreferredSize(new Dimension(1, 1));
		settingsPanel.add(new JLabel("Interpolation Method:"));
		settingsPanel.add(interpMode);
		settingsPanel.add(new JLabel("Width:"));
		settingsPanel.add(inputWidth);
		settingsPanel.add(new JLabel("Height:"));
		settingsPanel.add(inputHeight);
		settingsPanel.add(new JLabel("Equalize:"));
		settingsPanel.add(inputEqualize);
		settingsPanel.add(new JLabel("MIP Threshold:"));
		settingsPanel.add(mipThreshold);
		settingsPanel.add(resetButton);
		settingsPanel.add(updateButton);
		
		sliderPitch.setMajorTickSpacing(180);
		sliderYaw.setMajorTickSpacing(180);
		sliderRoll.setMajorTickSpacing(180);
		sliderPitch.setMinorTickSpacing(45);
		sliderYaw.setMinorTickSpacing(45);
		sliderRoll.setMinorTickSpacing(45);
		
		sliderPitch.setPaintLabels(true);
		sliderYaw.setPaintLabels(true);
		sliderRoll.setPaintLabels(true);
		sliderPitch.setPaintTicks(true);
		sliderYaw.setPaintTicks(true);
		sliderRoll.setPaintTicks(true);
		
		// Set up all of the event handlers
		UserInputHandler h = new UserInputHandler();
		sliderX.addChangeListener(h);
		sliderY.addChangeListener(h);
		sliderZ.addChangeListener(h);
		sliderPitch.addChangeListener(h);
		sliderYaw.addChangeListener(h);
		sliderRoll.addChangeListener(h);
		resetButton.addActionListener(h);
		updateButton.addActionListener(h);
		
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(0, 5, 20, 5);
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 3;
		mainPanel.add(title, c);
		
		c.gridwidth = 1;
		c.gridy = 1;
		mainPanel.add(imageXOut, c);
		c.gridx = 1;
		mainPanel.add(imageYOut, c);
		c.gridx = 2;
		mainPanel.add(imageZOut, c);
		
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 2;
		mainPanel.add(sliderX, c);
		c.gridx = 1;
		mainPanel.add(sliderY, c);
		c.gridx = 2;
		mainPanel.add(sliderZ, c);
		
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 3;
		c.gridheight = 3;
		c.weighty = 3;
		mainPanel.add(imageRotatedOut, c);
		
		c.anchor = GridBagConstraints.LINE_START;
		c.gridheight = 1;
		c.gridx = 1;
		c.weighty = 1;
		mainPanel.add(sliderPitch, c);
		c.gridy = 4;
		mainPanel.add(sliderYaw, c);
		c.gridy = 5;
		mainPanel.add(sliderRoll, c);
		
		c.anchor = GridBagConstraints.NORTH;
		c.gridx = 2;
		c.gridy = 3;
		c.gridheight = 3;
		mainPanel.add(settingsPanel, c);
		
		// Update the images so they have some values initially
		imageX = vd.sliceImage(imageX, VolumeData.Axis.X, sliderX.getValue(), getInterpolationMode(), inputEqualize.isSelected());
		imageY = vd.sliceImage(imageY, VolumeData.Axis.Y, sliderY.getValue(), getInterpolationMode(), inputEqualize.isSelected());
		imageZ = vd.sliceImage(imageZ, VolumeData.Axis.Z, sliderZ.getValue(), getInterpolationMode(), inputEqualize.isSelected());
		imageRotated = vd.getRotatedImage(imageRotated, 0, 0, 0, getInterpolationMode());
		
		setContentPane(mainPanel);
		setTitle("Volume Data Visualisation");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		pack();
		setVisible(true);
	}
	
	//Work out the interpolation method selected
	private VolumeData.Interpolation getInterpolationMode() {
		//Use this rather than getSelectedItem as no casting is needed
		return interpMode.getItemAt(interpMode.getSelectedIndex());
	}
	
	private final class UserInputHandler implements ChangeListener, ActionListener {
		@Override
		public void stateChanged(ChangeEvent e) {
			if(e.getSource() == sliderPitch || e.getSource() == sliderYaw || e.getSource() == sliderRoll) {
				// Only update the rotated image when the sliders are all released
				// As rendering the rotated image is quite costly
				if(!sliderPitch.getValueIsAdjusting() && !sliderYaw.getValueIsAdjusting() && !sliderRoll.getValueIsAdjusting()) {
					imageRotated = vd.getRotatedImage(imageRotated, 
							Math.toRadians(sliderPitch.getValue()), 
							Math.toRadians(sliderYaw.getValue()), 
							Math.toRadians(sliderRoll.getValue()),
							getInterpolationMode());
					imageRotatedOut.setIcon(new ImageIcon(imageRotated));
				}
			} else if(e.getSource() == sliderX) {
				imageX = vd.sliceImage(imageX, VolumeData.Axis.X, sliderX.getValue(), getInterpolationMode(), inputEqualize.isSelected());
				imageXOut.setIcon(new ImageIcon(imageX));
			} else if(e.getSource() == sliderY) {
				imageY = vd.sliceImage(imageY, VolumeData.Axis.Y, sliderY.getValue(), getInterpolationMode(), inputEqualize.isSelected());
				imageYOut.setIcon(new ImageIcon(imageY));
			} else if(e.getSource() == sliderZ) {
				imageZ = vd.sliceImage(imageZ, VolumeData.Axis.Z, sliderZ.getValue(), getInterpolationMode(), inputEqualize.isSelected());
				imageZOut.setIcon(new ImageIcon(imageZ));
			}
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if(e.getSource() == resetButton) {
				interpMode.setSelectedIndex(0);
				inputWidth.setText(String.valueOf(DEFAULT_IMAGE_WIDTH));
				inputHeight.setText(String.valueOf(DEFAULT_IMAGE_HEIGHT));
				inputEqualize.setSelected(false);
				mipThreshold.setValue(mipThreshold.getMaximum());
				
				if(resizeImages()) {
					redrawImages();
				}
			} else if(e.getSource() == updateButton) {
				//Only redraw the images if there wasn't a problem resizing them
				if(resizeImages()) {
					redrawImages();
				}
			}
		}
	}
	
	private boolean resizeImages() {
		int width;
		int height;
		
		try {
			width = Integer.valueOf(inputWidth.getText());
			height = Integer.valueOf(inputHeight.getText());
		} catch(Exception e) {
			JOptionPane.showMessageDialog(this, "Invalid width or height input");
			return false;
		}
		
		if(width < 16 || height < 16 || width > 2048 || height > 2048) {
			JOptionPane.showMessageDialog(this, "Width and height must be between 16 and 2048");
			return false;
		}
		
		imageX 			= new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		imageY 			= new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		imageZ 			= new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		imageRotated 	= new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		
		//Set the new mip threshold too
		vd.setMIPThreshold((short) mipThreshold.getValue());
		
		return true;
	}
	private void redrawImages() {
		imageX = vd.sliceImage(imageX, VolumeData.Axis.X, sliderX.getValue(), getInterpolationMode(), inputEqualize.isSelected());
		imageXOut.setIcon(new ImageIcon(imageX));
		imageY = vd.sliceImage(imageY, VolumeData.Axis.Y, sliderY.getValue(), getInterpolationMode(), inputEqualize.isSelected());
		imageYOut.setIcon(new ImageIcon(imageY));
		imageZ = vd.sliceImage(imageZ, VolumeData.Axis.Z, sliderZ.getValue(), getInterpolationMode(), inputEqualize.isSelected());
		imageZOut.setIcon(new ImageIcon(imageZ));
		imageRotated = vd.getRotatedImage(imageRotated, 
				Math.toRadians(sliderPitch.getValue()), 
				Math.toRadians(sliderYaw.getValue()), 
				Math.toRadians(sliderRoll.getValue()),
				getInterpolationMode());
		imageRotatedOut.setIcon(new ImageIcon(imageRotated));
	}
}
