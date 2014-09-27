package RUBT;

public class RUBTClient 
{
	public static void main(String[] args)
	{
		if(args.length > 2 || args.length <= 1)
		{
			System.err.println("Please enter two arguments.");
			System.exit(1);
		}
	}
}
