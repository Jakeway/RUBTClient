package RUBT;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import GivenTools.TorrentInfo;

public class RUBTClient 
{

	public static void main(String[] args)
	{	
		boolean DEBUG = false;
		
		TorrentInfo ti = null;
		File torrentFile = null;
		RandomAccessFile destFile = null;
		
		if(args.length > 3 || args.length <= 1)
		{
			System.err.println("Please enter two arguments.");
			System.exit(1);
		}
		
		if (DEBUG)
		{
			torrentFile = new File("Phase2.torrent");
			try 
			{
				destFile = new RandomAccessFile("test2.mp4", "rw");
			} 
			catch (FileNotFoundException e) 
			{
				e.printStackTrace();
			}
		}
		
		else
		{
			try
			{
				torrentFile = new File(args[0]);
				destFile = new RandomAccessFile(args[1], "rw");
			}
			catch (NullPointerException e)
			{
				e.printStackTrace();
				System.exit(1);
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
		}
		
		ti = Util.getTorrentInfo(torrentFile);
		String localID = Util.getRandomPeerId();
		Tracker t = new Tracker(ti, localID);
		
		try 
		{
			destFile.setLength(ti.file_length);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
		// need method that reads in the file to see if any of it has been downloaded yet
		// then make the corresponding piecesLeft list
		t.printResponseMap();
		PeerManager peerMgr = new PeerManager(ti.piece_length, destFile, Util.getPiecesLeft(ti.piece_hashes), t.getPeerList());
		peerMgr.start();

	}
}
