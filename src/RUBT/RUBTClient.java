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
		
		if(args.length > 3 || args.length <= 1)
		{
			System.err.println("Please enter two arguments.");
			System.exit(1);
		}
		
		File torrentFile = null;
		RandomAccessFile destFile = null;
		
		if (!(DEBUG))
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
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		}
		
		else
		{
			torrentFile = new File("Phase2.torrent");
			try {
				destFile = new RandomAccessFile("test2.mp4", "rw");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		ti = Util.getTorrentInfo(torrentFile);
		String localID = Util.getRandomPeerId();
		Tracker t = new Tracker(ti, localID);
		
		t.printResponseMap();
		try {
			destFile.setLength(ti.file_length);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		PeerManager peerMgr = new PeerManager(ti.file_length, ti.piece_length, destFile, Util.getPiecesLeft(ti.piece_hashes), t.getPeerList());
		peerMgr.startDownloading();

	}
}
