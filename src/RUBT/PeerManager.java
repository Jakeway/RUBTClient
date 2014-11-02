package RUBT;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;

public class PeerManager
{
	RandomAccessFile saveFile;
	ArrayList<Integer> piecesLeft;
	ArrayList<Peer> peers;
	ArrayList<Peer> rutgersPeers;
	int amountDownloaded = 0;
	int pieceLength;
	
	public PeerManager(int fileLength, int pieceLength, RandomAccessFile destFile, ArrayList<Integer> pieces, ArrayList<Peer> peers)
	{
		this.pieceLength = pieceLength;
		this.saveFile = destFile;
		this.piecesLeft = pieces;
		this.peers = peers;
		this.rutgersPeers = Util.findRutgersPeers(peers);
		printPeers();
	}
	
	private void printPeers()
	{
		for (Peer p : rutgersPeers)
		{
			System.out.println(p.getIP());
		}
	}
	
	public void startDownloading()
	{
		for (Peer p : rutgersPeers)
		{
			System.out.println("starting a peer");
			p.setPeerManager(this);
			p.start();
		}
	}
	
	public void digestPieceMessage(PieceMessage pm)
	{
		insertPieceInfo(pm);
		sendHaveMessages(pm.getPieceIndex());
		piecesLeft.remove((Object)pm.getPieceIndex());
	}
	
	public void insertPieceInfo(PieceMessage pm)
	{
		int pieceIndex = pm.getPieceIndex();
		try {
			saveFile.seek(pieceIndex * pieceLength);
			saveFile.write(pm.getBlock());
			amountDownloaded += pm.getBlock().length;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void sendHaveMessages(int pieceNum)
	{
		HaveMessage hm = new HaveMessage(pieceNum);
		for (Peer p : rutgersPeers)
		{
			HaveMessage.send(hm, p.getOutputStream());
		}
	}
	
	public void finishedDownloading()
	{
		System.out.println("finished downloading");
		try {
			System.out.println("downloaded " + amountDownloaded + " out of " + saveFile.length());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// if a peer calls this message, all the pieces have been downloaded
		/// interrupt all threads
		// write to file
		try 
		{
			saveFile.close();	
		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
		
	
	
	
	
		
}
	
	
	
	
