package Message;

import java.io.DataOutputStream;
import java.io.IOException;

public class HaveMessage extends Message {

	private int pieceIndex;
	
	public static final byte HAVE_ID = 4;
	
	public static final int HAVE_LENGTH = 5;

	public HaveMessage(int pieceIndex)
	{
		super(HAVE_LENGTH, HAVE_ID);
		this.pieceIndex = pieceIndex;
	}
	
	public int getPieceIndex()
	{
		return this.pieceIndex;
	}
	
	@Override
	protected void sendPayload(DataOutputStream out)
	{
		try
		{
			out.writeInt(pieceIndex);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

}
