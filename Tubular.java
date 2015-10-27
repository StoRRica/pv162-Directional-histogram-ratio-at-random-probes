package projekt;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.process.ByteProcessor;


public class Tubular implements PlugInFilter {
	private int numOfProbes = 100;
	private String title;

	public int setup(String arg, ImagePlus im) 
	{
		
		if (im != null)
		{
			// store the short image title for future use
			title = im.getShortTitle();
			
			// this plugin accepts 8-bit grayscale images
			return DOES_8G ; 
		}
		else
		{
			// no image is open
			IJ.noImage();
			return DONE;
		}
	}

	

	public void run(ImageProcessor ip) {
		int[] hist = ip.getHistogram();
		int count = ip.getPixelCount();

		// show the intensity histogram as a new image
		int histImgHeight = 130;
		ImageProcessor histIp = new ByteProcessor(hist.length, histImgHeight);
		histIp.setValue(255);
		histIp.fill();
		
		int histMax = hist[0];
		for (int i = 1; i < hist.length; ++i)
		{
			if (hist[i] > histMax)
			{
				histMax = hist[i];
			}
		}

		if (histMax == 0)
		{
			IJ.error("Empty histogram!");
			return;
		}

		int start = 0;
		int idx = 0;
		int total = 0;
		int percentage = count / 5;
		for (int i = 0; i < hist.length; ++i)
		{
			start = histImgHeight - (int)((double) hist[i] / histMax * histImgHeight);
			if (total < percentage){
				total += hist[i];
				idx = i;
			}
			for (int y = start; y < histImgHeight; ++y)
			{
				histIp.set(i, y, 0);	
			}
		}

		ImagePlus histImg = new ImagePlus(String.format("My histogram of %s ", title), histIp);
		histImg.show();
		IJ.error("percentage: "+percentage+" count: "+total+" till index: "+idx);
		ImageProcessor c = ip.duplicate();
		for (int i = 0; i <count ;i++){
			if (c.get(i) < idx){
				c.set(i,0);
			}
		}
		ImagePlus changedPicture = new ImagePlus(String.format("My backgound change of %s ", title), c);
		changedPicture.show();
	}	

}