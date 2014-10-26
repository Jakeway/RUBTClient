package RUBT;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import GivenTools.BencodingException;
import GivenTools.TorrentInfo;

public class Util
{

	public static String sendGetRequest(String URL)
	{
		URL u = null;
		try {
			u = new URL(URL);
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
	
	public static String byteArrayToURLString(byte in[]) 
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
	
	public static String getRandomPeerId()
	{
	    Random rand = new Random();
	    int min = 97;
	    int max = 122;
	    String peerId = "";
	    for (int i = 0; i < 20; i++)
	    {
	    	int randInt = rand.nextInt((max - min) + 1) + min;
	    	char c = (char) randInt;
	    	peerId += c;
	    }
	    return peerId.toUpperCase();
	}
	
	public static void addStringToByteArray(byte[] array, String s, int offset)
	{
		int c = 0;
		byte[] sBytes = s.getBytes();
		for(int i = offset; i < array.length; i++)
		{
			array[i] = sBytes[c];
			c++;
		}
	}
	
	// method which finds us the Rutgers peer to use
	public static Peer findPeer(ArrayList<Peer> peers)
	{
		for(Peer p : peers)
		{
			String pID = p.getPeerID().substring(0, 8);
			if(pID.equals("-AZ5400-"))
				return p;			
		}
		return null;
	}
	
	//method which finds all of the valid peers
	public static ArrayList<Peer> findMultiplePeers(ArrayList<Peer> peers)
	{
		ArrayList<Peer> listOfPeers = new ArrayList<Peer>();
		for(Peer p : peers)
		{
			String pIP = p.getPeerIP();
			if(pIP.equals("128.6.171.130") || pIP.equals("128.6.171.131"))
			{
				p.start();
				listOfPeers.add(p);
			}
		}
		return listOfPeers;
	}
	
	public static boolean verifyHash(byte[] input, byte[] hash)
	{
		MessageDigest sha1 = null;
		try 
		{
			sha1 = MessageDigest.getInstance("SHA-1");
		} 
		catch (NoSuchAlgorithmException e) 
		{
			e.printStackTrace();
		}
		byte[] hashed_input = sha1.digest(input);
		if (Arrays.equals(hashed_input, hash))
		{
			return true;
		}
		else
		{
			return false;
		}
	}
}
