package cn.edu.hunau.itc.imgrcg.ip;

import java.awt.Rectangle;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import org.jblas.DoubleMatrix;

import ij.CompositeImage;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.MaximumFinder;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.filter.RankFilters;
import ij.plugin.frame.RoiManager;
import ij.process.AutoThresholder.Method;
import ij.process.ByteProcessor;
import ij.process.ColorSpaceConverter;
import ij.process.ImageProcessor;

/**
 * Image pre processor.
 * 
 * @author liuyaoduan
 *
 */
public class Processor {
	private String imagePath;
	private String resultPath;
	
	private static Processor processor;
	
	/**
	 * Get a Processor to process image. Singleton mode.
	 * 
	 * @param imagePath The path of image file you will be processing. 
	 * @param resultPath The path of results you want to save.
	 * @return A Processor.
	 */
	public static Processor getProcessor(String imagePath, String resultPath) {
		if (Processor.processor == null) {
			Processor.processor = new Processor();
		}
		
		Processor.processor.imagePath = imagePath;
		Processor.processor.resultPath = resultPath;
		
		return Processor.processor;
	}
	
	/**
	 * Save image using the resultPath you defined in this class.
	 * 
	 * @param imp Image.
	 * @param fileName The name of saved file.
	 * @return Saving success(true) or failed(false).
	 */
	private boolean saveImage(ImagePlus imp, String fileName) {
		FileSaver saver = new FileSaver(imp);
		return saver.saveAsJpeg(resultPath + fileName + ".jpg");
	}
	/**
	 * Save image using the image title you defined in this class, file name is the title of image.
	 * 
	 * @param imp Image.
	 * @return Saving success(true) or failed(false).
	 */
	private boolean saveImage(ImagePlus imp) {
		return this.saveImage(imp, imp.getTitle());
	}
	
	/**
	 * Extend selection. 
	 * @param rect Raw selection.
	 * @param outline Extend pixels.
	 * @return New selection after extends.
	 */
	private Rectangle extendSelection(Rectangle rect, int outline) {
		rect.x = rect.x - outline;
		rect.width = rect.width + (outline * 2);
		rect.y = rect.y - outline;
		rect.height = rect.height + (outline * 2);

		return rect;
	}
	
	/**
	 * Normalization the data from the given column of Resultstable. 
	 * @param table Resultstable.
	 * @param columnIndex The column you will be normalized.
	 */
	private void normalization(ResultsTable table, int columnIndex) {
		DoubleMatrix matrix = null;
		double minus = 0;
		double min = 0;
		
		matrix = new DoubleMatrix(table.getColumnAsDoubles(columnIndex));
		minus = matrix.max() - matrix.min();
		min = matrix.min();
		matrix = matrix.sub(min).div(minus);
		
		for (int i = 0; i < matrix.data.length; i++) {
			table.setValue(columnIndex, i, matrix.data[i] * 2);
		}
	}
	
	/**
	 * Set all columns from results table to a decimal.
	 * @param table Resultstable.
	 * @param digits The number of decimal.
	 * @return After setting decimal's results table.
	 */
	private ResultsTable setDecimal(ResultsTable table, int digits) {
		for (int index = 0; index <= table.getLastColumn(); index++) {
			table.setDecimalPlaces(index, digits);
		}
		return table;
	}
	
	
	/**
	 * Processing
	 */
	public void process() {
		ImagePlus image = this.loadImage();
		CompositeImage stack = this.convertToLab(image);
		ByteProcessor maxProcessor = this.findMaximum(stack.getProcessor(2));
		
		ImagePlus maxTolerance = new ImagePlus("max-tolerance", maxProcessor);
		this.saveImage(maxTolerance);
		
		RoiManager manager = new RoiManager(false);
		ResultsTable table = this.particleAnalyzer(manager, maxTolerance);
		
		RankFilters filter = new RankFilters();
		int[][] pixels = new int[manager.getCount()][];
		Rectangle[] rects = new Rectangle[manager.getCount()];
		for (int i = 0; i < manager.getCount(); i++) {
			Roi roi = manager.getRoi(i);
			Rectangle rect = this.extendSelection(roi.getBounds(), 2);
			rects[i] = rect;
			
			image.setRoi(rect);
			ImagePlus raw = image.crop();
			raw.setTitle(roi.getName() + "-raw");
			this.saveImage(raw);
			
			ByteProcessor resultProcessor = this.scale(stack.getProcessor(1), rect);
			
			ImagePlus scale = new ImagePlus(roi.getName() + "-crop", resultProcessor);
			this.saveImage(scale);
			
			this.threshold(resultProcessor);
			this.medianFilter(resultProcessor, filter);
			
			ImagePlus bin = new ImagePlus(roi.getName() + "-bin", resultProcessor);
			this.saveImage(bin);
			
			pixels[i] = this.getPixelGray(resultProcessor);
			
			double[] gray = this.getMeanGray(manager, stack, i);
			table.setValue("a", i, gray[1]);
			table.setValue("b", i, gray[2]);
		}
		
		ResultsTable result = this.getResult(table);
		
		try {
			this.writeResults(result, pixels, rects);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Load image from imagePath you defined in this class.
	 * @return Image.
	 */
	private ImagePlus loadImage() {
		return new ImagePlus(this.imagePath);
	}
	
	/**
	 * Convert image to Lab color space.
	 * @param image Image to be converted.
	 * @return The Lab stack.
	 */
	private CompositeImage convertToLab(ImagePlus image) {
		ColorSpaceConverter converter = new ColorSpaceConverter();
		ImagePlus compositeImage = converter.RGBToLab(image);
		
		return new CompositeImage(compositeImage);
	}
	
	/**
	 * FindMaxima, noise-tolerance is 10px.
	 * 
	 * @param processor Image processor.
	 * @return Indexed image processor.
	 */
	private ByteProcessor findMaximum(ImageProcessor processor) {
		MaximumFinder finder = new MaximumFinder();
		ByteProcessor maxProcessor = finder.findMaxima(processor, 10, MaximumFinder.IN_TOLERANCE, true);
		maxProcessor.invertLut();
		
		return maxProcessor;
	}
	
	/**
	 * Particle analyze the image.
	 * @param manager Roi manager.
	 * @param image Image to be analyzed.
	 * @return Results table after analyzed.
	 */
	private ResultsTable particleAnalyzer(RoiManager manager, ImagePlus image) {
		ResultsTable table = new ResultsTable();
		ParticleAnalyzer analyzer = new ParticleAnalyzer();
		
		ParticleAnalyzer.setRoiManager(manager);
		analyzer = new ParticleAnalyzer(
				ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES | ParticleAnalyzer.SHOW_MASKS | ParticleAnalyzer.ADD_TO_MANAGER,
				Measurements.SHAPE_DESCRIPTORS, table, 0, Integer.MAX_VALUE, 0.0, 1.0);
		analyzer.setHideOutputImage(true);
		
		analyzer.analyze(image);
		
		return table;
	}
	
	/**
	 * Scale image to 20 * 20 pixels.
	 * @param processor image processor.
	 * @param rect roi's coordinate.
	 * @return Indexed image processor.
	 */
	private ByteProcessor scale(ImageProcessor processor, Rectangle rect) {
		processor.setRoi(rect);
		
		ByteProcessor resultProcessor = processor.crop().convertToByteProcessor();
		resultProcessor.setInterpolationMethod(ImageProcessor.BICUBIC);
		resultProcessor = resultProcessor.resize(20, 20, true).convertToByteProcessor();
		
		return resultProcessor;
	}
	
	/**
	 * Threshold use Mean method, and update Lut to Binary.
	 * @param processor Indexed image processor.
	 */
	private void threshold(ByteProcessor processor) {
		processor.setAutoThreshold(Method.Mean, false, ImageProcessor.BLACK_AND_WHITE_LUT);
		processor.applyLut();
	}
	
	/**
	 * Median filter, 1px.
	 * @param processor Indexed image processor.
	 * @param filter RankFilter.
	 */
	private void medianFilter(ByteProcessor processor, RankFilters filter) {
		filter.rank(processor, 1.0, RankFilters.MEDIAN);
	}
	
	/**
	 * Get each channel's mean gray value from selected roi in image.
	 * @param manager roi manager.
	 * @param image Multi channel image.
	 * @param index selected roi from roi manager.
	 * @return
	 */
	private double[] getMeanGray(RoiManager manager, CompositeImage image, int index) {
		manager.select(image, index);
		
		ResultsTable grayValueTable = manager.multiMeasure(image);
		double[] mean = grayValueTable.getColumnAsDoubles(grayValueTable.getColumnIndex("Mean1"));
		
		mean[1] = (mean[1] + 86.179443359) / (86.179443359 + 98.260360718) * 2;
		mean[2] = (mean[2] + 107.859588623) / (107.859588623 + 94.484642029) * 2;
		
		return mean;
	}
	
	/**
	 * Get binary image pixels value, 0 = 0, 255 = 1.
	 * 
	 * @param processor Indexed image processor.
	 * @return Pixel values array. 
	 */
	private int[] getPixelGray(ByteProcessor processor) {
		int width = processor.getWidth();
		int height = processor.getHeight();
		
		int[] pixels = new int[width * height];
		
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				pixels[y * height + x] = (processor.getPixel(x, y) == 0) ? 0 : 1;
			}
		}
		
		return pixels;
	}
	
	/**
	 * Delete some unusable data, normalization data.
	 * 
	 * @param table Before processing result table.
	 * @return A new processed result table.
	 */
	private ResultsTable getResult(ResultsTable table) {
		ResultsTable summaryTable = new ResultsTable();

		double[] ar = table.getColumnAsDoubles(table.getColumnIndex("AR"));
		double[] a = table.getColumnAsDoubles(table.getColumnIndex("a"));
		double[] b = table.getColumnAsDoubles(table.getColumnIndex("b"));
		
		for (int part = 0; part < table.size(); part++) {
			summaryTable.incrementCounter();
			
			summaryTable.setValue("AR", part, (((ar[part] > 5) ? 5 : ar[part]) - 1) / 2);
			summaryTable.setValue("a", part, (a[part] < 0) ? 0 : (a[part] > 2)? 2 : a[part]);
			summaryTable.setValue("b", part, (b[part] < 0) ? 0 : (b[part] > 2)? 2 : b[part]);
		}
		
		this.normalization(summaryTable, summaryTable.getColumnIndex("a"));
		this.normalization(summaryTable, summaryTable.getColumnIndex("b"));
		
		this.setDecimal(summaryTable, 9);
		
		return summaryTable;
	}
	
	/**
	 * Write all results to files.
	 * 
	 * @param table Contains AR, a, b.
	 * @param pixels Binary image pixel.
	 * @param rects The coordinate of features. 
	 * @throws IOException
	 */
	private void writeResults(ResultsTable table, int[][] pixels, Rectangle[] rects) throws IOException {
		FileWriter shapeFW = new FileWriter(new File(resultPath, "shape.txt"));
		BufferedWriter shapeBW = new BufferedWriter(shapeFW);
		FileWriter pixelFW = new FileWriter(new File(resultPath, "pixel.txt"));
		BufferedWriter pixelBW = new BufferedWriter(pixelFW);
		FileWriter rectFW = new FileWriter(new File(resultPath, "rect.txt"));
		BufferedWriter rectBW = new BufferedWriter(rectFW);
		
		for (int row = 0; row < table.size(); row++) {
			String s = table.getRowAsString(row);
			shapeBW.append(s.substring(s.indexOf("\t") + 1).replace("\t", ", ") + "\r\n");
		}
		for (int[] pixel : pixels) {
			String ps = Arrays.toString(pixel);
			pixelBW.append(ps.substring(1, ps.length() - 1) + "\r\n");
		}
		for (Rectangle rect : rects) {
			rectBW.append(String.format("[%d, %d, %d, %d]", rect.x, rect.y, rect.width, rect.height) + "\r\n");
		}
		
		shapeBW.close();
		shapeFW.close();
		pixelBW.close();
		pixelFW.close();
		rectBW.close();
		rectFW.close();
	}

}
