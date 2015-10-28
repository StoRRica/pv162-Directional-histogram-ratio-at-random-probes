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
	private int dNeighboursDistance = 4; /*length od the d-neighobour distance*/

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
		int percentage = histMax / 5;
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
		int[] result = new int[8]; 
		int[] tmp;
		int width = ip.getWidth();
		int height = ip.getHeight();
		Random rnd = new Random();
		int posx,posy;
		for (int i = 0 ; i< numOfProbes;i++){
			do {
				posx = rnd.nextInt(width);
			    posy = rnd.nextInt(height);} 
			while( ip.get(posx,posy) == 0);
			tmp = dNeighbours(ip,posx,posy,dNeighboursDistance);
			for (int j = 0; j<8;j++){
				result[j]+=tmp[j];
			}
		}

		return result;
	}
	/* 0 is left middle, 1 is left upper, 2 is upper, 3 is right upper, 4 is right middle, 5 is right down, 6 is down, 7 is left down*/
	private int[] dNeighbours(ImageProcessor ip, int x, int y, int distance){
		int[] result = new int[8];
		result[0] = getNeighbour(ip , x-distance, y);
		result[1] = getNeighbour(ip, x-distance,y-distance);
		result[2] = getNeighbour(ip,x,y-distance);
		result[3] = getNeighbour(ip,x+distance,y-distance);
		result[4] = getNeighbour(ip,x+distance,y);
		result[5] = getNeighbour(ip,x+distance,y+distance);
		result[6] = getNeighbour(ip,x,y+distance);
		result[7] = getNeighbour(ip,x-distance,y+distance);
		return result;
	}	

	int getNeighbour(ImageProcessor ip, int x, int y){
		int width = ip.getWidth();
		int height = ip.getHeight();
		if( (x<width)&&(x>0)&&(y<height)&&(y>0)){
			return ip.get(x,y) > 0 ? 1 : 0;
		}else{return 0;}
	}

	int[] getMinMax(int[] input){
		int min = numOfProbes+1;
		int max = 0;

		for (int i = 0; i < input.length;i++){
			if (input[i]<min){min = input[i];}
			if (input[i]>max){max = input[i];}
		}
		int[] res={min,max};
		return res;
	}

	private void cropImage(ImageProcessor ip){
		int cropWidth = ip.getWidth() / numOfWindows;
		int cropHeight = ip.getHeight() / numOfWindows;
		int[] res;
		int[] minMax;
		float Dr;
		ImageProcessor cropped;
		for (int i = 0; i < numOfWindows; i++){
			for (int j = 0; j<numOfWindows;j++){
				ip.setRoi(cropWidth * i, cropHeight * j, cropWidth, cropHeight);
				cropped = ip.crop();
				res = new int[8];
				res= PlantProbes(cropped);
				minMax = getMinMax(res);
				Dr = minMax[1] / minMax[0];
				new ImagePlus("croppedImage" + i +" " + j + " Dr :" + Dr, cropped).show();
			}
		}

	}
}