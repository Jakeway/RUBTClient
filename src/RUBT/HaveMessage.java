package RUBT;

public class HaveMessage extends Message {

	public int piece;

	public HaveMessage(int piece)
	{
		super(5, Message.HAVE_ID);
		this.piece = piece;
	}

}
