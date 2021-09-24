package com.max480.randomstuff.gae.discord.gamescommands.status;

import com.max480.randomstuff.gae.discord.gamescommands.Game;
import com.max480.randomstuff.gae.discord.gamescommands.OnePlayerGame;
import com.max480.randomstuff.gae.discord.gamescommands.games.Connect4;
import com.max480.randomstuff.gae.discord.gamescommands.games.Minesweeper;
import com.max480.randomstuff.gae.discord.gamescommands.games.Reversi;
import com.max480.randomstuff.gae.discord.gamescommands.games.TicTacToe;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import java.util.Random;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Those tests check if serialization and deserialization work by checking if serializing a random state
 * then deserializing it gives back the same game state.
 * This is particularly required for serialization since it writes data in very specific ways to save space...
 */
public class SerializationTests {
    private void serializationTestTwoPlayer(Supplier<GameState> gameStateGenerator) {
        Random r = new Random();
        for (int i = 0; i < 100000; i++) {
            GameState gameState = gameStateGenerator.get();
            gameState.setLatestCommand(RandomStringUtils.random(5));
            Game game = new Game(gameState, r.nextBoolean(), r.nextBoolean(),
                    r.nextLong(), r.nextLong(), r.nextInt(100), r.nextLong(), null, null);

            Game reserializedGame = Game.deserializeFromString(game.serializeToString(), null, null);
            assertThat(reserializedGame).usingRecursiveComparison().isEqualTo(game);
        }
    }

    @Test
    public void connect4SerializationTest() {
        serializationTestTwoPlayer(Connect4::generateRandom);
    }

    @Test
    public void reversiSerializationTest() {
        serializationTestTwoPlayer(Reversi::generateRandom);
    }

    @Test
    public void ticTacToeSerializationTest() {
        serializationTestTwoPlayer(TicTacToe::generateRandom);
    }

    @Test
    public void minesweeperSerializationTest() {
        Random r = new Random();
        for (int i = 0; i < 100000; i++) {
            GameState gameState = Minesweeper.generateRandom();
            gameState.setLatestCommand(RandomStringUtils.random(5));
            Game game = new OnePlayerGame(gameState, r.nextLong(), null, null);

            Game reserializedGame = Game.deserializeFromString(game.serializeToString(), null, null);
            assertThat(reserializedGame).usingRecursiveComparison().isEqualTo(game);
        }
    }
}
