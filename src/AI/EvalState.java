/*
 * EvalState: Interface for data structure containing evaluation information
 * and operations to modify the evaluation by making or undoing a move.
 */

package AI;

import java.awt.Point;
import java.util.LinkedList;

/**
 *
 * @author samuel
 */
abstract class EvalState {

    /* Produce a global evaluation of the game state */
    abstract double evaluate();

    /* Produce PQ of moves ordered by value */
    abstract LinkedList<Move> listMoves();

    /* Modify state by adding a move */
    abstract void move(int x, int y, int player);

    /* Modify state by undoing a move */
    abstract void undo();

    /* A point, value pair */
    protected static class Move implements Comparable<Move> {
        public Point p;
        public double value;
        public Move(Point p, double value) {
            this.p = p;
            this.value = value;
        }
        public int compareTo(Move m) {
            return new Double(value).compareTo(new Double(m.value));
        }
    }

}
