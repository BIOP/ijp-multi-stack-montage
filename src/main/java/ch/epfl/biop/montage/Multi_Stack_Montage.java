package ch.epfl.biop.montage;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Macro;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.Duplicator;
import ij.plugin.PlugIn;


/**
 * PlugIn to create montages out of multiple stacks or hyperstacks.
 * Macro recordable
 * @author Olivier Burri
 * @version 1.1
 */
public class Multi_Stack_Montage implements PlugIn {

	/**
	 * list of currently active images in imageJ 
	 */
	private ArrayList<String> imagenames;
	
	/**
	 * list of images that will be used to make a montage 
	 */
	private ArrayList<ImagePlus> imagestoprocess = new ArrayList<ImagePlus>(0);
	
	/**
	 * string for no image selection
	 */
	private final String nonestr ="*None*";
	
	/**
	 * for the dialog box, how many images can we montage.
	 * This does not limit the number of arguments to pass as a macro
	 */
	private final int maximages = 10;
	
	/**
	 * number of columns for montage
	 */
	private int nCols;
	
	/**
	 * number of rows for montage
	 */
	private int nRows;
	
	/* (non-Javadoc)
	 * @see ij.plugin.PlugIn#run(java.lang.String)
	 */
	@Override
	public void run(String arg0) {
		
		// Get all Active Images
		imagenames = new ArrayList<String>(Arrays.asList(WindowManager.getImageTitles()));
		imagenames.add("*None*");
		
		// Run the montage with the defined options.
		if (showDialog()) {
			ImagePlus res = StackMontage.montageImages(imagestoprocess, nRows, nCols);
			// Show the final image
			res.show();
		}
		
	}
	/**
	 * method to display a GenericDialog and make sure that the plugin is macro recordable
	 * and macro compatible
	 * @return true if the dialog was completed successfully
	 */
	private boolean showDialog() {
		
		// Check how many images we will need to show as a dialog
		int count = (imagenames.size()-1) > maximages?maximages:imagenames.size()-1;
		
		/*
		 *  Calculate the number of rows and columns
		 *  Based on MontageMaker, this tries to make the 
		 *  montage as square as possible
		 */
		nCols = (int)Math.sqrt(count);
		nRows = nCols;
		int n = count - nCols*nRows;
		if (n>0) nCols += (int)Math.ceil((double)n/nRows);
		
		/* 
		 * borrowed from RGBStackMerge, we check whether 
		 * this is being called as a macro to define the number of input parameters
		 * so as to not have blank or missing lines
		 */
		String options = Macro.getOptions();
		boolean macro = IJ.macroRunning() && options!=null;
		
		/*
		 *  if running from a macro, check how many images the user is going to montage
		 *  and change the count for the dialog accordingly
		 */
		if (macro) {
			count = 0;
			/**
			 * See <a href="http://stackoverflow.com/a/767828">Stack Overflow</a> 
			 */
			Pattern pattern = Pattern.compile("stack_\\d{1,3}");
	        Matcher  matcher = pattern.matcher(options);
			while (matcher.find())
			    count++;
		}
		
		// build the dialog based on the number of available/expected images
		GenericDialog gd = new GenericDialog("Multi Stack Montage");
		for(int i=0; i<count; i++) {
			// Shorthand if notation to either add the none string or an image.
			gd.addChoice("Stack_"+(i+1), imagenames.toArray(new String[0]), nonestr);
		}
		
		// Number of rows and columns
		gd.addNumericField("Rows", nRows, 0);
		gd.addNumericField("Columns", nCols, 0);
		gd.setResizable(true);
		// Display
		gd.showDialog();
		if (gd.wasCanceled())
			return false;
		
		// Recover the data
		for(int i=0; i<count; i++) {
			String tmp = gd.getNextChoice();
			// We don't count the onesthat say "none" 
			if (!tmp.equals(nonestr))
				imagestoprocess.add(WindowManager.getImage(tmp));
		}
		nRows = (int) gd.getNextNumber();
		nCols = (int) gd.getNextNumber();
		
		// If we reached this point, then we have completed the dialog
		return true;
	}
	

	/**
	 * Main method for debugging.
	 *
	 * For debugging, it is convenient to have a method that starts ImageJ, loads an
	 * image and calls the plugin, e.g. after setting breakpoints.
	 * A charming example by Lord Dr. Romain G, Queen Emeritus of Deadly Neo Australia 
	 * @param args unused
	 */
	public static void main(String[] args) {
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		Class<?> clazz = Multi_Stack_Montage.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
		ImageJ ij = new ImageJ();
		ij.exitWhenQuitting(true);

		// Make some nice images
		ImagePlus imp = IJ.openImage("http://imagej.nih.gov/ij/images/confocal-series.zip");
		String imageName = imp.getTitle();
		
		ImagePlus imp1 = new Duplicator().run(imp, 1, 2, 1, 25, 1, 1);
		IJ.run(imp1, "Blue", "");
		imp1.setTitle(imageName+"c1Blue");
		
		ImagePlus imp2 = new Duplicator().run(imp1, 1, 2, 1, 25, 1, 1);
		imp2.setC(2);
		IJ.run(imp2, "Magenta", "");
		imp2.setTitle(imageName+"c1Blue_c2Magenta");
		
		ImagePlus imp3 = new Duplicator().run(imp2, 1, 2, 1, 25, 1, 1);
		imp3.setC(1);
		IJ.run(imp3, "Cyan", "");
		imp3.setTitle(imageName+"c1Cyan_c2Magenta");
		
		IJ.run(imp3, "RGB Color", "slices");
		IJ.run(imp2, "RGB Color", "slices");
		IJ.run(imp1, "RGB Color", "slices");
		IJ.run(imp, "RGB Color", "slices");
		
		imp.show();
		imp1.show();
		imp2.show();
		imp3.show();
		
		// run the plugin with or without arguments
		IJ.runPlugIn(clazz.getName(), "");
	}

}
