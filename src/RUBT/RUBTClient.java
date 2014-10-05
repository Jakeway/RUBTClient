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
		
		final ByteBuffer KEY_PEERS = ByteBuffer.wrap(new byte[] {
				'p', 'e', 'e', 'r', 's' });
		
		final ByteBuffer KEY_PEER_ID = ByteBuffer.wrap(new byte[] {
				'p', 'e', 'e', 'r', ' ', 'i', 'd' });
		
		final ByteBuffer KEY_IP = ByteBuffer.wrap(new byte[] {
				'i', 'p' });
		
		final ByteBuffer KEY_PORT = ByteBuffer.wrap(new byte[] {
				'p', 'o', 'r', 't' });
		
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
		
		ti = getTorrentInfo(torrentFile);
		
		int length = ti.file_length;
		
		String encodedHash="";
		try 
		{
			encodedHash = Util.byteArrayToURLString(ti.info_hash.array());
		} catch (Exception e) {
		
			e.printStackTrace();
		}
		 
		
		String peer_id = "tomjakewaynrobcasale";

		announceURL = ti.announce_url.toExternalForm();
		String trackerURL = announceURL + "?info_hash=" + encodedHash
				+ "&peer_id=" + peer_id
				+ "&left=" + length
				+ "&port=" + "6881"
				+ "&downloaded=" + "0";
		
		System.out.println(trackerURL);
		
		byte[] trackerResponse = sendGetRequest(trackerURL).getBytes();
		HashMap<ByteBuffer, Object> trackerResponseMap = null;
		ArrayList<HashMap<ByteBuffer, Object>> peerDicts = null;
		
		ByteBuffer remotePeerIdBytes = null;
		
		byte[] handShake = new byte[68];
		HANDSHAKE_HEADER.get(handShake, 0, HANDSHAKE_HEADER.remaining());
		ti.info_hash.get(handShake, 28, ti.info_hash.remaining());
		
		try {
			trackerResponseMap = 
					(HashMap<ByteBuffer, Object>) Bencoder2.decode(trackerResponse);
			
			peerDicts = 
					(ArrayList<HashMap<ByteBuffer, Object>>)
					trackerResponseMap.get(KEY_PEERS);

			String ip = "128.6.171.131";
			int port = 61350;
			
			for (HashMap<ByteBuffer, Object> dicts : peerDicts)
			{
				remotePeerIdBytes = (ByteBuffer) dicts.get(KEY_PEER_ID);
				String remotePeerId = new String(remotePeerIdBytes.array(), "UTF-8");
				
				ByteBuffer remoteIPBytes = (ByteBuffer) dicts.get(KEY_IP);
				String remoteIP = new String(remoteIPBytes.array(), "UTF-8");
				
				int remotePort =  (Integer) dicts.get(KEY_PORT);
				
				if(remoteIP.equals("128.6.171.131"))
					break;
								
				System.out.println(remotePeerId);
				System.out.println(remoteIP);
				System.out.println(remotePort);
			}
			
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
			
		} catch (BencodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		ToolKit.printMap(trackerResponseMap, 0);
		
		
	}
	
	public static TorrentInfo getTorrentInfo(File torrentFile)
	{
		FileInputStream fis = null;
		TorrentInfo ti = null;
		
		try 
		{
			fis = new FileInputStream(torrentFile);
		} catch (FileNotFoundException e) 
		{
			System.err.println("File not found");
			System.exit(1);
		}
		
		byte [] bytes = new byte[(int) torrentFile.length()];
		try 
		{
			fis.read(bytes, 0, bytes.length);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try 
		{
			ti = new TorrentInfo(bytes);
		} catch (BencodingException e) {
			e.printStackTrace();
		}
		
		try
		{
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return ti;
	}
	
	public static String sendGetRequest(String trackerURL)
	{
		URL u = null;
		try {
			u = new URL(trackerURL);
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
		String contents = "";
		try 
		{
		
			BufferedReader br = new BufferedReader(new InputStreamReader(u.openStream()));
			String line = "";
			while ((line = br.readLine()) != null)
			{
				contents += line;
			}
			br.close();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return contents;
	}
	
	
	 /* public static String byteArrayToURLString(byte in[]) 
	  {
		    byte ch = 0x00;
		    int i = 0;
		    if (in == null || in.length <= 0)
		      return null;

		    String pseudo[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
		        "A", "B", "C", "D", "E", "F" };
		    StringBuffer out = new StringBuffer(in.length * 2);

		    while (i < in.length) {
		      // First check to see if we need ASCII or HEX
		      if ((in[i] >= '0' && in[i] <= '9')
		          || (in[i] >= 'a' && in[i] <= 'z')
		          || (in[i] >= 'A' && in[i] <= 'Z') || in[i] == '$'
		          || in[i] == '-' || in[i] == '_' || in[i] == '.'
		          || in[i] == '!') {
		        out.append((char) in[i]);
		        i++;
		      } else {
		        out.append('%');
		        ch = (byte) (in[i] & 0xF0); // Strip off high nibble
		        ch = (byte) (ch >>> 4); // shift the bits down
		        ch = (byte) (ch & 0x0F); // must do this is high order bit is
		        // on!
		        out.append(pseudo[(int) ch]); // convert the nibble to a
		        // String Character
		        ch = (byte) (in[i] & 0x0F); // Strip off low nibble
		        out.append(pseudo[(int) ch]); // convert the nibble to a
		        // String Character
		        i++;
		      }
		    }

		    String rslt = new String(out);

		    return rslt;
	  }*/
		
}
