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
import java.util.Collections;
import java.util.List;
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
	
	
	public static Peer findSpecificPeer(List<Peer> peers, String ip)
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
	public static List<Peer> findRutgersPeers(List<Peer> peers)
	{
		List<Peer> rutgersPeers = Collections.synchronizedList(new ArrayList<Peer>());
		for(Peer p : peers)
		{
			String pIP = p.getIP();
			if(pIP.equals("128.6.171.131") && p.getPeerId().contains("-AZ5400-"))
			{
				rutgersPeers.add(p);
			}
			
			else if (pIP.equals("128.6.171.130"))
			{
				rutgersPeers.add(p);
			}
			
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
	
	public static byte[] fileToBytes(RandomAccessFile raf, int pieceIndex, int pieceLength, int blockSize)
	{
		byte[] bytes = null;
		try 
		{
			raf.seek(pieceIndex * pieceLength);
			bytes = new byte[blockSize];
			raf.read(bytes);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		return bytes;
		
	}
	
	public static ArrayList<Integer> getPeerPieces(byte[] bitfield)
	{
		ArrayList<Integer> peerPieces = new ArrayList<Integer>();
		int pieceNum = 0;
		for (int byteIndex = 0; byteIndex < bitfield.length; byteIndex++)
		{
			for (int bitIndex = 0; bitIndex < 8; bitIndex++)
			{
				if(Util.isBitSet(bitfield[byteIndex], bitIndex))
				{
					peerPieces.add(pieceNum);
					pieceNum++;
				}
			}
		}
		return peerPieces;
	}
	
	public static void setBit(byte[] bitfield, int pieceIndex)
	{
		int byteIndex = pieceIndex / 8;
		int bitIndex = pieceIndex % 8;
		bitfield[byteIndex] =  bitfield[byteIndex] |= (1 << bitIndex);
	}
	
	public static boolean isBitSet(byte b, int bitIndex)
	{
		int bitMask = 1 << bitIndex;
		return ( b & bitMask) > 0;
	}
	
	public static boolean isBitSet(byte[] bitfield, int pieceIndex)
	{
		int byteIndex = pieceIndex / 8;
		int bitIndex = pieceIndex % 8;
		byte b = bitfield[byteIndex];
		return isBitSet(b, bitIndex);
	}
	
	public static void iterateBitfield(byte[] bitfield)
	{
		
		for (int j = 0; j < bitfield.length; j++)
		{
			
			for (int i = 0; i < 8; i++)
			{
				if (Util.isBitSet(bitfield[j], i))
				{
					System.out.println("Bit index " + i + " is set in byte " + j);
					int pieceIndex = (j * 8) + i;
					System.out.println("downloaded piece " + pieceIndex);
				}
			}
		}
	}
	
	public static void removeBit(byte[] bitfield, int pieceIndex)
	{
		int byteIndex = pieceIndex / 8;
		int bitIndex = pieceIndex % 8;
		byte b = bitfield[byteIndex];
		bitfield[byteIndex] = (byte) (b & ~(1 << bitIndex));
	}
}
