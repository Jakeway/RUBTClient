package RUBT;

import java.util.ArrayList;
import java.util.Collections;

public class testRarity 


{
	public static void main(String[] args)
	{
		
		ArrayList<Rarity> rarePieces = new ArrayList<Rarity>();
		for (int i = 0; i < 10; i++)
		{
			Rarity r = new Rarity(Util.getRandomInt(i + 1));
			r.setRarity(Util.getRandomInt(i + 1));
			rarePieces.add(r);
		}
		
		
		
		Collections.sort(rarePieces, Collections.reverseOrder());
		for (Rarity r : rarePieces)
		{
			System.out.println(r.getRarity());
		}
		
		
		
	}
	
	
}
