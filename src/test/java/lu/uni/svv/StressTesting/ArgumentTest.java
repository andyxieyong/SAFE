package lu.uni.svv.StressTesting;

import junit.framework.TestCase;
import lu.uni.svv.StressTesting.utils.ArgumentParser;

public class ArgumentTest extends TestCase {
	
	public ArgumentTest( String testName )
	{
		super( testName );
	}

	public void testArgument()
	{
		String[] args = {"-h"};//, "-a", "1", "-t", "10"};
		
		ArgumentParser parser = new ArgumentParser();
		try {
			String desc = "I'm currently trying to make a program that conjugates verbs into Spanish. I've created a Hash Table that contains a key and an instantiation of the object Verb.";
			parser.addOption(false,"Help", ArgumentParser.DataType.BOOLEAN, "h", "help", "To show helpTo show helpTo show helpTo show helpTo show helpTo show help");
			parser.addOption(false,"Test", ArgumentParser.DataType.INTEGER, "t", null, desc, 1);
			parser.addOption(false,"Aneed", ArgumentParser.DataType.INTEGER, "a", null, "Integer value");
			parser.addOption(false,"SettingFile", ArgumentParser.DataType.STRING, "f", null, "Base setting file.", "settings.json");
		}
		catch(Exception e){
			System.out.println("add Option error: "+ e.getMessage());
			return ;
		}
		try{
			parser.parseArgs(args);
		}
		catch(Exception e)
		{
			System.out.println("Error: "+e.getMessage());
			System.out.println("");
			System.out.println(parser.getHelpMsg());
			return;
		}
		
		System.out.println("Help: " + parser.getParam("Help"));
		System.out.println("Test: " + parser.getParam("Test"));
		System.out.println("Aneed: " + parser.getParam("Aneed"));
		System.out.println("Setting file: " + parser.getParam("SettingFile"));
		
	}
}
