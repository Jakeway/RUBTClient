package RUBT;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class PeerManager
{

	byte[] downloaded;
	File saveFile;
	ArrayList<Integer> piecesLeft;
	ArrayList<Peer> peers;
	ArrayList<Peer> rutgersPeers;

	public PeerManager(int fileLength, File saveFile, ArrayList<Integer> pieces, ArrayList<Peer> peers)
	{
		this.downloaded = new byte[fileLength];
		this.saveFile = saveFile;
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

		int pieceSize = pm.getPieceSize();

		int firstByteIndex = pieceIndex * pieceSize;

		for (int i = firstByteIndex, j=0; i < firstByteIndex + pieceSize; i++, j++)
		{
			downloaded[i] = pm.block[j];
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
		// if a peer calls this message, all the pieces have been downloaded
		/// interrupt all threads
		// write to file
		try 
		{
			FileOutputStream fos = new FileOutputStream(saveFile);
			fos.write(downloaded);
			fos.close();
			
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
	
	
	
	
