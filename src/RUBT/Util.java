package RUBT;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
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
	
	public static int getRandomInt(int size)
	{
		Random rand = new Random();
		int randInt = rand.nextInt(size);
		return randInt;
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
	
	
	public static Peer findSpecificPeer(ArrayList<Peer> peers, String ip)
	{
		for (Peer p : peers)
		{
			if (p.getIP().equals(ip))
			{
				return p;
			}
		}
		return null;
	}
	
	
	
	
	//method which finds all of the rutgers peers
	public static ArrayList<Peer> findRutgersPeers(ArrayList<Peer> peers)
	{
		ArrayList<Peer> rutgersPeers = new ArrayList<Peer>();
		for(Peer p : peers)
		{
			String pIP = p.getIP();
			// pIP.equals("128.6.171.130") currently down
			if(pIP.equals("128.6.171.131") && p.getPeerId().contains("-AZ5400-"))
			{
				rutgersPeers.add(p);
			}
		/*	
			if (pIP.equals("128.6.171.130"))
			{
				rutgersPeers.add(p);
			}
			*/
		}
		return rutgersPeers;
	}
	
	public static ArrayList<Integer> getPiecesLeft(int numPieces)
	{
		ArrayList<Integer> pieces = new ArrayList<Integer>();
		
		for(int i = 0; i < numPieces; i++)
		{
			pieces.add(i);
		}
		return pieces;
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
	
	public static byte[] fileToBytes(RandomAccessFile raf)
	{
		byte[] bytes = null;
		try 
		{
			bytes = new byte[(int) raf.length()];
			raf.readFully(bytes);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		return bytes;
		
	}
}
