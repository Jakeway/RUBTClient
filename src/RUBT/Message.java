package RUBT;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Message 
{
	
	private int length;
	private byte id;


	private static final byte CHOKE_ID = 0;
	
	private static final byte UNCHOKE_ID = 1;
	
	private static final byte INTERESTED_ID = 2;
	
	private static final byte UNINTERESTED_ID = 3;
	
	private static final byte HAVE_ID = 4;
	
	private static final byte REQUEST_ID = 6;
	
	private static final byte PIECE_ID = 7;
	
	// not an actual standard, doesn't get used.
	// KEEP_ALIVE Messages don't have an ID.
	private static final byte KEEP_ALIVE_ID = 8;
	
	private static final long KEEP_ALIVE_TIMER = 120000;
	
	// The following are Messages used to communicate with the Peer
	
	public static final Message INTERESTED_MSG = 
			new Message(1, INTERESTED_ID); 
	
	public static final Message KEEP_ALIVE_MSG =
			new Message(0, KEEP_ALIVE_ID);
	
	public static final Message CHOKE_MSG = 
			new Message(1, CHOKE_ID);

	public static final Message UNCHOKE_MSG = 
			new Message(1, UNCHOKE_ID);

	public static final Message UNINTERESTED_MSG = 
			new Message(1, UNINTERESTED_ID);
	

	private Message(int lenPrefix, byte ID)
	{
		this.length = lenPrefix;
		this.id = ID;
	}
	
	public static void send(Message m, DataOutputStream out)
	{
		try 
		{
			// write the Message length prefix
			out.writeInt(m.length);
			// don't need to write the Message ID of the keep alive message
			if (m.id != KEEP_ALIVE_ID)
			{
				out.writeByte(m.id);
			}
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	public String toString()
	{
		
		switch(this.id)
		{
			case CHOKE_ID:
				return "CHOKE_MSG";
			case UNCHOKE_ID:
				return "UNCHOKE_MSG";
			case INTERESTED_ID:
				return "INTERESTED_MSG";
			case UNINTERESTED_ID:
				return "UNINTERESTED_MSG";
			default:
				return "Unknown Message";
		}
	}
	
	public static Message receive(DataInputStream in)
	{
		try
		{
			int length = in.readInt();
			if(length == 0)
				return KEEP_ALIVE_MSG;
			else if(length == 1)
			{
				byte ID = in.readByte();
				switch(ID)
				{
					case CHOKE_ID:
						return CHOKE_MSG;
					case UNCHOKE_ID:
						return UNCHOKE_MSG;
					case INTERESTED_ID:
						return INTERESTED_MSG;
					case UNINTERESTED_ID:
						return UNINTERESTED_MSG;
				}
			}
			else if(length == 5)
			{
				return null;
			}
			else if(length == 13)
			{
				return null;
			}
			else if(length == 9)
			{
				return null;
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	
}