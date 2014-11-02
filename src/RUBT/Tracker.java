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
	String localId;
	
	// response from the Tracker after initial get request
	private byte[] response;
	
	// decoded list of peer dictionaries from Tracker response
	private ArrayList<HashMap<ByteBuffer, Object>> peerMaps;
	
	// decoded dictionary from Tracker response
	private HashMap<ByteBuffer, Object> trackerResponseMap;
	
	// list of Peers retrieved from Tracker response
	private ArrayList<Peer> peerList;
	
	// port to contact tracker with
	public static final String TRACKER_PORT = "6881";
	public static final int TRACKER_PORT_INT = 6881;
	
	private final ByteBuffer KEY_PEERS = ByteBuffer.wrap(new byte[] {
			'p', 'e', 'e', 'r', 's' });
	
	private final ByteBuffer KEY_PEER_ID = ByteBuffer.wrap(new byte[] {
			'p', 'e', 'e', 'r', ' ', 'i', 'd' });
	
	private final ByteBuffer KEY_IP = ByteBuffer.wrap(new byte[] {
			'i', 'p' });
	
	private final ByteBuffer KEY_PORT = ByteBuffer.wrap(new byte[] {
			'p', 'o', 'r', 't' });
	
	public Tracker(TorrentInfo ti, String localId)
	{
		this.ti = ti;
		this.localId = localId;
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
	
	// prints the decoded response map from the Tracker
	public void printResponseMap()
	{
		ToolKit.printMap(trackerResponseMap, 0);
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
	
	public ArrayList<Peer> getPeerList()
	{
		return peerList;
	}
	
	private void initPeerList()
	{
		ArrayList<Peer> peers = new ArrayList<Peer>();
		for (HashMap<ByteBuffer, Object> peerMap : peerMaps)
		{
			String ip = "";
			String peerId = "";
			ByteBuffer ipBuffer = (ByteBuffer) peerMap.get(KEY_IP);
			ByteBuffer peerIdBuffer = (ByteBuffer) peerMap.get(KEY_PEER_ID);
			try 
			{
				ip = new String(ipBuffer.array(), "UTF-8");
				peerId = new String(peerIdBuffer.array(), "UTF-8");
			} 
			catch (UnsupportedEncodingException e)
			{
				e.printStackTrace();
			}
			int port = (Integer) peerMap.get(KEY_PORT);
			Peer p = new Peer(ip, port, peerId, localId, ti.info_hash.array(), ti.piece_hashes, ti.piece_length, ti.file_length);
			peers.add(p);
		}
		this.peerList = peers;
	}
	
	public void getResponse()
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
				+ "&peer_id=" + this.localId
				+ "&left=" + length
				+ "&port=" + TRACKER_PORT
				+ "&downloaded=" + "0"
				+ "&event=" + "started";
		
		byte[] trackerResponse = Util.sendGetRequest(trackerURL).getBytes();
		this.response = trackerResponse;
	}
	
	
	private void initTracker()
	{
		getResponse();
		initResponseMap();
		initPeerMaps();
		initPeerList();
	}
	
	
	
}
	

