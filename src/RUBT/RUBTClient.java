package RUBT;


import java.io.File;

import GivenTools.TorrentInfo;

public class RUBTClient 
{

	public static void main(String[] args)
	{	
		boolean DEBUG = false;
		
		TorrentInfo ti = null;
		
		if(args.length > 3 || args.length <= 1)
		{
			System.err.println("Please enter two arguments.");
			System.exit(1);
		}
		
		File torrentFile = null;
		File destFile = null;
		
		if (!(DEBUG))
		{
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
		}
		
		else
		{
			torrentFile = new File("Phase2.torrent");
			destFile = new File("test2.mp4");
		}
		
		ti = Util.getTorrentInfo(torrentFile);
		String localID = Util.getRandomPeerId();
		Tracker t = new Tracker(ti, localID);
		
		PeerManager peerMgr = new PeerManager(ti.file_length, destFile, Util.getPiecesLeft(ti.piece_hashes), t.getPeerList());
		peerMgr.startDownloading();

	}
}
