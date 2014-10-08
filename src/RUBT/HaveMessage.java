package RUBT;

import java.io.DataOutputStream;
import java.io.IOException;

public class HaveMessage extends Message {

	private int piece;
	
	public static final byte HAVE_ID = 4;
	
	public static final int HAVE_LENGTH = 5;

	public HaveMessage(int piece)
	{
		super(HAVE_LENGTH, HAVE_ID);
		this.piece = piece;
	}
	
	public static void send(Message m, DataOutputStream out)
	{
		Message.send(m, out);
		HaveMessage hm = (HaveMessage) m;
		try
		{
			out.writeInt(hm.piece);
			out.flush();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

}
