package lu.uni.svv.StressTesting.utils;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;


public class ArgumentParser {
	
	public enum DataType {INTEGER, DOUBLE, STRING, BOOLEAN} ;
	
	/**
	 * Internal optionsLong
	 */
	private List<String>                variables;
	private HashMap<String, Boolean>    necessaries;
	private HashMap<String, String>     options;            // to save short indicators
	private HashMap<String, String>     optionsIdx;
	private HashMap<String, String>     optionsLong;        // to save long indicators
	private HashMap<String, String>     optionsLongIdx;
	
	private HashMap<String, String>     descriptions;       // to save descriptions
	private HashMap<String, DataType>   datatypes;
	private HashMap<String, Object>     defaults;
	
	private HashMap<String, Object>     params;             // to save parsed parameters
	public int                          screen = 80;        // screen length for showing message
	
	/**
	 * Constructor
	 */
	public ArgumentParser() {
		params       = new HashMap<String, Object>();
		
		variables    = new ArrayList<String>();
		
		necessaries  = new HashMap<String, Boolean>();
		options      = new HashMap<String, String>();
		optionsIdx      = new HashMap<String, String>();
		optionsLong  = new HashMap<String, String>();
		optionsLongIdx = new HashMap<String, String>();
		
		descriptions = new HashMap<String, String>();
		datatypes    = new HashMap<String, DataType>();
		defaults     = new HashMap<String, Object>();

	}
	
	public void addOption(Boolean _need, String _variable, DataType _datatype, String _shortIndicator) throws Exception {
		addOption(_need, _variable, _datatype, _shortIndicator, null, "", null);
	}
	public void addOption(Boolean _need, String _variable, DataType _datatype, String _shortIndicator, String _description) throws Exception {
		addOption(_need, _variable, _datatype, _shortIndicator, null, _description, null);
	}
	public void addOption(Boolean _need, String _variable, DataType _datatype, String _shortIndicator, String _longIndicator, String _description) throws Exception {
		addOption(_need, _variable, _datatype, _shortIndicator, _longIndicator, _description, null);
	}
	
	/**
	 * add a new option
	 * @param _need            [Necessary] It represents this option is necessary or not. If true means necessary.
	 * @param _variable   [Necessary] parameter variable name
	 * @param _datatype       [Necessary] data type of the parameter to add
	 * @param _shortIndicator [Necessary] short indicator to set this parameter
	 * @param _longIndicator  long indicator to set this parameter
	 * @param _description   descriptions of the parameter to add
	 * @param _defaultValue   default values of this parameter, if you don't give the value, this parameter is necessary parameter.
	 * @throws Exception, ParseException
	 */
	public void addOption(Boolean _need, String _variable, DataType _datatype, String _shortIndicator, String _longIndicator, String _description, Object _defaultValue) throws Exception
	{
		
		if (_variable == null)
			throw new Exception("Variable name is necessary");
		
		if (_need == null)
			throw new Exception("Need variable is necessary");
		
		if (_shortIndicator == null || _shortIndicator.isEmpty())
			if (_longIndicator == null || _longIndicator.isEmpty())
				throw new Exception("Short or long indicator is necessary");
		
		if (_datatype == null)
			throw new Exception("Data type is necessary.");
		
		if (_defaultValue != null && _datatype != getDataType(_defaultValue)){
			throw new Exception("Default value is different with data type.");
		}
		
		for(String key:variables){
			if (key.compareTo(_variable) == 0) {
				throw new Exception("Duplicate variable name: " + _variable);
			}
		}
		
		if (_shortIndicator != null) {
			for (String value : options.values()) {
				if (value.compareTo(_shortIndicator) == 0) {
					throw new Exception("Duplicate option name: " + _shortIndicator);
				}
			}
		}
		if (_longIndicator != null){
			for(String value:optionsLong.values()){
				if (value.compareTo(_longIndicator) == 0) {
					throw new Exception("Duplicate option long name: " + _longIndicator);
				}
			}
		}
		
		variables.add(_variable);
		Collections.sort(variables);
		
		if (_need == true) necessaries.put(_variable, _need);
		
		if (_shortIndicator != null) {
			options.put(_variable, _shortIndicator);
			optionsIdx.put(_shortIndicator, _variable);
		}
	
		if (_longIndicator != null) {
			optionsLong.put(_variable, _longIndicator);
			optionsLongIdx.put(_longIndicator, _variable);
		}
		
		if (_description == null || _description.isEmpty())
			descriptions.put(_variable, "");
		else
			descriptions.put(_variable, _description);
		
		datatypes.put(_variable, _datatype);
		
		if (_defaultValue != null) {
			defaults.put(_variable, _defaultValue);
		}
		else{
			if (_datatype == DataType.BOOLEAN)
				defaults.put(_variable, false);
		}
	}
	

	
	/**
	 * Parsing parameters from Command-Line
	 * @param args
	 * @return
	 */
	public void parseArgs(String[] args) throws ParseException {
		
		// checking each arg
		for (int x=0; x<args.length; x++){

			// check if the param is what we want to input
			String param = findKey(args[x], x);
			Object value;
			// check the validation of the value
			if (datatypes.get(param) != DataType.BOOLEAN) {
				value = getValue(param, args[x+1], x+1);
				x++;
			}
			else {
				value = true;
			}
			
			params.put(param, value);
		}
		
		// check the variable's necessary
		for (String key:necessaries.keySet()){
			if (params.containsKey(key) == false)
			{
				throw new ParseException("Not found a necessary parameter \""+ options.get(key) + "\"", -1);
			}
		}
		
		// check default value
		for (String key:variables){
			if (params.containsKey(key) == true) continue;
			
			if (defaults.containsKey(key)) {
				params.put(key, defaults.get(key));
			}
		}
		
		
	}
	
	private String findKey(String _input, int _position) throws ParseException {
		String key = _input;
		String param;
		
		// check if the param is what we want to input
		if (key.startsWith("--")) {
			key = key.substring(2);
			if (!optionsLongIdx.containsKey(key))
				throw new ParseException("Not supported option: "+ _input + "Please check this option.", _position);
			
			param = optionsLongIdx.get(key);
			
		} else if (key.startsWith("-")) {
			key = key.substring(1);
			if (!optionsIdx.containsKey(key))
				throw new ParseException("Not supported option: "+ _input + "Please check this option.", _position);
			param = optionsIdx.get(key);
			
		} else{
			throw new ParseException("Not supported option: " + _input + "Please check this option.", _position);
		}
		
		return param;
	}
	/**
	 * check validation of the value
	 * @param _key
	 * @param _value
	 * @return
	 */
	private Object getValue(String _key, String _value, int _position) throws ParseException {
		
		DataType expectedType = datatypes.get(_key);
		
		
		if (expectedType == DataType.INTEGER){
			try {
				return Integer.parseInt(_value);
			}
			catch (NumberFormatException e){
				throw new ParseException("Not proper value for " + optionsLong.get(_key) + ". This parameter should be Integer.", _position);
			}
		}
		else if (expectedType == DataType.DOUBLE){
			try {
				return Double.parseDouble(_value);
			}
			catch (NumberFormatException e){
				throw new ParseException("Not proper value for " + optionsLong.get(_key) + ". This parameter should be Double.", _position);
			}
		}
		else if (expectedType == DataType.STRING){
			return _value;
		}
		else if (expectedType == DataType.BOOLEAN){
			return true;
		}
		return null;
	}
	
	
	
	public DataType getDataType(Object object) throws ParseException {
		if (object instanceof Integer)
			return DataType.INTEGER;
		
		else if(object instanceof Double)
			return DataType.DOUBLE;
		
		else if(object instanceof String)
			return DataType.STRING;
		
		else if(object instanceof Boolean)
			return DataType.BOOLEAN;
		
		else
			throw new ParseException("Data Type is not correct!", -1);
	}
	
	/**
	 * make HelpMsg
	 */
	public String getHelpMsg() {
		String jarName = new java.io.File(Settings.class.getProtectionDomain()
				.getCodeSource()
				.getLocation()
				.getPath())
				.getName();
		
		StringBuilder usages = new StringBuilder(String.format("Usage: java -jar %s [-options]\r\n\r\n", jarName) );
		
		usages.append("where options can include:\r\n");
		
		boolean shortFlag = false;
		for (String param:variables){
			shortFlag = false;
			int length = 0;
			
			if (options.containsKey(param)) {
				usages.append("-");
				usages.append(options.get(param));
				shortFlag = true;
				length += 2;
			}
			
			if (optionsLong.containsKey(param)) {
				String longName = optionsLong.get(param);
				usages.append((shortFlag) ? ", --" : "--");
				usages.append(longName);
				length += (longName.length() + ((shortFlag) ? 4 : 2));
			}
			if (length<4)
				usages.append("\t\t\t");
			else if (length<8)
				usages.append("\t\t");
			else
				usages.append("\t");
			
			if (descriptions.containsKey(param)){
				String desc = getMultilineString(descriptions.get(param),screen-16, "\r\n\t\t\t");
				usages.append(desc);
			}
			else{
				usages.append("No description.");
			}
			
			usages.append("\r\n");
		}
		
		return usages.toString();
	}
	
	public String getMultilineString(String _text, int _size, String splitter){
		StringBuilder sb = new StringBuilder();
		
		while (true){
			if (_text.length() > _size){
				int idx = _text.lastIndexOf(' ', _size);
				sb.append(_text.substring(0, idx));
				sb.append(splitter);
				_text = _text.substring(idx+1);
			}
			else{
				sb.append(_text);
				break;
			}
		}
		return sb.toString();
	}
	
	
	public boolean containsParam(String key){
		return params.containsKey(key);
	}
	public Object getParam(String key){
		if (!params.containsKey(key))
			return null;
		return params.get(key);
	}
	
	public DataType getDataType(String key){
		if (!params.containsKey(key))
			return null;
		return datatypes.get(key);
	}
}
