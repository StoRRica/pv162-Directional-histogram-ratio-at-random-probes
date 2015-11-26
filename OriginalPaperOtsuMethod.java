package projekt;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.process.ByteProcessor;
import projekt.TresholdValues;

public class OriginalPaperOtsuMethod implements TresholdValues{

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