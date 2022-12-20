/*
 *------------------------------------------------------------------------------
 *  Copyright (C) 2017 University of Dundee & Open Microscopy Environment.
 *  All rights reserved.
 *  
 *  Adapted by Joost Willemse to return an ImagePlus ArrayList
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 */
package LeidenUniv.Omero;
import loci.formats.FormatException;
import loci.plugins.BF;
import loci.plugins.LociImporter;
import loci.plugins.in.DisplayHandler;
import loci.plugins.in.ImagePlusReader;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.ImporterOptions;
import loci.plugins.in.ImporterPrompter;
import loci.plugins.util.WindowTools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.Iterator;

import javax.activation.MimeType;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import java.awt.geom.Point2D;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Choice;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.gui.DialogListener;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;
import net.imagej.omero.OMEROLocation;
import net.imagej.omero.OMEROService;
import net.imagej.omero.OMEROSession;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import omero.gateway.Gateway;
import omero.gateway.SecurityContext;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.facility.DataManagerFacility;
import omero.gateway.facility.ROIFacility;
import omero.gateway.facility.TablesFacility;
import omero.gateway.LoginCredentials;
import omero.gateway.model.ImageData;
import omero.gateway.model.PolygonData;
import omero.gateway.model.DataObject;
import omero.gateway.model.DatasetData;
import omero.gateway.model.ProjectData;
import omero.gateway.model.ROIData;
import omero.gateway.model.ShapeData;
import omero.gateway.model.TableData;
import omero.gateway.model.TableDataColumn;
import omero.gateway.model.GroupData;
import omero.gateway.model.ExperimenterData;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.ServerError;
import omero.client;
import omero.api.RawFileStorePrx;
import omero.log.NullLogger;
import omero.log.SimpleLogger;
import omero.model.ChecksumAlgorithm;
import omero.model.ChecksumAlgorithmI;
import omero.model.FileAnnotation;
import omero.model.FileAnnotationI;
import omero.model.ImageAnnotationLink;
import omero.model.ImageAnnotationLinkI;
import omero.model.OriginalFile;
import omero.model.OriginalFileI;
import omero.model.enums.ChecksumAlgorithmSHA1160;
import io.scif.config.SCIFIOConfig.ImgMode;
import io.scif.services.DatasetIOService;
import io.scif.services.DefaultDatasetIOService;
import org.scijava.Context;
import org.scijava.io.location.AbstractLocation;
import org.scijava.io.location.URILocation;

import net.imglib2.img.display.imagej.ImageJFunctions;
//import net.imagej.legacy.convert.roi.box.BoxWrapper;


/**
 * This plugin is to get a connection to OMERO, obtain an image collection and to attach data back to OMERO 
*/
public class getOmeroDatasetAndAttachData implements DialogListener, PlugIn{

    // Edit value
    private String Username = "";
    private static String HOST = "";
    private static int PORT = 4064;
    private String Password = "";
    private boolean SaveCred = true;
	private BrowseFacility browser;
	private SecurityContext ctx;
	private LoginCredentials credentials;
	private String [] gNames, gSortedNames;
	private String [] pNames, pSortedNames;
	private String [] uNames, uSortedNames;
	private String [] sets, SortedSets; 
	private long [] gIds, uIds;
	private int prevgchoice, prevpchoice,schoice, prevuchoice, suchoice;
	private Collection<ProjectData> pjd=null;
	private long [] dataSetIds=null;
	private long dataChoice, cUid;
	private Gateway gateway = null;
	private Dataset d;
	private Object[] dsda;
	private Object[] groupsetarray;
	
    /**
     * Open an image using the Bio-Formats LOCI imageplusreader.
     *
     * @param imageId The id of the image to open
     * @param gid The group id to which the image belongs 
     * @throws Exception
     */
    
    protected ImagePlus openImagePlus(Long imageId, Long gid)
        throws Exception
    {
        OMEROLocation ol = new OMEROLocation(HOST,PORT,Username,Password); // causing conflict issues with omero update site and the downloadable jar from omero
		ImageJ ij = new ImageJ();
		Context context = ij.context();
		OMEROService dos = context.service(OMEROService.class); // also not found
		OMEROSession os = dos.createSession(ol);// Here we create a new session, but one should still exist based on the gateway connection previously made
		client cl = os.getClient();
		String credentials ="location=[OMERO] open=[omero:server=";
		credentials += cl.getProperty("omero.host");
		credentials +="\nuser=";
		credentials +=Username;
		credentials +="\npass=";
		credentials +=Password;
		credentials +="\ngroupID=";
		credentials +=gid;
		credentials +="\niid=";
		credentials +=imageId;
		credentials +="]";
		credentials +=" windowless=true ";
		LociImporter lc = new LociImporter();
		ImporterOptions options = null;
		ImagePlus[] imps = null;
		try {
		  	BF.debug("parse core options");
		  	options = new ImporterOptions();
		    options.loadOptions();
		    options.parseArg(credentials);
		    options.checkObsoleteOptions();
        
   			ImportProcess process = new ImportProcess(options);
      		BF.debug("display option dialogs");
      		new ImporterPrompter(process);
      		process.execute();
      		BF.debug("display metadata");
      		DisplayHandler displayHandler = new DisplayHandler(process);
      		displayHandler.displayOriginalMetadata();
      		displayHandler.displayOMEXML();
      		BF.debug("read pixel data");
      		ImagePlusReader reader = new ImagePlusReader(process);
      		if (options.isViewNone()) return null;
      	    if (!options.isQuiet()) reader.addStatusListener(displayHandler);
      	    imps = reader.openImagePlus();
      	    if (!process.getOptions().isVirtual()) process.getReader().close();
    	} catch (FormatException exc) {
  			boolean quiet = options == null ? false : options.isQuiet();
  			WindowTools.reportException(exc, quiet,
    		"Sorry, there was a problem during import.");
    	} catch (IOException exc) {
  			boolean quiet = options == null ? false : options.isQuiet();
  			WindowTools.reportException(exc, quiet,
    		"Sorry, there was an I/O problem during import.");
		}
		 //*/
		//lc.run(credentials); // this works!
		ImagePlus imp = imps[0];
		imp.hide();
        return imp;
    }

    /**
     * Connects to OMERO and returns a gateway instance allowing to interact with the server
     * @return See above
     * @throws Exception
     */
     
    private Gateway connectToOMERO() throws Exception {
    		JTextField tf,tf1,tf2;
    	try{
    		ResultsTable prevChoices=ResultsTable.open(IJ.getDirectory("plugins")+"Leidenuniv/Omero/OmeroSettings.csv");
    		HOST=prevChoices.getStringValue("Host",0);
    		if  (HOST==null){
    			HOST="omero.services.universiteitleiden.nl";
    		}
    		Username=prevChoices.getStringValue("Username",0);
    		PORT=Integer.parseInt(prevChoices.getStringValue("Port",0));
    		tf1 = new JTextField(HOST,25); //Host
    		tf = new JTextField(Username,25); //username
    		tf2 = new JTextField(""+PORT,15); //Port
    	} catch (Exception e){
    		tf1 = new JTextField(25); //Host
    		tf = new JTextField(25); //username
    		tf2 = new JTextField(15); //Port
    	}
    	IJ.log("\\Clear");
    	
    	//Menu for login pop-up, to add host, and port so it can be used universily
		GenericDialog userData = new GenericDialog("Fill in login information");
		GridBagConstraints gb = new GridBagConstraints();
        gb.gridx=GridBagConstraints.RELATIVE;
        gb.anchor=GridBagConstraints.WEST;
        gb.insets.left=20;
        userData.addMessage("Host");
  
        userData.add(tf1, gb);
        userData.addMessage("");
        userData.addMessage("Username");
        
        userData.add(tf, gb);
        userData.addMessage("");
        userData.addMessage("Password");
        JPasswordField jp = new JPasswordField(25);
        userData.add(jp, gb);
        userData.addMessage("");
        userData.addMessage("Port");
        
        userData.add(tf2, gb);
        userData.addMessage("");
        userData.addCheckbox("Save credentials", true);
        userData.showDialog();
        HOST = tf1.getText();
        Username = tf.getText();
        PORT=Integer.parseInt(tf2.getText());
        char[] ch = jp.getPassword();
        Password = new String(ch);
        SaveCred= userData.getNextBoolean();
        if (SaveCred){
        	File f1 = new File(IJ.getDirectory("plugins")+"Leidenuniv");
			if (!f1.exists()){
				new File(IJ.getDirectory("plugins")+"Leidenuniv").mkdir();
			}
			File f = new File(IJ.getDirectory("plugins")+"Leidenuniv/Omero");
			if (!f.exists()){
				new File(IJ.getDirectory("plugins")+"Leidenuniv/Omero").mkdir();
			}
			ResultsTable choicetable = new ResultsTable();
			choicetable.incrementCounter();
			choicetable.addValue("Host",HOST);
			choicetable.addValue("Username",Username);
			choicetable.addValue("Port",PORT);
			choicetable.saveAs(IJ.getDirectory("plugins")+"Leidenuniv/Omero/OmeroSettings.csv");
        }
        //IJ.log("0");
        credentials = new LoginCredentials(Username,Password,HOST,PORT);
        SimpleLogger simpleLogger = new SimpleLogger();
        //NullLogger nl = new NullLogger();
        Gateway gateway = new Gateway(simpleLogger);
        gateway.connect(credentials);
        // Make an option for storing username, and host, and port
        
        //IJ.log("1");
        return gateway;
    }

    /**
     * Returns the images contained in the specified dataset.
     *
     * @param gateway The gateway
     * @return See above
     * @throws Exception
     */
    private Collection<ImageData> getImages(Gateway gateway)
            throws Exception
    {
        browser = gateway.getFacility(BrowseFacility.class);
        ExperimenterData user = gateway.getLoggedInUser();
        cUid= user.getId();
        List<GroupData> lgd = user.getGroups(); // get all groups you have access to
        GroupData gd1= lgd.get(0);
        ctx = new SecurityContext(gd1.getId());
        Set<GroupData> groupset = gateway.getFacility(BrowseFacility.class).getAvailableGroups(ctx, user);
        groupsetarray = groupset.toArray();

//        Iterator<GroupData> gIt=lgd.iterator(); 
        gIds= new long[groupsetarray.length];
        gNames = new String[groupsetarray.length];
        gSortedNames = new String[groupsetarray.length];
        
        
        for (int g=0;g<groupsetarray.length;g++) {
        	GroupData gda= (GroupData)groupsetarray[g];
        	gNames[g]=(gda.getName());
			gSortedNames[g]=(gda.getName());
			gIds[g]=gda.getId();
        }
		
		GroupData grd2= (GroupData)groupsetarray[0];
		Set<ExperimenterData> users = grd2.getExperimenters();
        //IJ.log(""+users.size());
        uIds= new long[users.size()];
        if (users.size()<2) {
        	uNames = new String[] {"All"};
        } else {
        	Iterator<ExperimenterData> uIt=users.iterator();
        	
        	uNames = new String[users.size()];
        	uSortedNames = new String[users.size()];
        	int u=0;
	        while (uIt.hasNext()){// to select projects
	        	ExperimenterData ud=uIt.next();
				uNames[u]=ud.getUserName();
				uSortedNames[u]=ud.getUserName();
				uIds[u]=ud.getId();
				u++;
	        }
        }	
        
        pjd = browser.getProjects(ctx);//, user.getId()); // this can be used to get the users data specifically
        if (pjd.size()<1){
        	pNames = new String []{"All"};
        } else {
        	Iterator<ProjectData> pIt=pjd.iterator();
        	long [] pIds= new long[pjd.size()];
        	pNames = new String[pjd.size()];
        	pSortedNames = new String[pjd.size()];
        	int p=0;
	        while (pIt.hasNext()){// to select projects
	        	ProjectData pd=pIt.next();
				pNames[p]=pd.getName();
				pSortedNames[p]=pd.getName();
				pIds[p]=pd.getId();
				p++;
	        }
        }
        Set<DatasetData> dsd;
        ProjectData cP;
		Object[] pArr=pjd.toArray();
		//IJ.log(""+pArr.length);
		Iterator<DatasetData> SetIterator;
		int c=0;
		if(pArr.length>0) {
			cP=(ProjectData)pArr[0];
			 dsd = cP.getDatasets();
		} else {
			dsd=null;
		}
        if (dsd==null||dsd.size()<1){
			sets = new String[]{"All"};
        } else {
        	sets = new String[dsd.size()];
        	SortedSets = new String[dsd.size()];
        	SetIterator = dsd.iterator();
           	dsda = dsd.toArray();
            dataSetIds= new long [dsd.size()];
            
            
            while (SetIterator.hasNext()){
    			DatasetData da = SetIterator.next();
    			sets[c]=da.getName();
    			SortedSets[c]=da.getName();
    			dataSetIds[c]=da.getId();
    			c++;
    		}
        }
        if (SortedSets!=null ) {
        	Arrays.sort(SortedSets);
        	//IJ.log("Sorting sets");
        } else {
        	SortedSets = new String[1];
        	SortedSets[0]="No Datasets";
        }
        if (pSortedNames==null) {
        	pSortedNames = new String[1];
        	pSortedNames[0]="No Projects";
        } else if (pSortedNames.length>1) {
        	Arrays.sort(pSortedNames);
        }
        if (gSortedNames.length>1) {
        	Arrays.sort(gSortedNames);
        }
        if (uSortedNames==null){
        	uSortedNames = new String[1];
        	uSortedNames[0]="No Users";
        } else if (uSortedNames.length>1) {
        	Arrays.sort(uSortedNames);
        }
        
        
        GenericDialog gd = new GenericDialog("Select Group/User/Project/Dataset");
        gd.addDialogListener(this);
        gd.addChoice("Group", gSortedNames,gSortedNames[0]);
        // here we can add user
        gd.addChoice("User", uSortedNames,user.getUserName());
        gd.addChoice("Project", pSortedNames,pSortedNames[0]);
        gd.addChoice("Dataset", SortedSets,SortedSets[0]); //nullpointer? 
        prevpchoice=-1;
        prevgchoice=-1;
        prevuchoice=-1;
        dialogItemChanged(gd,null);
        gd.showDialog();
        
        if (gd.wasCanceled()){
        	return null;
        }
        @SuppressWarnings("unused")
		int gchoice = gd.getNextChoiceIndex();
        int uchoice = gd.getNextChoiceIndex();
        int pchoice = gd.getNextChoiceIndex();
        schoice = gd.getNextChoiceIndex();
        
        //IJ.log(""+gchoice+","+pchoice+","+schoice);

        
        //after all the choice load the right images
		pArr=pjd.toArray();
		int puchoice;
		int uuchoice;
		if (pchoice>-1) {
			String pString = pSortedNames[pchoice];
			puchoice = Arrays.asList(pNames).indexOf(pString);
		} else {
			puchoice=0;
		}
		
		if (uchoice>-1) {
			String uString = uSortedNames[uchoice];
			uuchoice = Arrays.asList(uNames).indexOf(uString);
		} else {
			uuchoice=0;
		}
		if (schoice==-1) {
			schoice=0;
		}
		
		
		cP=(ProjectData)pArr[puchoice]; //indexoutofboundsexception
        dsd = cP.getDatasets();
         if (dsd.size()<1){
        	IJ.showMessage("This Project contains no Datasets");
        	return null;
        }
       	SetIterator = dsd.iterator();
       	
       	dsda = dsd.toArray();
       	sets = new String[dsd.size()];
        long [] dataSetIds= new long [dsd.size()];
        c=0;
        while (SetIterator.hasNext()){
			DatasetData da = SetIterator.next();
			sets[c]=(da.getName());
			SortedSets[c]=(da.getName()); // this now gives an error indexoutofbounds
			dataSetIds[c]=da.getId();
			c++;
		}
        Arrays.parallelSort(SortedSets);
        String sString = SortedSets[schoice];
        suchoice = Arrays.asList(sets).indexOf(sString);
        List<Long> ids = Arrays.asList(dataSetIds[suchoice]); //this is based on the dataset value, also indexoutofbounds
        dataChoice=dataSetIds[suchoice];
        @SuppressWarnings({ "unchecked", "rawtypes" })
		Collection<Long> idc =(Collection)ids;
        Collection<ImageData> images = browser.getImagesForDatasets(ctx, idc);
        credentials.setGroupID(((DatasetData)dsda[suchoice]).getGroupId());
        gateway.connect(credentials);
        if (images.size()<1){
        	IJ.showMessage("This Dataset contains no Images");
        	return null;
        }
        return images;
    }

    /**
     * 
     * @return Gets the current image collection
     */
	public Collection<ImageData> getImageCollection(){
		Collection<ImageData> images=null;
		try {
			gateway = connectToOMERO(); // connects to omero
			
            images = getImages(gateway);
		} catch (DSOutOfServiceException e){
			IJ.log("Error in step 2");
			IJ.log(e.toString());
			IJ.showMessage(e.getMessage());
			StackTraceElement[] t = e.getStackTrace();
	    	for (int i=0;i<t.length;i++){
	    		IJ.log(t[i].toString());
	    	}
	    }catch (Exception e) {
	    	IJ.log("Error in step 3");
	    	IJ.log(e.toString());
	    	IJ.log(e.getMessage());
	    	StackTraceElement[] t = e.getStackTrace();
	    	for (int i=0;i<t.length;i++){
	    		IJ.log(t[i].toString());
	    	}
            IJ.showMessage("An error occurred while loading the Collection.");
       } 
        return images;   
	}
	
	/**
	 * 
	 * @return an arraylist of imageplusses contained in the dataset.
	 */
    public ArrayList<ImagePlus> getDatasetImages() { // First thing that is run from the main plugin
        
		ArrayList<ImagePlus> imps = new ArrayList<ImagePlus>();    
        try {
			gateway = connectToOMERO(); // connects to omero
			ExperimenterData ed = gateway.getLoggedInUser();
			Long gid= ed.getGroupId();
			
            Collection<ImageData> images = getImages(gateway); //gets the collection
            //IJ.showMessage("Dataset id "+curDataset);
            if (images!=null){
            	Iterator<ImageData> image = images.iterator(); 
            	int counter=0;  
	            while (image.hasNext()) {
	            	
	            	counter++;
	                ImageData data = image.next();
	                IJ.log("Loading image "+counter+" of "+images.size());
	                // here we load all single images, can be too much data
	                ImagePlus timp = openImagePlus(data.getId(), gid);
                	// here we have the image, and we can analyse and attach data}
	                imps.add(timp);
	            }
	            
            }
        }catch (DSOutOfServiceException e){
        	 IJ.showMessage(e.getMessage());
 	    }catch (Exception e) {
        	IJ.log(e.getMessage());
        	IJ.log(e.getLocalizedMessage());
        	IJ.log(e.toString());
        	StackTraceElement[] t = e.getStackTrace();
        	for (int i=0;i<t.length;i++){
        		IJ.log(t[i].toString());
        	}
            IJ.showMessage("An error occurred while loading the image.");
        } 
        return imps;
    }
    /**
     * 
     * @return the dataset id selected
     */
    protected long getDatasetID() {
    	return dataChoice;
    }
    /**
     * 
     * @param rt the resultstable to add to the data
     * @param stringcolumns the amount of string columns in the beginning
     * @param target the object that needs the resultstable attached
     */
    protected void attachDataToImage(ResultsTable rt, int stringcolumns, DataObject target) {
    	String headings4=rt.getColumnHeadings();
    	headings4.trim();
    	String [] headings5 = headings4.split("\t");
    	TableDataColumn[] tdc = new TableDataColumn[headings5.length];
    	//Double[][] data2omero=new Double[rt.size()][headings5.length-1]; seems to be the wrong way around?
    	Object[][] data2omero=new Object[headings5.length][rt.size()]; 
    	for (int i=0;i<headings5.length;i++) {
    		if (i<stringcolumns) {
    			tdc[i]=new TableDataColumn(headings5[i],headings5[i], i, String.class);
    			for (int j=0;j<rt.size();j++) {
        			String da =rt.getStringValue(headings5[i], j);
    				data2omero[i][j]=da;
        			//IJ.log(headings5[i+1]+" "+rt.getValue(headings5[i+1], j));
        		}
    		} else {
	    		tdc[i]=new TableDataColumn(headings5[i],headings5[i], i, Double.class);
	    		for (int j=0;j<rt.size();j++) {
	    			Double d =rt.getValue(headings5[i], j);
					data2omero[i][j]=d;
	    			//IJ.log(headings5[i+1]+" "+rt.getValue(headings5[i+1], j));
	    		}
    		}
    	}
       	TableData omeroTable = new TableData(tdc, data2omero);
        // dos.uploadTable(ol,"TestData",rt2,imageId);
       	// gives an error caused by AbstractConverService, so lets try without converting.
       	// The uploadTable uses the convertservice so we need to skip that
       	// The way things are added are via TablesFacility
       	try {
       		final TablesFacility tablesFacility = gateway.getFacility(TablesFacility.class);
       		//tablesFacility.addTable(ctx, (DatasetData)dsda[schoice], "Dataset Summary", omeroTable);
       		tablesFacility.addTable(ctx, target, "Image Data", omeroTable);
       	} catch (Exception e) {
       		IJ.log(e.getMessage());
        	StackTraceElement[] t = e.getStackTrace();
        	for (int i=0;i<t.length;i++){
        		IJ.log(t[i].toString());
        	}
       	}
    }
    
    /**
     * 
     * @param rt the resultstable to add
     * @param stringcolumns the amount of string columns in the beginning
     * @param target the object that needs the resultstable attached
     * @param title of the attached table
     */
    protected void attachDataToImage(ResultsTable rt, int stringcolumns, DataObject target, String title) {
    	String headings4=rt.getColumnHeadings();
    	headings4.trim();
    	String [] headings5 = headings4.split("\t");
    	TableDataColumn[] tdc = new TableDataColumn[headings5.length];
    	//Double[][] data2omero=new Double[rt.size()][headings5.length-1]; seems to be the wrong way around?
    	Object[][] data2omero=new Object[headings5.length][rt.size()]; 
    	for (int i=0;i<headings5.length;i++) {
    		if (i<stringcolumns) {
    			tdc[i]=new TableDataColumn(headings5[i],headings5[i], i, String.class);
    			for (int j=0;j<rt.size();j++) {
        			String da =rt.getStringValue(headings5[i], j);
    				data2omero[i][j]=da;
        			//IJ.log(headings5[i+1]+" "+rt.getValue(headings5[i+1], j));
        		}
    		} else {
	    		tdc[i]=new TableDataColumn(headings5[i],headings5[i], i, Double.class);
	    		for (int j=0;j<rt.size();j++) {
	    			Double d =rt.getValue(headings5[i], j);
					data2omero[i][j]=d;
	    			//IJ.log(headings5[i+1]+" "+rt.getValue(headings5[i+1], j));
	    		}
    		}
    	}
       	TableData omeroTable = new TableData(tdc, data2omero);
        // dos.uploadTable(ol,"TestData",rt2,imageId);
       	// gives an error caused by AbstractConverService, so lets try without converting.
       	// The uploadTable uses the convertservice so we need to skip that
       	// The way things are added are via TablesFacility
       	try {
       		final TablesFacility tablesFacility = gateway.getFacility(TablesFacility.class);
       		//tablesFacility.addTable(ctx, (DatasetData)dsda[schoice], "Dataset Summary", omeroTable);
       		tablesFacility.addTable(ctx, target, title, omeroTable);
       	} catch (Exception e) {
       		IJ.log(e.getMessage());
        	StackTraceElement[] t = e.getStackTrace();
        	for (int i=0;i<t.length;i++){
        		IJ.log(t[i].toString());
        	}
       	}
    }
    
    /**
     * 
     * @param rt Resultstable to add
     * @param stringcolumns number of string columns in the table
     */
    protected void attachDataToDataset(ResultsTable rt, int stringcolumns) {
    	String headings4=rt.getColumnHeadings();
    	headings4.trim();
    	String [] headings5 = headings4.split("\t");
    	TableDataColumn[] tdc = new TableDataColumn[headings5.length];
    	//Double[][] data2omero=new Double[rt.size()][headings5.length-1]; seems to be the wrong way around?
    	Object[][] data2omero=new Object[headings5.length][rt.size()]; 
    	for (int i=0;i<headings5.length;i++) {
    		if (i<stringcolumns) {
    			tdc[i]=new TableDataColumn(headings5[i],headings5[i], i, String.class);
    			for (int j=0;j<rt.size();j++) {
        			String da =rt.getStringValue(headings5[i], j);
    				data2omero[i][j]=da;
        			//IJ.log(headings5[i+1]+" "+rt.getValue(headings5[i+1], j));
        		}
    		} else {
	    		tdc[i]=new TableDataColumn(headings5[i],headings5[i], i, Double.class);
	    		for (int j=0;j<rt.size();j++) {
	    			Double d =rt.getValue(headings5[i], j);
					data2omero[i][j]=d;
	    			//IJ.log(headings5[i+1]+" "+rt.getValue(headings5[i+1], j));
	    		}
    		}
    	}
       	TableData omeroTable = new TableData(tdc, data2omero);
        // dos.uploadTable(ol,"TestData",rt2,imageId);
       	// gives an error caused by AbstractConverService, so lets try without converting.
       	// The uploadTable uses the convertservice so we need to skip that
       	// The way things are added are via TablesFacility
       	try {
       		final TablesFacility tablesFacility = gateway.getFacility(TablesFacility.class);
       		tablesFacility.addTable(ctx, (DatasetData)dsda[schoice], "Dataset Summary", omeroTable);
       	} catch (Exception e) {
       		IJ.log(e.toString());
       		IJ.log(e.getMessage());
        	StackTraceElement[] t = e.getStackTrace();
        	for (int i=0;i<t.length;i++){
        		IJ.log(t[i].toString());
        	}
       	}
    }
    /**
     * 
     * @param rt Resultstable to add
     * @param stringcolumns number of string columns in the table
     * @param title title of the attached table
     */
    protected void attachDataToDataset(ResultsTable rt, int stringcolumns, String title) {
    	String headings4=rt.getColumnHeadings();
    	headings4.trim();
    	String [] headings5 = headings4.split("\t");
    	TableDataColumn[] tdc = new TableDataColumn[headings5.length];
    	//Double[][] data2omero=new Double[rt.size()][headings5.length-1]; seems to be the wrong way around?
    	Object[][] data2omero=new Object[headings5.length][rt.size()]; 
    	for (int i=0;i<headings5.length;i++) {
    		if (i<stringcolumns) {
    			tdc[i]=new TableDataColumn(headings5[i],headings5[i], i, String.class);
    			for (int j=0;j<rt.size();j++) {
        			String da =rt.getStringValue(headings5[i], j);
    				data2omero[i][j]=da;
        			//IJ.log(headings5[i+1]+" "+rt.getValue(headings5[i+1], j));
        		}
    		} else {
	    		tdc[i]=new TableDataColumn(headings5[i],headings5[i], i, Double.class);
	    		for (int j=0;j<rt.size();j++) {
	    			Double d =rt.getValue(headings5[i], j);
					data2omero[i][j]=d;
	    			//IJ.log(headings5[i+1]+" "+rt.getValue(headings5[i+1], j));
	    		}
    		}
    	}
       	TableData omeroTable = new TableData(tdc, data2omero);
        // dos.uploadTable(ol,"TestData",rt2,imageId);
       	// gives an error caused by AbstractConverService, so lets try without converting.
       	// The uploadTable uses the convertservice so we need to skip that
       	// The way things are added are via TablesFacility
       	try {
       		final TablesFacility tablesFacility = gateway.getFacility(TablesFacility.class);
       		tablesFacility.addTable(ctx, (DatasetData)dsda[suchoice], title, omeroTable);
       	} catch (Exception e) {
       		IJ.log(e.getMessage());
        	StackTraceElement[] t = e.getStackTrace();
        	for (int i=0;i<t.length;i++){
        		IJ.log(t[i].toString());
        	}
       	}
    }
    /**
     * Happens on changing the selection in the genericdialog to select the correct dataset.
     */
    public boolean dialogItemChanged(GenericDialog dlog, AWTEvent ev){ // this happens when something is changed in the dataset selection menu
		Component [] cps =dlog.getComponents();
        int gchoice=0;
        int pchoice=0;
        int uchoice=0;
        @SuppressWarnings("unused")
		boolean noprojects=false;
		
		//IJ.log("prevg "+prevgchoice+", "+((Choice)cps[1]).getSelectedIndex());
        //IJ.log("prevu "+prevuchoice+", "+((Choice)cps[3]).getSelectedIndex());
        //IJ.log("prevp "+prevpchoice+", "+((Choice)cps[5]).getSelectedIndex());
		for (int i=0;i<cps.length;i++){ // run through all components
			switch (i){
				case 0:
					//this is the string group in the display, cannot change
				break;
				case 1: // a group change happened
					gchoice = ((Choice)cps[i]).getSelectedIndex(); // get the current groupchoice
					cps[i].setSize(180,10);
					if (prevgchoice!=gchoice){
						uchoice=0; // always reset to the first user on a group change
					}
				break;
				case 2://this is the string User in the display, cannot change
				break;
				case 3:
					uchoice = ((Choice)cps[i]).getSelectedIndex(); // get the current user choice
					cps[i].setSize(180,10);
					if (prevgchoice!=gchoice){ // we need to load new users
						try {
							String gString =gSortedNames[gchoice]; // gets the groupchoice
							int guchoice =Arrays.asList(gNames).indexOf(gString); // gets the index of this choice in the id array
							String uString =uSortedNames[uchoice];
							int uuchoice =Arrays.asList(uNames).indexOf(uString);
							//IJ.log(gString);
							ctx = new SecurityContext(gIds[guchoice]);
							cUid = uIds[uuchoice]; // error sometimes?
							
							GroupData grd2= (GroupData)groupsetarray[guchoice];
							Set<ExperimenterData> users = grd2.getExperimenters();
					        
							uIds= new long[users.size()];
					        ((Choice)cps[i]).removeAll();
					        if (users.size()<1){
					        	((Choice)cps[i]).add("No Users");
					        	((Choice)cps[i+2]).removeAll();
					        	((Choice)cps[i+2]).add("No Projects");
					        	((Choice)cps[i+4]).removeAll();
					        	((Choice)cps[i+4]).add("No Datasets");
					        	noprojects=true;
					        	return false;
					        }
					        Iterator<ExperimenterData> uIt=users.iterator();
					        int u=0;
					        ArrayList<String> newchoices=new ArrayList<String>();
					        while (uIt.hasNext()){// to select projects
					        	ExperimenterData ud=uIt.next();
					        	//((Choice)cps[i]).add(pd.getName()); need to replace this with an ordered version
					        	newchoices.add(ud.getUserName());
								// new choices need to be sorted
								uIds[u]=ud.getId();
								u++;
					        }
							String [] newstrings=new String [newchoices.size()];
							uNames=new String [newchoices.size()];
							for (int k=0;k<newchoices.size();k++) {
								newstrings[k]=newchoices.get(k);
								uNames[k]=newchoices.get(k);
							}

							Arrays.sort(newstrings);
					        for (int j=0;j<newstrings.length;j++) {
					        	((Choice)cps[i]).add(newstrings[j]);
					        }
					        uSortedNames=newstrings;
					        
						} catch (Exception e){
							IJ.log(e.getMessage());
				        	StackTraceElement[] t = e.getStackTrace();
				        	for (int k=0;k<t.length;k++){
				        		IJ.log(t[k].toString());
				        	}
						}
					} 
				break;
				case 4:
					//this is the string of projects , cannot change
				break;
				case 5:
					pchoice = ((Choice)cps[i]).getSelectedIndex();
					uchoice = ((Choice)cps[i-2]).getSelectedIndex(); // should not be needed since it gets updated with i=3
					cps[i].setSize(180,10);
					String uString =uSortedNames[uchoice];
					int uuchoice =Arrays.asList(uNames).indexOf(uString);
					if (uuchoice<0) { // if the user does not exist in the list then the first one is chosen
						uuchoice=0;
					}
					cUid = uIds[uuchoice];
					if (prevgchoice!=gchoice || prevuchoice!=uchoice){
						//IJ.log("Changing menu 2 :gchoice" +gchoice+","+prevgchoice);
						try {
							String gString =gSortedNames[gchoice];
							int guchoice =Arrays.asList(gNames).indexOf(gString);
							//IJ.log(gString);
							ctx = new SecurityContext(gIds[guchoice]);
					        pjd = browser.getProjects(ctx, cUid);//, user.getId());
					        // this does not work with the user id present
					        //pArr=pjd.toArray();
					        //IJ.log("group id "+gIds[gchoice]+", size "+pjd.size());
					        ((Choice)cps[i]).removeAll();
					        if (pjd.size()<1){
					        	((Choice)cps[i]).add("No Projects");
					        	((Choice)cps[i+2]).removeAll();
					        	((Choice)cps[i+2]).add("No Datasets");
					        	prevpchoice=pchoice;
					        	prevgchoice=gchoice;
					    		prevuchoice=uchoice;
					        	noprojects=true;
					        	return false;
					        }
					        Iterator<ProjectData> pIt=pjd.iterator();
					        long [] pIds= new long[pjd.size()];
					        int p=0;
					        ArrayList<String> newchoices=new ArrayList<String>();
					        while (pIt.hasNext()){// to select projects
					        	ProjectData pd=pIt.next();
					        	//((Choice)cps[i]).add(pd.getName()); need to replace this with an ordered version
					        	newchoices.add(pd.getName());
								// new choices need to be sorted
								pIds[p]=pd.getId();
								p++;
					        }
							String [] newstrings=new String [newchoices.size()];
							pNames=new String [newchoices.size()];
							for (int k=0;k<newchoices.size();k++) {
								newstrings[k]=newchoices.get(k);
								pNames[k]=newchoices.get(k);
							}

							Arrays.sort(newstrings);
					        for (int j=0;j<newstrings.length;j++) {
					        	((Choice)cps[i]).add(newstrings[j]);
					        }
					        pSortedNames=newstrings;
					        
						} catch (Exception e){
							IJ.log(e.getMessage());
				        	StackTraceElement[] t = e.getStackTrace();
				        	for (int k=0;k<t.length;k++){
				        		IJ.log(t[k].toString());
				        	}
						}
						
					}
				break;
				case 6://this is the string dataset in the display, cannot change
				break;
				case 7:
					cps[i].setSize(180,10);
					//schoice = ((Choice)cps[i]).getSelectedIndex();
					if (pjd!=null){
						if (prevpchoice!=pchoice || prevgchoice!=gchoice ||prevuchoice!=uchoice){
							if (prevgchoice!=gchoice||prevuchoice!=uchoice) {
								pchoice=0;
							}
							//IJ.log(""+pSortedNames.length);
							String pString = pSortedNames[pchoice];
							int puchoice = Arrays.asList(pNames).indexOf(pString);
							if (puchoice==-1) {
								puchoice=0;
							}
							//IJ.log(""+pString);
							Object[] pArr=pjd.toArray();
							ProjectData cP=(ProjectData)pArr[puchoice];
					        Set<DatasetData> dsd = cP.getDatasets();
					        ((Choice)cps[i]).removeAll();
					        if (dsd.size()<1){
						        ((Choice)cps[i]).add("No Datasets");
						        //IJ.log("pjs" +pjd.size());
						        //IJ.log("doing this");
					        } 
					       	Iterator<DatasetData> SetIterator = dsd.iterator();
					       	long [] dataSetIds= new long [dsd.size()];
					        int c=0;
					        ArrayList<String> newchoices=new ArrayList<String>();
					        while (SetIterator.hasNext()){
								DatasetData da = SetIterator.next();
								//((Choice)cps[i]).add(da.getName());
								newchoices.add(da.getName());
								dataSetIds[c]=da.getId();
								c++;
							}
					        String [] newstrings=new String [newchoices.size()];
							sets=new String [newchoices.size()];
							for (int k=0;k<newchoices.size();k++) {
								newstrings[k]=newchoices.get(k);
								sets[k]=newchoices.get(k);
							}
							Arrays.sort(newstrings);
					        for (int j=0;j<newstrings.length;j++) {
					        	((Choice)cps[i]).add(newstrings[j]);
					        }
					        SortedSets=newstrings;
						}
					    
					} 
				break;
				case 8:
				break;
			}
		}
		prevpchoice=pchoice;
		prevgchoice=gchoice;
		prevuchoice=uchoice;
		return true;
	}
    
    /**
     * 
     * @param rois array of rois to attach
     * @param target data object target
     * @param name the name of the object
     */
	protected void attachRoisToImage(Roi[] rois, DataObject target, String name) {
       	Collection<ROIData> roiList = roiArrayToCollection(rois);
       	try {
       		final ROIFacility RoiFacility = gateway.getFacility(ROIFacility.class);
       		RoiFacility.saveROIs(ctx, target.getId(), roiList);
       	} catch (Exception e) {
       		IJ.log(e.getMessage());
        	StackTraceElement[] t = e.getStackTrace();
        	for (int i=0;i<t.length;i++){
        		IJ.log(t[i].toString());
        	}
       	}
    }
	
	/**
	 * turns roi[] into collection of ROIData
	 * @param rois the roi [] to convert
	 * @return the converted roi array as collection of ROIData
	 */
	public Collection<ROIData> roiArrayToCollection(Roi[] rois){
		/* we now have a ij.gui.Roi array which needs to be converted to a ROIData List
		// a single ROIData contains a list of ShapeData which we can add
		// a ShapeData can be a PolygonData
		// a Polygondata can be set to contain a List of Point2D s
		// A point2D can be created using a x and y coordinate
		 * 
		 * A Roi Array can be used to get getFloatPolygon
		 * each FloatPolygon contains npoints in an array of xpoints and ypoints
		 */

		ROIData rd = new ROIData();
		for (int i =0;i<rois.length;i++) {
			Roi cRoi = rois[i];
			FloatPolygon fp = cRoi.getFloatPolygon();
			ArrayList<Point2D.Double> pts = new ArrayList<Point2D.Double>();
			for (int j=0; j<fp.npoints;j++) {
				pts.add(new Point2D.Double(fp.xpoints[j],fp.ypoints[j]));
			}
			PolygonData pd = new PolygonData((List<Point2D.Double>)pts);
			pd.setText(cRoi.getName());
			pd.setZ(cRoi.getZPosition());
			rd.addShapeData((ShapeData)pd);
		}
		ArrayList<ROIData> rds = new ArrayList<ROIData>();
		rds.add(rd);
		return rds;
	}
	/**
	 * 
	 * @param file the file to attach
	 * @param description a description of the file
	 * @param NAME_SPACE_TO_SET the name space of the filtype you want to attach
	 * @param datatype the type of data to set the Mimetype
	 * @param image the image to attach it to
	 */
	protected void attachFile(File file, String description, String NAME_SPACE_TO_SET, String datatype, ImageData image) {
		int INC = 262144;
		try {
			DataManagerFacility dm = gateway.getFacility(DataManagerFacility.class);
			//create the original file object.
			OriginalFile originalFile = new OriginalFileI();
			originalFile.setName(omero.rtypes.rstring(file.getName()));
			originalFile.setPath(omero.rtypes.rstring(file.getPath()));
			originalFile.setSize(omero.rtypes.rlong(file.length()));
			final ChecksumAlgorithm checksumAlgorithm = new ChecksumAlgorithmI();
			checksumAlgorithm.setValue(omero.rtypes.rstring(ChecksumAlgorithmSHA1160.value));
			originalFile.setHasher(checksumAlgorithm);
			originalFile.setMimetype(omero.rtypes.rstring(datatype)); // or "application/octet-stream"
			//Now we save the originalFile object
			originalFile = (OriginalFile) dm.saveAndReturnObject(ctx, originalFile);
			//Initialize the service to load the raw data
			RawFileStorePrx rawFileStore = gateway.getRawFileService(ctx);
			long pos = 0;
			int rlen;
			byte[] buf = new byte[INC];
			ByteBuffer bbuf;
			//Open file and read stream
			try (FileInputStream stream = new FileInputStream(file)) {
			    rawFileStore.setFileId(originalFile.getId().getValue());
			    while ((rlen = stream.read(buf)) > 0) {
			        rawFileStore.write(buf, pos, rlen);
			        pos += rlen;
			        bbuf = ByteBuffer.wrap(buf);
			        bbuf.limit(rlen);
			    }
			    originalFile = rawFileStore.save();
			} catch (ServerError e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
			   try {
				rawFileStore.close();
				} catch (ServerError e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			//now we have an original File in DB and raw data uploaded.
			//We now need to link the Original file to the image using
			//the File annotation object. That's the way to do it.
			FileAnnotation fa = new FileAnnotationI();
			fa.setFile(originalFile);
			fa.setDescription(omero.rtypes.rstring(description)); // The description set above e.g. PointsModel
			fa.setNs(omero.rtypes.rstring(NAME_SPACE_TO_SET)); // The name space you have set to identify the file annotation.

			//save the file annotation.
			fa = (FileAnnotation) dm.saveAndReturnObject(ctx, fa);

			//now link the image and the annotation
			ImageAnnotationLink link = new ImageAnnotationLinkI();
			link.setChild(fa);
			link.setParent(image.asImage());
			//save the link back to the server.
			link = (ImageAnnotationLink) dm.saveAndReturnObject(ctx, link);
			// o attach to a Dataset use DatasetAnnotationLink;
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DSOutOfServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DSAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
// used for testing when running the plugin stand alone
    //public static void run main(String[] args) {
	
	/**
	 * to run the plugin alone, used for testing purposes
	 */
	public void run (String args) {
    	//ImageJ imageJ = new ImageJ();
    	/*ImagePlus imp = IJ.openImage("http://imagej.nih.gov/ij/images/blobs.gif");
    	IJ.run(imp, "Convert to Mask", "");
    	RoiManager rm = RoiManager.getInstance();
		if (rm!=null){
			rm.close();
			rm = new RoiManager();
		} else {
			rm = new RoiManager();
		}
		IJ.run(imp, "Analyze Particles...", "size=100-Infinity exclude add");
		Roi[] Rois=rm.getRoisAsArray();
		IJ.log(""+Rois.length);*/
		
    	getOmeroDatasetAndAttachData om = new getOmeroDatasetAndAttachData();
    	Collection<ImageData> images = om.getImageCollection(); // this gives the version error
    	/*Long gid= om.gateway.getLoggedInUser().getGroupId();
        ExperimenterData user = om.gateway.getLoggedInUser();
        try {
			Set<GroupData> lgd3 = om.gateway.getFacility(BrowseFacility.class).getAvailableGroups(om.ctx, user);
			 Object[] grd = lgd3.toArray();
			 GroupData grd2= (GroupData)grd[0];
			 Set<ExperimenterData> ex1 = grd2.getExperimenters();
		} catch (DSOutOfServiceException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (DSAccessException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ExecutionException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}*/

    	if (images!=null){
        	Iterator<ImageData> image = images.iterator();
        	ImageData data = image.next();
        	File f = new File("f:/temp/Dots 0.tif");
        	om.attachFile(f, "test", "imagedata", "image/tiff", data);
        	 /*while (image.hasNext()) {
        		 ImageData data = image.next();
        		 try {
	                	//om.attachRoisToImage(Rois,data,"test");
        			 ImagePlus timp = om.openImagePlus(data.getId(), data,gid);
        			 timp.show();
	                } catch (Exception e) {
	                	 IJ.log(e.toString());
	                	 IJ.log(e.getLocalizedMessage());
	                	 IJ.log(e.getMessage());
	                	 StackTraceElement[] t = e.getStackTrace();
	                 	for (int i=0;i<t.length;i++){
	                 		IJ.log(t[i].toString());
	                 	}
	                }
        	 }*/
    	}
    }
}