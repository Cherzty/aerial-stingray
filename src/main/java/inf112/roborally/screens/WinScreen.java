package inf112.roborally.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import inf112.roborally.Main;

public class WinScreen implements Screen {

    private Stage stage;

    public WinScreen() {
        stage = new Stage(new StretchViewport(Main.WIDTH, Main.HEIGHT));
    }

    @Override
    public void show() {
        Table menuComponents = new Table();
        Skin menuSkin = new Skin(Gdx.files.internal("rusty-robot/skin/rusty-robot-ui.json"));

        Image robot = new Image(new Texture(Gdx.files.internal("rusty-robot/raw/robot.png")));
        robot.setPosition(1000, 0);

        TextField textField = new TextField("You Won!", menuSkin);
        textField.setDisabled(true);
        textField.setAlignment(Align.center);
        TextButton mainMenu = new TextButton("Main Menu", menuSkin);
        TextButton exit = new TextButton("Exit", menuSkin);

        mainMenu.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                ScreenManager.getInstance().setScreen(new MenuScreen());
            }
        });

        exit.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                Gdx.app.exit();
            }
        });

        menuComponents.add(textField).size(200, 75).expandX().padBottom(20);
        menuComponents.row();
        menuComponents.add(mainMenu).size(300, 120).expandX();
        menuComponents.row();
        menuComponents.add(exit).size(300, 120).expandX();
        menuComponents.setFillParent(true);

        stage.addActor(menuComponents);
        stage.addActor(robot);
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float v) {
        // Clear screen
        Gdx.gl.glClearColor(178 / 255f, 148 / 255f, 119 / 255f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(v);
        stage.draw();
    }

    /**
     * Non-finished method implemented from Screen.
     */
    @Override
    public void resize(int i, int i1) {

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

    /**
     * Non-finished method implemented from Screen.
     */
    @Override
    public void dispose() {

    }
}
