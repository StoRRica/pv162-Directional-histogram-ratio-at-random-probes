package projekt;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.process.ByteProcessor;

public interface TresholdValues{

	public int[] getTresholds(int[] histogram, int countOfPixels);

	public ImageProcessor doInterpolation(int[][] values, ImageProcessor ip);

}