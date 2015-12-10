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

	//TresholdValues getTresholdValues = new OriginalPaperOtsuMethod();

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
		int[] myOtsu = multiOtsu(ip.getHistogram() ,ip.getPixelCount()); //getTresholdValues.getTresholds(ip.getHistogram() ,ip.getPixelCount());
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
		//ImagePlus changedPicture = new ImagePlus(String.format("My backgound change of %s ", title), c);
		//changedPicture.show();
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
		int[][] tresholds = new int[numOfWindows][];
		for (int i =0; i < numOfWindows;i++){
			tresholds[i] = new int[numOfWindows];
		}
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
        		//myOtsu = multiOtsu(ip.getHistogram() ,ip.getPixelCount());
        		
        		if (Dr > 2 ){
        			otsu = makeTreshold(myOtsu[1],otsu);
        			tresholds[i][j] = myOtsu[1];
        			returnValue.treshold[i][j]=myOtsu[1];
        			cropped1 = tresholdImage(cropped1, 255);
        		}else{
        			otsu = makeTreshold(myOtsu[0],otsu);
        			tresholds[i][j] = myOtsu[0];
        			returnValue.treshold[i][j] = myOtsu[0];
        			cropped1 = tresholdImage(cropped1, 0);
        		} 

        		/*0 to copy over the pixels in  result*/
        		result.copyBits(cropped,cropWidth * i, cropHeight * j,0);
        		cropped = tresholdImage(cropped,returnValue.treshold[i][j]);
        		tresholdsImage.copyBits(cropped,cropWidth * i, cropHeight * j,0);
        		upperLower.copyBits(cropped1,cropWidth * i, cropHeight * j,0);
				//new ImagePlus("croppedImage" + i +" " + j + " Dr :" + Dr + " lower: " + myOtsu[0] +" upper: "+myOtsu[1] , cropped).show();
			}
						
		}
		
		returnValue.procesor = result;

		ImagePlus imageOfTresholds = new ImagePlus(String.format("My tresholds of %s ", title), tresholdsImage);
		imageOfTresholds.show();
		//ImagePlus imageOfTresholds1 = new ImagePlus(String.format("upper lower treholds of %s ", title), upperLower);
		//imageOfTresholds1.show();	
		ImageProcessor interpolationImage = ip.duplicate();
		doInterpolation(tresholds, interpolationImage);
		ImagePlus imageOfTresholds2 = new ImagePlus(String.format("billinear interpolation of %s ", title), interpolationImage);
		imageOfTresholds2.show();
		return returnValue;
	}

	private ImageProcessor tresholdImage(ImageProcessor ip, int value){
		for (int i = 0; i < ip.getPixelCount();i++){
			ip.set(i,value);
		}
		return ip;
	}


	/*
	R1 = ((x2 – x)/(x2 – x1))*Q11 + ((x – x1)/(x2 – x1))*Q21

	R2 = ((x2 – x)/(x2 – x1))*Q12 + ((x – x1)/(x2 – x1))*Q22

	After the two R values are calculated, the value of P can finally be calculated by a weighted average of R1 and R2.

	P = ((y2 – y)/(y2 – y1))*R1 + ((y – y1)/(y2 – y1))*R2
	*/

	private ImageProcessor doInterpolation(int[][] values, ImageProcessor ip){
		ImageProcessor interpolatedTresholds = ip.duplicate();
		int cropWidth = ip.getWidth() / numOfWindows;
		int crwh = cropWidth / 2 ;//polovica crop width

		int cropHeight = ip.getHeight() / numOfWindows;
		int crhh = cropHeight / 2 ; //polovica cropHeight
		int x,y,w,h;
		int posx,posy;
		int tre11,tre12,tre21,tre22,x1,x2,y1,y2;
		int modi,modj;
		for (int i = crwh;i<cropWidth * numOfWindows-crwh;i++ ) {
			x = (i -crwh) / cropWidth;
			posx = i /cropWidth;
			modi = (i  ) % cropWidth;
			for (int j = crhh; j < cropHeight * numOfWindows-crhh; j++) {
				y = (j-crhh) / cropHeight;
				modj = (j) % cropWidth;
				posy = j /cropHeight;
				x1 = x * cropWidth + crwh;
				x2 = x1+cropWidth;
				y2 = y * cropHeight + crhh;
				y1 = y2+cropHeight;
				tre11 = values[x][y+1];
				tre12 = values[x][y];
				tre21 = values[x+1][y+1];
				tre22 = values[x+1][y];
				double tresholdResult = interpolateBilinear(i,j,x1,y2,tre12,x2,y1,tre22,tre11,tre21);
				interpolatedTresholds.set(i,j,(int)tresholdResult);
				ip.set(i, j, ip.get(i,j) < tresholdResult?0:255);
			}
		}
		for (int i = 0;i<crwh ;i++ ) {
			for (int j = 0 ;j<crhh ;j++ ) {
				interpolatedTresholds.set(i,j,values[0][0]);
				ip.set(i,j,ip.get(i,j)<values[0][0]?0:255);
			}
		}
		for (int i = cropWidth * numOfWindows-crwh;i<cropWidth * numOfWindows;i++){
			for (int j = 0;j< crhh ; j++) {
				interpolatedTresholds.set(i,j,values[numOfWindows-1][0]);
				ip.set(i,j,ip.get(i,j)<values[0][0]?0:255);
			}
		}
		for (int i = cropWidth * numOfWindows-crwh;i<cropWidth * numOfWindows;i++){
			for (int j = cropHeight * numOfWindows-crhh;j< cropHeight * numOfWindows ; j++) {
				interpolatedTresholds.set(i,j,values[numOfWindows-1][numOfWindows-1]);
				ip.set(i,j,ip.get(i,j)<values[0][0]?0:255);
			}
		}
		for (int i = 0;i<crwh ;i++){
			for (int j = cropHeight * numOfWindows-crhh;j< cropHeight * numOfWindows ; j++) {
				interpolatedTresholds.set(i,j,values[0][numOfWindows-1]);
				ip.set(i,j,ip.get(i,j)<values[0][0]?0:255);
			}
		}

		for (int i = crwh;i<cropWidth * numOfWindows-crwh;i++ ) {
			x = (i -crwh) / cropWidth;
			for (int j = 0; j<crhh; j++) {
				x1 = x * cropWidth + crwh;
				x2 = x1+cropWidth;
				y2 = 0;
				y1 = crhh;
				tre11 = values[x][0];
				tre12 = values[x+1][0];
				tre21 = values[x][0];
				tre22 = values[x+1][0];
				double tresholdResult = interpolateBilinear(i,j,x1,y2,tre11,x2,y1,tre12,tre21,tre22);
				interpolatedTresholds.set(i,j,(int)tresholdResult);
				ip.set(i, j, ip.get(i,j) < tresholdResult?0:255);
			}
		}

		for (int i = crwh;i<cropWidth * numOfWindows-crwh;i++ ) {
			x = (i -crwh) / cropWidth;
			for (int j = cropHeight * numOfWindows-crhh;j< cropHeight * numOfWindows; j++) {
				x1 = x * cropWidth + crwh;
				x2 = x1+cropWidth;
				y2 = cropHeight * numOfWindows-crhh;
				y1 = cropHeight * numOfWindows;
				tre11 = values[x][numOfWindows-1];
				tre12 = values[x+1][numOfWindows-1];
				tre21 = values[x][numOfWindows-1];
				tre22 = values[x+1][numOfWindows-1];
				double tresholdResult = interpolateBilinear(i,j,x1,y2,tre11,x2,y1,tre12,tre21,tre22);
				interpolatedTresholds.set(i,j,(int)tresholdResult);
				ip.set(i, j, ip.get(i,j) < tresholdResult?0:255);
			}
		}


		for (int i = cropWidth * numOfWindows-crwh;i<cropWidth * numOfWindows;i++ ) {
			x = (i -crwh) / cropWidth;
			for (int j = crhh;j< cropHeight * numOfWindows-crhh; j++) {
				y = (j-crhh) / cropHeight;
				x1 = cropWidth * numOfWindows-crwh;
				x2 = cropWidth * numOfWindows;
				y2 = y * cropHeight + crhh;
				y1 = y2+cropHeight;
				tre11 = values[x][y];
				tre12 = values[x][y];
				tre21 = values[x][y+1];
				tre22 = values[x][y+1];
				double tresholdResult = interpolateBilinear(i,j,x1,y2,tre11,x2,y1,tre12,tre21,tre22);
				interpolatedTresholds.set(i,j,(int)tresholdResult);
				ip.set(i, j, ip.get(i,j) < tresholdResult?0:255);
			}
		}

		for (int i = 0;i<crwh;i++ ) {
			x = (i -crwh) / cropWidth;
			for (int j = crhh;j< cropHeight * numOfWindows-crhh; j++) {
				y = (j-crhh) / cropHeight;
				x1 = 0;
				x2 = crwh;
				y2 = y * cropHeight + crhh;
				y1 = y2+cropHeight;
				tre11 = values[x][y];
				tre12 = values[x][y];
				tre21 = values[x][y+1];
				tre22 = values[x][y+1];
				double tresholdResult = interpolateBilinear(i,j,x1,y2,tre11,x2,y1,tre12,tre21,tre22);
				interpolatedTresholds.set(i,j,(int)tresholdResult);
				ip.set(i, j, ip.get(i,j) < tresholdResult?0:255);
			}
		}

		ImagePlus imageOfTresholds2 = new ImagePlus(String.format("result of billinear tresholds interpolation of %s ", title), interpolatedTresholds);
		imageOfTresholds2.show();
		return ip;
	}


	private double interpolateBilinear(int x, int y, int x1, int y1, int tre1, int x2, int y2, int tre2, int tre3, int tre4){
		double R1 = ((double)(x2 - x)/(x2 - x1))*tre1 + ((double)(x - x1)/(x2 - x1))*tre2;
		double R2 = ((double)(x2 - x)/(x2 - x1))*tre3 + ((double)(x - x1)/(x2 - x1))*tre4;
		double res = ((double)(y1 - y)/(y1 - y2))*R2 + ((double)(y - y2)/(y1 - y2))*R1;
		
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

	public int[] multiOtsu(int[] histogram,int countOfPixels){
		int N = countOfPixels;

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
	

	interface TresholdValues{

		public int[] getTresholds(int[] histogram, int countOfPixels);

		public ImageProcessor doInterpolation(int[][] values, ImageProcessor ip);

	}

	class OriginalPaperOtsuMethod implements TresholdValues{

		public int[] getTresholds(int[] histogram, int countOfPixels){
			int N = countOfPixels;

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

		public ImageProcessor doInterpolation(int[][] values, ImageProcessor ip){

			return null;
		}
	}
}