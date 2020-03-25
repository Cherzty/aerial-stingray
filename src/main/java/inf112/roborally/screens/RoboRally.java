package inf112.roborally.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import inf112.roborally.Main;
import inf112.roborally.cards.Deck;
import inf112.roborally.cards.ProgramCard;
import inf112.roborally.entities.Color;
import inf112.roborally.entities.Player;
import inf112.roborally.events.EventUtil;
import inf112.roborally.ui.Board;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Handles logic of game & controlling players.
 */
public class RoboRally extends InputAdapter implements Screen {

    /**
     * Const.
     */
    private final int DECK_WINDOW_SIZE = 280;
    private final int MAX_SELECTED_CARDS = 5;
    private final int NUM_CARDS_SERVED = 9;

    /**
     * Rendering
     */
    public SpriteBatch batch;
    private BitmapFont font;
    private TiledMapRenderer mapRenderer;
    private OrthographicCamera camera;

    /**
     * Components are contained by stage.
     */
    private Stage stage;

    /**
     * Board to be played on.
     */
    private Board board;

    /**
     * Deck to choose cards from.
     */
    private Deck deck;

    /**
     * Players on the board
     */
    private ArrayList<Player> players;

    /**
     * As of now, only handle one player.
     * Reference to the player associated with this instance of RoboRally
     */
    private Player thisPlayer;

    /**
     * Storage for the cards displayed to player, and for the card held by player.
     */
    private LinkedList<ProgramCard> cardsChosen;
    private ProgramCard[] cardsToChoseFrom;
    private ImageButton[] cardButtons;


    public RoboRally(int numPlayers) {
        setupGameComponents();
        setupPlayers(numPlayers);
        setupRendering();
        setupUI();
        setupInput();
    }

    private void setupGameComponents() {
        deck = new Deck();
        board = new Board();
        cardsToChoseFrom = new ProgramCard[NUM_CARDS_SERVED];
        cardButtons = new ImageButton[NUM_CARDS_SERVED];
    }

    private void setupPlayers(int numPlayers) {
        players = new ArrayList<>();
        cardsChosen = new LinkedList<>();
        Color[] colors = {Color.RED, Color.GREEN, Color.BLUE, Color.PINK};
        Vector2[] startPos = {new Vector2(6,1), new Vector2(9,1), new Vector2(13,1), new Vector2(16,1)};

        for (int i = 0; i < numPlayers; i++) {
            players.add(new Player(startPos[i], colors[i]));
        }
        // Player p1 = new Player(startPos[0], colors[0]);
        // players.add(p1);

        thisPlayer = players.get(0);
    }

    private void setupRendering() {
        resize(Main.WIDTH, Main.HEIGHT + DECK_WINDOW_SIZE);

        batch = new SpriteBatch();
        font = new BitmapFont();
        mapRenderer = new OrthogonalTiledMapRenderer(board.getMap());
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.y -= DECK_WINDOW_SIZE;
        camera.update();
    }

    private void setupUI() {
        stage = new Stage();

        Skin skin = new Skin(Gdx.files.internal("rusty-robot/skin/rusty-robot-ui.json"));
        TextButton submitCards = new TextButton("Lock in cards!", skin);
        submitCards.setSize(200, 80);
        submitCards.setPosition((float) Main.WIDTH / 2 - (submitCards.getWidth() / 2), 200);
        submitCards.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                lockInCards();
            }
        });
        stage.addActor(submitCards);

        int startX = 21;
        int margin = 155;
        for (int i = 0; i < NUM_CARDS_SERVED; i++) {
            int index_copy = i;

            ProgramCard card = deck.pop();
            cardsToChoseFrom[i] = card;

            ImageButton btn = card.getImageButton();
            btn.setPosition(i * margin + startX, 0);
            btn.setSize(140, 200);
            btn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (!btn.isChecked())
                        removeCardFromStack(index_copy);

                    if (cardsChosen.size() == MAX_SELECTED_CARDS) {
                        btn.setChecked(false);
                        System.err.println("Can't choose more cards (MAX=5).");
                    } else if (btn.isChecked())
                        addCardToStack(index_copy);
                }
            });
            cardButtons[i] = btn;

            stage.addActor(btn);
        }

        ScreenManager.getInstance().setScreen(this);
    }

    private void setupInput() {
        // Input - both for controlling player and selecting cards
        InputMultiplexer im = new InputMultiplexer();
        im.addProcessor(this);
        im.addProcessor(stage);
        Gdx.input.setInputProcessor(im);
    }

    /**
     * Locks in the selected cards and furthers the process
     * of executing them.
     */
    public void lockInCards() {
        if (cardsChosen.size() == MAX_SELECTED_CARDS) {
            System.out.println("Locking in cards!");
            printChosenCards();
            executeCards(cardsChosen);
        } else if (cardsChosen.size() > MAX_SELECTED_CARDS)
            System.err.println("Too many cards to lock in.");
        else
            System.err.println("Too few cards to lock in.");
    }

    /**
     * Executes a stack of cards
     *
     * @param cardStack The cards to execute
     */
    public void executeCards(Queue<ProgramCard> cardStack) {
        while (!cardStack.isEmpty()) {
            ProgramCard currentCard = cardStack.poll();

            Vector2 oldPos = thisPlayer.getPos();
            setCellToNull(oldPos);

            thisPlayer.executeCard(board, currentCard);
            System.out.println(thisPlayer.showStatus());
        }

        recycleAndDisplayNewCards();
    }

    /**
     * Sets the cell in the player layer at a certain pos to null.
     *
     * @param pos The position of the cell
     */
    public void setCellToNull(Vector2 pos) {
        board.getPlayerLayer().setCell((int) pos.x, (int) pos.y, null);
    }

    /**
     * Wrapper method for:
     * - recycling selected cards
     * - deselecting all cards
     * - getting new cards from the deck
     * - updating ImageButtons of the cards
     */
    private void recycleAndDisplayNewCards() {
        recycleCards();
        deselectAllCards();
        fillInNewCards();
        refreshImageButtons();
    }

    /**
     * Recycles cards that has been executed back to deck.
     * Also empties the cards currently chosen.
     */
    public void recycleCards() {
        for (int i = 0; i < NUM_CARDS_SERVED; i++) {
            if (cardsToChoseFrom[i].getImageButton().isChecked()) {
                deck.recycle(cardsToChoseFrom[i].copy());
            }
        }
        cardsChosen = new LinkedList<>();
    }

    /**
     * Deselecting all ImageButtons representing the cards.
     */
    public void deselectAllCards() {
        for (int i = 0; i < NUM_CARDS_SERVED; i++) {
            if (cardButtons[i].isChecked()) {
                cardButtons[i].setChecked(false);
                cardsToChoseFrom[i] = null;
            }
        }
    }

    /**
     * Fetching new cards from the deck to replace the 5 executed.
     */
    public void fillInNewCards() {
        for (int i = 0; i < NUM_CARDS_SERVED; i++) {
            if (cardsToChoseFrom[i] == null) {
                cardsToChoseFrom[i] = deck.pop();
            }
        }
    }

    /**
     * Updating the ImageButtons to correspond to the new cards.
     */
    public void refreshImageButtons() {
        for (int i = 0; i < NUM_CARDS_SERVED; i++) {
            ImageButton.ImageButtonStyle oldImageButtonStyle = cardButtons[i].getStyle();
            oldImageButtonStyle.imageUp = cardsToChoseFrom[i].getImageUp();
            oldImageButtonStyle.imageChecked = cardsToChoseFrom[i].getImageDown();
            oldImageButtonStyle.imageDown = cardsToChoseFrom[i].getImageDown();

            cardButtons[i].setStyle(oldImageButtonStyle);
        }
    }

    /**
     * Adding a card to the queue.
     *
     * @param index Index of the card in the array of cards to chose.
     */
    public void addCardToStack(int index) {
        if (cardsChosen.size() >= MAX_SELECTED_CARDS) {
            System.err.println("Can't add any more cards to stack.");
            return;
        }

        System.out.println("Adding " + cardsToChoseFrom[index].getType() + " to selected hand!");

        cardsChosen.add(cardsToChoseFrom[index]);
        printChosenCards();
    }

    /**
     * Removing a card from the queue.
     *
     * @param index Index of the card in the array of cards to chose.
     */
    public void removeCardFromStack(int index) {
        if (cardsChosen.size() == 0) {
            System.err.println("Can't remove any more cards from stack.");
            return;
        }

        System.out.println("Removing " + cardsToChoseFrom[index].getType() + " from stack.");

        cardsChosen.remove(cardsToChoseFrom[index]);
        printChosenCards();
    }

    /**
     * Prints all the cards the user currently has chosen.
     */
    public void printChosenCards() {
        System.out.print("Chosen cards: [");
        for (int i = 0; i < cardsChosen.size(); i++) {
            if (i != cardsChosen.size() - 1)
                System.out.print("(" + i + ", " + cardsChosen.get(i).getType() + "), ");
            else
                System.out.println("(" + i + ", " + cardsChosen.get(i).getType() + ")]");
        }
    }

    /**
     * KeyListener for the robot - purely for debugging.
     *
     * @return true if the player moved, false otherwise
     */
    @Override
    public boolean keyUp(int keycode) {
        boolean moved = false;
        int x = (int) thisPlayer.getPos().x;
        int y = (int) thisPlayer.getPos().y;

        if (keycode == Input.Keys.UP) {
            thisPlayer.move(board, thisPlayer.getDir(), 1);
            moved = true;
        } else if (keycode == Input.Keys.DOWN) {
            thisPlayer.move(board, thisPlayer.getOppositeDir(), 1);
            moved = true;
        } else if (keycode == Input.Keys.LEFT) {
            thisPlayer.rotate(false);
            moved = true;
        } else if (keycode == Input.Keys.RIGHT) {
            thisPlayer.rotate(true);
            moved = true;
        }

        if (moved) {
            board.getPlayerLayer().setCell(x, y, null);
            System.out.println(thisPlayer.showStatus());
            EventUtil.handleEvent(board, thisPlayer);
        }

        return moved;
    }

    @Override
    public void render(float v) {
        clearScreen();

        drawPlayer();
        thisPlayer.checkIfWon();

        actAndRender(Gdx.graphics.getDeltaTime());
    }

    /**
     * Draws player on the map.
     */
    public void drawPlayer() {
        //board.getPlayerLayer().setCell((int) thisPlayer.getPos().x, (int) thisPlayer.getPos().y, thisPlayer.getPlayerIcon());
        for (Player player : this.players){
            board.getPlayerLayer().setCell((int) player.getPos().x, (int) player.getPos().y, player.getPlayerIcon());
        }
    }

    /**
     * Clears the screen with a set background color.
     */
    public void clearScreen() {
        Gdx.gl.glClearColor(178 / 255f, 148 / 255f, 119 / 255f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }

    /**
     * Wrapper method for acting and rendering the stage, map and camera.
     *
     * @param v The delta-time used (usually Gdx.graphics.getDeltaTime())
     */
    public void actAndRender(float v) {
        stage.act(v);
        stage.draw();

        camera.update();
        mapRenderer.setView(camera);
        mapRenderer.render();
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
    }

    @Override
    public void show() {
    }

    @Override
    public void resize(int width, int height) {
        Gdx.graphics.setWindowedMode(width, height);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {

    }
}
