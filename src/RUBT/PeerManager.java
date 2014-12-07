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
	private ArrayList<Peer> trackerPeers;
	private ArrayList<Peer> connectedPeers;
	//private List<Peer> rutgersPeers;
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
	private int numUnchokedConnections;
	
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
		this.trackerPeers = tracker.getPeerList();
		//this.rutgersPeers = Util.findRutgersPeers(peers);
		this.DEBUG = DEBUG;
		bitfield = new byte[getBitfieldLength()];
		amountLeft = fileLength;
		jobs = new LinkedBlockingQueue<Job>();
		numUnchokedConnections = 0;
		connectedPeers = new ArrayList<Peer>();
	}
	
	public int getBitfieldLength()
	{
		return (int)Math.ceil(numPieces / 8.0);
	}
	
	public byte[] getBitfield()
	{
		return bitfield;
	}
	
	public List<Peer> getConnectedPeers()
	{
		return connectedPeers;
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
			for (Peer p : trackerPeers)
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
		for (Iterator<Peer> iterator = connectedPeers.iterator(); iterator.hasNext();)
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
		//need to check the peers bitfiled and see what pieces it has and then request a piece based on that
		//System.out.println(piecesLeft.size());
		
		
		// this code was used to try to figure out why we were getting stuck at 4 pieces left.
		// turns out the rutgers .131 peer is not sending the correct bitfield.
		// to remedy this, TA just told us to assume .131 has all pieces and don't look at it's bitfield.
		
		/*
		if (piecesLeft.size() == 4)
		{
			for (int i : piecesLeft)
			{
				System.out.println(i);
			}
			System.out.println("our bitfield");
			Util.iterateBitfield(bitfield);
			
			System.out.println("their bitfield");
			Util.iterateBitfield(p.getBitfield());
		}
		*/
		int random = Util.getRandomInt(piecesLeft.size());
		int pieceIndex = piecesLeft.get(random);
		while (true)
		{
			//				 the peer has piece we need			        we don't yet have this piece
			if (	(Util.isBitSet(p.getBitfield(), pieceIndex))  && (!Util.isBitSet(bitfield, pieceIndex))		)
			{
				break;
			}
			else
			{
				random = Util.getRandomInt(piecesLeft.size());
				pieceIndex = piecesLeft.get(random);
			}
		}
		int length;
		if (pieceIndex == numPieces-1) // getting the last piece
		{
			length = fileLength % pieceLength;
		}
		else
		{
			length = pieceLength;
		}
		RequestMessage rm = new RequestMessage(pieceIndex, 0, length);
		if (!sendMessage(rm, p))
		{
			removeConnectedPeer(p);
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
		for (Peer p : connectedPeers)
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
				
				switch (msg.getID())
				{
					case Message.KEEP_ALIVE_ID:
						break;
					
					case Message.INTERESTED_ID:
						
						// set peer to be in an interested state
						p.setPeerInterested(true);
						
						
						
						// perhaps should keep two lists of connected peers. (... or just use a boolean?)
						// one for peers that are unchoked, one for are choked
						// then when we do optimistic choking, pick peer with worst download rate of unchoked, and try a peer from the choked list
						
						if (numUnchokedConnections > 3)
						{
							if (!sendMessage(Message.CHOKE_MSG, p))
							{
								removeConnectedPeer(p);
								break;
							}
						}
						else
						{
							if (!sendMessage(Message.UNCHOKE_MSG, p))
							{
								removeConnectedPeer(p);
								break;
							}
							else
							{
								p.setPeerChoked(false);
								numUnchokedConnections++;
								break;
							}
						}
					case Message.UNINTERESTED_ID:
						// set peer to be in an uninterested state
						p.setPeerInterested(false);
						break;
						
					case Message.CHOKE_ID:
						p.setClientChoked(true);
						break;
					
					case Message.UNCHOKE_ID:
						// if peer isn't choked and is interested, send request message
						p.setClientChoked(false);
						if(p.getClientInterested())
						{
							generateRequestMessage(p);
						}
						break;
						
					case HaveMessage.HAVE_ID:
						HaveMessage hm = (HaveMessage) msg;
						Util.setBit(p.getBitfield(), hm.getPieceIndex());
						if(interestedInBitfield(p.getBitfield()))
						{
							if(!sendMessage(Message.INTERESTED_MSG, p))
							{
								removeConnectedPeer(p);
								break;
							}
							p.setClientInterested(true);
						}
						break;
					
					case PieceMessage.PIECE_ID:
						
						// only evaluate this piece if we were previously interested and unchoked
						if (p.getClientInterested() && !p.getClientChoked())
						{
							PieceMessage pm = (PieceMessage) msg;
							if (Util.verifyHash(pm.getBlock(), piece_hashes[pm.getPieceIndex()].array()))
							{
								digestPieceMessage(pm);
							}
							if ( piecesLeft.size() > 0)
							{
								generateRequestMessage(p);
							}
							else
							{
								finishedDownloading();
							}
						}
						break;
					
					case RequestMessage.REQUEST_ID:
						RequestMessage rMsg = (RequestMessage) msg;
						int pieceIndex = rMsg.getPieceIndex();
						
						// if this peer is interested and unchoked
						if(!(p.getPeerChoked()) && p.getPeerInterested())
						{
							// if we have downloaded this piece
							if (Util.isBitSet(bitfield, pieceIndex))
							{
								byte[] block = fileToBytes(pieceIndex, rMsg.getBlockLength());
								PieceMessage pieceMsg = new PieceMessage(pieceIndex, rMsg.getByteOffset(), block);
								if (!sendMessage(pieceMsg, p))
								{
									removeConnectedPeer(p);
									break;
								}
								amountUploaded += rMsg.getBlockLength();
							}
							//we do not have the piece they requested
							else
							{
								if (!sendMessage(Message.CHOKE_MSG, p))
								{
									removeConnectedPeer(p);
									break;
								}
								p.setPeerChoked(true);
								numUnchokedConnections--;
							}
						}
						break;
						
					case BitfieldMessage.BITFIELD_ID:
						
						BitfieldMessage bm = (BitfieldMessage) msg;
						byte[] receivedBitfield = bm.getBitfield();
						
						// NOTE: TA said to assume that .131 address has all pieces
						// for some reason, the bitfield it sends is missing pieces 432-435
						// just set these pieces as a work-around
						if (p.getIP().equals("128.6.171.131"))
						{
							
							Util.setBit(receivedBitfield, 432);
							Util.setBit(receivedBitfield, 433);
							Util.setBit(receivedBitfield, 434);
							Util.setBit(receivedBitfield, 435);
						}
						
						p.setBitfield(receivedBitfield);
						if (interestedInBitfield(receivedBitfield))
						{
							p.setClientInterested(true);
							if (!sendMessage(Message.INTERESTED_MSG, p))
							{
								removeConnectedPeer(p);
								break;
							}
						}
						else
						{
							p.setClientInterested(false);
						}
						break;
					
					case Message.ERROR_ID:
						System.err.println("received an unkown message from " + p.getIP());
						removeConnectedPeer(p);
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
	
	public void addConnectedPeer(Peer p)
	{
		this.connectedPeers.add(p);
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
		catch (NullPointerException e)
		{
			return false;
		}
		return true;
	}
	
	public void removeConnectedPeer(Peer p)
	{
		connectedPeers.remove(p);
		p.stopListening();
	}
	
	/*
	public List<Peer> getRutgersPeers()
	{
		return this.rutgersPeers;
	}
	*/
}
	
	
	
	
