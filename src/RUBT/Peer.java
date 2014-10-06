package RUBT;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import GivenTools.TorrentInfo;

public class Peer {

	private final static ByteBuffer HANDSHAKE_HEADER = ByteBuffer.wrap(new byte[] { 
							'B','i','t','T','o','r','r','e','n','t',' ',
							'p','r','o','t','o','c','o','l'});
	private String ip;
	
	private int port;
	
	private String localID;
	
	private String peerID;
	
	private byte[] infoHash;
	
	private byte[] handshake;
	
	private byte[] response;
	
	private DataOutputStream outStream;
	
	private DataInputStream inStream;
	
	
	
	public Peer(String ip, int port, String peerID,
				String localID, byte[] infoHash)
	{
		this.ip = ip;
		this.port = port;
		this.localID = localID;
		this.peerID = peerID;
		this.infoHash = infoHash;
		initPeer();
	}
	
	private void initPeer()
	{
		generateHandshake();
		getConnection();
		this.getPeerResponse();
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
		} catch (UnknownHostException e) {
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
}
