package RUBT;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import GivenTools.TorrentInfo;
import Message.BitfieldMessage;
import Message.HaveMessage;
import Message.Message;
import Message.PieceMessage;
import Message.RequestMessage;
import TimedTasks.AnnounceTimerTask;
import TimedTasks.OptimisticChokingTimerTask;

public class PeerManager extends Thread
{
	private RandomAccessFile saveFile;
	private ArrayList<Integer> piecesLeft;
	private ArrayList<Peer> trackerPeers;
	private ArrayList<Peer> connectedPeers;
	
	
	// peers we have choked who we can download from
	private ArrayList<Peer> chokedSeeds;
	
	// peers we have choked who we can upload to
	private ArrayList<Peer> chokedPeers;
	
	// active peers are the ones we are currently downloading from
	private ArrayList<Peer> activeSeeds;
	
	// active peers we are currently uploading to
	private ArrayList<Peer> activePeers;
	
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
	
	
	private Timer optChokingTimer;
	private TimerTask optChokingTask;
	
	private Timer announceTimer;

	
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
		connectedPeers = new ArrayList<Peer>();
		chokedPeers = new ArrayList<Peer>();
		activePeers = new ArrayList<Peer>();
		chokedSeeds = new ArrayList<Peer>();
		activeSeeds = new ArrayList<Peer>();
		optChokingTimer = new Timer();
		optChokingTask = new OptimisticChokingTimerTask(this);
		announceTimer = new Timer();
	}
	
	public Timer getOptChokingTimer()
	{
		return optChokingTimer; 	
	}
	
	public void stopTimers()
	{
		optChokingTimer.cancel();
		announceTimer.cancel();
	}
	
	public ArrayList<Peer> getChokedPeers()
	{
		return chokedPeers;
	}
	
	public ArrayList<Peer> getActivePeers()
	{
		return activePeers;
	}
	
	public ArrayList<Peer> getChokedSeeds()
	{
		return chokedSeeds;
	}
	
	public ArrayList<Peer> getActiveSeeds()
	{
		return activeSeeds;
	}
	
	public Peer getRandomlyChokedElement(ArrayList<Peer> choked)
	{
		int r = Util.getRandomInt(choked.size());
		return choked.get(r);
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
		// implements rarest piece first algorithm
		ArrayList<Rarity> rarePieces = new ArrayList<Rarity>();
		for (int i = 0; i < this.numPieces; i++)
		{
			rarePieces.add(new Rarity(i));
		}
		
		for (Peer connectedPeer : this.connectedPeers)
		{
			byte[] bitfield = connectedPeer.getBitfield();
			if (bitfield == null) continue;
			for (int i = 0; i < this.numPieces; i++)
			{
				if (Util.isBitSet(bitfield, i))
				{
					rarePieces.get(i).decreaseRarity();
				}
			}
		}
		// sorts in descending order (rarest piece is first)
		Collections.sort(rarePieces, Collections.reverseOrder());
		
		int pieceIndex = 0;
		for (Rarity rarePiece : rarePieces)
		{
			pieceIndex = rarePiece.getPieceIndex();
			//			 the peer has piece we need			            we don't yet have this piece
			if (    (Util.isBitSet(p.getBitfield(), pieceIndex))  && (!Util.isBitSet(bitfield, pieceIndex))     ) 
			{
				break;
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
		
		// schedule the optmistic choking task to run every 30 seconds, 30 seconds from now.
		optChokingTimer.scheduleAtFixedRate(optChokingTask, 30 * 1000, 30 * 1000);
		
		// we can't schedule this task at fixed rate, because interval changes dynamically
		announceTimer.schedule(new AnnounceTimerTask(this), tracker.getInterval() * 1000);
		
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
						
						// interested.. they want to download from us, so we are seed.. they are peer
						
						p.setPeerInterested(true);
						if (activePeers.size() >= 3)
						{
							
							if (!sendMessage(Message.CHOKE_MSG, p))
							{
								removeConnectedPeer(p);
								break;
							}
							p.setPeerChoked(true);
							chokedPeers.add(p);
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
								activePeers.add(p);
								break;
							}
						}
					case Message.UNINTERESTED_ID:
						// set peer to be in an uninterested state
						p.setPeerInterested(false);
						break;
						
					case Message.CHOKE_ID:
						// a peer we are downloading from (a seed) will send choke messages
						p.setClientChoked(true);
						if (activeSeeds.contains(p))
						{
							activeSeeds.remove(p);
						}
						if (!chokedSeeds.contains(p))
						{
							chokedSeeds.add(p);
						}
						break;
					
					case Message.UNCHOKE_ID:
						// if peer isn't choked and is interested, send request message
						
						if(p.getClientInterested())
						{
							// already downloading from 3 seeds, add to choked seeds
							if (activeSeeds.size() >= 3)
							{
								chokedSeeds.add(p);
								p.setClientChoked(true);
								break;
							}
							else
							{
								activeSeeds.add(p);
								p.setClientChoked(false);
								generateRequestMessage(p);
							}
							
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
								p.setPiecesUploaded(p.getPiecesUploaded() + 1);
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
								p.setPiecesDownloaded(p.getPiecesDownloaded() + 1);
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
								// punish peer for bad behavior
								activePeers.remove(p);
								chokedPeers.add(p);
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
	
	public Tracker getTracker()
	{
		return this.tracker;
	}
	
	public int getAmountLeft()
	{
		return this.amountLeft;
	}
	
	public int getAmountUploaded()
	{
		return this.amountUploaded;
	}
	
	public int getAmountDownloaded()
	{
		return this.amountDownloaded;
	}
	
	
	public Timer getAnnounceTimer()
	{
		return this.announceTimer;
	}

	public void createNewAnnounceTimer()
	{
		this.announceTimer = new Timer();
	}
}