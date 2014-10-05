package RUBT;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import GivenTools.Bencoder2;
import GivenTools.BencodingException;
import GivenTools.ToolKit;
import GivenTools.TorrentInfo;

public class RUBTClient 
{
	public static void main(String[] args)
	{
		
		
		final ByteBuffer HANDSHAKE_HEADER = ByteBuffer.wrap(new byte[] { 
				19,'B','i','t','T','o','r','r','e','n','t',' ',
					'p','r','o','t','o','c','o','l'});
		
		
		TorrentInfo ti = null;
		String announceURL = "";
		
		if(args.length > 2 || args.length <= 1)
		{
			System.err.println("Please enter two arguments.");
			System.exit(1);
		}
		File torrentFile = null;
		File destFile = null;
		try
		{
			torrentFile = new File(args[0]);
			destFile = new File(args[1]);
		}
		catch (NullPointerException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		
		ti = Util.getTorrentInfo(torrentFile);
		
		String peer_id = "tomjakewaynrobcasale";

		
		byte[] handShake = new byte[68];
		HANDSHAKE_HEADER.get(handShake, 0, HANDSHAKE_HEADER.remaining());
		ti.info_hash.get(handShake, 28, ti.info_hash.remaining());
		
		
			Util.addStringToByteArray(handShake, peer_id);
			
			System.out.println(new String(handShake));
			
			try
			{
				Socket peerSocket = new Socket(ip, port);
				PrintWriter sendToPeer = new PrintWriter(peerSocket.getOutputStream(), true);
				BufferedReader fromPeer = new BufferedReader(new InputStreamReader(peerSocket.getInputStream()));
				
				sendToPeer.println(handShake);
				System.out.println("peer: " + fromPeer.readLine());
				
				//close everything
				peerSocket.close();
				sendToPeer.close();
				fromPeer.close();
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
		
		
	}
	

		
}
