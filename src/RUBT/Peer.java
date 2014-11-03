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
	
	private byte[] handshake;
	
	byte[] response;
	
	private Socket s;
	
	private DataOutputStream outStream;
	
	private DataInputStream inStream;
	
	private boolean verified = false;
	
	private final int VALIDATION_ATTEMPTS = 1;

	private boolean interested;
	
	private boolean choked;
	
	private PeerManager pMgr;
	private boolean clientInterested;

	public Peer(
			String ip,
			int port,
			String peerID,
			String localID,
			byte[] infoHash)
	{
		
		this.ip = ip;
		this.port = port;
		this.localID = localID;
		this.peerID = peerID;
		this.infoHash = infoHash;
		this.clientInterested = false;
		this.interested = false;
		this.choked = true;
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
			this.outStream.flush();
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
	
	private boolean validateHandshake()
	{
		for (int i = 1; i <= VALIDATION_ATTEMPTS; i++)
		{
			System.out.println("Attempting to validate peer response - Attempt: " + i);
			getPeerResponse();
			// might be able to make this method not take in any params
			if (verifyResponse(response))
			{
				verified = true;
				return true;
			}
		}
		if (!(verified))
		{
			System.err.println("Failed to validate handshake from remote peer after "
					+ VALIDATION_ATTEMPTS + " times. Try again.");
		}
		return false;
	}

	public void stopListening()
	{
		continueRunning = false;
		closeConnection();
		System.out.println("closing peer connection");
	}
	
	volatile boolean continueRunning = true;
	public void run()
	{
		getConnection();
		generateHandshake();
		if (!validateHandshake())
		{
			closeConnection();
		}
			
		else
		{
			BitfieldMessage bm =  new BitfieldMessage(pMgr.getBitfieldLength(), pMgr.getBitfield());
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
}



