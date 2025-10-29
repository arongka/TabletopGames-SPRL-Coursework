package evaluation;

import core.AbstractParameters;
import core.AbstractPlayer;
import core.interfaces.IGameRunner;
import evaluation.listeners.IGameListener;
import evaluation.tournaments.RoundRobinTournament;
import evaluation.tournaments.SkillGrid;
import games.GameType;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import players.PlayerFactory;
import players.PlayerType;
import players.basicMCTS.BasicMCTSPlayer;
import players.mcts.MCTSPlayer;
import players.rmhc.RMHCPlayer;
import players.simple.OSLAPlayer;
import players.simple.RandomPlayer;
import utilities.Pair;
import utilities.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import static evaluation.RunArg.*;
import static java.util.stream.Collectors.toList;


public class RunGames implements IGameRunner {

    // Config
    Map<RunArg, Object> config = new HashMap<>();

    // Vars for running
    Map<GameType, int[]> gamesAndPlayerCounts;
    private LinkedList<AbstractPlayer> agents;
    private String timeDir;

    /**
     * Main function, creates and runs the tournament with the given settings and players.
     */
    @SuppressWarnings({"ConstantConditions"})
    public static void main(String[] args) {
        List<String> argsList = Arrays.asList(args);
        if (argsList.contains("--help") || argsList.contains("-h")) {
            RunArg.printHelp(Usage.RunGames);
            return;
        }

        /* 1. Settings for the tournament */
        RunGames runGames = new RunGames();
        runGames.config = parseConfig(args, Collections.singletonList(Usage.RunGames));

        runGames.initialiseGamesAndPlayerCount();
        if (!runGames.config.get(RunArg.gameParams).equals("") && runGames.gamesAndPlayerCounts.keySet().size() > 1)
            throw new IllegalArgumentException("Cannot yet provide a gameParams argument if running multiple games");

        // 2. Setup
        LinkedList<AbstractPlayer> agents = new LinkedList<>();
        if (!runGames.config.get(playerDirectory).equals("")) {
            agents.addAll(PlayerFactory.createPlayers((String) runGames.config.get(playerDirectory)));
        } else {
       //     agents.add(new MCTSPlayer());
            agents.add(new BasicMCTSPlayer());
            agents.add(new RandomPlayer());
            agents.add(new RMHCPlayer());
            agents.add(new OSLAPlayer());
        }
        runGames.agents = agents;

        if (!runGames.config.get(focusPlayer).equals("")) {
            // if a focus Player is provided, then this override some other settings
            runGames.config.put(mode, "onevsall");
            AbstractPlayer fp = PlayerFactory.createPlayer((String) runGames.config.get(focusPlayer));
            agents.add(0, fp);  // convention is that they go first in the list of agents
        }

        runGames.timeDir = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());

        // 3. Run!
        if (runGames.config.get(mode).equals("sequential")) {
            SkillGrid main = new SkillGrid(agents, runGames.config);
            main.run();
        } else {
            runGames.run();
        }
    }

    @Override
    public void run() {
        // We are now restricted to SushiGo and 3 players
        GameType sushiGoGame = GameType.SushiGo;
        int[] playerCounts = {3}; // Set player count to 3

        // Run the SushiGo game with 3 players
        System.out.println("Starting SushiGo game with 3 players...");
        String gameName = sushiGoGame.name();

        // Setup the parameters for the game
        AbstractParameters params = config.get(gameParams).equals("") ? null : AbstractParameters.createFromFile(sushiGoGame, (String) config.get(gameParams));

        // Create a RoundRobin tournament for this game
        RoundRobinTournament tournament = new RoundRobinTournament(agents, sushiGoGame, 3, params, config);

        // Add listeners for results tracking
        //noinspection unchecked
        for (String listenerClass : ((List<String>) config.get(listener))) {
            try {
                IGameListener gameTracker = IGameListener.createListener(listenerClass);
                tournament.addListener(gameTracker);
                String outputDir = (String) config.get(destDir);
                List<String> directories = new ArrayList<>(Arrays.asList(outputDir.split(Pattern.quote(File.separator))));
                directories.add(gameName);
                if ((boolean) config.get(addTimeStamp))
                    directories.add(timeDir);
                gameTracker.setOutputDirectory(directories.toArray(new String[0]));
            } catch (IllegalArgumentException e) {
                System.out.println("Error creating listener: " + e.getMessage());
            }
        }

        // Run the tournament
        tournament.run();

        // Print results to console after the game finishes
        System.out.println("SushiGo game with 3 players has finished.");
    }

    private void initialiseGamesAndPlayerCount() {
        // Set gameArg to only include SushiGo
        String gameArg = "SushiGo";  // Only SushiGo
        String playerRange = "3";    // Only 3 players
        int np = (int) config.get(RunArg.nPlayers);
        if (np > 0)
            playerRange = String.valueOf(np);

        List<String> tempGames = new ArrayList<>(Arrays.asList(gameArg.split("\\|")));
        List<String> games = tempGames;
        if (tempGames.get(0).equals("all")) {
            tempGames.add("-GameTemplate"); // So that we always remove this one
            games = Arrays.stream(GameType.values()).map(Enum::name).filter(name -> !tempGames.contains("-" + name)).collect(toList());
        }

        // This creates a <MinPlayer, MaxPlayer> Pair for each game#
        List<Pair<Integer, Integer>> nPlayers = Arrays.stream(playerRange.split("\\|"))
                .map(str -> {
                    if (str.contains("-")) {
                        int hyphenIndex = str.indexOf("-");
                        return new Pair<>(Integer.valueOf(str.substring(0, hyphenIndex)), Integer.valueOf(str.substring(hyphenIndex + 1)));
                    } else if (str.equals("all")) {
                        return new Pair<>(-1, -1); // The next step will fill in the correct values
                    } else
                        return new Pair<>(Integer.valueOf(str), Integer.valueOf(str));
                }).collect(toList());

        // Ensure SushiGo is set with 3 players
        for (int i = 0; i < nPlayers.size(); i++) {
            GameType game = GameType.valueOf(games.get(i));
            if (game == GameType.SushiGo) {
                nPlayers.set(i, new Pair<>(3, 3)); // Force 3 players for SushiGo
            }
        }

        gamesAndPlayerCounts = new LinkedHashMap<>();
        for (int i = 0; i < games.size(); i++) {
            GameType game = GameType.valueOf(games.get(i));
            int minPlayers = nPlayers.get(i).a;
            int maxPlayers = nPlayers.get(i).b;

            if (maxPlayers < minPlayers)
                continue;

            int[] playerCounts = new int[maxPlayers - minPlayers + 1];
            Arrays.setAll(playerCounts, n -> n + minPlayers);
            gamesAndPlayerCounts.put(game, playerCounts);
        }
    }
}
