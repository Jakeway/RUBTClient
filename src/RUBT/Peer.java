package RUBT;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class Peer extends Thread
{

	private final static ByteBuffer HANDSHAKE_HEADER = ByteBuffer.wrap(new byte[] { 
							'B','i','t','T','o','r','r','e','n','t',' ',
							'p','r','o','t','o','c','o','l'});
	private String ip;
	
	private int port;
	
	private String localID;
	
	private String peerID;
	
	private byte[] infoHash;
	
	private ByteBuffer[] piece_hashes;
	
	private int pieceLength;
	
	private byte[] handshake;
	
	byte[] response;
	
	private Socket s;
	
	private DataOutputStream outStream;
	
	private DataInputStream inStream;
	
	private boolean verified = false;
	
	private final int VALIDATION_ATTEMPTS = 1;
	
	private int file_length;

	private PeerManager pMgr;

	
	
	
	
	public Peer(
			String ip,
			int port,
			String peerID,
			String localID,
			byte[] infoHash,
			ByteBuffer[] piece_hashes,
			int piece_length,
			int file_length)
	{
		this.ip = ip;
		this.port = port;
		this.localID = localID;
		this.peerID = peerID;
		this.infoHash = infoHash;
		this.piece_hashes = piece_hashes;
		this.pieceLength = piece_length;
		this.file_length = file_length;
	}
	
	
	public String getIP()
	{
		return ip;
	}
	
	public void setPeerManager(PeerManager pMgr)
	{
		this.pMgr = pMgr;
	}
	
	public String getPeerId()
	{
		return peerID;
	}
	
	public DataOutputStream getOutputStream()
	{
		return this.outStream;
	}
	
	
	private void generateHandshake()
	{
		byte[] handShake = new byte[68];
		handShake[0] = 19;
		HANDSHAKE_HEADER.get(handShake, 1, HANDSHAKE_HEADER.remaining());
		System.arraycopy(infoHash, 0, handShake, 28, infoHash.length);
		Util.addStringToByteArray(handShake, localID, 48);
		this.handshake = handShake;
	}
	
	private void getConnection()
	{
		Socket peerSocket = null;
		try {
			peerSocket = new Socket(ip, port);
			this.s = peerSocket;
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			DataOutputStream dos = new DataOutputStream(peerSocket.getOutputStream());
			DataInputStream dis = new DataInputStream(peerSocket.getInputStream());
			this.inStream = dis;
			this.outStream = dos;
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void closeConnection()
	{
		try {
			this.inStream.close();
			this.outStream.close();
			this.s.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void getPeerResponse()
	{
		byte[] handshakeResponse = new byte[68];
		try 
		{
			outStream.write(handshake);
			outStream.flush();
			inStream.readFully(handshakeResponse);
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		this.response = handshakeResponse;
	}
	
	public void printResponse()
	{
		try 
		{
			System.out.println(new String(response, "UTF-8"));
		} 
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
	}
	
	public String getPeerID()
	{
		return this.peerID;
	}
	public String getPeerIP()
	{
		return this.ip;
	}
	
	private Boolean verifyResponse(byte[] peerHandshake)
	{
		
		if(peerHandshake == null)
		{
			return false;
		}
		else if(peerHandshake.length != 68)
		{
			return false;
		}
		
		// check bit torrent protocol and info hash
		for (int i = 0; i < 48; i++)
		{
			// skip the 8 reserved bytes
			if (i > 19 && i < 28)
			{
				continue;
			}
			if (peerHandshake[i] != handshake[i])
			{
				return false;
			}
		}
	
		// check the peerID
		byte[] peerIdArray = this.peerID.getBytes();
		for (int i = 48; i < peerHandshake.length; i++)
		{
			
			if (peerHandshake[i] != peerIdArray[i-48])
			{
				return false;
			}
		}
		return true;
	}
	private void validateHandshake()
	{
		for (int i = 1; i <= VALIDATION_ATTEMPTS; i++)
		{
			System.out.println("Attempting to validate peer response - Attempt: " + i);
			getPeerResponse();
			// might be able to make this method not take in any params
			if (verifyResponse(response))
			{
				verified = true;
				System.out.println("Verified handshake.");
				break;
			}
		}
		if (!(verified))
		{
			System.err.println("Failed to validate handshake from remote peer after "
					+ VALIDATION_ATTEMPTS + " times. Try again.");
			System.exit(1);
		}
	}
	



	/*
	 * This method is responsible for getting a PieceMessage, nothing more.
	 */
	
	public PieceMessage getPieceMessage(int pieceNum)
	{
		
		Message pieceMessage;
		RequestMessage rm = new RequestMessage(pieceNum, 0, pieceLength);
		RequestMessage.send(rm, outStream);
	
		pieceMessage = Message.receive(inStream);
	
		if(pieceMessage != null)
		{
			// should be piece message at this point
			// note: should write an equals method for each message type
			
			if (pieceMessage.getID() == PieceMessage.PIECE_ID)
			{
				PieceMessage pm = (PieceMessage) pieceMessage;
				if (Util.verifyHash
						(pm.getBlock(), piece_hashes[pm.getPieceIndex()].array()))
				{
					return pm;
				}
				else
				{
					System.err.println("Unable to verify piece message");
					System.exit(1);
				}
			}
		}
		
		return null;
	}
	
	
	public synchronized void getLastMessage()
	{
		//int bytesLeft = file_length % pieceLength;
		int pieceSize = file_length % pieceLength;
		System.out.println("bytes left: " + pieceSize);
		RequestMessage rm = new RequestMessage(piece_hashes.length-1, 0, pieceSize);
		RequestMessage.send(rm, outStream);
		Message m = Message.receive(inStream);
		if (m != null)
		{
			if (m.getID() == PieceMessage.PIECE_ID)
			{
				PieceMessage pm = (PieceMessage) m;
				if (Util.verifyHash
						(pm.getBlock(), piece_hashes[pm.getPieceIndex()].array()))
				{
					System.out.println("verified last piece");
					pMgr.digestPieceMessage(pm);
				}
				else
				{
					System.err.println("Unable to verify piece message");
					System.exit(1);
				}
			}
		}
		else
		{
			System.err.println("Received a null message as the last message");
			System.exit(1);
		}
	}
		

	
	
	public void run()
	{
		generateHandshake();
		getConnection();
		validateHandshake();
		
		// first message after handshake is bitfield message (not always the case, need to check for this)
		BitfieldMessage bm = (BitfieldMessage) Message.receive(inStream);
		
		System.out.println("Sending interested message");
		Message.send(Message.INTERESTED_MSG, outStream);
		

		//Message.LAST_MESSAGE_TIME = System.currentTimeMillis();
		
	
		Message m = Message.receive(inStream);
		if(m.toString().equals("UNCHOKE_MSG"))
		{
			System.out.println("Received unchoked message");
			System.out.println("Starting download... Please wait patiently.");
			
			//getLastMessage();
			while(pMgr.piecesLeft.size() != 0) 
			{
				int random = Util.getRandomInt(pMgr.piecesLeft.size());
				int pieceToGet = pMgr.piecesLeft.get(random);
				//System.out.println("Getting piece: " + pieceToGet);
				PieceMessage pieceMessage = getPieceMessage(pieceToGet);
				if (pieceMessage != null)
				{
					pMgr.digestPieceMessage(pieceMessage);
				}
			}
			// at this point, there is only one piece left to get. peers need to fight to death to determine who gets to download last piece
			System.out.println("downloading last message");
			getLastMessage();
			// tell the peer manager we have downloaded last piece, he's going to be so proud of us
			pMgr.finishedDownloading();
			
		}
		closeConnection();
	}

}

