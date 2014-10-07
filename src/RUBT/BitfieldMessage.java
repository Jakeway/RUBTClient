package RUBT;

public class BitfieldMessage extends Message {

	private byte[] bitfield;

	public BitfieldMessage(byte[] bitfield)
	{
		super(bitfield.length + 1, Message.BITFIELD_ID);
		this.bitfield = bitfield;
	}
}
