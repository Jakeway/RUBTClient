package RUBT;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import GivenTools.BencodingException;
import GivenTools.TorrentInfo;

public class RUBTClient 
{
	public static void main(String[] args)
	{
		
		TorrentInfo ti = null;
		
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
		
		System.out.println(ti.file_name);
		System.out.println(ti.announce_url);
		
		String trackerResponse = sendGetRequest(ti.announce_url);
		System.out.println(trackerResponse);
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
	
	public static String sendGetRequest(URL u)
	{
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
}
