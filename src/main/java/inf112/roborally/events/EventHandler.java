package inf112.roborally.events;

import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import inf112.roborally.entities.Directions;
import inf112.roborally.entities.Player;
import inf112.roborally.ui.Board;

import static inf112.roborally.ui.Board.TILE_SIZE;

/**
 * Provides static methods for dealing with tile-events.
 */
public class EventHandler {

    private static boolean fromConveyor;


    /**
     * Handles an event on a current tile with a given map & player.
     *
     * @param board  The current Board which holds all tiles
     * @param player The player who stands on the tile
     *               <p>
     *               Temporary implementation for conveyors with two directions
     *               <p>
     *               Known bug: Express_Conveyor_SouthWest case calls on Express_Conveyor_South
     *               case instead of its actual case.
     */
    public static void handleEvent(Board board, Player player) {
        String movers = getTileType(board, "OMovers", player.getPos());

        switch (movers) {

            case "Normal_Conveyor_North":
                player.move(board, Directions.NORTH, 1);
                fromConveyor = true;
                break;

            case "Normal_Conveyor_East":
                player.move(board, Directions.EAST, 1);
                fromConveyor = true;
                break;

            case "Normal_Conveyor_South":
                player.move(board, Directions.SOUTH, 1);
                fromConveyor = true;
                break;

            case "Normal_Conveyor_West":
                player.move(board, Directions.WEST, 1);
                fromConveyor = true;
                break;

            case "Normal_Conveyor_EastNorth":
                if (fromConveyor)
                    player.rotate(false);
                player.move(board, Directions.NORTH, 1);
                fromConveyor = true;
                break;

            case "Normal_Conveyor_NorthEast":
                if (fromConveyor)
                player.move(board, Directions.EAST, 1);
                fromConveyor = true;
                break;

            case "Normal_Conveyor_EastSouth":
                if (fromConveyor)
                    player.rotate(true);
                player.move(board, Directions.SOUTH, 1);
                fromConveyor = true;
                break;

            case "Normal_Conveyor_SouthEast":
                if (fromConveyor)
                    player.rotate(false);
                player.move(board, Directions.EAST, 1);
                fromConveyor = true;
                break;

            case "Express_Conveyor_North":
                player.move(board, Directions.NORTH, 2);
                fromConveyor = true;
                break;

            case "Express_Conveyor_West":
                player.move(board, Directions.WEST, 2);
                fromConveyor = true;
                break;

            case "Express_Conveyor_South":
                player.move(board, Directions.SOUTH, 2);
                fromConveyor = true;
                break;

            case "Express_Conveyor_East":
                player.move(board, Directions.EAST, 2);
                fromConveyor = true;
                break;

            case "Express_Conveyor_EastNorth":
                if (fromConveyor)
                    player.rotate(false);
                player.move(board, Directions.NORTH, 2);
                fromConveyor = true;
                break;

            case "Express_Conveyor_NorthEast":
                if (fromConveyor)
                    player.rotate(true);
                player.move(board, Directions.EAST, 2);
                fromConveyor = true;
                break;

            case "Express_Conveyor_EastSouth":
                if (fromConveyor)
                    player.rotate(true);
                player.move(board, Directions.SOUTH, 2);
                fromConveyor = true;
                break;

            case "Express_Conveyor_SouthWest":
                if (fromConveyor)
                    player.rotate(true);
                player.move(board, Directions.WEST, 2);
                fromConveyor = true;
                break;

            case "Express_Conveyor_WestNorth":
                if (fromConveyor)
                    player.rotate(true);
                player.move(board, Directions.NORTH, 2);
                fromConveyor = true;
                break;
        }

        String events = getTileType(board, "OEvents", player.getPos());

        switch (events) {

            case "Hole":
                player.subtractLife();
                player.respawn();
                fromConveyor = false;
                break;

            case "RotateLeft":
                player.rotate(false);
                fromConveyor = false;
                break;

            case "RotateRight":
                player.rotate(true);
                fromConveyor = false;
                break;

            case "Flag1":
                player.addFlag(1);
                fromConveyor = false;
                break;

            case "Flag2":
                if (player.getFlags()[0])
                    player.addFlag(2);
                fromConveyor = false;
                break;

            case "Flag3":
                if (player.getFlags()[0] && player.getFlags()[1])
                    player.addFlag(3);
                fromConveyor = false;
                break;

            case "Flag4":
                if (player.getFlags()[0] && player.getFlags()[1] && player.getFlags()[2])
                    player.addFlag(4);
                fromConveyor = false;
                break;

            case "Floor":
                fromConveyor = false;
                break;
        }
    }

    /**
     * Checks if the player can move / be pushed in a certain direction.
     * E.g. false if player wants to move north but there is a wall there.
     * TODO add support for moving across multiple tiles.
     *
     * @param board  The current Board which holds all tiles
     * @param player Representing player, with it's direction
     * @return A boolean true if you can go in a specific direction
     */
    public static boolean canGo(Board board, Player player, Directions dir, int steps) {
        // Getting position of player
        Vector2 nextPos;
        if (dir == Directions.NORTH)
            nextPos = new Vector2(player.getPos().x, player.getPos().y + steps);
        else if (dir == Directions.SOUTH)
            nextPos = new Vector2(player.getPos().x, player.getPos().y - steps);
        else if (dir == Directions.EAST)
            nextPos = new Vector2(player.getPos().x + steps, player.getPos().y);
        else
            nextPos = new Vector2(player.getPos().x - steps, player.getPos().y);

        // Can go from current tile
        String wallType = getTileType(board, "OWalls", player.getPos());
        if (wallType != null) {
            switch (wallType) {
                case "Wall_North":
                    if (dir == Directions.NORTH)
                        return false;
                    break;

                case "Wall_South":
                    if (dir == Directions.SOUTH)
                        return false;
                    break;

                case "Wall_East":
                    if (dir == Directions.EAST)
                        return false;
                    break;

                case "Wall_West":
                    if (dir == Directions.WEST)
                        return false;
                    break;
            }
        }

        // Can go to next tile
        wallType = getTileType(board, "OWalls", nextPos);
        if (wallType != null) {
            switch (wallType) {
                case "Wall_North":
                    if (dir == Directions.SOUTH)
                        return false;
                    break;

                case "Wall_South":
                    if (dir == Directions.NORTH)
                        return false;
                    break;

                case "Wall_East":
                    if (dir == Directions.WEST)
                        return false;
                    break;

                case "Wall_West":
                    if (dir == Directions.EAST)
                        return false;
                    break;
            }
        }

        return true;
    }

    /**
     * Gets tile-type at a certain position.
     *
     * @param board The Board which holds the tiles
     * @param pos   Position of the cell
     * @return A String representing the type of tile at the pos.
     */
    private static String getTileType(Board board, String layer, Vector2 pos) {
        for (MapObject mo : board.getObjectLayer(layer)) {
            if (mo instanceof RectangleMapObject) {
                Rectangle rect = ((RectangleMapObject) mo).getRectangle();
                int rectX = (int) rect.x / TILE_SIZE;
                int rectY = (int) rect.y / TILE_SIZE;

                if (pos.x == rectX && pos.y == rectY) {
                    return (String) mo.getProperties().get("type");
                }
            }
        }

        return "";
    }
}
