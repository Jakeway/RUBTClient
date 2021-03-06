package Message;

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
	
	public int getPieceIndex()
	{
		return pieceIndex;
	}
	
	public int getBlockLength()
	{
		return blockLength;
	}
	
	public int getByteOffset()
	{
		return byteOffset;
	}
	
	
	
	@Override
	protected void sendPayload(DataOutputStream out) throws IOException 
	{
		out.writeInt(pieceIndex);
		out.writeInt(byteOffset);
		out.writeInt(blockLength);
	}


	

}
