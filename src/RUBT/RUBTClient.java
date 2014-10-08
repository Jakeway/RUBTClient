package RUBT;


import java.io.*;
import java.net.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.*;

import GivenTools.*;

public class RUBTClient 
{
	
	byte[] downloaded;
	File f;
	
	public RUBTClient(int fileLength, File saveFile)
	{
		downloaded = new byte[fileLength];
		f = saveFile;
	}
	
	public static void main(String[] args)
	{	
		
		TorrentInfo ti = null;
		
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
		
		RUBTClient rubt = new RUBTClient(ti.file_length, destFile);
		
		String localID = Util.getRandomPeerId();
		Tracker t = new Tracker(ti, localID);
		//t.printResponseMap();
		Peer rutgersPeer = Util.findPeer(t.getPeerList());
		//t.printResponseMap();
		
		
		rutgersPeer.run(rubt, t, ti);
			
	}
}
