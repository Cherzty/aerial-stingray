package inf112.roborally.cards;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public class ProgramCard {

    /**
     * A unique priority to decide which card plays first.
     */
    private final int priority;

    /**
     * The type of the card.
     */
    private final CardType type;


    public ProgramCard(CardType type, int priority) {
        this.type = type;
        this.priority = priority;
    }

    /**
     * A method for getting the image up texture used on an ImageButton.
     *
     * @return A texture used on an ImageButton.
     */
    public TextureRegionDrawable getImageUp() {
        return this.getTextureFromType(this.getType(), false);
    }

    /**
     * A method for getting the image down texture used on an ImageButton.
     *
     * @return A texture used on an ImageButton.
     */
    public TextureRegionDrawable getImageDown() {
        return this.getTextureFromType(this.getType(), true);
    }

    /**
     * Given a CardType, returns corresponding texture.
     *
     * @param type The type of the card
     * @return Corresponding texture of cardtype
     */
    private TextureRegionDrawable getTextureFromType(CardType type, boolean pressed) {
        String state = "_notPressed.png";
        if (pressed)
            state = "_pressed.png";

        TextureRegionDrawable texture;
        switch (type) {
            case MOVE1:
                texture = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("cards/move1" + state))));
                break;

            case MOVE2:
                texture = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("cards/move2" + state))));
                break;

            case MOVE3:
                texture = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("cards/move3" + state))));
                break;

            case BACKUP:
                texture = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("cards/backup" + state))));
                break;

            case TURN_U:
                texture = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("cards/uTurn" + state))));
                break;

            case TURN_LEFT:
                texture = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("cards/leftRotate" + state))));
                break;

            case TURN_RIGHT:
                texture = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("cards/rightRotate" + state))));
                break;

            default:
                texture = null;
        }

        return texture;
    }

    public CardType getType() {
        return type;
    }

    public int getPriority() {
        return priority;
    }
}
