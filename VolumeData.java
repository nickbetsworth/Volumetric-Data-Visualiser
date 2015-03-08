import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/*
 * Name: Nicholas Betsworth
 * All of the code found within this file is my own work 
 * (besides some of the base code we were provided with by Mark Jones)
 */

public class VolumeData {
	// Used to determine which axis an image or calculation is based off
	public enum Axis {
		X, Y, Z;
	}
	//Used to determine what sampling method is to be used
	public enum Interpolation {
		Linear("Linear"), NearestNeighbour("Nearest Neighbour");
		
		private final String stringVal;
		private Interpolation(String stringVal) {
			this.stringVal = stringVal;
		}
		
		public String toString() {
			return stringVal;
		}
	}
	
	private static final int DEFAULT_DATA_WIDTH = 256;
	private static final int DEFAULT_DATA_HEIGHT = 113;
	private static final int DEFAULT_DATA_DEPTH = 256;
	
	// Store the dimensions of our data
	private int dataWidth;
	private int dataHeight;
	private int dataDepth;
	
	// Array to store all of the image data
	private short[][][] imageData;
	// Stores the minimum and maximum values found in the data set
	private short min;
	private short max;
	private short mipThreshold;
	
	private Color color;
	
	private short[] histogramMapping;
	
	public VolumeData(String filename) throws IOException {
		this(filename, DEFAULT_DATA_WIDTH, DEFAULT_DATA_HEIGHT, DEFAULT_DATA_DEPTH);
	}
	
	/*
	 * Allow different sized sets of volume data to be used by our class
	 */
	public VolumeData(String filename, int dataWidth, int dataHeight, int dataDepth) throws IOException {
		this.dataWidth = dataWidth;
		this.dataHeight = dataHeight;
		this.dataDepth = dataDepth;
		
		imageData = new short[dataHeight][dataDepth][dataWidth];
		
		// Initialise all variables and load in data
		// Read in the volume data
		File f = new File(filename);
		
		DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(f)));
		
		min = Short.MAX_VALUE;
		max = Short.MIN_VALUE;
		
		for(int z = 0; z < dataHeight; z++) {
			for(int y = 0; y < dataDepth; y++) {
				for(int x = 0; x < dataWidth; x++) {
					/*
					 * & 0xFF because there is no unsigned data types in java
					 * This masks out extra bits, leaving us with just 8
					 */
					short c1 = (short) (in.readByte() & 0xFF);
					short c2 = (short) (in.readByte() & 0xFF);
					
					// Reverse the order so c2 is first by bit shifting left and then joining c1 on
					short c3 = (short) ((c2 << 8) | c1);
					
					// Update the min and max values if necessary
					if(c3 > max) {
						max = c3;
					}
					if(c3 < min) {
						min = c3;
					}
					
					// Store this value in the appropriate array position
					imageData[z][y][x] = c3;
				}
			}
		}
		
		in.close();
		//You can change the colour if you wish
		//color = new Color(0, 255, 50);
		color = new Color(255,255,255);
		
		//Set the threshold to maximum by default
		mipThreshold = max;
		histogramMapping = getEqualizationMapping();
	}
	
	public BufferedImage getRotatedImage(BufferedImage image, double angleP, double angleQ, double angleR, Interpolation interpolation) {
		//long startTime = System.currentTimeMillis();
		double matrixP[][] = {	{1, 0, 0},
								{0, Math.cos(angleP), -Math.sin(angleP)},
								{0, Math.sin(angleP), Math.cos(angleP)}};
		double matrixQ[][] = {	{Math.cos(angleQ), 0, Math.sin(angleQ)},
								{0, 1, 0},
								{-Math.sin(angleQ), 0, Math.cos(angleQ)}};
		double matrixR[][] = {	{Math.cos(angleR), -Math.sin(angleR), 0},
								{Math.sin(angleR), Math.cos(angleR), 0},
								{0, 0, 1}};
		
		double matrixN[][] = multiply(multiply(matrixP, matrixQ), matrixR);
		
		byte[] thisImageData = getImageData(image);
		
		int w = image.getWidth();
		int h = image.getHeight();
		
		float wr = (float)dataWidth / (float)w;
		float hr = (float)dataHeight / (float)h;
		
		for(int z = 0; z < h; z++) {
			for(int y = 0; y < w; y++) {
				short dataMax = Short.MIN_VALUE;
				for(int x = -(dataWidth / 2); x < (dataWidth / 2); x++) {
					float scaledZ = (z * hr) - (dataHeight / 2);
					float scaledY = (y * wr) - (dataDepth / 2);
					
					float newX = (float) (matrixN[0][0] * x + matrixN[0][1] * scaledY + matrixN[0][2] * scaledZ);
					float newY = (float) (matrixN[1][0] * x + matrixN[1][1] * scaledY + matrixN[1][2] * scaledZ);
					float newZ = (float) (matrixN[2][0] * x + matrixN[2][1] * scaledY + matrixN[2][2] * scaledZ);
					
					newX += (dataWidth / 2);
					newY += (dataDepth / 2);
					newZ += (dataHeight / 2);
					
					short val;
					
					if(newX < 0 || newY < 0 || newZ < 0 ||
							newX >= dataWidth || newY >= dataDepth || newZ >= dataHeight) {
						val = min;
					} else {
						if(interpolation == Interpolation.NearestNeighbour || (wr == 1 && hr == 1)) {
							val = imageData[(int)newZ][(int)newY][(int)newX];
						} else if(interpolation == Interpolation.Linear) {
							val = getTrilinearInterpValue(newX, newY, newZ);
						} else {
							val = min;
						}
					}
					
					if(val > dataMax) {
						dataMax = val;
						
						//If we have passed the threshold then break out of the loop
						if(val > mipThreshold) {
							break;
						}
					}
				}
				
				Color c = getRGB(dataMax);
				thisImageData[(3 * y) + (3 * z * w)] = (byte) c.getBlue();
				thisImageData[(3 * y) + (3 * z * w) + 1] = (byte) c.getGreen();
				thisImageData[(3 * y) + (3 * z * w) + 2] = (byte) c.getRed();
			}
		}
		//long runTime = System.currentTimeMillis() - startTime;
		
		//System.out.println("Image took " + runTime + " milliseconds to generate");
		
		return image;
	}
	
	/*
	 * Returns/updates a BufferedImage with the specified slice on the specified axis
	 */
	public BufferedImage sliceImage(BufferedImage image, Axis a, int slice, Interpolation interpolation, boolean equalize) {
		int w = image.getWidth();
		int h = image.getHeight();
		
		// Work out the width and height ratio
		float wr = 0;
		float hr = 0;
		switch(a) {
		case X:
			wr = (float)dataWidth / (float)w;
			hr = (float)dataHeight / (float)h;
			break;
		case Y:
			wr = (float)dataDepth / (float)w;
			hr = (float)dataHeight / (float)h;
			break;
		case Z:
			wr = (float)dataWidth / (float)w;
			hr = (float)dataDepth / (float)h;
			break;
		default:
			throw new IllegalArgumentException("Invalid axis specified: " + a);
		}
		
		byte[] thisImageData = getImageData(image);
		
		for(int y = 0; y < h; y++) {
			for(int x = 0; x < w; x++) {
				float scaledX = (float)x * wr;
				float scaledY = (float)y * hr;
				
				//Set the value to the minimum by default
				short val;
				switch(interpolation) {
				case Linear:
					val = getBilinearInterpValue(scaledX, scaledY, slice, a);
					break;
				case NearestNeighbour:
					val = getValue(scaledX, scaledY, slice, a);
					break;
				default:
					val = min;
					break;
				}
				
				if(equalize) {
					//Bound the index to 0 and max, it's possible to get < 0 for resized images
					int index = val - min;
					
					if(index < 0) {
						index = 0;
					} else if(index >= histogramMapping.length) {
						index = histogramMapping.length - 1;
					}
					
					val = histogramMapping[index];
				}
				Color c = getRGB(val);
				thisImageData[(3 * x) + (3 * y * w)] = (byte) c.getBlue();
				thisImageData[(3 * x) + (3 * y * w) + 1] = (byte) c.getGreen();
				thisImageData[(3 * x) + (3 * y * w) + 2] = (byte) c.getRed();
			}
		}
		return image;
	}
	
	public short getBilinearInterpValue(float x, float y, int slice, Axis a) {
		int maxX;
		int maxY;
		switch(a) {
		case X:
			maxX = dataDepth;
			maxY = dataHeight;
			break;
		case Y:
			maxX = dataWidth;
			maxY = dataHeight;
			break;
		case Z:
			maxX = dataWidth;
			maxY = dataDepth;
			break;
		default:
			maxX = 0;
			maxY = 0;
			break;
		}
		
		float x1 = 0, x2 = 0, y1 = 0, y2 = 0;
		if(x == 0) {
			x2 = 1;
		} else if(x > maxX - 1) {
			x2 = maxX - 1;
		} else {
			x2 = (float) Math.ceil(x);
		}
		
		if(y == 0) {
			y2 = 1;
		} else if(y > maxY - 1) {
			y2 = maxY - 1;
		} else {
			y2 = (float) Math.ceil(y);
		}
		
		x1 = x2 - 1;
		y1 = y2 - 1;
		
		float x1y1 = getValue(x1, y1, slice, a);
		float x2y1 = getValue(x2, y1, slice, a);
		float x1y2 = getValue(x1, y2, slice, a);
		float x2y2 = getValue(x2, y2, slice, a);
		
		float xRatio1 = (x - x1) / (x2 - x1);
		float xRatio2 = (x2 - x) / (x2 - x1);
		float x1Interp = xRatio2 * x1y1 + xRatio1 * x2y1;
		float x2Interp = xRatio2 * x1y2 + xRatio1 * x2y2;
		
		short result = (short) (((y2 - y) / (y2 - y1)) * x1Interp + ((y - y1) / (y2 - y1)) * x2Interp);
		
		return result;
	}
	
	public short getTrilinearInterpValue(float x, float y, float z) {
		float x1, x2, y1, y2, z1, z2;
		// If we encounter some pixels on the border, we will ignore them for 
		// the sake of efficiency as they are generally of minimum value
		if(x == 0 || y == 0 || z == 0 ||
				x > dataWidth - 1 || y > dataDepth - 1 || z > dataHeight - 1)
			return min;

		x2 = (float) Math.ceil(x);
		y2 = (float) Math.ceil(y);
		z2 = (float) Math.ceil(z);
		
		x1 = x2 - 1;
		y1 = y2 - 1;
		z1 = z2 - 1;
		
		float xRatio = (x - x1) / (x2 - x1);
		float yRatio = (y - y1) / (y2 - y1);
		float zRatio = (z - z1) / (z2 - z1);
		
		float x1y1z1 = imageData[(int)z1][(int)y1][(int)x1];
		float x2y1z1 = imageData[(int)z1][(int)y1][(int)x2];
		float x1y2z1 = imageData[(int)z1][(int)y2][(int)x1];
		float x2y2z1 = imageData[(int)z1][(int)y2][(int)x2];
		float x1y1z2 = imageData[(int)z2][(int)y1][(int)x1];
		float x1y2z2 = imageData[(int)z2][(int)y2][(int)x1];
		float x2y1z2 = imageData[(int)z2][(int)y1][(int)x2];
		float x2y2z2 = imageData[(int)z2][(int)y2][(int)x2];
		
		float c00 = x1y1z1 * (1 - xRatio) + x2y1z1 * xRatio;
		float c10 = x1y2z1 * (1 - xRatio) + x2y2z1 * xRatio;
		float c01 = x1y1z2 * (1 - xRatio) + x2y1z2 * xRatio;
		float c11 = x1y2z2 * (1 - xRatio) + x2y2z2 * xRatio;
		
		float c0 = c00 * (1 - yRatio) + c10 * yRatio;
		float c1 = c01 * (1 - yRatio) + c11 * yRatio;
		
		float c = c0 * (1 - zRatio) + c1 * zRatio;
		
		return (short) c;
	}
	
	public short[][][] resizeData(int newWidth, int newDepth, int newHeight) {
		float wr = (float)dataWidth / (float)newWidth;
		float dr = (float)dataDepth / (float)newDepth;
		float hr = (float)dataHeight / (float)newHeight;
		
		short[][][] newImageData = new short[newHeight][newDepth][newWidth];
		
		for(int z = 0; z < newHeight; z++) {
			for(int y = 0; y < newDepth; y++) {
				for(int x = 0; x < newWidth; x++) {
					newImageData[z][y][x] = getTrilinearInterpValue((float) x * wr, (float) y * dr, (float) z * hr);
				}
			}
		}
		
		return newImageData;
	}
	public short[] getEqualizationMapping() {
		//Have to use integers here as the values are too large for short
		int[] histogram = new int[(max - min) + 1];
		short[] mapping = new short[(max - min) + 1];
		//Total is equivalent of t in notes, but without use of array
		int total = 0;
		//Size of our data set
		int size = dataHeight * dataDepth * dataWidth;
		
		//Initialise the histogram array
		for(int i = 0; i < histogram.length; i++) {
			histogram[i] = 0;
		}
		
		for(int z = 0; z < dataHeight; z++) {
			for(int y = 0; y < dataDepth; y++) {
				for(int x = 0; x < dataWidth; x++) {
					short val = imageData[z][y][x];
					histogram[val - min]++;
				}
			}
		}
		
		for(int i = 0; i < histogram.length; i++) {
			total += histogram[i];
			//We keep the mapping to our initial range of min -> max rather than using 0 -> 255
			mapping[i] = (short) ((max - min) * ((float)total / (float)size) + min);
		}
		
		return mapping;
	}
	/*
	 * Returns a value from the data set using the specified axis
	 */
	public short getValue(int x, int y, int slice, Axis a) {
		switch(a) {
		case X:
			return imageData[y][x][slice];
		case Y:
			return imageData[y][slice][x];
		case Z:
			return imageData[slice][y][x];
		default:
			return -1;
		}
	}
	
	/*
	 * Overload our getValue method to enable us to pass float co-ordinates to make calls a bit shorter
	 */
	public short getValue(float x, float y, int slice, Axis a) {
		return getValue((int)x, (int)y, slice, a);
	}
	
	/*
	 * Returns a pointer to our image data for quick manipulation
	 */
	public static byte[] getImageData(BufferedImage image) {
		WritableRaster r = image.getRaster();
		DataBuffer db = r.getDataBuffer();
		
		if(db.getDataType() != DataBuffer.TYPE_BYTE) {
			throw new IllegalStateException("Data is not of type byte");
		} else {
			// Cast the data buffer to a byte data buffer
			DataBufferByte dbb = (DataBufferByte) db;
			return dbb.getData();
		}
	}
	
	public short[][][] getImageData() {
		return imageData;
	}
	public void setImageData(short[][][] imageData) {
		this.imageData = imageData;
		dataHeight = imageData.length;
		dataDepth = imageData[0].length;
		dataWidth = imageData[0][0].length;
		
		//Re calculate the min and max as it is possible it has changed
		min = Short.MAX_VALUE;
		max = Short.MIN_VALUE;
		for(int z = 0; z < dataHeight; z++) {
			for(int y = 0; y < dataDepth; y++) {
				for(int x = 0; x < dataWidth; x++) {
					short val = imageData[z][y][x];

					if(val > max) {
						max = val;
					}
					if(val < min) {
						min = val;
					}
				}
			}
		}
		
		//Histogram has also probably changed
		histogramMapping = getEqualizationMapping();
	}
	/*
	 * Returns an RGB Color based off a value from the data set
	 * Uses the color assigned in this class
	 */
	public Color getRGB(short val) {
		// Interpolation may give us values above or below the min/max
		// So we ensure they're within the expected range here
		if(val < min)
			val = min;
		if(val > max)
			val = max;
		
		float fratio = ((float)val - (float)min) / (max - min);
		int r = (int) (color.getRed() * fratio);
		int g = (int) (color.getGreen() * fratio);
		int b = (int) (color.getBlue() * fratio);
		
		return new Color(r, g, b);
	}
	public int getDataWidth() {
		return dataWidth;
	}
	public int getDataHeight() {
		return dataHeight;
	}
	public int getDataDepth() {
		return dataDepth;
	}
	public short getMinValue() {
		return min;
	}
	public short getMaxValue() {
		return max;
	}
	public void setMIPThreshold(short mipThreshold) {
		this.mipThreshold = mipThreshold;
	}
	public Color getColor() {
		return color;
	}
	public void setColor(Color color) {
		this.color = color;
	}
	
	public static double[][] multiply(double[][] matrixP, double[][] matrixQ) {
		int pRows = matrixP.length;
		int pCols = matrixP[0].length;
		int qCols = matrixQ[0].length;
		double result[][] = new double[pRows][qCols];
		
		for(int i = 0; i < pRows; i++) {
			for(int j = 0; j < qCols; j++) {
				for(int k = 0; k < pCols; k++) {
					result[i][j] += matrixP[i][k] * matrixQ[k][j];
				}
			}
		}
		
		return result;
	}
}
