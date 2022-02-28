package org.volt4.wordle.controller;

import javafx.scene.layout.GridPane;
import javafx.scene.shape.Rectangle;
import org.volt4.wordle.*;
import org.volt4.wordle.animation.tile.row.RowBounce;
import org.volt4.wordle.animation.tile.row.RowReveal;
import org.volt4.wordle.animation.tile.TileBounce;
import org.volt4.wordle.animation.tile.TileFlip;

/**
 * Represents a wordgrid.
 */
public class WordGrid extends GridPane {

    // Number of rows and columns.
    public static final int N_ROWS = 6;
    public static final int N_COLUMNS = 5;

    // Tiles in this wordgrid.
    private WordGridTile[][] tiles;

    // Keeps track of where you are typing.
    private int currentRow;
    private int currentColumn;

    // Wordle answer.
    private String answer;

    // True if the current game has been on.
    private boolean hasWon;

    // True if the current game has been lost.\
    private boolean hasLost;

    /**
     * Constructs a WordGrid and set a parameter.
     */
    public WordGrid() {
        getStyleClass().add("word-grid");
        tiles = new WordGridTile[N_ROWS][N_COLUMNS];
        for (int i = 0; i < N_ROWS; i++)
            for (int j = 0; j < N_COLUMNS; j++) {
                tiles[i][j] = new WordGridTile();
                add(tiles[i][j], j, i);
            }
        // Create clipping pane.
        Rectangle clip = new Rectangle(70 * N_COLUMNS, 70 * N_ROWS);
        clip.setLayoutX(getLayoutX());
        clip.setLayoutY(getLayoutY());
        setClip(clip);
        currentRow = 0;
        currentColumn = 0;
        AnimationManager.initTileAnimations(tiles);
        answer = WordLists.pickRandomAnswer(); // First answer.
        hasWon = false;
    }

    /**
     * Resets the grid.
     */
    public void reset() {
        // Reset indices.
        currentRow = 0;
        currentColumn = 0;
        // Reset tiles.
        for (int i = 0; i < N_ROWS; i++)
            for (int j = 0; j < N_COLUMNS; j++)
                if (tiles[i][j].getLetter() != Letter.EMPTY)
                    AnimationManager.playTileFlipAnimation(i, j, TileColor.DARK_GREY, true);
        // Hide the lose card (if the game was lost).
        if (hasLost)
            AnimationManager.playLoseCardHideAnimation();
        // Pick new answer.
        answer = WordLists.pickRandomAnswer();
        hasWon = false;
        hasLost = false;
    }

    /**
     * Inputs the letter into the grid.
     * @param letter Letter to be inputted.
     */
    public void inputLetter(Letter letter) {
        if (hasWon)
            return;
        if (currentColumn >= N_COLUMNS)
            return;
        AnimationManager.playTilePopulateAnimation(currentRow, currentColumn, letter);
        currentColumn++;
        if (Settings.HelpfulKeyboard && currentColumn != 5)
            WordleApplication.getWordle().getKeyboard().updateSmartKeyboard(buildWord(), currentColumn);
        else if (Settings.HelpfulKeyboard && currentColumn == 5)
            WordleApplication.getWordle().getKeyboard().setAllKeysDisabled(true);
    }

    /**
     * Removed the last letter.
     */
    public void deleteLetter() {
        if (hasWon)
            return;
        if (currentColumn > 0)
            currentColumn--;
        tiles[currentRow][currentColumn].setLetter(Letter.EMPTY);
        if (Settings.HelpfulKeyboard)
            WordleApplication.getWordle().getKeyboard().updateSmartKeyboard(buildWord(), currentColumn);
    }

    /**
     * Builds the current word in the grid.
     * @return The current word in the grid.
     */
    private String buildWord() {
        String word = "";
        for (int i = 0; i < N_COLUMNS; i++)
            if (tiles[currentRow][i].getLetter() != Letter.EMPTY)
                word += tiles[currentRow][i].getLetter().getLetter();
            else
                word += " ";
        return word;
    }

    /**
     * Attempts to enter the current inputted word.
     * @return True if the answer is correct.
     */
    public boolean enterWord() {
        if (hasWon)
            return true;
        if (hasLost)
            return false;
        // Make sure the word is the correct length.
        if (currentColumn != N_COLUMNS)
            return false;
        // Get the word inputted.
        String word = "";
        for (int i = 0; i < N_COLUMNS; i++)
            word += tiles[currentRow][i].getLetter().getLetter();
        // Check if the guess is correct.
        if (!WordLists.guessIsValid(word)) {
            AnimationManager.playRowShakeAnimation(currentRow);
            return false;
        }
        if (word.equals(answer)) {
            AnimationManager.playRowBounceAnimation(currentRow, RowReveal.ANIMATION_DURATION + TileFlip.ANIMATION_DURATION);
            AnimationManager.playRowDoubleFlipAnimation(currentRow, RowReveal.ANIMATION_DURATION + TileFlip.ANIMATION_DURATION + RowBounce.ANIMATION_DURATION + TileBounce.ANIMATION_DURATION);
            hasWon = true;
        }
        // Get the colors to flip to.
        TileColor[] colors = new TileColor[N_COLUMNS];
        for (int i = 0; i < N_COLUMNS; i++) {
            String letter = tiles[currentRow][i].getLetter().getLetter();
            colors[i] = TileColor.LIGHT_GREY;
            for (int j = 0; j < N_COLUMNS; j++)
                if (("" + answer.charAt(j)).equals(letter))
                    if (i == j) {
                        colors[i] = TileColor.GREEN;
                        break;
                    } else
                        colors[i] = TileColor.YELLOW;
        }
        // Create letters array.
        Letter[] letters = new Letter[N_COLUMNS];
        for (int i = 0; i < letters.length; i++)
            letters[i] = Letter.getMatch(word.substring(i, i + 1));
        // Play animation.
        AnimationManager.playRowRevealAnimation(currentRow, colors, letters);
        // Increment row.
        currentRow++;
        currentColumn = 0;
        if (currentRow == N_ROWS && !hasWon) {
            // Game has been lost.
            hasLost = true;
            AnimationManager.playLoseCardShowAnimation(answer, RowReveal.ANIMATION_DURATION + TileFlip.ANIMATION_DURATION);
        }
        if (Settings.HelpfulKeyboard)
            WordleApplication.getWordle().getKeyboard().setAllKeysDisabled(false);
        return word.equals(answer);
    }

}
