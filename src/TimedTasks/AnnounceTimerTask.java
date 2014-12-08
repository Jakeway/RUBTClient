package TimedTasks;

import java.util.TimerTask;

import RUBT.PeerManager;

public class AnnounceTimerTask extends TimerTask
{
	
	private PeerManager pMgr;
	
	
	public AnnounceTimerTask(PeerManager pMgr)
	{
		this.pMgr = pMgr;
	}
	
	
	
	@Override
	public void run() 
	{
	
		System.out.println("announcing to peer list");
		pMgr.getTracker().announce(
				pMgr.getAmountLeft(),
				Integer.toString(pMgr.getAmountUploaded()),
				Integer.toString(pMgr.getAmountDownloaded()),
				"");
		
		pMgr.getAnnounceTimer().cancel();
		pMgr.createNewAnnounceTimer();
		pMgr.getAnnounceTimer().schedule(this, pMgr.getTracker().getInterval() * 1000);
	}

}
