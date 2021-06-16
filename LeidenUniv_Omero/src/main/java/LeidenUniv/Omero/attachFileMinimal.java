package LeidenUniv.Omero;
import ij.IJ;
import ij.ImagePlus;
import omero.gateway.Gateway;
import omero.gateway.LoginCredentials;
import omero.log.SimpleLogger;
import net.imagej.omero.*;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import omero.client;
import omero.gateway.SecurityContext;
import net.imglib2.img.display.imagej.ImageJFunctions;
import org.scijava.Context;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.model.ImageData;
import omero.gateway.model.GroupData;
import omero.gateway.model.ExperimenterData;
import java.util.List;
import java.util.Iterator;
import omero.gateway.model.TableData;
import omero.gateway.model.TableDataColumn;
import omero.gateway.facility.TablesFacility;

public class attachFileMinimal {
    private String Username = "willemsejj";
    private static String HOST = "omero.services.universiteitleiden.nl";
    private static int PORT = 4064;
    private String Password = "Jan2020J005t";
	private long imageId=1535;

    public void uploadFile() {
        Gateway gateway = null;
        SecurityContext ctx=null;
        try {
			LoginCredentials credentials = new LoginCredentials(Username,Password,HOST,PORT);
        	SimpleLogger simpleLogger = new SimpleLogger();
       		gateway = new Gateway(simpleLogger);
        	gateway.connect(credentials);
        	ExperimenterData ed = gateway.getLoggedInUser();
        	List<GroupData> grda= ed.getGroups();
        	//This is the way to get via browsefacility
        	BrowseFacility browser = gateway.getFacility(BrowseFacility.class);
        	Iterator<GroupData> gidit=grda.iterator();
        	int counter =0;
        	ImageData id =null;
        	IJ.log("ngroups "+grda.size()); 
        	while (gidit.hasNext() && id==null){
	        	try {
	        		ctx = new SecurityContext(grda.get(counter).getGroupId());
	        		id = browser.getImage(ctx, imageId);
	        		credentials.setGroupID(ctx.getGroupID());
	        		gateway.connect(credentials);
	        	} catch (Exception e){
	        		//IJ.showMessage("Image not found in group " +grda.get(counter).getGroupId());
	        	}
	        	counter++;
        	}
        	if (id==null) {
        		IJ.showMessage("Image not found");
        		return;
        	}
        	ImageJ ij = new ImageJ();
        	Context context = ij.getContext();
        	OMEROService dos = context.service(OMEROService.class);
        	OMEROLocation ol = new OMEROLocation(HOST,PORT,Username,Password);
        	OMEROSession os = dos.createSession(ol);	
			client cl = os.getClient();
            Dataset d = dos.downloadImage(cl,imageId);
            @SuppressWarnings("rawtypes")
			ImgPlus implu=d.getImgPlus();
           	@SuppressWarnings("unchecked")
			ImagePlus imp = ImageJFunctions.wrap(implu,"My desired imageplus");
           	imp.show();
           	Double[][] data = new Double[2][2];
           	for (int i=0;i<2;i++){
           		for (int j=0;j<2;j++){
           			data[i][j] =new Double((i+1)*(j+2));
           		}
           	}
           	TableDataColumn[] tdc = new TableDataColumn[2];
           	tdc[0]=new TableDataColumn("First", 1, Double.class);
           	tdc[1]=new TableDataColumn("2nd", 2, Double.class);
           	TableData omeroTable = new TableData(tdc, data);
            // dos.uploadTable(ol,"TestData",rt2,imageId);
           	// gives an error caused by AbstractConverService, so lets try without converting.
           	// The uploadTable uses the convertservice so we need to skip that
           	// The way things are added are via TablesFacility
           	final TablesFacility tablesFacility = gateway.getFacility(TablesFacility.class);
        	tablesFacility.addTable(ctx, id, "My random table", omeroTable);
        } catch (Exception e) {
        	IJ.log(e.getMessage());
        	StackTraceElement[] t = e.getStackTrace();
        	for (int i=0;i<t.length;i++){
        		IJ.log(t[i].toString());
        	}
            IJ.showMessage("An error occurred while loading the image.");
        } finally {
            if (gateway != null) gateway.disconnect();
        }
    }
// used for testing when running the plugin stand alone
    public static void main(String[] args) {
    	attachFileMinimal om = new attachFileMinimal();
        om.uploadFile();
    }
}