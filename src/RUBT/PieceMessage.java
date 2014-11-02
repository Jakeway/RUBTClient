package RUBT;


public class PieceMessage extends Message {

	private int pieceIndex;
	private int byteOffset;
	byte[] block;
	
	public static final byte PIECE_ID = 7;

	public PieceMessage(int pieceIndex, int byteOffset, byte[] block)
	{
		super(block.length + 9, PIECE_ID);
		this.pieceIndex = pieceIndex;
		this.byteOffset = byteOffset;
		this.block = block;
	}
	
	public int getPieceIndex()
	{
		return pieceIndex;
	}
	
	public byte[] getBlock()
	{
		return block;
	}
	
	
	
}
