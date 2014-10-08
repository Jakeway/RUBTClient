package RUBT;

public class PieceMessage extends Message {

	private int piece;
	private int byteOffset;
	private byte[] block;

	public PieceMessage(int piece, int byteOffset, byte[] block)
	{
		super(block.length + 9, Message.PIECE_ID);
		this.piece = piece;
		this.byteOffset = byteOffset;
		this.block = block;
	}

}
