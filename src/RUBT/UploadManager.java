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
				if (pMgr.getPeers().contains(Util.findSpecificPeer(pMgr.getPeers(), ip)))
				{
					System.out.println("closing connection from " + ip + ":" + port);
					peerSocket.close();
				}
				else
				{
					System.out.println("starting a peer which will be uploading");
					Peer p = new Peer(ip, port, localId, peerSocket);
					p.setPeerManager(pMgr);
					p.start();
					pMgr.addPeer(p);
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
	
	
	
	

