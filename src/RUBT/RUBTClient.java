package RUBT;


import java.io.*;
import java.net.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.*;

import GivenTools.*;

public class RUBTClient 
{
	public static void main(String[] args)
	{		
		TorrentInfo ti = null;
		String announceURL = "";
		
		if(args.length > 2 || args.length <= 1)
		{
			System.err.println("Please enter two arguments.");
			System.exit(1);
		}
		File torrentFile = null;
		File destFile = null;
		try
		{
			torrentFile = new File(args[0]);
			destFile = new File(args[1]);
		}
		catch (NullPointerException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		
		ti = Util.getTorrentInfo(torrentFile);
		
		String localID = "tomjakewaynrobcasale";
		
		Tracker t = new Tracker(ti);
		t.printResponseMap();
		Peer test = new Peer("128.6.171.131", 61350, "-AZ5400-Z0HeJJzWqxUU");
		test.getPeerResponse(ti, localID);
	}
}
