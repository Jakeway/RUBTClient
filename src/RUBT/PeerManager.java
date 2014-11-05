package RUBT;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
	private List<Peer> peers;
	private List<Peer> rutgersPeers;
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
	private Peer debugPeer;
	
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
	
	public List<Peer> getPeers()
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
			this.debugPeer = p;
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
		for (Iterator<Peer> iterator = rutgersPeers.iterator(); iterator.hasNext();)
		{
			Peer p = iterator.next();
			if (!sendMessage(hm, p))
			{
				p.stopListening();
				iterator.remove();
			}
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
		if (!sendMessage(rm, p))
		{
			removePeer(p);
		}
	}
	
	public void stopProcessingJobs()
	{
		keepRunning = false;
		if (DEBUG)
		{
			this.debugPeer.stopListening();
			return;
		}
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
						if (!sendMessage(Message.UNCHOKE_MSG, p))
						{
							removePeer(p);
							break;
						}
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
						HaveMessage hm = (HaveMessage) msg;
						if(p.getBitfield() == null)
							System.out.println("bitfield is null");
						Util.setBit(p.getBitfield(), hm.getPieceIndex());
						if(interestedInBitfield(p.getBitfield()))
						{
							if(!sendMessage(Message.INTERESTED_MSG, p))
							{
								removePeer(p);
								break;
							}
							p.setClientInterested(true);
						}
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
								byte[] block = fileToBytes(pieceIndex, rMsg.getBlockLength());
								PieceMessage pieceMsg = new PieceMessage(pieceIndex, rMsg.getByteOffset(), block);
								if (!sendMessage(pieceMsg, p))
								{
									removePeer(p);
									break;
								}
								amountUploaded += rMsg.getBlockLength();
							}
							else
							{
								
								if (!sendMessage(Message.CHOKE_MSG, p))
								{
									removePeer(p);
									break;
								}
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
							if (!sendMessage(Message.INTERESTED_MSG, p))
							{
								removePeer(p);
								break;
							}
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
	
	public byte[] fileToBytes(int pieceIndex, int blockSize)
	{
		byte[] bytes = null;
		try 
		{
			saveFile.seek(pieceIndex * pieceLength);
			bytes = new byte[blockSize];
			saveFile.read(bytes);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		return bytes;
	}
	
	public void addPeer(Peer p)
	{
		this.rutgersPeers.add(p);
	}
	
	
	
	public boolean sendMessage(Message m, Peer p)
	{
		try
		{
			m.send(p.getOutputStream());
		} 
		catch (IOException e) 
		{
			return false;
		}
		return true;
	}
	
	private void removePeer(Peer p)
	{
		rutgersPeers.remove(p);
		p.stopListening();
	}
}
	
	
	
	
