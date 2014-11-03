package RUBT;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

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
	
	public PeerManager(
			int pieceLength,
			ByteBuffer[] piece_hashes,
			int fileLength,
			RandomAccessFile destFile,
			ArrayList<Integer> pieces,
			Tracker t,
			boolean DEBUG)
	{
		this.DEBUG = DEBUG;
		this.pieceLength = pieceLength;
		this.piece_hashes = piece_hashes;
		this.saveFile = destFile;
		this.piecesLeft = pieces;
		this.fileLength = fileLength;
		this.tracker = t;
		this.peers = t.getPeerList();
		this.numPieces = piece_hashes.length;
		this.rutgersPeers = Util.findRutgersPeers(peers);
		bitfield = new byte[getBitfieldLength()];
		amountLeft = fileLength;
		jobs = new LinkedBlockingQueue<Job>();
		printPeers();
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
	
	public void startDownloading()
	{
		startTime = System.nanoTime();
		if (DEBUG)
		{
			Peer p = Util.findSpecificPeer(peers, RUBTClient.getDownloadFromIP());
			System.out.println("starting specific download from ip " + RUBTClient.getDownloadFromIP());
			p.setPeerManager(this);
			p.start();
		}
		else
		{
			for (Peer p : rutgersPeers)
			{
				System.out.println("starting a peer");
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
		
		// need to tell the tracker that we have finished downloading
		// if a peer calls this message, all the pieces have been downloaded
		
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
		// getting the last piece
		int length;
		if (pieceToGet == numPieces-1)
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
				
				switch (msg.getID())
				{
					case Message.KEEP_ALIVE_ID:
						Message.KEEP_ALIVE_MSG.send(p.getOutputStream());
						break;
					
					case Message.INTERESTED_ID:
						// set peer to be in an interested state
						p.setInterested(true);
						Message.UNCHOKE_MSG.send(p.getOutputStream());
						p.setChoked(false);
						break;
					
					case Message.UNINTERESTED_ID:
						// set peer to be in an uninterested state
						p.setInterested(false);
						Message.KEEP_ALIVE_MSG.send(p.getOutputStream());
						break;
						
					case Message.CHOKE_ID:
						// set peer to be choked
						break;
					
					case Message.UNCHOKE_ID:
						// if peer isn't choked and is interested, send piece request
						if(p.getClientInterested())
						{
							generateRequestMessage(p);
						}
						else
						{
							Message.KEEP_ALIVE_MSG.send(p.getOutputStream());
						}
						// pick a piece, request it.
						// else, just send a keep alive
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
						if(!(p.getChoked()) && p.getInterested())
						{
							if (piecesLeft.contains((Object) pieceIndex))
							{
								byte[] block = new byte[rMsg.getBlockLength()];
								byte[] fileInBytes = Util.fileToBytes(saveFile);
								System.arraycopy(fileInBytes, rMsg.getByteOffset(), block, 0, rMsg.getBlockLength());
								PieceMessage pieceMsg = new PieceMessage(pieceIndex, rMsg.getByteOffset(), block);
								pieceMsg.send(p.getOutputStream());
								amountUploaded += rMsg.getBlockLength();
								break;
							}
							else
							{
								Message.CHOKE_MSG.send(p.getOutputStream());
								p.setChoked(true);
							}
						}	
						case BitfieldMessage.BITFIELD_ID:
							Message.INTERESTED_MSG.send(p.getOutputStream());
							p.setClientInterested(true);
							break;
				}
			}
			catch (InterruptedException e) 
			{
				keepRunning = false;
			}
			
		}

		
	}
		
}
	
	
	
	
