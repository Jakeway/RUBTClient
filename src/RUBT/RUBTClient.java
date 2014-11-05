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
			System.err.println("Check your arguments. Please rerun program.");
			System.exit(1);
		}
		
		
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
			System.exit(1);
		}
		
		
		ti = Util.getTorrentInfo(torrentFile);
		
		String localID = Util.getRandomPeerId();
		if (!DEBUG)
		{
			localID = "DEBUGUPLOAD123456789";
		}
		
		Tracker tracker = new Tracker(ti, localID);
		try 
		{
			destFile.setLength(ti.file_length);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
		PeerManager peerMgr = new PeerManager(ti, destFile, tracker, DEBUG);
		peerMgr.start();
		
		UploadManager uMgr = null;
		
		// if we are debugging, don't start up the upload manager
		if (!DEBUG)
		{
			uMgr = new UploadManager(peerMgr, localID);
			uMgr.start();
		}
		
	//	System.out.println(localID);
		
		Scanner sc = new Scanner(System.in);
		System.out.println("Type \'quit\' to quit");
		String input = "";
		while (sc.hasNext())
		{
			input = sc.nextLine();
			if (input.toLowerCase().equals("quit"))
			{
				if (!DEBUG)
				{
					System.out.println("stopping upload manager");
					uMgr.stopUploading();
					uMgr.interrupt();
				}
				System.out.println("stopping peer manager");
				peerMgr.closeSaveFile();
				peerMgr.stopProcessingJobs();
				peerMgr.interrupt();
				break;
			}
		}
		sc.close();
		
	}
	
	public static String getDownloadFromIP()
	{
		return downloadFromIP;
	}
}
