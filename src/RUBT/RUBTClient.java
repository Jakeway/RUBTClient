package RUBT;

import java.io.File;
import java.io.FileNotFoundException;

public class RUBTClient 
{
	public static void main(String[] args)
	{
		if(args.length > 2 || args.length <= 1)
		{
			System.err.println("Please enter two arguments.");
			System.exit(1);
		}

		File torretFile = new File(args[0]);
		File destFile = new File(args[1]);
		
		
	}
}
