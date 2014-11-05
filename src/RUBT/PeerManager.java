package RUBT;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import GivenTools.TorrentInfo;
import Message.BitfieldMessage;
import Message.HaveMessage;
import Message.Message;
import Message.PieceMessage;
import Message.RequestMessage;

public class PeerManager extends Thread
{
	private RandomAccessFile saveFile;
	private ArrayList<Integer> piecesLeft;
	private ArrayList<Peer> peers;
	private ArrayList<Peer> rutgersPeers;
	private Tracker tracker;
	private byte[] bitfield;
	private int amountDownloaded = 0;
	private int amountUploaded = 0;
	private int pieceLength;
	private long startTime;
	private long endTime;
	private int amountLeft;
	private int numPieces;
	private int fileLength;
	private LinkedBlockingQueue<Job> jobs;
	private ByteBuffer[] piece_hashes;
	private boolean DEBUG;
	private byte[] info_hash;
	
	public PeerManager(TorrentInfo ti, RandomAccessFile destFile,
			Tracker tracker, boolean DEBUG) 
	{
		this.info_hash = ti.info_hash.array();
		this.pieceLength = ti.piece_length;
		this.piece_hashes = ti.piece_hashes;
		this.saveFile = destFile;
		this.numPieces = piece_hashes.length;
		this.piecesLeft = Util.getPiecesLeft(numPieces);
		this.fileLength = ti.file_length;
		this.tracker = tracker;
		this.peers = tracker.getPeerList();
		this.rutgersPeers = Util.findRutgersPeers(peers);
		this.DEBUG = DEBUG;
		bitfield = new byte[getBitfieldLength()];
		amountLeft = fileLength;
		jobs = new LinkedBlockingQueue<Job>();
		//printPeers();
	}

	private void printPeers()
	{
		for (Peer p : rutgersPeers)
		{
			System.out.println(p.getIP());
		}
	}
	
	public int getBitfieldLength()
	{
		return (int)Math.ceil(numPieces / 8.0);
	}
	
	public byte[] getBitfield()
	{
		return bitfield;
	}
	
	public ArrayList<Peer> getPeers()
	{
		return peers;
	}
	
	public byte[] getIndexHash()
	{
		return this.info_hash;
	}
	
	public void startDownloading()
	{
		startTime = System.nanoTime();
		
		if (DEBUG)
		{
			Peer p = new Peer(RUBTClient.getDownloadFromIP(), 6881, "DEBUGUPLOAD123456789", tracker.localId);
			System.out.println("starting specific download from ip " + RUBTClient.getDownloadFromIP());
			p.setPeerManager(this);
			p.start();
		}
		else
		{
			for (Peer p : rutgersPeers)
			{
				System.out.println("starting a peer with ip " + p.getIP());
				p.setPeerManager(this);
				p.start();
			}
		}
	}
	
	public void digestPieceMessage(PieceMessage pm)
	{
		insertPieceInfo(pm);
		sendHaveMessages(pm.getPieceIndex());
		piecesLeft.remove((Object)pm.getPieceIndex());
		amountDownloaded += pm.getBlock().length;
		amountLeft = amountLeft - pm.getBlock().length;
		// update our bitfield to reflect the piece we just downloaded
		Util.setBit(bitfield, pm.getPieceIndex());
	}
	
	private void insertPieceInfo(PieceMessage pm)
	{
		int pieceIndex = pm.getPieceIndex();
		try 
		{
			saveFile.seek(pieceIndex * pieceLength);
			saveFile.write(pm.getBlock());
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
	}
	
	private void sendHaveMessages(int pieceNum)
	{
		if (DEBUG)
		{
			return;
		}
		HaveMessage hm = new HaveMessage(pieceNum);
		for (Peer p : rutgersPeers)
		{
			hm.send(p.getOutputStream());
		}
	}
	
	public void finishedDownloading()
	{

		tracker.announce(0, "0", Integer.toString(fileLength), "completed");
		endTime = System.nanoTime();
		long elapsedTimeSeconds = TimeUnit.SECONDS.convert(endTime-startTime, TimeUnit.NANOSECONDS);
		int minutes = (int) (elapsedTimeSeconds / 60);
		int seconds = (int) (elapsedTimeSeconds % 60);
		
		System.out.println("took " + minutes + " minutes and " + seconds + " seconds to download file");
		
	}
	
	public void closeSaveFile()
	{
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

	
	
	public void acceptMessage(Message m, Peer p)
	{
		Job j = new Job(m, p);
		try 
		{
			jobs.put(j);
		} 
		catch (InterruptedException e) 
		{
			e.printStackTrace();
		}
	}
	
	
	public void generateRequestMessage(Peer p)
	{
		int random = Util.getRandomInt(piecesLeft.size());
		int pieceToGet = piecesLeft.get(random);
		int length;
		if (pieceToGet == numPieces-1) // getting the last piece
		{
			 length = fileLength % pieceLength;
		}
		else
		{
			length = pieceLength;
		}
 		RequestMessage rm = new RequestMessage(pieceToGet, 0, length);
		rm.send(p.getOutputStream());
	}
	
	public void stopProcessingJobs()
	{
		keepRunning = false;
		for (Peer p : rutgersPeers)
		{
			p.stopListening();
		}
		
	}
	
	volatile boolean keepRunning = true;
	public void run()
	{
		startDownloading();
		
		while (keepRunning)
		{
			try
			{
				Job j = jobs.take();
				Message msg = j.getMessage();
				Peer p = j.getPeer();
				
				if (DEBUG)
				{
					System.out.println(msg);
				}
				
				
				
				switch (msg.getID())
				{
					case Message.KEEP_ALIVE_ID:
						break;
					
					case Message.INTERESTED_ID:
						// set peer to be in an interested state
						p.setPeerInterested(true);
						
						// we should only unchoke a certain amount of peers in long run, for now, just unchoke everyone
						Message.UNCHOKE_MSG.send(p.getOutputStream());
						p.setPeerChoked(false);
						break;
					
					case Message.UNINTERESTED_ID:
						// set peer to be in an uninterested state
						p.setPeerInterested(false);
						break;
						
					case Message.CHOKE_ID:
						p.setClientChoked(true);
						break;
					
					case Message.UNCHOKE_ID:
						// if peer isn't choked and is interested, send piece request
						p.setClientChoked(false);
						if(p.getClientInterested())
						{
							generateRequestMessage(p);
						}
						break;
						
					case HaveMessage.HAVE_ID:
						// update peers bitfield array
						// we are downloading only from rutgers peers for now
						//don't worry about this message for now
						break;
					
					case PieceMessage.PIECE_ID:
						
						PieceMessage pm = (PieceMessage) msg;
						if (Util.verifyHash(pm.getBlock(), piece_hashes[pm.getPieceIndex()].array()))
						{
							digestPieceMessage(pm);
						}
						if (piecesLeft.size() > 0)
						{
							generateRequestMessage(p);
						}
						else
						{
							finishedDownloading();
						}
						break;
					
						
					case RequestMessage.REQUEST_ID:
						RequestMessage rMsg = (RequestMessage) msg;
						int pieceIndex = rMsg.getPieceIndex();
						// if this peer is interested and unchoked
						if(!(p.getPeerChoked()) && p.getPeerInterested())
						{
							// if we have downloaded this piece
							if (!piecesLeft.contains((Object) pieceIndex))
							{
								byte[] block = new byte[rMsg.getBlockLength()];
								byte[] fileInBytes = Util.fileToBytes(saveFile);
								System.arraycopy(fileInBytes, pieceIndex * this.pieceLength, block, 0, block.length);
								PieceMessage pieceMsg = new PieceMessage(pieceIndex, rMsg.getByteOffset(), block);
								pieceMsg.send(p.getOutputStream());
								amountUploaded += rMsg.getBlockLength();
								
							}
							else
							{
								Message.CHOKE_MSG.send(p.getOutputStream());
								p.setPeerChoked(true);
							}
						}
						break;
						
					case BitfieldMessage.BITFIELD_ID:
						
						// need to check whether or not bitfield message bitfield has pieces we dont have
						BitfieldMessage bm = (BitfieldMessage) msg;
						byte[] receivedBitfield = bm.getBitfield();
						if (interestedInBitfield(receivedBitfield))
						{
							p.setClientInterested(true);
							Message.INTERESTED_MSG.send(p.getOutputStream());
						}
						else
						{
							p.setClientInterested(false);
						}
						break;
						
	
				}
			}
			catch (InterruptedException e) 
			{
				keepRunning = false;
			}
			
		}

		
	}
	
	// returns true if we are interested in the pieces in this bitfield
	private boolean interestedInBitfield(byte[] bfield)
	{
		if (amountLeft == 0)
		{
			return false;
		}
		for (int byteIndex = 0; byteIndex < bitfield.length; byteIndex++)
		{
			for (int bitIndex = 0; bitIndex < 8; bitIndex++)
			{
				try
				{
				if (!Util.isBitSet(this.bitfield[byteIndex], bitIndex) 
						&& Util.isBitSet(bfield[byteIndex], bitIndex))
				{
					return true;
				}
				}
				catch (ArrayIndexOutOfBoundsException e)
				{
					System.out.println("byte index: " + byteIndex);
					System.out.println("bit index: " + bitIndex);
				}
			}
		}
		
		return false;
	}
		
}
	
	
	
	
