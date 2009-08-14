/**
 * 
 */
package edu.tum.cs.srl;

import edu.tum.cs.tools.StringTool;

public class Signature {
	public String returnType;
	public String[] argTypes;
	public String functionName;

	public Signature(String functionName, String returnType, String[] argTypes) {
		this.returnType = returnType;
		this.argTypes = argTypes;
		this.functionName = functionName;
	}
	
	public void replaceType(String oldType, String newType) {
		if(this.returnType.equals(oldType))
			this.returnType = newType;
		for(int i = 0; i < argTypes.length; i++) {
			if(argTypes[i].equals(oldType))
				argTypes[i] = newType;
		}
	}
	
	public boolean isBoolean() {
		return returnType.equalsIgnoreCase("boolean");
	}
	
	@Override
	public String toString() {
		return String.format("%s %s(%s)", returnType, functionName, StringTool.join(",", argTypes));
	}

	public static String formatVarName(String functionName, String[] args) {
		return String.format("%s(%s)", functionName, StringTool.join(",", args));
	}
}