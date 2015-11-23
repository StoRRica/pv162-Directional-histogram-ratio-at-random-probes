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
	private int numOfWindows =15; /*the number will be squared*/
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

	class ImageWithTreshold{
		ImageProcessor procesor;
		int[][] treshold;
	}

	public void run(ImageProcessor ip) {
		int[] hist = ip.getHistogram();
		int count = ip.getPixelCount();

		
		/**/	
		int idx = 0;
		for (int i = 1; i < hist.length; ++i)
		{
			if (hist[i] > 0)
			{
				idx = i;
			}
		}

		if (idx == 0)
		{
			IJ.error("Empty histogram!");
			return;
		}

		int percentage =idx *5/100 ;
		int total = 0;	
		int[] myOtsu = otsuMulti(ip.getHistogram() /*hist*/,ip.getPixelCount());
		ImageProcessor c = ip.duplicate();
		for (int i = 0; i <count ;i++){
			if (c.get(i) < percentage){
				c.set(i,0);
				total++;
			}
		}
		//IJ.error("till idx: "+percentage+" number of changed pixels: "+total);
		/*kind of wiev for a development purposes only*/
		/*begin*/
		ImagePlus changedPicture = new ImagePlus(String.format("My backgound change of %s ", title), c);
		changedPicture.show();
		/*end*/
		//int[] res = PlantProbes(ip);
		ImageWithTreshold afterTreshold = executeCroping(c,myOtsu);
		ImagePlus treshold = new ImagePlus(String.format("My treshold change of %s ", title), afterTreshold.procesor);
		treshold.show();
	}	

	private int[] PlantProbes(ImageProcessor ip){
		int[] result = new int[8]; 
		int[] tmp;
		int width = ip.getWidth();
		int height = ip.getHeight();
		int tries = 0;
		Random rnd = new Random();
		int posx,posy;
		for (int i = 0 ; i< numOfProbes;i++){
			tries = 0;
			do {
				posx = rnd.nextInt(width);
			    posy = rnd.nextInt(height);
				tries++;} 
			while( (ip.get(posx,posy) == 0)||( tries < 150));
			if (ip.get(posx,posy) != 0){
				tmp = dNeighbours(ip,posx,posy,dNeighboursDistance);
				for (int j = 0; j<8;j++){
					result[j]+=tmp[j];
				}
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
		if (min == 0){min = 1;}
		int[] res={min,max};
		return res;
	}
	/*mena premennych a funkcii pomenit*/
	/*interpolacie prahov, pozriet a dorobit*/
	private ImageWithTreshold executeCroping(ImageProcessor ip, int[] otsuTresholds){
		int cropWidth = ip.getWidth() / numOfWindows;
		int cropHeight = ip.getHeight() / numOfWindows;
		double lower,upper;
		int[] res;
		int[] minMax;
		/*int[][] tresholds = new int[numOfWindows][];
		for (int i =0; i < numOfWindows;i++){
			tresholds[i] = new int[numOfWindows];
		}*/
		int[] myOtsu = otsuTresholds;
		double Dr;
		ImageProcessor result;
		result = ip.duplicate();
		ImageProcessor cropped;
		ImageWithTreshold returnValue = new ImageWithTreshold();
		returnValue.treshold = new int[numOfWindows][];
		for (int i =0; i< numOfWindows;i++){
			returnValue.treshold[i] = new int[numOfWindows];
		}
		ImageProcessor tresholdsImage = ip.duplicate();
		ImageProcessor upperLower = ip.duplicate();
		ImageProcessor cropped1;

		for (int i = 0; i < numOfWindows; i++){
			for (int j = 0; j<numOfWindows;j++){
				ip.setRoi(cropWidth * i, cropHeight * j, cropWidth, cropHeight);
				cropped = ip.crop();
				cropped1 = cropped.duplicate();
				res = new int[8];
				res = PlantProbes(cropped);
				minMax = getMinMax(res);
				Dr = (double)minMax[1] / (double)minMax[0];
				ImagePlus otsu = new ImagePlus("Pokus", cropped);
				IJ.setAutoThreshold(otsu,"Otsu dark");
        		lower = otsu.getChannelProcessor().getMinThreshold();
        		upper = otsu.getChannelProcessor().getMaxThreshold();
        		otsu.getChannelProcessor().resetThreshold();
        		
        		/*pozriet tie levely na otsu*/

        		if (Dr > 2 ){
        			otsu = makeTreshold(myOtsu[1],otsu);
        			//tresholds[i][j] = myOtsu[0];
        			returnValue.treshold[i][j]=myOtsu[1];
        			cropped1 = tresholdImage(cropped1, 255);
        		}else{
        			otsu = makeTreshold(myOtsu[0],otsu);
        			//tresholds[i][j] = myOtsu[1];
        			returnValue.treshold[i][j] = myOtsu[0];
        			cropped1 = tresholdImage(cropped1, 0);
        		} 

        		//IJ.error("onkno wid: "+i*cropWidth+" hei: "+j*cropHeight+"prahy nizs:"+myOtsu[0]+", vyssi: "+myOtsu[1]+", vybrany: "+returnValue.treshold[i][j]);
        		//IJ.error("Treshold for window "+i+" , "+j+" : "+returnValue.treshold[i][j]);
        		/*0 to copy over the pixels in  result*/
        		result.copyBits(cropped,cropWidth * i, cropHeight * j,0);
        		cropped = tresholdImage(cropped,returnValue.treshold[i][j]);
        		tresholdsImage.copyBits(cropped,cropWidth * i, cropHeight * j,0);
        		upperLower.copyBits(cropped1,cropWidth * i, cropHeight * j,0);
				//new ImagePlus("croppedImage" + i +" " + j + " Dr :" + Dr + " lower: " + myOtsu[0] +" upper: "+myOtsu[1] , cropped).show();
			}
						
		}
		
		returnValue.procesor = result;
		/*ImageProcessor afterInterpolation = doInterpolation(returnValue.treshold,ip);
		ImagePlus treshold = new ImagePlus(String.format("My bilinear interpolation treshold change of %s ", title), afterInterpolation);
		treshold.show();*/
		ImagePlus imageOfTresholds = new ImagePlus(String.format("My tresholds of %s ", title), tresholdsImage);
		imageOfTresholds.show();
		ImagePlus imageOfTresholds1 = new ImagePlus(String.format("upper lower treholds of %s ", title), upperLower);
		imageOfTresholds1.show();	
		return returnValue;
	}

	private ImageProcessor tresholdImage(ImageProcessor ip, int value){
		for (int i = 0; i < ip.getPixelCount();i++){
			ip.set(i,value);
		}
		return ip;
	}

	private ImageProcessor doInterpolation(int[][] values, ImageProcessor ip){
		int cropWidth = ip.getWidth() / numOfWindows;
		int crwh = cropWidth / 2 ;//polovica crop width
		int cropHeight = ip.getHeight() / numOfWindows;
		int crhh = cropHeight / 2 ; //polovica cropHeight
		int x,y,w,h;
		int tre1,tre2,tre3,tre4,x1,x2,y1,y2;
		for (int i = 0; i<ip.getWidth();i++){
			w = i - crwh;
			w = w<0?0:w;
			x = w / cropWidth;
			for (int j = 0; j<ip.getHeight();j++){
				h = j - crhh;
				h = h<0?0:h;
				y = h / cropWidth;
				x1 = x * cropWidth + crwh;
				x2 = x1 + cropWidth;
				y2 = y * cropHeight + crhh;
				y1 = y2 + cropHeight;
				if ((x == numOfWindows-1)||(y==numOfWindows-1)){
					if (x == numOfWindows-1){
						x1 = ip.getWidth() - crwh ;
						x2 = ip.getWidth();
						tre2 = values[x][y];
						tre1 = tre2;
						if (y == numOfWindows){
							y1 = ip.getHeight();
							y2 = y1 - crhh;
							tre3 = tre2; 
						} else{
							tre3 = values[x][y+1];
						}
						tre4 = tre3;
					}else{
						y1 = ip.getHeight();
						y2 = y1 - crhh;							
						if (i < crwh){
							x1 = 0;
							x2 = crwh;
							tre1 = values[0][y];
							tre2=tre1;
							tre3=tre1;
							tre4=tre1;
						}else{
							tre1 = values[x][y];
							tre2 = values[x+1][y];
							tre3 = tre1;
							tre4 = tre2;
						}
					}
				}else{
					tre1 = values[x][y];
					tre2 = values[x+1][y];
					tre3 = values[x][y+1];
					tre4 = values[x+1][y+1];
					if (i<crwh){
						tre3 = tre4;
						tre1 = tre2;
						x1 = 0;
						x2 = crwh;
					}

					if (j < crhh){
						tre3 = tre1;
						tre4 = tre2;
						y2 = 0;
						y1 = crhh; 
					}
				}
				double tresholdResult = interpolateBilinear(i,j,x1,y1,tre1,x2,y2,tre2,tre3,tre4);
				ip.set(i, j, ip.get(i,j) < tresholdResult?0:255);
			}
		}
		//ip.autoThreshold();
		return ip;
	}

	/*
	R1 = ((x2 – x)/(x2 – x1))*Q11 + ((x – x1)/(x2 – x1))*Q21

	R2 = ((x2 – x)/(x2 – x1))*Q12 + ((x – x1)/(x2 – x1))*Q22

	After the two R values are calculated, the value of P can finally be calculated by a weighted average of R1 and R2.

	P = ((y2 – y)/(y2 – y1))*R1 + ((y – y1)/(y2 – y1))*R2
	*/

	private double interpolateBilinear(int x, int y, int x1, int y1, int tre1, int x2, int y2, int tre2, int tre3, int tre4){
		double R1 = ((x2 - x)/(x2 - x1))*tre1 + ((x - x1)/(x2 - x1))*tre2;
		double R2 = ((x2 - x)/(x2 - x1))*tre3 + ((x - x1)/(x2 - x1))*tre4;
		double res = ((y2 - y)/(y2 - y1))*R1 + ((y - y1)/(y2 - y1))*R2;
		return res;
 	}

	private ImagePlus makeTreshold(double treshold, ImagePlus imp){
		if (treshold == 0){treshold = 1;}
		//IJ.error("Treshold : "+treshold);
		ImageProcessor ip = imp.getChannelProcessor();
		for (int i = 0; i < ip.getPixelCount();i++){
			ip.set( i , ip.get(i)<treshold ? 0 : 255);
		}
		ip.autoThreshold();
		return imp;
	}

	
	int[] otsuMulti(int[] histogram, int total) {
    	int N = total;

       	double W0K, W1K, W2K, M0, M1, M2, currVarB, optimalThresh1, optimalThresh2, maxBetweenVar, M0K, M1K, M2K, MT;

	    optimalThresh1 = 0;
	    optimalThresh2 = 0;

	    W0K = 0;
	    W1K = 0;

	    M0K = 0;
	    M1K = 0;

	    MT = 0;
	    maxBetweenVar = 0;
	    for (int k = 0; k <= 255; k++) {
	        MT += k * (histogram[k] / (double) N);
	    }


	    for (int t1 = 0; t1 <= 255; t1++) {
	        W0K += histogram[t1] / (double) N; //Pi
	        M0K += t1 * (histogram[t1] / (double) N); //i * Pi
	        M0 = M0K / W0K; //(i * Pi)/Pi

	        W1K = 0;
	        M1K = 0;

	        for (int t2 = t1 + 1; t2 <= 255; t2++) {
	            W1K += histogram[t2] / (double) N; //Pi
	            M1K += t2 * (histogram[t2] / (double) N); //i * Pi
	            M1 = M1K / W1K; //(i * Pi)/Pi

	            W2K = 1 - (W0K + W1K);
	            M2K = MT - (M0K + M1K);

	            if (W2K <= 0) break;

	            M2 = M2K / W2K;

	            currVarB = W0K * (M0 - MT) * (M0 - MT) + W1K * (M1 - MT) * (M1 - MT) + W2K * (M2 - MT) * (M2 - MT);

	            if (maxBetweenVar < currVarB) {
	                maxBetweenVar = currVarB;
	                optimalThresh1 = t1;
	                optimalThresh2 = t2;
	            }
	        }
	    }
	    int[] result = {(int)optimalThresh1, (int)optimalThresh2};
	    return result;
	}
}