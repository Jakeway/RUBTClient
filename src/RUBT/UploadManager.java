package RUBT;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class UploadManager extends Thread

{
	PeerManager pMgr;
	ServerSocket server;
	String localId;

	public UploadManager(PeerManager pMgr, String localId)
	{
		this.pMgr = pMgr;
		this.localId = localId;
		try 
		{
			server = new ServerSocket(Tracker.TRACKER_PORT_INT);
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void stopUploading()
	{
		this.running = false;
		try 
		{
			server.close();
		} 
		catch (IOException e) 
		{
			
		}
	}
	
	volatile boolean running = true;
	public void run()
	{
		while (running)
		{
			Socket peerSocket;
			try 
			{
				peerSocket = server.accept();
				String remoteAddress = peerSocket.getRemoteSocketAddress().toString();
				int colonIndex = remoteAddress.indexOf(':');
				String ip = remoteAddress.substring(1, colonIndex);
				int port = peerSocket.getPort();
				System.out.println("received connection from: " + ip + ":" + port);
				if (pMgr.getConnectedPeers().contains(Util.findSpecificPeer(pMgr.getConnectedPeers(), ip)))
				{
					System.out.println("already have connection from " + ip + ":" + port);
					peerSocket.close();
				}
				else
				{
					Peer p = new Peer(ip, port, localId, peerSocket);
					p.setPeerManager(pMgr);
					p.start();
				}
				
			}
			catch (SocketException e)
			{
				running = false;
			}
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
	}
}
	
	
	
	

