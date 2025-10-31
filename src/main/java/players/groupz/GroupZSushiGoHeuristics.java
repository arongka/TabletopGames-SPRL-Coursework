package players.groupz;

import core.AbstractGameState;
import core.CoreConstants;
import core.components.Counter;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;
import games.sushigo.SGGameState;
import games.sushigo.SGParameters;
import games.sushigo.cards.SGCard.SGCardType;

/**
 * Tunable heuristic for SushiGo (Group Z).
 * Corrected so helper methods return values (primitives can't be modified by reference).
 */
public class GroupZSushiGoHeuristics extends TunableParameters implements IStateHeuristic {

    // Base score factors
    double FACTOR_CURRENT_SCORE = 1.0;
    double FACTOR_POTENTIAL_SCORE = 0.8;

    // Set completion factors
    double FACTOR_TEMPURA_SET = 0.7;
    double FACTOR_SASHIMI_SET = 0.7;
    double FACTOR_DUMPLING_SET = 0.6;

    // Special card factors
    double FACTOR_WASABI_VALUE = 0.5;
    double FACTOR_MAKI_POSITION = 0.6;
    double FACTOR_PUDDING_POSITION = 0.5;
    double FACTOR_CHOPSTICKS_VALUE = 0.3;

    public GroupZSushiGoHeuristics() {
        addTunableParameter("FACTOR_CURRENT_SCORE", FACTOR_CURRENT_SCORE);
        addTunableParameter("FACTOR_POTENTIAL_SCORE", FACTOR_POTENTIAL_SCORE);
        addTunableParameter("FACTOR_TEMPURA_SET", FACTOR_TEMPURA_SET);
        addTunableParameter("FACTOR_SASHIMI_SET", FACTOR_SASHIMI_SET);
        addTunableParameter("FACTOR_DUMPLING_SET", FACTOR_DUMPLING_SET);
        addTunableParameter("FACTOR_WASABI_VALUE", FACTOR_WASABI_VALUE);
        addTunableParameter("FACTOR_MAKI_POSITION", FACTOR_MAKI_POSITION);
        addTunableParameter("FACTOR_PUDDING_POSITION", FACTOR_PUDDING_POSITION);
        addTunableParameter("FACTOR_CHOPSTICKS_VALUE", FACTOR_CHOPSTICKS_VALUE);
    }

    @Override
    public void _reset() {
        FACTOR_CURRENT_SCORE = (double) getParameterValue("FACTOR_CURRENT_SCORE");
        FACTOR_POTENTIAL_SCORE = (double) getParameterValue("FACTOR_POTENTIAL_SCORE");
        FACTOR_TEMPURA_SET = (double) getParameterValue("FACTOR_TEMPURA_SET");
        FACTOR_SASHIMI_SET = (double) getParameterValue("FACTOR_SASHIMI_SET");
        FACTOR_DUMPLING_SET = (double) getParameterValue("FACTOR_DUMPLING_SET");
        FACTOR_WASABI_VALUE = (double) getParameterValue("FACTOR_WASABI_VALUE");
        FACTOR_MAKI_POSITION = (double) getParameterValue("FACTOR_MAKI_POSITION");
        FACTOR_PUDDING_POSITION = (double) getParameterValue("FACTOR_PUDDING_POSITION");
        FACTOR_CHOPSTICKS_VALUE = (double) getParameterValue("FACTOR_CHOPSTICKS_VALUE");
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        SGGameState state = (SGGameState) gs;
        SGParameters params = (SGParameters) state.getGameParameters();

        // First check win/lose conditions and modify base score
        double score = state.getGameScore(playerId);
        if (gs.getPlayerResults()[playerId] == CoreConstants.GameResult.WIN_GAME)
            return score * 1.5;
        if (gs.getPlayerResults()[playerId] == CoreConstants.GameResult.LOSE_GAME)
            return score * 0.5;

        // Base score from current game state
        score *= FACTOR_CURRENT_SCORE;

        // Evaluate current board position
        Counter[] scores = state.getPlayerScore();
        double maxOppScore = 0;
        for (int i = 0; i < state.getNPlayers(); i++) {
            if (i != playerId) {
                maxOppScore = Math.max(maxOppScore, scores[i].getValue());
            }
        }

        // Add relative position to score
        score += (scores[playerId].getValue() - maxOppScore) * FACTOR_CURRENT_SCORE;

        // Evaluate sets in progress (returning added value)
        score += evaluateSetsInProgress(state, playerId, params);

        // Special card evaluations
        score += evaluateSpecialCards(state, playerId, params);

        return score;
    }

    private double evaluateSetsInProgress(SGGameState state, int playerId, SGParameters params) {
        double add = 0.0;

        // Tempura sets
        int tempuraCount = state.getPlayedCardTypes(SGCardType.Tempura, playerId).getValue();
        add += (tempuraCount / 2) * params.valueTempuraPair * FACTOR_TEMPURA_SET;
        if (tempuraCount % 2 == 1) {
            add += params.valueTempuraPair * FACTOR_TEMPURA_SET * 0.3; // Partial set
        }

        // Sashimi sets
        int sashimiCount = state.getPlayedCardTypes(SGCardType.Sashimi, playerId).getValue();
        add += (sashimiCount / 3) * params.valueSashimiTriple * FACTOR_SASHIMI_SET;
        if (sashimiCount % 3 > 0) {
            add += params.valueSashimiTriple * FACTOR_SASHIMI_SET * (sashimiCount % 3) * 0.2; // Partial set
        }

        // Dumpling value
        int dumplingCount = state.getPlayedCardTypes(SGCardType.Dumpling, playerId).getValue();
        if (dumplingCount < params.valueDumpling.length) {
            add += params.valueDumpling[dumplingCount] * FACTOR_DUMPLING_SET;
        } else {
            // if more dumplings than array size, use last element as approximation
            add += params.valueDumpling[params.valueDumpling.length - 1] * FACTOR_DUMPLING_SET;
        }

        return add;
    }

    private double evaluateSpecialCards(SGGameState state, int playerId, SGParameters params) {
        double specialScore = 0.0;

        // Wasabi value (unused wasabi)
        int wasabiCount = state.getPlayedCardTypes(SGCardType.Wasabi, playerId).getValue();
        int nigiriCount = state.getPlayedCardTypes(SGCardType.SquidNigiri, playerId).getValue()
                + state.getPlayedCardTypes(SGCardType.SalmonNigiri, playerId).getValue()
                + state.getPlayedCardTypes(SGCardType.EggNigiri, playerId).getValue();
        if (wasabiCount > nigiriCount) {
            specialScore += params.multiplierWasabi * FACTOR_WASABI_VALUE;
        }

        // Maki competition
        int playerMaki = state.getPlayedCardTypes(SGCardType.Maki, playerId).getValue();
        int maxOpponentMaki = 0;
        for (int i = 0; i < state.getNPlayers(); i++) {
            if (i != playerId) {
                maxOpponentMaki = Math.max(maxOpponentMaki, state.getPlayedCardTypes(SGCardType.Maki, i).getValue());
            }
        }
        if (playerMaki >= maxOpponentMaki) {
            specialScore += params.valueMakiMost * FACTOR_MAKI_POSITION;
        } else if (playerMaki == maxOpponentMaki - 1) {
            specialScore += params.valueMakiSecond * FACTOR_MAKI_POSITION;
        }

        // Pudding position (last round)
        if (state.getRoundCounter() == params.nRounds - 1) {
            int playerPudding = state.getPlayedCardTypesAllGame()[playerId].get(SGCardType.Pudding).getValue();
            int maxPudding = Integer.MIN_VALUE, minPudding = Integer.MAX_VALUE;
            for (int i = 0; i < state.getNPlayers(); i++) {
                int oppP = state.getPlayedCardTypesAllGame()[i].get(SGCardType.Pudding).getValue();
                maxPudding = Math.max(maxPudding, oppP);
                minPudding = Math.min(minPudding, oppP);
            }
            if (playerPudding > maxPudding) {
                specialScore += params.valuePuddingMost * FACTOR_PUDDING_POSITION;
            } else if (playerPudding < minPudding) {
                specialScore += params.valuePuddingLeast * FACTOR_PUDDING_POSITION;
            }
        }

        // Chopsticks value
        if (state.getPlayedCardTypes(SGCardType.Chopsticks, playerId).getValue() > 0) {
            specialScore += 5 * FACTOR_CHOPSTICKS_VALUE; // Potential double play
        }

        return specialScore;
    }

    @Override
    protected GroupZSushiGoHeuristics _copy() {
        GroupZSushiGoHeuristics copy = new GroupZSushiGoHeuristics();
        copy.FACTOR_CURRENT_SCORE = FACTOR_CURRENT_SCORE;
        copy.FACTOR_POTENTIAL_SCORE = FACTOR_POTENTIAL_SCORE;
        copy.FACTOR_TEMPURA_SET = FACTOR_TEMPURA_SET;
        copy.FACTOR_SASHIMI_SET = FACTOR_SASHIMI_SET;
        copy.FACTOR_DUMPLING_SET = FACTOR_DUMPLING_SET;
        copy.FACTOR_WASABI_VALUE = FACTOR_WASABI_VALUE;
        copy.FACTOR_MAKI_POSITION = FACTOR_MAKI_POSITION;
        copy.FACTOR_PUDDING_POSITION = FACTOR_PUDDING_POSITION;
        copy.FACTOR_CHOPSTICKS_VALUE = FACTOR_CHOPSTICKS_VALUE;
        return copy;
    }

    @Override
    protected boolean _equals(Object o) {
        if (o instanceof GroupZSushiGoHeuristics) {
            GroupZSushiGoHeuristics other = (GroupZSushiGoHeuristics) o;
            return other.FACTOR_CURRENT_SCORE == FACTOR_CURRENT_SCORE &&
                    other.FACTOR_POTENTIAL_SCORE == FACTOR_POTENTIAL_SCORE &&
                    other.FACTOR_TEMPURA_SET == FACTOR_TEMPURA_SET &&
                    other.FACTOR_SASHIMI_SET == FACTOR_SASHIMI_SET &&
                    other.FACTOR_DUMPLING_SET == FACTOR_DUMPLING_SET &&
                    other.FACTOR_WASABI_VALUE == FACTOR_WASABI_VALUE &&
                    other.FACTOR_MAKI_POSITION == FACTOR_MAKI_POSITION &&
                    other.FACTOR_PUDDING_POSITION == FACTOR_PUDDING_POSITION &&
                    other.FACTOR_CHOPSTICKS_VALUE == FACTOR_CHOPSTICKS_VALUE;
        }
        return false;
    }

    @Override
    public GroupZSushiGoHeuristics instantiate() {
        return this._copy();
    }
}
