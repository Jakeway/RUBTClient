package RUBT;

import java.io.DataOutputStream;
import java.io.IOException;

public class RequestMessage extends Message {

	private int pieceIndex;
	private int byteOffset;
	private int blockLength;
	
	public static final byte REQUEST_ID = 6;
	
	public static final int REQUEST_LENGTH = 13;

	public RequestMessage(int pieceIndex, int byteOffset, int blockLength)
	{
		super(REQUEST_LENGTH, REQUEST_ID);
		this.pieceIndex = pieceIndex;
		this.byteOffset = byteOffset;
		this.blockLength = blockLength;
		
	}
	
	public static void send(Message m, DataOutputStream out)
	{
		Message.send(m, out);
		RequestMessage rm = (RequestMessage) m;
		try
		{
			out.writeInt(rm.pieceIndex);
			out.writeInt(rm.byteOffset);
			out.writeInt(rm.blockLength);
			out.flush();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}


	

}
