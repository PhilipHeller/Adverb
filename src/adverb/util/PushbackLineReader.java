/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *    
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *    
 *    Copyright (C) 2021 Philip Heller.
 *    
 */


package adverb.util;


import java.io.*;
import java.util.Stack;


public class PushbackLineReader extends BufferedReader 
{
	private Stack<String>		pushbackStack;
	
	
	public PushbackLineReader(Reader src) throws IOException
	{
		super(src);
		pushbackStack = new Stack<String>();
	}
	
	
	public String readLine() throws IOException
	{
		return  pushbackStack.isEmpty()  ?  super.readLine()  :  pushbackStack.pop();
	}
	
	
	public String peek() throws IOException
	{
		if (pushbackStack.isEmpty())
		{
			String s = readLine();
			if (s != null)
				pushbackStack.push(s);
			return s;
		}
		
		else
			return pushbackStack.peek();
	}
	
	
	public void push(String s)			{ pushbackStack.push(s); }	
}
