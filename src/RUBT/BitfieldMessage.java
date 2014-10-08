package RUBT;

public class BitfieldMessage extends Message 
{


	private byte[] bitfield;
	


	public BitfieldMessage(byte[] bitfield)
	{
		super(bitfield.length + 1, Message.BITFIELD_ID);
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
}
