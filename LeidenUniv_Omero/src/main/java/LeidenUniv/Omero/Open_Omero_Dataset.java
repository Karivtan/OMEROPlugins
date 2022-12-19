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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Arrays;
import java.util.Set;
import java.util.Iterator;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import java.awt.Window;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Choice;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.DialogListener;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import loci.formats.FormatException;
import loci.plugins.BF;
import loci.plugins.LociImporter;
import loci.plugins.in.DisplayHandler;
import loci.plugins.in.ImagePlusReader;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.ImporterOptions;
import loci.plugins.in.ImporterPrompter;
import loci.plugins.util.WindowTools;
import net.imagej.omero.*;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import omero.gateway.Gateway;
import omero.gateway.SecurityContext;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.LoginCredentials;
import omero.gateway.model.ImageData;
import omero.gateway.model.DatasetData;
import omero.gateway.model.ProjectData;
import omero.gateway.model.GroupData;
import omero.gateway.model.ExperimenterData;
import omero.gateway.exception.DSOutOfServiceException;
import omero.client;
import omero.log.NullLogger;
import omero.log.SimpleLogger;
import org.scijava.Context;
import net.imglib2.img.display.imagej.ImageJFunctions;


/**
 * This script uses ImageJ to open a complete dataset of images
 * TODO after selecting alg adimns group the menu does not update properly
 */
public class Open_Omero_Dataset implements PlugIn, DialogListener{

    // Edit value
    private String Username = "";
    private static String HOST = "omero.services.universiteitleiden.nl";
    //"omero.liacs.nl";
    //"omeroweb.services.universiteitleiden.nl";
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
	long [] dataSetIds=null;
	private long dataChoice, cUid;
	private Gateway gateway = null;
	private Dataset d;
	private Object[] dsda;
	private Object[] groupsetarray;
    /**
     * Open an image using the Bio-Formats importer.
     *
     * @param imageId The id of the image to open
     * @throws Exception
     */
    
    @SuppressWarnings("rawtypes")
	private ImagePlus openImagePlus(Long imageId, ImageData data) //should be able to construct a general class for this!
        throws Exception
    {
        OMEROLocation ol = new OMEROLocation(HOST,PORT,Username,Password);
		net.imagej.ImageJ ij = new net.imagej.ImageJ();
		Context context =ij.context();
		OMEROService dos = context.service(OMEROService.class);
		OMEROSession os = dos.createSession(ol);
		client cl = os.getClient();
		
		String credentials ="location=[OMERO] open=[omero:server=";
		credentials += cl.getProperty("omero.host");
		credentials +="\nuser=";
		credentials +=Username;
		credentials +="\npass=";
		credentials +=Password;
		credentials +="\ngroupID=";
		credentials +=data.getGroupId();
		credentials +="\niid=";
		credentials +=imageId;
		credentials +="]";
		credentials +=" windowless=true ";
		LociImporter lc = new LociImporter();
		// uses a display handler to show the file. can potentially be done without that by recoding the importer here
		/*
		 *  
		 *  
		 *  /
		 */
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
        
        /*
        Dataset d = dos.downloadImage(cl,imageId);
        ImgPlus implu=d.getImgPlus();
        @SuppressWarnings("unchecked")
		ImagePlus imp = ImageJFunctions.wrap(implu,"My desired imageplus");
        //imp.show();
        return imp;*/
    }

    /**
     * Connects to OMERO and returns a gateway instance allowing to interact with the server
     * 
     * @return the connected gateway
     * @throws Exception when things go wrong connecting
     */
     
    private Gateway connectToOMERO() throws Exception { 
    		JTextField tf,tf1,tf2;
    	try{
    		ResultsTable prevChoices=ResultsTable.open(IJ.getDirectory("plugins")+"Leidenuniv/Omero/OmeroSettings.csv"); // this loads the latest OMERO login settings to remember user, server, and port number
    		HOST=prevChoices.getStringValue("Host",0);
    		Username=prevChoices.getStringValue("Username",0);
    		PORT=Integer.parseInt(prevChoices.getStringValue("Port",0));
    		tf1 = new JTextField(HOST,25); //Host
    		tf = new JTextField(Username,25); //username
    		tf2 = new JTextField(""+PORT,15); //Port
    	} catch (Exception e){// if nothing can be loaded start an empty field
    		tf1 = new JTextField(25); //Host
    		tf = new JTextField(25); //username
    		tf2 = new JTextField(15); //Port
    	}
    	IJ.log("\\Clear");
    	//Menu for login pop-up, to add host, and port so it can be used universally
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
        credentials = new LoginCredentials(Username,Password,HOST,PORT);
        SimpleLogger simpleLogger = new SimpleLogger();
        //NullLogger simpleLogger = new NullLogger();
        Gateway gateway = new Gateway(simpleLogger);
        gateway.connect(credentials);
        // Make an option for storing username, and host, and port
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
        return gateway;
    }

    /**
     * Returns the images contained in the dataset that can be selected.
     *
     * @param gateway The gateway
     * @return See above
     * @throws Exception
     * 
     */
     
    //private Collection<ImageData> getImages(Gateway gateway) //2nd part of the plugin after omero connection is established
    private Collection<ImageData> getImages(Gateway gateway)
            throws Exception
    {
        browser = gateway.getFacility(BrowseFacility.class); // set the browsefacility
        ExperimenterData user = gateway.getLoggedInUser(); // get logged in uder
        cUid= user.getId(); // get user id
        List<GroupData> lgd = user.getGroups(); // get all groups you have access to
        GroupData gd1= lgd.get(0); // get the first group,used to get the allowed access data
        ctx = new SecurityContext(gd1.getId()); // reload the security context
        Set<GroupData> groupset = gateway.getFacility(BrowseFacility.class).getAvailableGroups(ctx, user); // get all available groups
        groupsetarray = groupset.toArray();// make an array of the groups people have access to

        gIds= new long[groupsetarray.length]; // for the groupIDs
        gNames = new String[groupsetarray.length]; //for the group names
        gSortedNames = new String[groupsetarray.length]; //for the alfabetically sorted names 
        
        for (int g=0;g<groupsetarray.length;g++) { // go through all the groups you have access to
        	GroupData gda= (GroupData)groupsetarray[g]; // take the current Groupdata
        	gNames[g]=(gda.getName()); // get the name
			gSortedNames[g]=(gda.getName()); // add the name also to the sortedarray
			gIds[g]=gda.getId(); // add the ids to the id array
        }

        GroupData grd2= (GroupData)groupsetarray[0]; // get the first group to display in the menu
		Set<ExperimenterData> users = grd2.getExperimenters(); // get all potential users in that group
        uIds= new long[users.size()]; // make a user id array
        if (users.size()<2) { // if there is only one used display all
        	uNames = new String[] {"All"};
        } else { // otherwise add all the potential users to a list
        	Iterator<ExperimenterData> uIt=users.iterator();
        	uNames = new String[users.size()];
        	uSortedNames = new String[users.size()];
        	int u=0;
	        while (uIt.hasNext()){// get data of all users you have access to
	        	ExperimenterData ud=uIt.next();
				uNames[u]=ud.getUserName();
				uSortedNames[u]=ud.getUserName();
				uIds[u]=ud.getId();
				u++;
	        }
        }	
        
        pjd = browser.getProjects(ctx);//, user.getId()); // this can be used to get the users data specifically
        if (pjd.size()<1){// if there is only one project display all
        	pNames = new String []{"All"};
        } else {// else show the names
        	Iterator<ProjectData> pIt=pjd.iterator();
        	long [] pIds= new long[pjd.size()];
        	pNames = new String[pjd.size()];
        	pSortedNames = new String[pjd.size()];
        	int p=0;
	        while (pIt.hasNext()){// add all project names to the menu
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
	// Add all datasetdata to the choice lists
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
        
// Sort all the data for the menu
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

// create the menu for selecting which data you want to open
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
        dialogItemChanged(gd,null); // runs through the updating of the menu once
        gd.showDialog();
        
        if (gd.wasCanceled()){ // if cancel was pressed to not open anything.
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
		
		cP=(ProjectData)pArr[puchoice]; 
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
			SortedSets[c]=(da.getName()); 
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
        gateway.connect(credentials); // reconnect to get the right privileges
        if (images.size()<1){
        	IJ.showMessage("This Dataset contains no Images");
        	return null;
        }
        return images;
    }
 
    /**
     * 
     * @return the arraylist of imagePlus's in the dataset
     */
    public ArrayList<ImagePlus> getDatasetImages() {
        Gateway gateway = null; // initialize the gateway
		ArrayList<ImagePlus> imps = new ArrayList<ImagePlus>(); //create an empty arraylist   
        try {
			gateway = connectToOMERO(); // connects to omero
            Collection<ImageData> images = getImages(gateway); //gets the collection
            if (images!=null){
            	Iterator<ImageData> image = images.iterator(); 
            	int counter=0;  
	            while (image.hasNext()) {
	            	
	            	counter++;
	                ImageData data = image.next();
	                //need to get the log window and position it
	                Window logwindow =WindowManager.getWindow("Log");
	                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
					int width = (int)screenSize.getWidth();
					int height = (int)screenSize.getHeight();
	                //logwindow.setSize(width/4, height/2);
	                //logwindow.setLocation(0, 0);
	                //logwindow.toFront();
	                IJ.log("Loading image "+counter+" of "+images.size());
	                ImagePlus timp = openImagePlus(data.getId(), data);
	                imps.add(timp);
	            }
            }
        }catch (DSOutOfServiceException e){
        	 IJ.showMessage(e.getMessage());
 	    }catch (Exception e) {
        	IJ.log(e.getMessage());
        	StackTraceElement[] t = e.getStackTrace();
        	for (int i=0;i<t.length;i++){
        		IJ.log(t[i].toString());
        	}
            IJ.showMessage("An error occurred while loading the image.");
        } finally {
            //if (gateway != null) gateway.disconnect();
        }
        return imps;
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
// used for testing when running the plugin stand alone
    /**
     * runs the plugin and opens a selected dataset image set and displays them.
     * @param args not used currently
     * for testing the plugin
     */
    public static void main(String[] args) {
    	Open_Omero_Dataset om = new Open_Omero_Dataset();
        ArrayList<ImagePlus> imps = om.getDatasetImages();
        for (int i=0;i<imps.size();i++){
        	imps.get(i).show();
        }
    }

	@Override
	/**
	 * runs the plugin and opens a selected dataset image set and displays them.
	 */
	public void run(String arg0) {
		ArrayList<ImagePlus> imps = getDatasetImages();
        for (int i=0;i<imps.size();i++){
        	imps.get(i).show();
        }
	}
}