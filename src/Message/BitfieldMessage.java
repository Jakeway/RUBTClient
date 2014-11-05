package Message;

import java.io.DataOutputStream;
import java.io.IOException;

import RUBT.Util;

public class BitfieldMessage extends Message 
{


	private byte[] bitfield;
	
	public static final byte BITFIELD_ID = 5;
	
	

	public BitfieldMessage(int length, byte[] bitfield)
	{
		super(1 + length, BITFIELD_ID);
		this.bitfield = bitfield;
	}
	
	
	@Override
	protected void sendPayload(DataOutputStream out) throws IOException
	{
		out.write(bitfield);
	}
	
	
	public byte[] getBitfield()
	{
		return this.bitfield;
	}
	
	public void iterateBitfield()
	{
		
		for (int j = 0; j < bitfield.length; j++)
		{
			
			for (int i = 0; i < 8; i++)
			{
				if (Util.isBitSet(bitfield[j], i))
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
