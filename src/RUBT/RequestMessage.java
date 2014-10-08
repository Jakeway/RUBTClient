package RUBT;

public class RequestMessage extends Message {

	private int piece;
	private int byteOffset;
	private int byteLength;

	public RequestMessage(int piece, int byteOffset, int byteLength)
	{
		super(13, Message.REQUEST_ID);
		this.piece = piece;
		this.byteOffset = byteOffset;
		this.byteLength = byteLength;
	}

}
