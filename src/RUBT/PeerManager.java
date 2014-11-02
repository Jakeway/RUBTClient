package RUBT;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class PeerManager extends Thread
{
	RandomAccessFile saveFile;
	ArrayList<Integer> piecesLeft;
	ArrayList<Peer> peers;
	ArrayList<Peer> rutgersPeers;
	int amountDownloaded = 0;
	int pieceLength;
	long startTime;
	long endTime;
	ServerSocket servSock;
	
	public PeerManager(int pieceLength, RandomAccessFile destFile, ArrayList<Integer> pieces, ArrayList<Peer> peers)
	{
		this.pieceLength = pieceLength;
		this.saveFile = destFile;
		this.piecesLeft = pieces;
		this.peers = peers;
		this.rutgersPeers = Util.findRutgersPeers(peers);
		try 
		{
			servSock = new ServerSocket(Tracker.TRACKER_PORT_INT);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
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
		startTime = System.nanoTime();
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
		try 
		{
			saveFile.seek(pieceIndex * pieceLength);
			saveFile.write(pm.getBlock());
			amountDownloaded += pm.getBlock().length;
		} 
		catch (IOException e)
		{
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
		endTime = System.nanoTime();
		long elapsedTimeSeconds = TimeUnit.SECONDS.convert(endTime-startTime, TimeUnit.NANOSECONDS);
		int minutes = (int) (elapsedTimeSeconds / 60);
		int seconds = (int) (elapsedTimeSeconds % 60);
		
		System.out.println("took " + minutes + " minutes and " + seconds + " seconds to download file");
		
		// need to tell the tracker that we have finished downloading
		
		
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
	
	public void startUploading()
	{
		Peer p;
		while (true)
		{
			try 
			{
				Socket s = servSock.accept();
				
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
			
		}
	}
	
		
	public void run()
	{
		startDownloading();
		startUploading();
		
	}
	
	
	
		
}
	
	
	
	
