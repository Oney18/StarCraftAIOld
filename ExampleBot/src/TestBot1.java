import java.util.ArrayList;
import java.util.HashSet;

import bwapi.*;
import bwta.BWTA;
import bwta.BaseLocation;

public class TestBot1 extends DefaultBWListener {

    private Mirror mirror = new Mirror();

    private Game game;

    private Player self;

    private boolean overlordMorphing;
    private Unit spawnWorker;
    private Unit scout;
    
    private MilitaryManager militaryManager;
    private boolean isScouting = false;
    private HashSet<Position> enemyBuildingLocation;
    
    public void run() {
        mirror.getModule().setEventListener(this);
        mirror.startGame();
    }

    @Override
    public void onUnitCreate(Unit unit) {
        System.out.println("New unit discovered " + unit.getType());
        
        //he shall be our scout
        if(!militaryManager.hasScout() && unit.getType() == UnitType.Zerg_Drone ){
        	militaryManager.addUnit(unit);
        	scout = unit;
        }
        else if(unit.getType() == UnitType.Zerg_Overlord || unit.getType() == UnitType.Zerg_Zergling)
        {
        	// Military Unit
        	militaryManager.addUnit(unit);
        }
    }
    
    @Override
    public void onUnitMorph(Unit unit){
    	if(unit.getType() == UnitType.Zerg_Zergling)
    		militaryManager.addUnit(unit);
    	
    	if(unit.getType() == UnitType.Zerg_Spawning_Pool || unit.getType() == UnitType.Zerg_Hatchery)
    		spawnWorker = null;
    	
    	System.out.println(unit.getType().toString());
    	
    }

    @Override
    public void onStart() {
        game = mirror.getGame();
        self = game.self();

        //slow is bad
        game.setLocalSpeed(5);
        
        overlordMorphing = false;
        spawnWorker = null;
        scout = null;
        
        //Use BWTA to analyze map
        //This may take a few minutes if the map is processed first time!
        System.out.println("Analyzing map...");
        BWTA.readMap();
        BWTA.analyze();
        System.out.println("Map data ready");
        
        militaryManager = new MilitaryManager(game, self);
        enemyBuildingLocation = new HashSet<Position>();
        
        int i = 0;
        for(BaseLocation baseLocation : BWTA.getBaseLocations()){
        	System.out.println("Base location #" + (++i) + ". Printing location's region polygon:");
        	for(Position position : baseLocation.getRegion().getPolygon().getPoints()){
        		System.out.print(position + ", ");
        	}
        	System.out.println();
        }

    }
    

    @Override
    public void onFrame() {
        //game.setTextSize(10);
        game.drawTextScreen(10, 10, "Playing as " + self.getName() + " - " + self.getRace());

        StringBuilder units = new StringBuilder("My units:\n");

      //if we can't find the enemy continue scouting
    	if(enemyBuildingLocation.isEmpty())
    	{
    		isScouting = false;
    	}
    	
    	//make sure we are scouting  	
    	if(!isScouting)
    	{
    		militaryManager.command(Command.Scout, 1.0, null);
    		isScouting = true;
    	}
    	
    	updateEnemyBuildingLocations();
    	militaryManager.update();
    	
    	if(!enemyBuildingLocation.isEmpty() && militaryManager.getArmyCount() > 6){
    		for(Position pos : enemyBuildingLocation){
    			militaryManager.command(Command.Attack, 1.0, pos);
    		}
    	}
    	
//    	for(Unit unit : game.enemy().getUnits())
//    	{
//    		if(!unit.getType().isBuilding()){
//    			militaryManager.command(Command.Attack, 1.0, unit.getPosition());
//    		}
//    	}
        
        
        //iterate through my units
        for (Unit myUnit : self.getUnits()) {
            units.append(myUnit.getType()).append(" ").append(myUnit.getTilePosition()).append("\n");

            
            //build a spawning pool, make the zerglings!
            if(myUnit.getType().isWorker() && self.allUnitCount(UnitType.Zerg_Spawning_Pool) < 1 &&
            		self.minerals() >= 200 && spawnWorker == null && myUnit.isGatheringMinerals())
            {
            	TilePosition buildSpot = getBuildTile(myUnit, UnitType.Zerg_Spawning_Pool, self.getStartLocation());
            	
            	if(buildSpot != null){
            		myUnit.build(UnitType.Zerg_Spawning_Pool, buildSpot);
            		spawnWorker = myUnit;
            	}
            }
            
            //build new hatchery
            if(myUnit.getType().isWorker() && self.allUnitCount(UnitType.Zerg_Hatchery) < 2 &&
            		self.minerals() >= 300 && spawnWorker == null && myUnit.isGatheringMinerals())
            {
            	
            	TilePosition buildSpot = getBuildTile(myUnit, UnitType.Zerg_Hatchery, self.getStartLocation());
            	
            	if(buildSpot != null){
            		myUnit.build(UnitType.Zerg_Hatchery, buildSpot);
            		spawnWorker = myUnit;
            	}
            }
            
            //if need more supply, build overlord
            if (myUnit.getType() == UnitType.Zerg_Larva && self.minerals() >= 100 && 
            		(self.supplyTotal() - self.supplyUsed()) <= 1 && overlordMorphing == false ) {
                myUnit.morph(UnitType.Zerg_Overlord);
                overlordMorphing = true;
            }
            
            //if there's enough minerals, train a drone, if theres less than 5
            if (myUnit.getType() == UnitType.Zerg_Larva && self.minerals() >= 50 && 
            		(self.supplyTotal() - self.supplyUsed()) > 0 &&
            		self.allUnitCount(UnitType.Zerg_Drone) < 6){
                myUnit.morph(UnitType.Zerg_Drone);
            }
            
          //if there's enough minerals, train a zergling
            if (myUnit.getType() == UnitType.Zerg_Larva && self.minerals() >= 50 && 
            		(self.supplyTotal() - self.supplyUsed()) > 0 &&
            		self.allUnitCount(UnitType.Zerg_Spawning_Pool) > 0){
                myUnit.morph(UnitType.Zerg_Zergling);
            }

            //if it's a worker and it's idle, send it to the closest mineral patch
            if (myUnit.getType().isWorker() && myUnit.isIdle()) {
                Unit closestMineral = null;

                //find the closest mineral
                for (Unit neutralUnit : game.neutral().getUnits()) {
                    if (neutralUnit.getType().isMineralField()) {
                        if (closestMineral == null || myUnit.getDistance(neutralUnit) < myUnit.getDistance(closestMineral)) {
                            closestMineral = neutralUnit;
                        }
                    }
                }

                //if a mineral patch was found, send the worker to gather it
                if (closestMineral != null) {
                    myUnit.gather(closestMineral, false);
                }
            }
        }
        
      
    	
    	

        //draw my units on screen
        game.drawTextScreen(10, 25, units.toString());
    }

     
 // Returns a suitable TilePosition to build a given building type near 
 // specified TilePosition aroundTile, or null if not found. (builder parameter is our worker)
 public TilePosition getBuildTile(Unit builder, UnitType buildingType, TilePosition aroundTile) {
 	TilePosition ret = null;
 	int maxDist = 3;
 	int stopDist = 40;
 	
 	while ((maxDist < stopDist) && (ret == null)) {
 		for (int i=aroundTile.getX()-maxDist; i<=aroundTile.getX()+maxDist; i++) {
 			for (int j=aroundTile.getY()-maxDist; j<=aroundTile.getY()+maxDist; j++) {
 				if (game.canBuildHere(new TilePosition(i,j), buildingType, builder, false)) {
 					// units that are blocking the tile
 					boolean unitsInWay = false;
 					for (Unit u : game.getAllUnits()) {
 						if (u.getID() == builder.getID()) continue;
 						if ((Math.abs(u.getTilePosition().getX()-i) < 4) && (Math.abs(u.getTilePosition().getY()-j) < 4)) unitsInWay = true;
 					}
 					if (!unitsInWay) {
 						return new TilePosition(i, j);
 					}
 					// creep for Zerg
 					if (buildingType.requiresCreep()) {
 						boolean creepMissing = false;
 						for (int k=i; k<=i+buildingType.tileWidth(); k++) {
 							for (int l=j; l<=j+buildingType.tileHeight(); l++) {
 								if (!game.hasCreep(k, l)) creepMissing = true;
 								break;
 							}
 						}
 						if (creepMissing) continue; 
 					}
 				}
 			}
 		}
 		maxDist += 2;
 	}
 	
 	if (ret == null) game.printf("Unable to find suitable build position for "+buildingType.toString());
 	return ret;
 }
 
 
    public static void main(String[] args) {
        new TestBot1().run();
    }
    
    
    
    
    private void updateEnemyBuildingLocations()
    {
    	//Add any buildings we see to list.
    	for(Unit u: game.enemy().getUnits())
    	{
    		//if this unit is a building add it to the hash
    		if(u.getType().isBuilding())
    		{
    			//check if we have it's position in memory and add it if we don't
    			if(!enemyBuildingLocation.contains(u.getPosition()))
    			{
    				enemyBuildingLocation.add(u.getPosition());
    			}
    		}
    	}
    	
    	ArrayList<Position> toRemove = new ArrayList<Position>();
    	
    	//loop over the visible enemy units that we remember
    	if(enemyBuildingLocation == null) System.out.println("Uh Oh!");
    	for(Position p : enemyBuildingLocation)
    	{
    		TilePosition tileCorrespondingToP = new TilePosition(p.getX()/32, p.getY()/32);
    		
    		//if visible
    		if(game.isVisible(tileCorrespondingToP))
    		{
    			//loop over the visible enemy buildings and find out if at least
    			// one of them is still at the remembered position
    			boolean buildingStillThere = false;
    			for(Unit u: game.enemy().getUnits())
    			{
    				if(u.getType().isBuilding() && u.getPosition().equals(p) && u.exists())
    				{
    					buildingStillThere = true;
    					break;
    				}
    			}
    			
    			if(!buildingStillThere)
    			{
    				toRemove.add(p);
    				break;//TODO check if this is necessary
    			}
    		}
    	}
    }
}