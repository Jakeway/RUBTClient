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
	ArrayList<Integer> needPieces;
	
	public RUBTClient(int fileLength, File saveFile, ArrayList<Integer> pieces)
	{
		downloaded = new byte[fileLength];
		f = saveFile;
		needPieces = pieces;
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
		
		RUBTClient rubt = new RUBTClient(ti.file_length, destFile, Util.needPieces(ti.piece_hashes));
		
		String localID = Util.getRandomPeerId();
		Tracker t = new Tracker(ti, localID, rubt);
		//t.printResponseMap();
		//Peer rutgersPeer = Util.findPeer(t.getPeerList());
		t.printResponseMap();
		for(int i = 0; i < rubt.needPieces.size(); i++)
		{
			System.out.print(rubt.needPieces.get(i) + " ");
		}
		ArrayList<Peer> rutgersPeers = Util.findMultiplePeers(t.getPeerList());
		long startTime = System.nanoTime();
		//rutgersPeer.run(rubt, t, ti);
		long endTime = System.nanoTime() - startTime;
		System.out.println("Total time in nanoseconds: " + endTime);
		System.out.println("Download complete... Enjoy!");
	}
}
