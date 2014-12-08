package RUBT;

// Rarity describes how many peers have this particular piece
public class Rarity implements Comparable<Rarity>
{

	private int peersWithPiece;
	private int pieceIndex;
	
	public Rarity(int pieceIndex)
	{
		this.peersWithPiece = 0;
		this.pieceIndex = pieceIndex;
	}
	
	public void decreaseRarity()
	{
		peersWithPiece++;
	}
	
	public int getPieceIndex()
	{
		return pieceIndex;
	}
	
	public void setRarity(int rarity)
	{
		this.peersWithPiece = rarity;
	}
	
	public int getRarity()
	{
		return this.peersWithPiece;
	}
	@Override
	public int compareTo(Rarity r)
	{
		return r.peersWithPiece - this.peersWithPiece;	
	}

}
