package Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Message 
{
	
	private int length;
	private byte id;

	public static final byte CHOKE_ID = 0;
	
	public static final byte UNCHOKE_ID = 1;
	
	public static final byte INTERESTED_ID = 2;
	
	public static final byte UNINTERESTED_ID = 3;
	
	public static final byte CANCEL_ID = 8;
	
	// not an actual standard, doesn't get used.
	// KEEP_ALIVE Messages don't have an ID.
	public static final byte KEEP_ALIVE_ID = -1;
	
	public static final long KEEP_ALIVE_TIMER = 120000;
	
	public static long LAST_MESSAGE_TIME = 0;
	
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
	

	public Message(int lenPrefix, byte ID)
	{
		this.length = lenPrefix;
		this.id = ID;
	}
	
	public void send(DataOutputStream out)
	{
		try 
		{
			// write the Message length prefix
			out.writeInt(this.length);
			// don't need to write the Message ID of the keep alive message
			if (id != KEEP_ALIVE_ID)
			{
				out.writeByte(id);
			}
			this.sendPayload(out);
			out.flush();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}

	}
	
	protected void sendPayload(DataOutputStream out) 
	{
		// general Message class has no payload
		// Have, Bitfield, Request, Piece do.
	}

	public String toString()
	{
		if (this.length == 0)
		{
			return "KEEPALIVE_MSG";
		}
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
			case HaveMessage.HAVE_ID:
				return "HAVE_MSG";
			case RequestMessage.REQUEST_ID:
				return "REQUEST_MSG";
			case BitfieldMessage.BITFIELD_ID:
				return "BITFIELD_MSG";
			case PieceMessage.PIECE_ID:
				return "PIECE_MSG";
			default:
				return "Unknown Message";
		}
	}
	
	public static Message receive(DataInputStream in) throws IOException
	{
		
			int length = in.readInt();
			//System.out.println("length of message: " + length);
			//System.out.println("id of message: " + id);
			if (length < 0)
			{
				System.err.println("Received a corrupt message: " + length);
				System.exit(1);
			}
			
			if (length == 0)
			{
				return KEEP_ALIVE_MSG;
			}
			
			byte id = in.readByte();
			
			if(length == 1)
			{
				switch(id)
				{
					case CHOKE_ID:
						return CHOKE_MSG;
					case UNCHOKE_ID:
						return UNCHOKE_MSG;
					case INTERESTED_ID:
						return INTERESTED_MSG;
					case UNINTERESTED_ID:
						return UNINTERESTED_MSG;
					default:
						System.err.println("Received unrecognized message with length 1. ID: " + id);
				}
			}
			else if(length == 5 && id == HaveMessage.HAVE_ID)
			{
				int piece = in.readInt();
				return new HaveMessage(piece);
			}
			else if(length == 13 && id == RequestMessage.REQUEST_ID)
			{
				int piece = in.readInt();
				int byteOffset = in.readInt();
				int byteLength = in.readInt();
				return new RequestMessage(piece, byteOffset, byteLength);
			}
			else if(length >= 9 && id == PieceMessage.PIECE_ID)
			{
				int piece = in.readInt();
				int byteOffset = in.readInt();
				byte[] block = new byte[length - 9];
				in.readFully(block);
				return new PieceMessage(piece, byteOffset, block);
			}
			else if(length == 13 && id == CANCEL_ID)
			{
				System.out.println("Cancel_Msg");
				return null;
			}
			else if(id == BitfieldMessage.BITFIELD_ID)
			{
				byte[] bitfield = new byte[length - 1];
				in.readFully(bitfield);
				return new BitfieldMessage(length, bitfield);
			}
		return null;
	}
	
	public byte getID()
	{
		return this.id;
	}
	
	
}