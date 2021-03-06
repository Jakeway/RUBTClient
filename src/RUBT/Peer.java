package RUBT;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.ByteBuffer;

import Message.BitfieldMessage;
import Message.Message;

public class Peer extends Thread
{
	
	private final static ByteBuffer HANDSHAKE_HEADER = ByteBuffer.wrap(new byte[] { 
							'B','i','t','T','o','r','r','e','n','t',' ',
							'p','r','o','t','o','c','o','l'});
	
	
	private String ip;
	
	private int port;
	
	private String peerId;
	
	private String localId;
	
	private final int VALIDATION_ATTEMPTS = 1;
	
	private byte[] handshake;
	
	private Socket s;
	
	private DataOutputStream outStream;
	
	private DataInputStream inStream;

	private boolean peerInterested;
	
	private boolean peerChoked;
	
	private boolean clientChoked;
	
	private PeerManager pMgr;
	
	private boolean clientInterested;

	private byte[] bitfield;
	
	// pieces the peer has downloaded from us
	private int piecesDownloaded;
	
	// pieces the peer has uploaded to us
	private int piecesUploaded;


	//private ArrayList<Integer> peerPieces;

	public Peer(String ip,
			int port,
			String peerID,
			String localID)
	{
		this.ip = ip;
		this.port = port;
		this.localId = localID;
		this.peerId = peerID;
		this.clientInterested = false;
		this.peerInterested = false;
		this.clientChoked = true;
		this.peerChoked = true;
		this.piecesDownloaded = 0;
		this.piecesUploaded = 0;
	}
	
	public Peer(String ip, int port, String localID, Socket s)
	{
		this.ip = ip;
		this.port = port;
		this.localId = localID;
		this.clientInterested = false;
		this.peerInterested = false;
		this.clientChoked = true;
		this.peerChoked = true;
		this.s = s;
		this.piecesDownloaded = 0;
		this.piecesUploaded = 0;
	}
	
	
	public String getIP()
	{
		return ip;
	}
	
	public void setPeerManager(PeerManager pMgr)
	{
		this.pMgr = pMgr;
		this.bitfield = new byte[pMgr.getBitfieldLength()];
	}
	
	public byte[] getBitfield()
	{
		return this.bitfield;
	}
	
	public void setBitfield(byte[] bitfield)
	{
		this.bitfield = bitfield;
	}

	
	public boolean getClientInterested()
	{
		return this.clientInterested;
	}
	
	public void setClientInterested(boolean state)
	{
		this.clientInterested = state;
	}
	
	public boolean getPeerInterested()
	{
		return this.peerInterested;
	}
	
	public void setPeerInterested(boolean state)
	{
		this.peerInterested = state;
	}
	
	public void setPeerId(String peerId)
	{
		this.peerId = peerId;
	}
	
	public boolean getPeerChoked()
	{
		return this.peerChoked;
	}
	
	public void setPeerChoked(boolean state)
	{
		this.peerChoked = state;
	}
	
	public boolean getClientChoked()
	{
		return this.clientChoked;
	}
	
	public void setClientChoked(boolean state)
	{
		this.clientChoked = state;
	}
	
	public String getPeerId()
	{
		return peerId;
	}
	
	public DataOutputStream getOutputStream()
	{
		return this.outStream;
	}
	
	public int getPiecesDownloaded()
	{
		return this.piecesDownloaded;
	}
	
	public void setPiecesDownloaded(int piecesDownloaded)
	{
		this.piecesDownloaded = piecesDownloaded;
	}
	
	public int getPiecesUploaded()
	{
		return this.piecesUploaded;
	}
	
	public void setPiecesUploaded(int piecesUploaded)
	{
		this.piecesUploaded = piecesUploaded;
	}
	
	private void generateHandshake()
	{
		byte[] infoHash = pMgr.getIndexHash();
		byte[] handShake = new byte[68];
		handShake[0] = 19;
		System.arraycopy(HANDSHAKE_HEADER.array(), 0, handShake, 1, HANDSHAKE_HEADER.array().length);
		System.arraycopy(infoHash, 0, handShake, 28, infoHash.length);
		Util.addStringToByteArray(handShake, localId, 48);
		this.handshake = handShake;
	}
	
	
	private void sendHandshake()
	{
		try 
		{
			outStream.write(handshake);
			outStream.flush();
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	private byte[] getHandshakeResponse()
	{
		byte[] handshakeResponse = new byte[68];
		try 
		{
			inStream.readFully(handshakeResponse);
		} 
		catch (IOException e) 
		{
			return null;
			
		}
		return handshakeResponse;
	}
	
	
	

	// new peer is set to true if this peer has connected to us and we don't know its peer id
	private boolean verifyResponse(byte[] peerHandshake, boolean newPeer)
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

		if (!newPeer)
		{
			// check the peerID
			byte[] peerIdArray = this.peerId.getBytes();
			for (int i = 48; i < peerHandshake.length; i++)
			{
				if (peerHandshake[i] != peerIdArray[i-48])
				{
					return false;
				}
			}
			return true;
		}
		else
		// this response checks out, set the new peers peer id to the peer id it sent in handshake
		{
			parsePeerId(peerHandshake);
			return true;
		}

	}
	
	private void parsePeerId(byte[] handshakeResponse) 
	{
		byte[] peerId = new byte[20];
		for (int i = 48; i < handshakeResponse.length; i++)
		{
			peerId[i-48] = handshakeResponse[i];
		}
		try 
		{
			setPeerId(new String(peerId, "UTF-8"));
		} 
		catch (UnsupportedEncodingException e) 
		{
			e.printStackTrace();
		}
	}
	
	
	// sends handshake, and validates response
	// newPeer is set to true if this is a peer connecting to us to download
	private boolean sendAndValidateHandshake(boolean newPeer)
	{
		for (int i = 1; i <= VALIDATION_ATTEMPTS; i++)
		{
			//System.out.println("Attempting to validate peer response - Attempt: " + i);
			sendHandshake();
			//System.out.println("handshake sent");
			byte[] handshakeResponse = getHandshakeResponse();
			if (handshakeResponse == null)
			{
				return false;
			}
			if (verifyResponse(handshakeResponse, newPeer))
			{
				return true;
			}
		}
		return false;
	}
	
	private boolean getConnection()
	{
		Socket peerSocket = null;
		try {
			peerSocket = new Socket(ip, port);
			this.s = peerSocket;
		} catch (IOException e)
		{
			return false;
		}

		if (getStreams())
		{
			return true;
		}
		else
		{
			try {
				peerSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
	public boolean getStreams()
	{
		try {
			DataOutputStream dos = new DataOutputStream(s.getOutputStream());
			DataInputStream dis = new DataInputStream(s.getInputStream());
			this.inStream = dis;
			this.outStream = dos;
			
		} catch (IOException e) {
			return false;
		}
		return true;
	}
	
	public void closeConnection()
	{
		try {
			this.inStream.close();
			this.outStream.flush();
			this.outStream.close();
			this.s.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			
			System.out.println("null pointer exception with " + this.ip);
		}
	}
	
	
	public void printResponse(byte[] handshakeResponse)
	{
		try 
		{
			System.out.println(new String(handshakeResponse, "UTF-8"));
		} 
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
	}

	public void stopListening()
	{
		continueRunning = false;
		closeConnection();
		System.out.println("closing peer connection to " + ip);
	}
	
	
	private volatile boolean continueRunning = true;
	public void run()
	{
		boolean newPeer;
		generateHandshake();
		byte[] handshakeResponse;
		
		// this happens when we are the ones who want to download
		if (s == null)
		{
			newPeer = false;
			if (getConnection())
			{
				System.out.println("got connection to " + this.ip);
				pMgr.addConnectedPeer(this);
			}
			else
			{
				System.out.println("couldnt get connection to " + this.ip);
				return;
			}
			if (!sendAndValidateHandshake(newPeer))
			{
				System.err.println("couldn't verify handshake with " + this.ip);
				pMgr.removeConnectedPeer(this);
				return;
			}
		}
		// happens when a peer has connected to us
		else
		{
			newPeer = true;
			if (getStreams())
			{
				System.out.println("acquired streams for incoming connection " + this.ip);
				pMgr.addConnectedPeer(this);
			}
			else
			{
				// if can't get the streams, then close the socket.
				System.out.println("couldn't get streams for " + this.ip);
				try 
				{
					this.s.close();
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
				}
				return;
			}
			handshakeResponse = getHandshakeResponse();
			if (!verifyResponse(handshakeResponse, newPeer))
			{
				System.err.println("couldn't verify handshake with " + this.ip);
				pMgr.removeConnectedPeer(this);
				return;
			}
			else
			{
				System.out.println("verified response from " + this.ip);
			}
			sendHandshake();	
		}
		
		
		BitfieldMessage bm =  new BitfieldMessage(pMgr.getBitfieldLength(), pMgr.getBitfield());
		pMgr.sendMessage(bm, this);
		
		while (continueRunning)
		{
			try
			{
				Message m = Message.receive(inStream);
				pMgr.acceptMessage(m, this);
			}
			catch (IOException e)
			{
				continueRunning = false;
			}
		}
	}
}




