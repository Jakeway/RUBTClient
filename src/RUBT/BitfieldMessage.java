package RUBT;

public class BitfieldMessage extends Message 
{


	private byte[] bitfield;
	
	public static final byte BITFIELD_ID = 5;
	
	

	public BitfieldMessage(int length, byte[] bitfield)
	{
		super(length, BITFIELD_ID);
		this.bitfield = bitfield;
	}
	
	private boolean isBitSet(int byteIndex, int bitIndex)
	{
		int bitMask = 1 << bitIndex;
		return ( bitfield[byteIndex] & bitMask) > 0;
	}
	
	public void iterateBitfield()
	{
		
		for (int j = 0; j < bitfield.length; j++)
		{
			
		for (int i = 0; i < 8; i++)
		{
			if (isBitSet(j, i))
			{
				System.out.println("Bit index " + i + " is set in byte " + j);
			}
		}
		}
	}
	
	public int getBitfieldLength()
	{
		return bitfield.length;
	}
}
