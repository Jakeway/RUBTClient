package RUBT;

public class Job 
{
	private Message message;
	private Peer peer;

	public Job(Message m, Peer p)
	{
		this.message = m;
		this.peer = p;
	}
	public Message getMessage()
	{
		return message;
	}
	
	public Peer getPeer()
	{
		return peer;
	}
	
	
	
}
