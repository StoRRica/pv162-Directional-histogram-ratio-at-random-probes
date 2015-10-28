package projekt;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.process.ByteProcessor;
import java.util.Random;

public class Tubular implements PlugInFilter {
	private int numOfProbes = 100;
	private String title;
	private int numOfWindows = 3; /*the number will be squared*/

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

		/*ImagePlus histImg = new ImagePlus(String.format("My histogram of %s ", title), histIp);
		histImg.show();*/
		IJ.error("percentage: "+percentage+" count: "+total+" till index: "+idx);
		ImageProcessor c = ip.duplicate();
		for (int i = 0; i <count ;i++){
			if (c.get(i) < idx){
				c.set(i,0);
			}
		}
		/*kind of wiev for a development purposes only*/
		/*begin*/
		ImagePlus changedPicture = new ImagePlus(String.format("My backgound change of %s ", title), c);
		changedPicture.show();
		/*end*/
		//int[] res = PlantProbes(ip);
		cropImage(ip);
	}	

	private int[] PlantProbes(ImageProcessor ip){
		int[] result = new int[256]; 
		int width = ip.getWidth();
		int height = ip.getHeight();
		Random rnd = new Random();
		int posx,posy;
		for (int i = 0 ; i< numOfProbes;i++){
			posx = rnd.nextInt(width+1);
			posy = rnd.nextInt(height+1);
			result[ip.get(pos)]++;  
		}

		/*not supported yet*/
		return result;
	}

	private void cropImage(ImageProcessor ip){
		int cropWidth = ip.getWidth() / numOfWindows;
		int cropHeight = ip.getHeight() / numOfWindows;
		ImageProcessor cropped;
		for (int i = 0; i < numOfWindows; i++){
			for (int j = 0; j<numOfWindows;j++){
				ip.setRoi(cropWidth * i, cropHeight * j, cropWidth, cropHeight);
				cropped = ip.crop();
				int[] = PlantProbes(cropped);
				new ImagePlus("croppedImage" + i +" " + j, cropped).show();
			}
		}

	}
}