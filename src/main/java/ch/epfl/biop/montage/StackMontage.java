/*-
 * #%L
 * This plugin brings a bit more functionality that was not available using the Make Montage... Plugin, namely making montages out of multiple stacks and hyperstacks.
 * %%
 * Copyright (C) 2009 - 2022 BIOP
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package ch.epfl.biop.montage;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.CompositeConverter;
import ij.plugin.HyperStackConverter;
import ij.plugin.RGBStackConverter;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.util.ArrayList;
import java.util.Arrays;

public class StackMontage {

	/**
	 * main method for image montage. 
	 * @param theimages ArrayList of ImagePluses to montage
	 * @param nrows Number of rows for the montage
	 * @param ncols Number of columns for the montage
	 * @return an ImagePlus object containing the montaged images as a hyperstack 
	 */
	public static ImagePlus montageImages(ArrayList<ImagePlus> theimages, int nrows, int ncols) {
		
		// Check XYZT size and type matches
		int bd = theimages.get(0).getBitDepth();
		int[] dims = theimages.get(0).getDimensions();
		
		for (ImagePlus img : theimages) {
			if(bd != img.getBitDepth() || !Arrays.equals(dims, img.getDimensions()) ) {
				IJ.error("Stack Montage", "Dimension Mismatch");
				return null;
			}
		}
		
		// Prepare a new image
		ImageStack fstack = new ImageStack(dims[0]*ncols, dims[1]*nrows);
		
		// Copy all the stack, take care of putting it back into a hyperstack later
		for (int z=0; z<dims[2]*dims[3]*dims[4];z++) {
			
			// Temporary image processor to store the ip of the current image
			ImageProcessor ip;
			switch (bd) {
				case 8:
					ip = new ByteProcessor(dims[0]*ncols, dims[1]*nrows);
					break;
				case 16:
					ip = new ShortProcessor(dims[0]*ncols, dims[1]*nrows);
					break;
				case 24:
					ip = new ColorProcessor(dims[0]*ncols, dims[1]*nrows);
					break;
				case 32:
					ip = new FloatProcessor(dims[0]*ncols, dims[1]*nrows);
					break;
				default:
					ip = null;
					break;
			}

			// Make each slice of the new stack
			for(int r=0; r<nrows; r++) {
				for(int c=0; c<ncols; c++) {
					// Get the image to use
					int imgnumber = c+ncols*r;
					
					// it can happen that there are more rows/cols than images. 
					// Make sure that this does not cause an error
					if (imgnumber < theimages.size()) {
						// Get the image position and the image
						int x = dims[0]*c;
						int y = dims[1]*r;
						ImagePlus imp = theimages.get(imgnumber);

						// Copy it at the right position
						ip.copyBits(imp.getStack().getProcessor(z+1), x,y, Blitter.ADD);
						// It's disgustingly awesome that it is this simple...
					}
				}
			}
			
			// Add the new slice to the final image
			fstack.addSlice(ip);
		} // end going through all the slices
		
		// Create the final ImagePlus Object
		ImagePlus fimp = new ImagePlus("Montage of Stacks", fstack);

		// If we are to make a hyperstack, we need to convert the RGB
		// images to multichannel, then back...
		if(bd == 24) { 
			fimp = CompositeConverter.makeComposite(fimp);
			dims[2] = 3;
		}
		
		if (theimages.get(0).isHyperStack())
			// Make the hyperstack
			fimp = HyperStackConverter.toHyperStack(fimp, dims[2], dims[3], dims[4]);
		
		// Convert the RGB image back to RGB
		if (bd == 24) {
			RGBStackConverter.convertToRGB(fimp);
		}
			
		// We're done
		return fimp;
	}
}
