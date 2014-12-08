package TimedTasks;

import java.util.TimerTask;

import Message.Message;
import RUBT.Peer;
import RUBT.PeerManager;

public class OptimisticChokingTimerTask extends TimerTask
{
	private PeerManager pMgr;
	
	public OptimisticChokingTimerTask(PeerManager pMgr)
	{
		this.pMgr = pMgr;
	}
	
	@Override
	public void run() 
	{
		System.out.println("running task");
		// only attempt if a) there are 3 unchoked connections and b) there is at least one choked connection
		
		// randomly choose choked peer
		// out of active peers, choose one with least amount of pieces downloaded
		// trade places with these two chosen peers
		// set all the peers involved peers piecesDownloaded field to 0
		// send relevant choke / unchoke messages
		
		
		if (pMgr.getChokedPeers().size() > 0 && pMgr.getActivePeers().size() < 3)
		{
			Peer chokedPeer = pMgr.getRandomlyChokedElement(pMgr.getChokedPeers());
			
			int min = 1000;
			Peer minPeer = null;
			for (Peer p : pMgr.getActivePeers())
			{
				int piecesDownloaded = p.getPiecesDownloaded();
				p.setPiecesDownloaded(0);
				if (piecesDownloaded < min)
				{
					minPeer = p;
					min = piecesDownloaded;
				}
			}
			
			// handle the min peer
			pMgr.getActivePeers().remove(minPeer);
			pMgr.getChokedPeers().add(minPeer);
			pMgr.sendMessage(Message.CHOKE_MSG, minPeer);
			minPeer.setPeerChoked(true);
			
			// handles previously choked peer
			pMgr.getChokedPeers().remove(chokedPeer);
			pMgr.getActivePeers().add(chokedPeer);
			pMgr.sendMessage(Message.UNCHOKE_MSG, chokedPeer);
			chokedPeer.setPeerChoked(false);	
		}
		
		
		// seeds are peers that we download from
		if (pMgr.getChokedSeeds().size() > 0 && pMgr.getActiveSeeds().size() < 3)
		{
			Peer chokedSeed = pMgr.getRandomlyChokedElement(pMgr.getChokedSeeds());
			
			int min = 1000;
			Peer minSeed = null;
			for (Peer p : pMgr.getActiveSeeds())
			{
				int piecesUploaded = p.getPiecesUploaded();
				p.setPiecesUploaded(0);
				if (piecesUploaded < min)
				{
					minSeed = p;
					min = piecesUploaded;
				}
			}
			
			// handle the min seed
			pMgr.getActiveSeeds().remove(minSeed);
			pMgr.getChokedSeeds().add(minSeed);
			// don't need to send any messages, just don't send anymore request messages
			minSeed.setClientChoked(true);
			
			// handles previously choked seed
			pMgr.getChokedSeeds().remove(chokedSeed);
			pMgr.getActivePeers().add(chokedSeed);
			pMgr.generateRequestMessage(chokedSeed);
			chokedSeed.setClientChoked(false);
			
		}

	}

	
	
	
	
	
	
}
