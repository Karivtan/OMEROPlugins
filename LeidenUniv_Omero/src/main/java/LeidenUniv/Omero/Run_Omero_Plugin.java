package LeidenUniv.Omero;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.awt.MenuBar;
import java.awt.AWTEvent;
import java.awt.Choice;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.Toolkit;
import java.awt.Window;
import LeidenUniv.Fish.Pixel_counter_FIJI_v2;
import LeidenUniv.Tools.PI_Hoechst_Measurements;
import ij.ImagePlus;
import ij.WindowManager;
import ij.IJ;
import ij.ImageJ;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import omero.gateway.model.ImageData;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import LeidenUniv.Fish.*;

public class Run_Omero_Plugin implements PlugIn, DialogListener {
	private String[][] plugins;
	private int prevFchoice=0,Fchoice=0;
    public void run(String args) {
        //gets omero plugins and lets you choose
 		ImageJ imageJ = IJ.getInstance();
        MenuBar b = imageJ.getMenuBar();
        Menu m = b.getMenu(5);
        //IJ.log(""+m.getItemCount());
        String cmnd=args;
        for (int i=0;i<m.getItemCount();i++){
			if (m.getItem(i).getLabel().equals("LeidenUniv")){ //meaning we read all plugins from LeidenUniv
				MenuItem cmi=m.getItem(i); // these most likely need to go in a list that gets adapted if we change the first group
				// this would mean we can just adapt the plugins we want to work with for omero
				Menu lu =(Menu)cmi;
				String [] submenus = new String[lu.getItemCount()];
				plugins = new String[lu.getItemCount()][];
				MenuItem[][] pluginItems = new MenuItem[lu.getItemCount()][];
				for (int j=0;j<lu.getItemCount();j++){
					submenus[j]=lu.getItem(j).getLabel();
					MenuItem omp=lu.getItem(j);
					Menu ompm =(Menu)omp; // this goes wrong now? why
					MenuItem[] omeitems = new MenuItem[ompm.getItemCount()];
					String [] OmeroList = new String[ompm.getItemCount()];
					for (int k=0;k<ompm.getItemCount();k++){
						omeitems[k]=ompm.getItem(k);
						OmeroList[k]=ompm.getItem(k).getLabel();
					}
					plugins[j]=OmeroList;
					pluginItems[j]=omeitems;		
					/*
					if (lu.getItem(j).getLabel().equals("Omero")){
						MenuItem omp=lu.getItem(j);
						Menu ompm =(Menu)omp;
						MenuItem[] omeitems = new MenuItem[ompm.getItemCount()];
						String [] OmeroList = new String[ompm.getItemCount()];
						for (int k=0;k<ompm.getItemCount();k++){
							omeitems[k]=ompm.getItem(k);
							OmeroList[k]=ompm.getItem(k).getLabel();
							}
						GenericDialog gd = new GenericDialog("Choose your plugin");
						gd.addChoice("Choose plugin to run",OmeroList,OmeroList[0]);
						gd.showDialog();
						choice = gd.getNextChoiceIndex();
						cmnd = omeitems[choice].getActionCommand();						
					}*/
				}
				GenericDialog gd = new GenericDialog("Choose your plugin");
				gd.addChoice("Choose plugin folder",submenus,submenus[0]);
				gd.addChoice("Choose plugin to run",plugins[0],plugins[0][0]);
				gd.addDialogListener(this);
				Component [] cps =gd.getComponents();
				cps[1].setSize(250, 10);
				cps[3].setSize(250, 10);
				gd.showDialog();
				if (gd.wasCanceled()) {
					return;
				}
				
				int Folderchoice = gd.getNextChoiceIndex();
				int pluginchoice = gd.getNextChoiceIndex();
				cmnd = pluginItems[Folderchoice][pluginchoice].getActionCommand();	
			}
		}
    	

        if (cmnd.equalsIgnoreCase("getImage Area Mean Stdev")){
        	ArrayList<Integer> meas = new ArrayList<Integer>();
			meas.add(ResultsTable.AREA);
			meas.add(ResultsTable.STD_DEV);
			meas.add(ResultsTable.MEAN);
        	// gets images from the dataset, 
        	getOmeroDatasetAndAttachData om = new getOmeroDatasetAndAttachData();
        	//needs to be split to seperately analyse each image
            //ArrayList<ImagePlus> imps = om.getDatasetImages();
        	//instead we get the collection
        	Collection<ImageData> images = om.getImageCollection();
        	ResultsTable combineResults=ResultsTable.getResultsTable();
        	if (images!=null){
            	Iterator<ImageData> image = images.iterator(); 
            	if (combineResults ==null) {
            		combineResults = new ResultsTable(images.size());
            	}
            	int counter=0;  
	            while (image.hasNext()) {
	            	counter++;
	                ImageData data = image.next();
	                setLogWindowSizeAndLocation();
	                IJ.log("Loading image "+counter+" of "+images.size());
	                // here we load all single images, can be too much data
	                try {
	                	ImagePlus timp = om.openImagePlus(data.getId(), data, data.getGroupId());
		                ResultsTable rt2 = getImageData.getSingleImageResultsTable(timp, meas);
		                rt2.addResults();
	                } catch (Exception e) {
	                	 IJ.log("Error Loading image "+counter+" of "+images.size());
	                	 IJ.log(e.getMessage());
	                    	StackTraceElement[] t = e.getStackTrace();
	                    	for (int i=0;i<t.length;i++){
	                    		IJ.log(t[i].toString());
	                    	}
	                }
                	// here we have the image, and we can analyse and attach data}

	            }
            }
            combineResults.show("Results");
			om.attachDataToDataset(combineResults,1);
			//rt2.show("Measured images");
        	/*// gets images from the dataset, 
        	getOmeroDatasetAndAttachData om = new getOmeroDatasetAndAttachData();
        	//needs to be split to seperately analyse each image
            ArrayList<ImagePlus> imps = om.getDatasetImages();
            long currentDataset = om.getDatasetID();
            ImagePlus[] cim =new ImagePlus[imps.size()];
            imps.toArray(cim);
        	ArrayList<Integer> meas = new ArrayList<Integer>();
			meas.add(ResultsTable.AREA);
			meas.add(ResultsTable.STD_DEV);
			meas.add(ResultsTable.MEAN);
			ResultsTable rt2=getImage_Area_Mean_Stdev.OmeroBatchAnalysis(cim,meas);
			ResultsTable []rta = getImage_Area_Mean_Stdev.OmeroSingleAnalysis(cim);*/
			//om.attachDataToDataset(rt2);
			//rt2.show("Measured images");
        } else if (cmnd.equalsIgnoreCase("Open Omero Dataset")) {
        	IJ.showMessage("This plugin is not suitable for use in this manner\nThis can only be used to download a dataset into FIJI");
        } else if (cmnd.equalsIgnoreCase("Run Omero Plugin")) {
        	IJ.showMessage("Circulare referencing not allowed");
        } else if (cmnd.equalsIgnoreCase("PixelCount")){
        	getOmeroDatasetAndAttachData om = new getOmeroDatasetAndAttachData();
        	Collection<ImageData> images = om.getImageCollection(); // this gives the version error
        	// SaveImageSequences sis = new SaveImageSequences();
        	// sis.saveImages(images);
        	ResultsTable combineResults=new ResultsTable();
        	if (images!=null){
            	Iterator<ImageData> image = images.iterator(); 
            	int counter=0; 
            	GenericDialog gd = new GenericDialog("Settings");
        		gd.addMessage("For multilayered tifs numerically fill\nin the channel you want to analyse");
        		gd.addNumericField("Channel to analyse:", 1, 0); 
        		gd.showDialog();
        		int Channel = (int)gd.getNextNumber();
        		double totalArea=0;
        		double totalIntDen=0;
	            while (image.hasNext()) {
	            	counter++;
	                ImageData data = image.next();
	                setLogWindowSizeAndLocation();
	                IJ.log("Loading image "+counter+" of "+images.size());
	                // here we load all single images, can be too much data
	                try {
	                	ImagePlus timp = om.openImagePlus(data.getId(), data, data.getGroupId());
	                	Pixel_counter_FIJI_v2 pc = new Pixel_counter_FIJI_v2();
		                ResultsTable rt2 = pc.getOmeroData(timp,Channel);
		                //rt2.show("returned data");
		                // here we need to add this data to the image
		                om.attachDataToImage(rt2, 2,data);
		                //rt2.addResults();
		                //how do we combine all the results to a total table?
		                combineResults.incrementCounter();
		                combineResults.addValue("Title", timp.getTitle());
		                combineResults.addValue("Area", rt2.getValue("Area", rt2.size()-3));
		                combineResults.addValue("Integrated Density", rt2.getValue("Integrated Density", rt2.size()-3));
		                totalArea+=rt2.getValue("Area", rt2.size()-3);
		                totalIntDen+=rt2.getValue("Integrated Density", rt2.size()-3);
	                } catch (Exception e) {
	                	 IJ.log("Error Loading image "+counter+" of "+images.size());
	                	 IJ.log(e.toString());
	                	 IJ.log(e.getLocalizedMessage());
	                	 IJ.log(e.getMessage());
	                    	StackTraceElement[] t = e.getStackTrace();
	                    	for (int i=0;i<t.length;i++){
	                    		IJ.log(t[i].toString());
	                    	}
	                }
                	// here we have the image, and we can analyse and attach data}

	            }
	            combineResults.incrementCounter();
	            combineResults.addValue("Title", "Total");
                combineResults.addValue("Area", totalArea);
                combineResults.addValue("Integrated Density", totalIntDen);
            }
            //combineResults.show("Results");
            om.attachDataToDataset(combineResults,1);
            // still need to attach
        }   else if (cmnd.equalsIgnoreCase("Neighbourhood localization analysis")){
        	IJ.log("Running omero plugin");
        	getOmeroDatasetAndAttachData om = new getOmeroDatasetAndAttachData();
        	Collection<ImageData> images = om.getImageCollection(); // this gives the version error
        	if (images!=null){
            	Iterator<ImageData> image = images.iterator(); 
            	int counter=0; 
            	String [] thm = {"Bernsen","Phansalkar","Contrast","Mean","Median","MidGrey","Niblack","Otsu","Sauvola" };
        		GenericDialog gd = new GenericDialog("Options");
        		gd.addMessage("The plugin will crash when selecting channels that are not in you images");
        		gd.addSlider("Select the channel that contains the main features",1.0, 6.0,1.0); // last value = default channel
        		gd.addSlider("Select the channel which you want to check for co-localization",1.0, 6.0,2.0); // last value = default channel
        		gd.addNumericField("Minimum object size: ", 100.0, 0,5," in pixels");
        		gd.addNumericField("Neighbourhood size: ", 5.0,0,5," in pixels");
        		gd.addChoice("Main local threshold method: ",thm,thm[0]);
        		gd.showDialog();
        		if (gd.wasCanceled()) return;
        		int fC=(int)gd.getNextNumber()-1;
        		int dC=(int)gd.getNextNumber()-1;
        		int MinObjSize=(int)gd.getNextNumber();
        		int RimSize=(int)gd.getNextNumber();
        		int thmi = gd.getNextChoiceIndex();
        		ResultsTable settings = new ResultsTable();
        		settings.addValue("Threshold method", thm[thmi]);
        		settings.addValue("Main channel", fC);
        		settings.addValue("Secondary channel", dC);
        		settings.addValue("Minimum object size", MinObjSize);
        		settings.addValue("Neighbourhood size", RimSize);
	            while (image.hasNext()) {
	            	counter++;
	                ImageData data = image.next();
	                //need to get the log window and position it
	                setLogWindowSizeAndLocation();
	                IJ.log("Loading image "+counter+" of "+images.size());
	                // here we load all single images, can be too much data
	                try {
	                	ImagePlus timp = om.openImagePlus(data.getId(), data, data.getGroupId());
	                	NeighbourhoodLocalizationAnalysis nla = new NeighbourhoodLocalizationAnalysis();
	                	ResultsTable [] rts = nla.getNeighbourhoodLocalizationAnalysis(timp, fC, dC, MinObjSize, RimSize, thmi);
		                om.attachDataToImage(rts[1], 2,data, "details");
		                om.attachDataToImage(rts[0], 1,data, "totals");
		                om.attachDataToImage(settings, 1,data, "settings");
		                om.attachRoisToImage(nla.getNeighbourhoodRois(), data, "rois");
	                } catch (Exception e) {
	                	 IJ.log("Error Loading image "+counter+" of "+images.size());
	                	 IJ.log(e.toString());
	                	 IJ.log(e.getLocalizedMessage());
	                	 IJ.log(e.getMessage());
                    	StackTraceElement[] t = e.getStackTrace();
                    	for (int i=0;i<t.length;i++){
                    		IJ.log(t[i].toString());
                    	}
	                }
	            }
            }
        	IJ.log("Done unning omero plugin");
        } else if (cmnd.equalsIgnoreCase("PI Hoechst Quantification")) {
        	IJ.log("Running quantification");
        	getOmeroDatasetAndAttachData om = new getOmeroDatasetAndAttachData();
        	Collection<ImageData> images = om.getImageCollection(); // this gives the version error
        	if (images!=null){
        		int counter=0; 
            	Iterator<ImageData> image = images.iterator(); 
            	ResultsTable dataSet = new ResultsTable();
            	//add menu
            	GenericDialog gd = new GenericDialog("Settings");
            	gd.addNumericField("Region size", 5.0, 0);
            	gd.addNumericField("Prominence for maxima finding", 10.0, 0);
            	gd.addNumericField("Channel number of Hoechst channel", 1, 0);
            	gd.addNumericField("Channel number of PI channel", 2, 0);
            	gd.addNumericField("Cut off 1st channel", 45, 0);
            	gd.addNumericField("Cut off last channel", 33, 0);
            	gd.showDialog();
            	
            	int rsize = (int)gd.getNextNumber();
            	int prom = (int)gd.getNextNumber();
            	int hch= (int)gd.getNextNumber();
            	int pch= (int)gd.getNextNumber();
            	int hLimit = (int)gd.getNextNumber();
            	int pLimit = (int)gd.getNextNumber();
            	
            	while (image.hasNext()) {
            	//if (image.hasNext()) {
            		counter++;
            		ImageData data = image.next();
            		setLogWindowSizeAndLocation();
            		try {
	                	ImagePlus timp = om.openImagePlus(data.getId(), data, data.getGroupId());
	                	ResultsTable [] rts = PI_Hoechst_Measurements.getOmeroData(timp,hch,pch,rsize,prom,pLimit,hLimit);
		                om.attachDataToImage(rts[0], 0,data, "details");
		                dataSet.incrementCounter();
		                dataSet.addValue("Image Name",rts[1].getStringValue("Image Name",0));
		                dataSet.addValue("% Cell Death",rts[1].getValue("% Cell Death", 0));
	                } catch (Exception e) {
	                	 IJ.log("Error Loading image "+counter+" of "+images.size());
	                	 IJ.log(e.toString());
	                	 IJ.log(e.getLocalizedMessage());
	                	 IJ.log(e.getMessage());
                    	StackTraceElement[] t = e.getStackTrace();
                    	for (int i=0;i<t.length;i++){
                    		IJ.log(t[i].toString());
                    	}
	                }
            		
            	}
            	try {
            		om.attachDataToDataset(dataSet, 1, "Dataset Summary");// to implement
            		
            	} catch (Exception e) {
               	 IJ.log("Could not attach table to dataset");
               	 IJ.log(e.toString());
               	 IJ.log(e.getLocalizedMessage());
               	 IJ.log(e.getMessage());
               	StackTraceElement[] t = e.getStackTrace();
               	for (int i=0;i<t.length;i++){
               		IJ.log(t[i].toString());
               	}
               }
        	}
        	IJ.log("Finished quantification");
        } else {
        	IJ.log(cmnd);
			IJ.showMessage("not implemented yet");
		}
    }
    
    public static void main (String[] args) {
    	//ImageJ imageJ = new ImageJ();
    	//imageJ.getInfo();
    	//new Run_Omero_Plugin().run("PI Hoechst Quantification");
    	String [] uns = {"aa","ab","ac","bb","dd","ae","af"};
    	String [] uns2 = {"aa","ab","ac","bb","dd","ae","af"};
    	GenericDialog gd = new GenericDialog("test");
    	
    	Arrays.sort(uns);
    	
    	for (int i=0;i<uns.length;i++) {
    		System.err.print(uns[i]+",");
    	}
    	System.err.println();
    	gd.addChoice("Select", uns, uns[0]);
    	gd.showDialog();
    	for (int i=0;i<uns2.length;i++) {
    		System.err.print(uns2[i]+",");
    	}
    	int chi1 = gd.getNextChoiceIndex();
    	String ch = uns[chi1];
    	List<String> chl = Arrays.asList(uns2);
    	int chi=chl.indexOf(ch);
    	System.err.print(chi1+",");
    	System.err.print(chi);
    	System.err.print(uns[chi1]+","+uns2[chi]);
    	
    	System.err.println();
    	
    	
    }
    
    public void setLogWindowSizeAndLocation() {
    	Window logwindow =WindowManager.getWindow("Log");
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int width = (int)screenSize.getWidth();
		int height = (int)screenSize.getHeight();
        logwindow.setSize(width/4, height/2);
        logwindow.setLocation(0, 0);
        logwindow.toFront();
    }

	@Override
	public boolean dialogItemChanged(GenericDialog dlog, AWTEvent arg1) {
		// TODO Auto-generated method stub
		Component [] cps =dlog.getComponents();
       	Fchoice = ((Choice)cps[1]).getSelectedIndex();
       	//Pchoice = ((Choice)cps[3]).getSelectedIndex();
       	if (Fchoice!=prevFchoice) {
	        ((Choice)cps[3]).removeAll();
	        for (int i=0;i<plugins[Fchoice].length;i++) {
	        	((Choice)cps[3]).add(plugins[Fchoice][i]);
	        }
       	}
       	prevFchoice=Fchoice;
		return true;
	}
}