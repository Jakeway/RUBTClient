package RUBT;

public class HaveMessage extends Message {

	public int piece;
	
	public static final byte HAVE_ID = 4;
	
	public static final int HAVE_LENGTH = 5;

	public HaveMessage(int piece)
	{
		super(HAVE_LENGTH, HAVE_ID);
		this.piece = piece;
	}

}
