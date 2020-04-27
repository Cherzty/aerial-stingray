package inf112.roborally.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import inf112.roborally.Main;
import inf112.roborally.cards.Deck;
import inf112.roborally.cards.ProgramCard;
import inf112.roborally.entities.Color;
import inf112.roborally.entities.Player;
import inf112.roborally.server.Client;
import inf112.roborally.server.Server;
import inf112.roborally.ui.Board;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;

/**
 * Handles logic of game & controlling players.
 */
public class RoboRally implements Screen {

    /**
     * Constants
     */
    public final static int NUM_CARDS_SERVED = 9;
    private final int MAX_SELECTED_CARDS = 5;
    private final int DECK_WINDOW_SIZE = 280;

    /**
     * Rendering
     */
    public SpriteBatch batch;
    private BitmapFont font;
    private TiledMapRenderer mapRenderer;
    private OrthographicCamera camera;
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
     * All players in the game.
     */
    private ArrayList<Player> players;

    /**
     */
    private ImageButton[] cardButtons;

    private int numPlayers;

    /**
     * Variables to be used in networking/multiplayer
     */
    public Server server;
    public Client client;
    public ArrayList<String> playerNames;
    public int playersInGame;
    public int amountOfPlayers;
    public int readyPlayers;
    public boolean connected;
    public ArrayList<String> deadPlayers;


    public RoboRally(int numPlayers) {
        this.numPlayers = numPlayers;
        setupGameComponents();
        setupPlayers(numPlayers);
        setupRendering();
        setupUI();
        setupInput();

        dealCardsToAll();  // First phase
    }

    /**
     * Phase 1 - deal cards to all players.
     */
    private void dealCardsToAll() {
        System.out.println("[  PHASE 1  ] Dealing out cards to all");

        refillCardsToAll();
        refreshImageButtons();
        uncheckAllCards();
    }

    /**
     * Fills in missing cards in in available cards.
     */
    private void refillCardsToAll() {
        for (Player player : players) {
            for (int i = 0; i < NUM_CARDS_SERVED; i++) {
                if (player.getAvailableCards()[i] == null)
                    player.getAvailableCards()[i] = deck.pop();
            }
        }
    }

    /**
     * Phase 2 - called when player cards is selected.
     */
    private void selectCards() {
        System.out.println("[  PHASE 2  ] Cards selected!");
        printThisPlayerSelectedCards();

        selectCardsForBots();

        promptPowerDown();  // Next phase - Not functional atm.
    }

    /**
     * Phase 3 - ask if each player want to power down.
     * Starts to ask this player.
     * All the other players choose on a pseudo-random basis.
     */
    private void promptPowerDown() {
        // Other players deciding if power down - 5 % chance
        Random random = new Random();
        for (Player player : players) {
            if (random.nextInt(21) == 0)
                player.setPowerDown(true);
            else
                player.setPowerDown(false);
        }

        // This player deciding if power down
        Skin skin = new Skin(Gdx.files.internal("rusty-robot/skin/rusty-robot-ui.json"));
        Dialog dialog = new Dialog("Power Down", skin) {
            @Override
            protected void result(Object object) {
                try {
                    String str = (String) object;
                    if (str.equals("Power down")) {
                        System.out.println("[  THIS_ROBOT  ] Power down");
                        getThisPlayer().setPowerDown(true);
                        executeRobotCards();  // Next phase
                    } else if (str.equals("Don't power down")) {
                        System.out.println("[  THIS_ROBOT  ] Don't power down");
                        getThisPlayer().setPowerDown(false);
                        executeRobotCards();  // Next phase
                    }
                } catch (ClassCastException cce) {
                    cce.printStackTrace();
                }
            }
        };
        dialog.text("Do you want to power down your robot next round?");

        dialog.button("Yes", "Power down");
        dialog.button("No", "Don't power down");
        System.out.println("[  PHASE 3  ] Prompting for power down");

        dialog.show(stage);
    }

    /**
     * Phase 4 - executes all player cards, in order, based on card power.
     */
    private void executeRobotCards() {
        System.out.println("[  PHASE 4  ] Ready to execute cards!");
        System.out.println("=======================================");

        for (int i = 0; i < 5; i++) {  // 5 cards
            LinkedList<Player> highestPriority = getPriorityList(i);  // List of players, the one with highest card this iteration is first

            System.out.println("ITERATION = " + i);
            for (Player player : highestPriority) {  // Each player in correct order
                ProgramCard card = player.getSelectedCards().get(i);

                String identifier;
                if (player.equals(getThisPlayer())) identifier = "[  THIS_PLAYER  ]";
                else identifier = "[  ROBOT_" + players.indexOf(player) + "  ]";
                System.out.println(identifier + " Executes card " + card.getType() + " with priority " + card.getPriority());
                executeCard(player, player.getSelectedCards().get(i));
            }
            System.out.println("=======================================");
        }

        cleanUp();  // Next phase
    }

    /**
     * Returns a sorted List of players, based on which card (at 'index') has highest priority.
     *
     * @param index Index of the card in selected cards - inventory
     * @return A sorted list of players, based on prisority cards
     */
    private LinkedList<Player> getPriorityList(int index) {
        LinkedList<Player> copy = new LinkedList<>(players);

        Collections.sort(copy, (player, t1) -> t1.getSelectedCards().get(index).getPriority() - player.getSelectedCards().get(index).getPriority());
        return copy;
    }

    /**
     * Executes a card
     *
     * @param selectedCard The card to execute
     */
    public void executeCard(Player player, ProgramCard selectedCard) {
        Vector2 oldPos = player.getPos();
        setCellToNull(oldPos);

        player.executeCard(board, selectedCard, players);
    }

    /**
     * Phase 5 - ending round and cleaning up board.
     */
    private void cleanUp() {
        System.out.println("[  PHASE 5  ] Ending round and cleaning up board.");

        recycleCards();
        dealCardsToAll();  // Starting back at phase 1
    }

    private Player getThisPlayer() {
        for (Player player : players) {
            if (!player.isAI()) return player;
        }

        throw new NullPointerException("\"This player\" could not be found!");
    }

    private void setupGameComponents() {
        deck = new Deck();
        board = new Board();
        cardButtons = new ImageButton[NUM_CARDS_SERVED];
    }

    private void setupPlayers(int numPlayers) {
        players = new ArrayList<>();
        Color[] colors = {Color.RED, Color.GREEN, Color.BLUE, Color.PINK};
        Vector2[] startPos = {new Vector2(6, 1), new Vector2(9, 1), new Vector2(13, 1), new Vector2(16, 1)};

        for (int i = 0; i < numPlayers; i++) {
            if (i == 0)
                players.add(new Player(startPos[i], colors[i], i, false));  // Human
            else
                players.add(new Player(startPos[i], colors[i], i));
        }
    }

    private void setupRendering() {
        resize(Main.WIDTH, Main.HEIGHT + DECK_WINDOW_SIZE);

        batch = new SpriteBatch();
        font = new BitmapFont();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.y -= DECK_WINDOW_SIZE;
        camera.update();
        mapRenderer = new OrthogonalTiledMapRenderer(board.getMap());
        mapRenderer.setView(camera);
    }

    private void setupUI() {
        stage = new Stage(new StretchViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));

        setupButtons();
        setupCardButtons();

        ScreenManager.getInstance().setScreen(this);
    }

    /**
     * Adds necessary buttons to the screen.
     * As of now, only one button.
     */
    private void setupButtons() {
        Skin skin = new Skin(Gdx.files.internal("rusty-robot/skin/rusty-robot-ui.json"));
        TextButton submitCards = new TextButton("Lock in cards!", skin);
        submitCards.setSize(200, 80);
        submitCards.setPosition((float) Main.WIDTH / 2 - (submitCards.getWidth() / 2), 200);
        submitCards.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // If player can successfully lock in cards, begin round
                if (canSelectCards()) {
                    selectCards();
                }
            }
        });
        stage.addActor(submitCards);
    }

    /**
     * Adds the programming cards (9 of them) to the screen.
     * Init cards.
     */
    private void setupCardButtons() {
        int startX = 21;
        int margin = 155;
        for (int i = 0; i < NUM_CARDS_SERVED; i++) {
            int index_copy = i;

            ImageButton btn = new ImageButton(null, null, null);
            btn.setPosition(i * margin + startX, 0);
            btn.setSize(140, 200);
            btn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (!btn.isChecked())
                        removeClickedCardFromStack(index_copy);

                    if (getThisPlayer().getSelectedCards().size() == MAX_SELECTED_CARDS) {
                        btn.setChecked(false);
                        System.err.println("Can't choose more cards (MAX=5).");
                    } else if (btn.isChecked())
                        addClickedCardToStack(index_copy);
                }
            });
            cardButtons[i] = btn;

            stage.addActor(btn);
        }
    }

    /**
     * Sets up input processor.
     */
    private void setupInput() {
        Gdx.input.setInputProcessor(stage);
    }

    /*
    private void run(){
        for (Player player : players) {
            EventUtil.handleEvent(board, player, players);
        }
    }
    */

    /**
     * Locks in the selected cards and furthers the process
     * of executing them.
     */
    public boolean canSelectCards() {
        if (getThisPlayer().getSelectedCards().size() == MAX_SELECTED_CARDS) {
            return true;
        } else if (getThisPlayer().getSelectedCards().size() > MAX_SELECTED_CARDS)
            System.err.println("Too many cards to lock in.");
        else
            System.err.println("Too few cards to lock in.");

        return false;
    }

    /**
     * Sets a cell in the player layer, at a certain position, to null.
     *
     * @param pos The position of the cell
     */
    public void setCellToNull(Vector2 pos) {
        board.getPlayerLayer().setCell((int) pos.x, (int) pos.y, null);
    }

    /**
     * Recycles cards that has been executed back to deck.
     * Also empties the cards currently chosen.
     */
    public void recycleCards() {
        for (Player player : players) {
            deck.recycleAll(player.getSelectedCards());
            player.setSelectedCards(new LinkedList<>());
        }
    }

    /**
     * Unchecking all ImageButtons representing the cards.
     * This only needs to be done for this player, as only that player has GUI.
     */
    public void uncheckAllCards() {
        for (int i = 0; i < NUM_CARDS_SERVED; i++) {
            if (cardButtons[i].isChecked()) {
                cardButtons[i].setChecked(false);
            }
        }
    }

    /**
     * Updating the ImageButtons to correspond to the new cards.
     *
     * This only needs to be done for this player, as only that player has GUI.
     */
    public void refreshImageButtons() {
        for (int i = 0; i < NUM_CARDS_SERVED; i++) {
            ImageButton.ImageButtonStyle oldImageButtonStyle = cardButtons[i].getStyle();
            oldImageButtonStyle.imageUp = getThisPlayer().getAvailableCards()[i].getImageUp();
            oldImageButtonStyle.imageChecked = getThisPlayer().getAvailableCards()[i].getImageDown();
            oldImageButtonStyle.imageDown = getThisPlayer().getAvailableCards()[i].getImageDown();

            cardButtons[i].setStyle(oldImageButtonStyle);
        }
    }

    /**
     * Adding a card to the queue.
     * This only needs to be done for this player, as only that player has GUI.
     *
     * @param index Index of the card in the array of cards to chose.
     */
    public void addClickedCardToStack(int index) {
        if (getThisPlayer().getSelectedCards().size() >= MAX_SELECTED_CARDS) {
            System.err.println("[  THIS_ROBOT  ] Can't add any more cards to stack.");
            return;
        }

        getThisPlayer().selectCard(index);
        printThisPlayerSelectedCards();
    }

    /**
     * Removing a card from the queue.
     * This only needs to be done for this player, as only that player has GUI.
     *
     * @param index Index of the card in the array of cards to chose.
     */
    public void removeClickedCardFromStack(int index) {
        if (getThisPlayer().getSelectedCards().size() == 0) {
            System.err.println("[  THIS_ROBOT  ] Can't remove any more cards from stack.");
            return;
        }

        System.out.println("[  THIS_ROBOT  ] Removing " + getThisPlayer().getSelectedCards().get(index) + " from stack.");
        getThisPlayer().deselectCard(index);
    }

    /**
     * Selects 5 cards from available cards for all bots.
     */
    private void selectCardsForBots() {
        for (Player player : players) {
            if (player.equals(getThisPlayer())) continue;  // Skip this player

            // Selects the first 5 available cards
            for (int i = 0; i < MAX_SELECTED_CARDS; i++) {
                if (player.getSelectedCards().size() >= MAX_SELECTED_CARDS) break;

                player.selectCard(i);
            }
        }
    }

    /**
     * Prints all the cards the user currently has chosen.
     */
    public void printThisPlayerSelectedCards() {
        if (getThisPlayer().getSelectedCards().size() == 0) return;

        System.out.print("[  THIS_ROBOT  ] : [");
        for (int i = 0; i < getThisPlayer().getSelectedCards().size(); i++) {
            if (i != getThisPlayer().getSelectedCards().size() - 1)
                System.out.print("(" + i + ", " + getThisPlayer().getSelectedCards().get(i).getType() + "), ");
            else
                System.out.println("(" + i + ", " + getThisPlayer().getSelectedCards().get(i).getType() + ")]");
        }
    }

    @Override
    public void render(float v) {
        clearScreen();

        actPlayers();
        actAndRender(Gdx.graphics.getDeltaTime());
    }

    /**
     * Clears the screen with a set background color.
     */
    public void clearScreen() {
        Gdx.gl.glClearColor(178 / 255f, 148 / 255f, 119 / 255f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }

    /**
     * Updates all players' position and if any won.
     */
    public void actPlayers() {
        int i = 0;
        for (Player player : this.players) {
            board.getPlayerLayer().setCell((int) player.getPos().x, (int) player.getPos().y, player.getPlayerIcon());
            board.getPlayerLayer().getCell((int) player.getPos().x, (int) player.getPos().y).getTile().setId(i);
            i++;

            player.checkIfWon();
        }
    }

    /**
     * Wrapper method for acting and rendering the stage, map and camera.
     *
     * @param v The delta-time used (usually Gdx.graphics.getDeltaTime())
     */
    public void actAndRender(float v) {
        camera.update();
        mapRenderer.render();

        stage.act(v);
        stage.draw();
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
    }

    /**
     * Non-finished method implemented from Screen.
     */
    @Override
    public void show() {
    }

    @Override
    public void resize(int width, int height) {
        Gdx.graphics.setWindowedMode(width, height);
    }

    /**
     * Non-finished method implemented from Screen.
     */
    @Override
    public void pause() {
    }

    /**
     * Non-finished method implemented from Screen.
     */
    @Override
    public void resume() {
    }

    /**
     * Non-finished method implemented from Screen.
     */
    @Override
    public void hide() {
    }
}