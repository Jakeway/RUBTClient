package RUBT;

public class RequestMessage extends Message {

	private int piece;
	private int byteOffset;
	private int byteLength;
	
	public static final byte REQUEST_ID = 6;
	
	public static final int REQUEST_LENGTH = 13;

	public RequestMessage(int piece, int byteOffset, int byteLength)
	{
		super(REQUEST_LENGTH, REQUEST_ID);
		this.piece = piece;
		this.byteOffset = byteOffset;
		this.byteLength = byteLength;
	}

}
