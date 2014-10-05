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
	
	private String peerID;
	
	public Peer(String ip, int port, String peerID)
	{
		this.ip = ip;
		this.port = port;
		this.peerID = peerID;
	}
	
	public String getPeerResponse(TorrentInfo ti, String localID)
	{
		byte[] handShake = new byte[68];
		handShake[0] = 19;
		HANDSHAKE_HEADER.get(handShake, 1, HANDSHAKE_HEADER.remaining());
		System.arraycopy(ti.info_hash.array(), 0, handShake, 28, ti.info_hash.array().length);
		Util.addStringToByteArray(handShake, localID, 48);
		
		try {
			System.out.println(new String(handShake, "UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try
		{
			Socket peerSocket = new Socket(ip, port);

			DataOutputStream dos = new DataOutputStream(peerSocket.getOutputStream());
			DataInputStream dis = new DataInputStream(peerSocket.getInputStream());
			
			dos.write(handShake);
			dos.flush();
			
			byte[] handshakeResponse = new byte[68];
			dis.readFully(handshakeResponse);
			
			System.out.println(new String(handshakeResponse, "UTF-8"));
			//close everything
			dos.close();
			dis.close();
			peerSocket.close();
		}
		catch (UnknownHostException e)
		{
			System.err.println("UnknownHostException: " + e.getMessage());
			System.exit(1);
		}
		catch (SocketException e)
		{
			System.err.println("SocketException: " + e.getMessage());
			System.exit(1);
		}
		catch (IOException e)
		{
			System.err.println("IOException: " + e.getMessage());
			System.exit(1);
		}
		return null;
	}
}
