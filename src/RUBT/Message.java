package RUBT;

public class Message 
{
	
	// 4 bytes
	byte[] lengthPrefix;
	
	byte id;
	
	// message dependent
	byte[] payload;
	
	
	public static final int CHOKE_ID = 0;
	
	public static final int UNCHOKE_ID = 1;
	
	public static final int INTERESTED_ID = 2;
	
	public static final int UNINTERESTED_ID = 3;
	
	public static final int HAVE_ID = 4;
	
	public static final int REQUEST_ID = 6;
	
	
	
	byte[] keepAlive;
	byte[] choke;
	byte[] unchoke;
	byte[] interested;
	byte[] uninterested;
	byte[] have;
	byte[] bitfield;
	byte[] request;
	byte[] piece;
	byte[] cancel;

	public void createMessage()
	{
				
	}
}