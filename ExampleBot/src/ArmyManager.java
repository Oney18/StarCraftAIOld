//package StarcraftAI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import bwapi.Game;
import bwapi.Player;
import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;
import bwta.BWTA;
import bwta.BaseLocation;

/**
 * ArmyManager Class
 * 	This class is responsible for handling all direct orders to
 * 	military units when they are not in combat situations.
 * 
 * @author Casey Sigelmann
 * @author Alex Bowns
 *
 */
public class ArmyManager{
	private Player self;
	private Game game;
	
	private ArrayList<TilePosition> scoutQueue = new ArrayList<TilePosition>();
	private HashMap<SquadType, Squad> squads;
	private TilePosition queuedTile = null;
	private List<BaseLocation> baseLocations = new ArrayList<BaseLocation>(); 
	
	private ArrayList<Position> allPositions = new ArrayList<Position>();
	/**
	 * ctor
	 * Set up the game, player and squads for the Army Manager. 
	 * These will be given to the Army manager by the Military Manager
	 */
	public ArmyManager(HashMap<SquadType, Squad> squads, Player self, Game game)
	{
		this.squads = squads;
		this.self = self;
		this.game = game;
		
		int x = 0, y = 0; 
		Position searchPos = new Position(x, y);
	
		while(searchPos.isValid())
		{
			while(searchPos.isValid())
			{
					allPositions.add(searchPos);
				x += 32;
				searchPos = new Position(x, y);
			}
			x = 0; 
			y += 32; 
			searchPos = new Position(x, y);
		}
	}

	/**
	 * setSquads
	 * given a list of squads, set those squads as the squads 
	 * for this class to act on. 
	 * 
	 * @param squads - a list of squads 
	 */
	public void setSquads(HashMap<SquadType, Squad> squads)
	{
		this.squads = squads;
	}

	/**
	 * defend()
	 * Positions the defend squad in a defensive position around our base.
	 */
	public void defend()
	{

	}

	/**
	 * engage()
	 * Makes the offensive squad attack the given location.
	 * 
	 * @param position - the location to attack
	 */
	public void engage(Position position)
	{
		squads.get(SquadType.Offense).attackMove(position);
	}

	/**
	 * scoutBases()
	 * Moves units in the scout squad to unexplored locations.
	 */
	public void scoutBases()
	{	
		// get base Locations
		List<BaseLocation> baseLocations = BWTA.getStartLocations();
		ArrayList<Position> basePoss = new ArrayList<Position>();
		
		for(BaseLocation base : baseLocations)
		{			
			// if base location is not start location and a starting location add it
			if (!base.getPosition().equals(BWTA.getStartLocation(self).getPosition()))
			{
				basePoss.add(base.getPosition());
			}
    	}
		
		//Add home as the last place to go
		basePoss.add(BWTA.getStartLocation(self).getPosition());
		
		squads.get(SquadType.Scout).moveQueue(basePoss);
	}
	
	public void scoutMap()
	{
		for(Unit unit : squads.get(SquadType.Offense).getUnits())
		{
			if(unit.isIdle())
			{
				unit.move(allPositions.get((int)(Math.random()*allPositions.size())));
			}
		}
	}
	
	
}
