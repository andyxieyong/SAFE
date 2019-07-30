package lu.uni.svv.StressTesting.search;

import com.github.rcaller.rstuff.*;
import weka.core.pmml.jaxbbindings.True;

import java.util.logging.Level;
import java.util.logging.Logger;


public class RManager {
	public static String type_String = "[Ljava.lang.String;";
	public static String type_Double = "[Ljava.lang.double;";
	public static String type_Integer = "[Ljava.lang.Integer;";
	RCaller rcaller;
	RCode rcode;
	
	
	public RManager() {
		if (this.detectOS().startsWith("Mac")){
			String RPath = "/Library/Frameworks/R.framework/Resources/bin/R";
			String RScriptPath = "/Library/Frameworks/R.framework/Resources/bin/Rscript";
			RCallerOptions options =
					RCallerOptions.create(RScriptPath, RPath,
							FailurePolicy.RETRY_1,
							3000L,
							100L,
							RProcessStartUpOptions.create());
			rcaller = RCaller.create(options);
		}
		else{
			rcaller = RCaller.create();
		}
		this.initCode();
	}
	
	public void addCode(String codeTxt) {
		rcode.addRCode(codeTxt);
	}
	
	public void initCode() {
		rcode = RCode.create();
		rcaller.setRCode(rcode);
	}
	
	
	public boolean execute(String var) {
		try{
			rcaller.runAndReturnResultOnline(var);
			//rcaller.runAndReturnResult(var);
		} catch (Exception e) {
			/**
			 * Note that, RCaller does some OS based works such as creating an external process and
			 * reading files from temporary directories or creating images for plots. Those operations
			 * may cause exceptions for those that user must handle the potential errors.
			 */
			Logger.getLogger(lu.uni.svv.StressTesting.search.FinegrainedSearch.class.getName()).log(Level.SEVERE, e.getMessage());
			return false;
		}
		return true;
	}
	
	public Object[] get(String var, String target, String type){
		if ( !this.execute(var) )
			return null;
		
		if (target == null)
			return this.get(var, type);
		else
			return this.get(target, type);
	}
	
	public Object[] get(String target, String type){
		if (type.equals(type_String)) {
			return(rcaller.getParser().getAsStringArray(target));
			
		} else if (type.equals(type_Integer)) {
			int[] res = rcaller.getParser().getAsIntArray(target);
			Integer[] ints = new Integer[res.length];
			for (int i=0;i<res.length;i++){
				ints[i] = res[i];
			}
			return(ints);
			
		} else if (type.equals(type_Double)) {
			double[] res = rcaller.getParser().getAsDoubleArray(target);
			Double[] dbls = new Double[res.length];
			for (int i=0;i<res.length;i++){
				dbls[i] = res[i];
			}
			return(dbls);
			
		} else {
			return(null);
		}
	}
	
	
	/**
	 * Detect OS
	 */
	public String detectOS(){
		String OS = System.getProperty("os.name").toLowerCase();
		
		if (OS.contains("win")) {
			return "Windows";
		} else if (OS.contains("mac")) {
			return "Mac";
		} else if (OS.contains("nix") || OS.contains("nux") || OS.contains("aix") ) {
			return "Linux";
		} else if (OS.contains("sunos")) {
			return "Solaris";
		} else {
			return OS;
		}
	}
	
}
