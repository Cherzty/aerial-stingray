package inf112.roborally.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;
import com.badlogic.gdx.math.Vector2;
import inf112.roborally.events.EventHandler;
import inf112.roborally.screens.LoseScreen;
import inf112.roborally.screens.ScreenManager;
import inf112.roborally.screens.WinScreen;
import inf112.roborally.ui.Board;

/**
 * Represents the "robot"/playing piece the human player is associated with.
 */
public class Player {

    /**
     * Graphics
     */
    private Vector2 backup;

    /**
     * Player color
     */
    public Color color;

    /**
     * Coordinates
     */
    private Vector2 pos;

    /**
     * Life, damage & flags
     */
    private int life = 3;
    private int damage = 0;
    private boolean[] flags = {false, false, false, false};

    /**
     * Direction
     */
    private Direction dir = Direction.NORTH;

    /**
     * Current rotation
     * 0 = south
     * 1 = east
     * 2 = north
     * 3 = west
     */
    private int currentRotation = 2;

    public Player(Vector2 pos, Color color) {
        this.pos = pos;
        this.backup = new Vector2(pos.x, pos.y);
        this.color = color;
    }

    /**
     * @return player direction icon
     */
    private TextureRegion getNorthTextureRegion() {
        return new TextureRegion(new Texture("player-skin/"+color+"/player-north.png"));
    }
    private TextureRegion getSouthTextureRegion() {
        return new TextureRegion(new Texture("player-skin/"+color+"/player-south.png"));
    }
    private TextureRegion getWestTextureRegion() {
        return new TextureRegion(new Texture("player-skin/"+color+"/player-west.png"));
    }
    private TextureRegion getEastTextureRegion() {
        return new TextureRegion(new Texture("player-skin/"+color+"/player-east.png"));
    }

    public TiledMapTileLayer.Cell getPlayerIcon() {
        return getPlayerNormalCell();
    }

    /**
     * Moves the player in a certain direction with specified num. of steps.
     * Checks that the player can go on each tile for each step.
     *
     * @param board The board to move on
     * @param dir   The direction to move 1 step towards
     * @param steps Number of steps to take
     */
    public void move(Board board, Direction dir, int steps) {
        for (int i = 0; i < steps; i++) {
            switch (dir) {
                case NORTH:
                    if (EventHandler.canGo(board, this, Direction.NORTH, 1))
                        getPos().y++;
                    break;
                case EAST:
                    if (EventHandler.canGo(board, this, Direction.EAST, 1))
                        getPos().x++;
                    break;
                case SOUTH:
                    if (EventHandler.canGo(board, this, Direction.SOUTH, 1))
                        getPos().y--;
                    break;

                case WEST:
                    if (EventHandler.canGo(board, this, Direction.WEST, 1))
                        getPos().x--;
                    break;

                default:
                    System.err.println("Non-valid move!");
                    break;
            }
        }
    }

    public Direction getDir() {
        return dir;
    }

    public Direction getOppositeDir() {
        if (dir == Direction.NORTH)
            return Direction.SOUTH;
        else if (dir == Direction.EAST)
            return Direction.WEST;
        else if (dir == Direction.SOUTH)
            return Direction.NORTH;
        else
            return Direction.EAST;
    }

    /**
     * TiledMapTileLayer.Cell
     */
    public TiledMapTileLayer.Cell getPlayerNormalCell() {
        TextureRegion textureRegion = getNorthTextureRegion();
        switch (dir) {
            case NORTH:
                textureRegion = getNorthTextureRegion();
                break;
            case EAST:
                textureRegion = getEastTextureRegion();
                break;
            case SOUTH:
                textureRegion = getSouthTextureRegion();
                break;
            case WEST:
                textureRegion = getWestTextureRegion();
                break;
            default:
                System.err.println("Non-valid direction!");
                break;
        }
        TiledMapTileLayer.Cell tileCell = new TiledMapTileLayer.Cell();
        tileCell.setTile(new StaticTiledMapTile(textureRegion));

        return tileCell;
    }


    public boolean hasAllFlags() {
        return flags[0] && flags[1] && flags[2] && flags[3];
    }

    public void rotate(Boolean right) {
        if (right)
            currentRotation = Math.floorMod((currentRotation - 1), 4);
        else
            currentRotation = (currentRotation + 1) % 4;

        switch (currentRotation) {
            case 0:
                dir = Direction.SOUTH;
                break;
            case 1:
                dir = Direction.EAST;
                break;
            case 2:
                dir = Direction.NORTH;
                break;
            case 3:
                dir = Direction.WEST;
                break;
            default:
                System.err.println("Non-valid rotation!");
                break;
        }
    }

    public Vector2 getPos() {
        return pos;
    }

    public void setPos(Vector2 pos) {
        this.pos = pos;
    }

    /**
     * Changes position to backup-pos.
     * Also sets player icon to normal-mode.
     */
    public void respawn() {
        setPos(new Vector2(backup.x, backup.y));
        System.out.println(backup);
    }

    /**
     * Removes one life.
     */
    public void subtractLife() {
        life--;
        respawn();
        if (life <= 0){
            ScreenManager.getInstance().setScreen(new LoseScreen());
        }
    }

    /**
     * add one damage
     */
    public void takeDamage(){
        damage++;
        if (damage >= 10){
            subtractLife();
            damage = 10;
        }
    }

    public boolean[] getFlags() {
        return flags;
    }

    public String showStatus() {
        if (life <= 0) return "You are dead";
        String str = "Life: " + life + ", Damage: " + damage;
        if (hasAllFlags())
            str += "\n You have all flags";
        else if (flags[2] && flags[1] && flags[0])
            str += "\n You have 3 flags";
        else if (flags[1] && flags[0])
            str += "\n You have 2 flags";
        else if (flags[0])
            str += "\n You have flag 1";

        return str;
    }

    /**
     * Adds a flag to the player inventory.
     *
     * @param flagNum Flag number to add
     * @throws IllegalArgumentException when flagNum is not 1-4 (inclusive)
     */
    public void addFlag(int flagNum) throws IllegalArgumentException {
        if (flagNum <= 0 || flagNum > 4)
            throw new IllegalArgumentException("Flag number must be between 1-4 (inclusive).");

        flags[flagNum - 1] = true;
    }

    public void winCondition() {
        if (flags[3]){
            ScreenManager.getInstance().setScreen(new WinScreen());
        }
    }
}
