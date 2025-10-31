package players.groupz;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.interfaces.IStateHeuristic;
import core.AbstractForwardModel;    // ✅ Use this instead of IForwardModel
import java.util.List;
import java.util.Random;

/**
 * Group Z OSLA Player - One Step Look Ahead agent using a custom heuristic.
 */
public class GroupZOSLAPlayer extends AbstractPlayer {

    private final Random random;
    private final IStateHeuristic heuristic;

    public GroupZOSLAPlayer() {
        this(new Random(), new GroupZSushiGoHeuristics());
    }

    public GroupZOSLAPlayer(Random random, IStateHeuristic heuristic) {
        super(null, "GroupZOSLAPlayer");
        this.random = random;
        this.heuristic = heuristic;
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gs, List<AbstractAction> possibleActions) {
        int playerId = gs.getCurrentPlayer();
        double bestValue = Double.NEGATIVE_INFINITY;
        AbstractAction bestAction = possibleActions.get(0);

        // ✅ Use the Forward Model that the framework injects into the player
        AbstractForwardModel fm = getForwardModel();

        for (AbstractAction action : possibleActions) {
            AbstractGameState gsCopy = gs.copy();

            // Roll the state forward using the forward model
            fm.next(gsCopy, action);

            // Evaluate the resulting state
            double value = heuristic.evaluateState(gsCopy, playerId);

            // Add tiny random noise to break ties
            value += random.nextDouble() * 1e-6;

            if (value > bestValue) {
                bestValue = value;
                bestAction = action;
            }
        }

        return bestAction;
    }

    @Override
    public GroupZOSLAPlayer copy() {
        IStateHeuristic heuristicCopy =
                (heuristic instanceof GroupZSushiGoHeuristics)
                        ? ((GroupZSushiGoHeuristics) heuristic).instantiate()
                        : heuristic;
        return new GroupZOSLAPlayer(new Random(random.nextInt()), heuristicCopy);
    }

    @Override
    public String toString() {
        return "GroupZOSLAPlayer";
    }
}
