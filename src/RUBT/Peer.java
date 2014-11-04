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
	
	private String peerId;
	
	private String localId;
	
	private final int VALIDATION_ATTEMPTS = 1;
	
	private byte[] handshake;
	
	private Socket s;
	
	private DataOutputStream outStream;
	
	private DataInputStream inStream;

	private boolean interested;
	
	private boolean choked;
	
	private PeerManager pMgr;
	
	private boolean clientInterested;

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
		this.interested = false;
		this.choked = true;
		
		System.out.println("peer id: " + peerId);
		System.out.println("local client id: " + localId);
	}
	
	public Peer(String ip, int port, String localID, Socket s)
	{
		this.ip = ip;
		this.port = port;
		this.localId = localID;
		this.clientInterested = false;
		this.interested = false;
		this.choked = true;
		this.s = s;
		System.out.println("local client id: " + localId);
	}
	
	
	public String getIP()
	{
		return ip;
	}
	
	public void setPeerManager(PeerManager pMgr)
	{
		this.pMgr = pMgr;
	}
	
	public boolean getClientInterested()
	{
		return this.clientInterested;
	}
	
	public void setClientInterested(boolean state)
	{
		this.clientInterested = state;
	}
	
	public boolean getInterested()
	{
		return this.interested;
	}
	
	public void setInterested(boolean state)
	{
		this.interested = state;
	}
	
	public void setPeerId(String peerId)
	{
		this.peerId = peerId;
	}
	
	public boolean getChoked()
	{
		return this.choked;
	}
	
	public void setChoked(boolean state)
	{
		this.choked = state;
	}
	
	public String getPeerId()
	{
		return peerId;
	}
	
	public DataOutputStream getOutputStream()
	{
		return this.outStream;
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
			e.printStackTrace();
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
			System.out.println("parsing peer id");
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
			System.out.println("Attempting to validate peer response - Attempt: " + i);
			sendHandshake();
			System.out.println("handshake sent");
			byte[] handshakeResponse = getHandshakeResponse();
			if (verifyResponse(handshakeResponse, newPeer))
			{
				return true;
			}
		}
		System.err.println("Failed to validate handshake from remote peer after "
					+ VALIDATION_ATTEMPTS + " times. Try again.");
		return false;
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

		getStreams();
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
		System.out.println("closing peer connection");
	}
	
	public void getStreams()
	{
		try {
			DataOutputStream dos = new DataOutputStream(s.getOutputStream());
			DataInputStream dis = new DataInputStream(s.getInputStream());
			this.inStream = dis;
			this.outStream = dos;
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private volatile boolean continueRunning = true;
	public void run()
	{
		boolean newPeer;
		generateHandshake();
		System.out.println("printing generated handshake");
		printResponse(handshake);
		byte[] handshakeResponse;
		// this happens when we are the ones who want to download
		if (s == null)
		{
			newPeer = false;
			System.out.println("getting connection");
			getConnection();
			if (!sendAndValidateHandshake(newPeer))
			{
				closeConnection();
				return;
			}
			System.out.println("validated handshake");
		}
		// happens when a peer has connected to us
		else
		{
			newPeer = true;
			System.out.println("getting streams");
			getStreams();
			handshakeResponse = getHandshakeResponse();
			System.out.println("about to verify handshake");
			this.printResponse(handshakeResponse);
			if (!verifyResponse(handshakeResponse, newPeer))
			{
				System.out.println("couldnt verify handshake");
				closeConnection();
				return;
			}
			System.out.println("verified handshake");
			sendHandshake();	
		}
		
		
		BitfieldMessage bm =  new BitfieldMessage(pMgr.getBitfieldLength(), pMgr.getBitfield());
		System.out.println("sent bitfield message");
		bm.send(outStream);
		
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




