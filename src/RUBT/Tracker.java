package RUBT;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import GivenTools.Bencoder2;
import GivenTools.BencodingException;
import GivenTools.ToolKit;
import GivenTools.TorrentInfo;

public class Tracker 
{
	
	TorrentInfo ti;
	String peerId = "wearesogoingtofail!!";
	
	private byte[] response;
	
	private ArrayList<HashMap<ByteBuffer, Object>> peerMaps;
	
	private HashMap<ByteBuffer, Object> trackerResponseMap;
	
	
	private final String TRACKER_PORT = "6881";
	
	private final ByteBuffer KEY_PEERS = ByteBuffer.wrap(new byte[] {
			'p', 'e', 'e', 'r', 's' });
	
	private final ByteBuffer KEY_PEER_ID = ByteBuffer.wrap(new byte[] {
			'p', 'e', 'e', 'r', ' ', 'i', 'd' });
	
	private final ByteBuffer KEY_IP = ByteBuffer.wrap(new byte[] {
			'i', 'p' });
	
	private final ByteBuffer KEY_PORT = ByteBuffer.wrap(new byte[] {
			'p', 'o', 'r', 't' });

	public Tracker(TorrentInfo ti)
	{
		this.ti = ti;
		// generate random peer_id
		// this.peerId = Util.randomID();
		
		initTracker();
	}
	
	
	
	// returns the decoded map response from Tracker
	public HashMap<ByteBuffer, Object> getResponseMap()
	{
		return this.trackerResponseMap;
	}
	
	
	
	// returns the decoded list of peer hash maps from Tracker 
	public ArrayList<HashMap<ByteBuffer, Object>> getPeerMaps()
	{
		return this.peerMaps;
	}
	
	
	@SuppressWarnings("unchecked")
	private void initResponseMap()
	{
		HashMap<ByteBuffer, Object> trackerResponseMap = null;
		try 
		{
			trackerResponseMap = 
					(HashMap<ByteBuffer, Object>) Bencoder2.decode(this.response);
			
		} 
		catch (BencodingException e) 
		{
			e.printStackTrace();
		}
		this.trackerResponseMap = trackerResponseMap;
	}
	
	@SuppressWarnings("unchecked")
	private ArrayList<HashMap<ByteBuffer, Object>> initPeerMaps()
	{
		
		ArrayList<HashMap<ByteBuffer, Object>> peerMaps = null;

		peerMaps = (ArrayList<HashMap<ByteBuffer, Object>>)
				trackerResponseMap.get(KEY_PEERS);

		this.peerMaps = peerMaps;
		return peerMaps;
	}
	
	private void getResponse()
	{
		
		int length = ti.file_length;
		String encodedHash="";
		try 
		{
			encodedHash = Util.byteArrayToURLString(ti.info_hash.array());
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		String announceURL = ti.announce_url.toExternalForm();
		String trackerURL = announceURL + "?info_hash=" + encodedHash
				+ "&peer_id=" + this.peerId
				+ "&left=" + length
				+ "&port=" + TRACKER_PORT
				+ "&downloaded=" + "0";
		
		byte[] trackerResponse = Util.sendGetRequest(trackerURL).getBytes();
		this.response = trackerResponse;
	}
	
	private void initTracker()
	{
		getResponse();
		initResponseMap();
		initPeerMaps();
	}
	
}
	
