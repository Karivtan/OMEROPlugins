package LeidenUniv.Omero;
import ij.plugin.PlugIn;
import ij.WindowManager;
import java.util.ArrayList;
import ij.IJ;
import ij.measure.ResultsTable;
import ij.ImagePlus;
import ij.process.ImageStatistics;
import ij.ImageJ;
/**
 * This is a template for a plugin that does not require one image
 * (be it that it does not require any, or that it lets the user
 * choose more than one image in a dialog).
 */
public class getImageData implements PlugIn {
	/**
	 * This method gets called by ImageJ / Fiji.
	 *
	 * @param arg can be specified in plugins.config
	 * @see ij.plugin.PlugIn#run(java.lang.String)
	 */
	public void run(String arg) {
        //String[] ims = ij.WindowManager.getImageTitles();
        int[] ids = WindowManager.getIDList();
        ImagePlus[] imps=new ImagePlus[WindowManager.getImageCount()];
        if (ids==null||ids.length<1){
        	IJ.showMessage("No images open");
        	return;
        } else {
        	for (int i=0;i<ids.length;i++){
        		if (ids[i]<0) {
        			imps[i]=WindowManager.getImage(ids[i]);
        		}
        	}
        	ArrayList<Integer> meas = new ArrayList<Integer>();
        	meas.add(ResultsTable.AREA);
        	meas.add(ResultsTable.MEAN);
        	meas.add(ResultsTable.STD_DEV);
        	ResultsTable rt = getResultsTablePerArray(imps,meas);
        	rt.show("Results");
        }
	}
	static public ResultsTable getSingleImageResultsTable(ImagePlus imp, ArrayList<Integer> mOptions) {
		ResultsTable rt2=  ResultsTable.getResultsTable();
		int sumOptions=0;
		for (int i=0;i<mOptions.size();i++) { // makes a summed int
			sumOptions+=mOptions.get(i);
		}
		IJ.run(imp, "Select All", "");
    	ImageStatistics is = imp.getStatistics(sumOptions);
    	// we need to check what needs to be added
    	rt2.incrementCounter();
    	rt2.addValue("Image Title", imp.getTitle());
    	rt2=fillResultsTableRow(rt2,mOptions,is);
    	rt2.updateResults();
		return rt2;
	}

	static public ResultsTable getResultsTablePerArray(ImagePlus[] imps, ArrayList<Integer> mOptions) {
		//somehow this only creates a one line resulttable. So lets do this differently
		// Lets use imagestatistics and create our own resultstable
		ResultsTable rt2=  new ResultsTable();
		int sumOptions=0;
		for (int i=0;i<mOptions.size();i++) { // makes a summed int
			sumOptions+=mOptions.get(i);
		} 
        for (int i=0;i<imps.length;i++){
			rt2.incrementCounter();
        	ImagePlus imp = imps[i];
        	IJ.run(imp, "Select All", "");
        	ImageStatistics is = imp.getStatistics(sumOptions);
        	// we need to check what needs to be added
        	rt2.addValue("Image Title", imp.getTitle());
        	rt2=fillResultsTableRow(rt2,mOptions,is);
        }
        return rt2;
    }
	
	static private ResultsTable fillResultsTableRow(ResultsTable rt, ArrayList<Integer> mOptions, ImageStatistics is) {
		if (mOptions.contains(ResultsTable.ANGLE)) {
			rt.addValue("Angle", is.angle);
		}
		if (mOptions.contains(ResultsTable.AREA)) {
			rt.addValue("Area", is.area);
		}
		if (mOptions.contains(ResultsTable.AREA_FRACTION)) {
			rt.addValue("Area Fraction", is.areaFraction);
		}
		if (mOptions.contains(ResultsTable.ASPECT_RATIO)) {
			rt.addValue("Aspect Ratio", is.major/is.minor);
		}
		/*if (mOptions.contains(ResultsTable.CIRCULARITY)) {
			rt.addValue("Channel", 4*Math.PI*is.area/is.);
		}
		if (mOptions.contains(ResultsTable.FERET)) {
			rt.addValue("Angle", is.f);
		}*/
		if (mOptions.contains(ResultsTable.MEAN)) {
			rt.addValue("Mean", is.mean);
		}
		if (mOptions.contains(ResultsTable.STD_DEV)) {
			rt.addValue("Std.Dev", is.stdDev);
		}
		if (mOptions.contains(ResultsTable.Y_CENTROID)) {
			rt.addValue("Centroid Y", is.yCentroid);
		}
		if (mOptions.contains(ResultsTable.X_CENTROID)) {
			rt.addValue("Centroid X", is.xCentroid);
		}
		if (mOptions.contains(ResultsTable.Y_CENTER_OF_MASS)) {
			rt.addValue("Center of Mass Y", is.yCenterOfMass);
		}
		if (mOptions.contains(ResultsTable.X_CENTER_OF_MASS)) {
			rt.addValue("Center of Mass X", is.xCenterOfMass);
		}
		if (mOptions.contains(ResultsTable.KURTOSIS)) {
			rt.addValue("Kurtosis", is.kurtosis);
		}
		if (mOptions.contains(ResultsTable.MAX)) {
			rt.addValue("Max", is.max);
		}
		if (mOptions.contains(ResultsTable.MIN)) {
			rt.addValue("Min", is.min);
		}
		if (mOptions.contains(ResultsTable.MEDIAN)) {
			rt.addValue("Median", is.median);
		}
		if (mOptions.contains(ResultsTable.INTEGRATED_DENSITY)) {
			rt.addValue("Integrated Density", is.mean*is.area);
		}
		return rt;
	}
	static public ResultsTable[] getResultsTablePerImage(ImagePlus[] imps) {
		ResultsTable rta[]=  new ResultsTable[imps.length];
		
        for (int i=0;i<imps.length;i++){
        	ResultsTable rt=ResultsTable.getResultsTable();
        	if (rt!=null){
    			rt.reset();
    		}
        	ImagePlus imp = imps[i];
        	IJ.run(imp, "Select All", "");
			IJ.run(imp, "Measure", "");
			
			rta[i]=rt;
        }
        return rta;
	}
	
	
	
	public static void main (String [] arg) {
		ImageJ ij = new ImageJ();
		ij.getInfo();
		new getImageData().run("");
	}
}
