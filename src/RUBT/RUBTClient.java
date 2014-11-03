package RUBT;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Scanner;

import GivenTools.TorrentInfo;

public class RUBTClient 
{

	
	static String downloadFromIP = "";
	
	public static void main(String[] args)
	{	
		boolean DEBUG = false;
		if (args.length == 3)
		{
			DEBUG = true;
			downloadFromIP = args[2];
		}
		TorrentInfo ti = null;
		File torrentFile = null;
		RandomAccessFile destFile = null;
		
		if(args.length > 3 || args.length <= 1)
		{
			System.err.println("Can't have more than 3 arguments. Please rerun program.");
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
		
		System.out.println(localID);
		
		Tracker tracker = new Tracker(ti, localID);
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
		if (DEBUG)
		{
			tracker.printResponseMap();
		}
		
		tracker.printResponseMap();
	
		
		PeerManager peerMgr = new PeerManager(ti.piece_length, ti.piece_hashes, ti.file_length,
				destFile, Util.getPiecesLeft(ti.piece_hashes), tracker, DEBUG);
		

		peerMgr.start();
		
		Scanner sc = new Scanner(System.in);
		System.out.println("Type \'quit\' to quit");
		String input = "";
		while (sc.hasNext())
		{
			input = sc.nextLine();
			if (input.toLowerCase().equals("quit"))
			{
				System.out.println("stopping peer Manager");
				peerMgr.stopProcessingJobs();
				peerMgr.interrupt();
				break;
			}
		}
		
	}
	
	public static String getDownloadFromIP()
	{
		return downloadFromIP;
	}
}
