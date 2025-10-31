//package players.groupz;
//
//// Core classes from the game framework
//import core.AbstractGameState;
//import core.AbstractPlayer;
//import core.actions.AbstractAction;
//import core.interfaces.IStateHeuristic;
//
//import java.util.List;
//import java.util.Random;
//
//// Utility for adding small noise to scores
//import static utilities.Utils.noise;
//
//public class GroupZOSLAMinimaxPlayer extends AbstractPlayer {
//    // --- Member variables ---
//
//    private IStateHeuristic heuristic;  // Heuristic used to evaluate game states
//    private int maxDepth = 1;           // Maximum depth of lookahead (1 = classic OSLA)
//
//    // --- Constructors ---
//
//    public GroupZOSLAMinimaxPlayer(Random random) {
//        super(null, "SuperOSLA");       // Call superclass constructor
//        this.rnd = random;              // Set the random generator
//    }
//
//    public GroupZOSLAMinimaxPlayer() {
//        this(new Random());             // Default constructor: uses a new Random
//    }
//
//    public GroupZOSLAMinimaxPlayer(IStateHeuristic heuristic) {
//        this(heuristic, 1, new Random()); // OSLA with heuristic and default 1-step lookahead
//    }
//
//    public GroupZOSLAMinimaxPlayer(IStateHeuristic heuristic, int maxDepth, Random random) {
//        this(random);                    // Call basic constructor
//        this.heuristic = heuristic;      // Set the heuristic function
//        this.maxDepth = Math.max(1, maxDepth); // Ensure depth is at least 1
//        setName("OSLA");                 // Set player name
//    }
//
//    // --- Main method to get action ---
//    @Override
//    public AbstractAction _getAction(AbstractGameState gs, List<AbstractAction> actions) {
//        if (maxDepth == 1) {
//            // ------------------------
//            // OSLA behavior (1-step lookahead)
//            // ------------------------
//            return oneStepLookAhead(gs, actions);
//        } else {
//            // ------------------------
//            // Minimax behavior with alpha-beta pruning
//            // ------------------------
//            return minimaxAction(gs, actions, maxDepth);
//        }
//    }
//
//    // ------------------------
//    // OSLA: One-step lookahead
//    // ------------------------
//    private AbstractAction oneStepLookAhead(AbstractGameState gs, List<AbstractAction> actions) {
//        double maxQ = Double.NEGATIVE_INFINITY;  // Track best heuristic value
//        AbstractAction bestAction = null;        // Track best action
//        int playerID = gs.getCurrentPlayer();    // Get our player ID
//
//        for (AbstractAction action : actions) {
//            AbstractGameState gsCopy = gs.copy();           // Copy game state
//            getForwardModel().next(gsCopy, action);        // Apply action to copied state
//
//            // Evaluate the resulting state using heuristic (OSLA core)
//            double val = heuristic != null ? heuristic.evaluateState(gsCopy, playerID)
//                    : gsCopy.getHeuristicScore(playerID);
//
//            // Optional small noise to break ties or add stochasticity
//            double Q = noise(val, getParameters().noiseEpsilon, rnd.nextDouble());
//
//            // Update best action if value is higher
//            if (Q > maxQ || bestAction == null) {
//                maxQ = Q;
//                bestAction = action;
//            }
//        }
//        return bestAction; // Return best OSLA action
//    }
//
//    // ------------------------
//    // Minimax: Action selection for deeper lookahead
//    // ------------------------
//    private AbstractAction minimaxAction(AbstractGameState gs, List<AbstractAction> actions, int depth) {
//        double bestValue = Double.NEGATIVE_INFINITY;   // Best value found
//        AbstractAction bestAction = null;              // Best action
//        int rootPlayer = gs.getCurrentPlayer();       // Player we are maximizing for
//
//        for (AbstractAction action : actions) {
//            AbstractGameState nextState = gs.copy();        // Copy state
//            getForwardModel().next(nextState, action);     // Apply action
//
//            // Call recursive minimax with alpha-beta pruning
//            double value = minimax(nextState, depth - 1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, rootPlayer);
//
//            // Add optional noise for stochasticity
//            value = noise(value, getParameters().noiseEpsilon, rnd.nextDouble());
//
//            // Update best action
//            if (value > bestValue || bestAction == null) {
//                bestValue = value;
//                bestAction = action;
//            }
//        }
//        return bestAction;
//    }
//
//    // ------------------------
//    // Minimax recursive function with alpha-beta pruning
//    // ------------------------
//    private double minimax(AbstractGameState state, int depth, double alpha, double beta, int rootPlayer) {
//
//        // ------------------------
//        // Terminal condition or depth limit
//        // ------------------------
//        if (depth == 0 || state.isTerminal()) {
//            // Evaluate state using heuristic
//            return heuristic != null ? heuristic.evaluateState(state, rootPlayer)
//                    : state.getHeuristicScore(rootPlayer);
//        }
//
//        int currentPlayer = state.getCurrentPlayer();      // Current player to move
//        List<AbstractAction> actions = state.getActions(currentPlayer);
//
//        // If no actions are available, treat as leaf
//        if (actions.isEmpty()) {
//            return heuristic != null ? heuristic.evaluateState(state, rootPlayer)
//                    : state.getHeuristicScore(rootPlayer);
//        }
//
//        // ------------------------
//        // Maximizing player branch
//        // ------------------------
//        if (currentPlayer == rootPlayer) {
//            double maxEval = Double.NEGATIVE_INFINITY;
//            for (AbstractAction action : actions) {
//                AbstractGameState nextState = state.copy();
//                getForwardModel().next(nextState, action);
//
//                // Recursive call (switch to minimizing opponent)
//                double eval = minimax(nextState, depth - 1, alpha, beta, rootPlayer);
//
//                maxEval = Math.max(maxEval, eval);
//                alpha = Math.max(alpha, eval);  // Update alpha (best for maximizer)
//
//                if (beta <= alpha) break;       // ---------- Alpha-beta pruning ----------
//            }
//            return maxEval;
//        }
//        // ------------------------
//        // Minimizing opponent branch
//        // ------------------------
//        else {
//            double minEval = Double.POSITIVE_INFINITY;
//            for (AbstractAction action : actions) {
//                AbstractGameState nextState = state.copy();
//                getForwardModel().next(nextState, action);
//
//                // Recursive call (maximize after opponent)
//                double eval = minimax(nextState, depth - 1, alpha, beta, rootPlayer);
//
//                minEval = Math.min(minEval, eval);
//                beta = Math.min(beta, eval);    // Update beta (best for minimizer)
//
//                if (beta <= alpha) break;       // ---------- Alpha-beta pruning ----------
//            }
//            return minEval;
//        }
//    }
//
//    // ------------------------
//    // Copy method
//    // ------------------------
//    @Override
//    public GroupZOSLAMinimaxPlayer copy() {
//        GroupZOSLAMinimaxPlayer copy = new GroupZOSLAMinimaxPlayer(heuristic, maxDepth, new Random(rnd.nextInt()));
//        copy.setForwardModel(getForwardModel());
//        return copy;
//    }
//}
