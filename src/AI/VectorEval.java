/*
 * VectorEval: Evaluator that keeps track of game state through set(s) of
 * "vectors", i.e. connected lines of stones.
 */

package AI;

import Main.MainPanel;
import java.awt.Point;
import java.util.*;

/**
 *
 * @author samuel
 */
class VectorEval extends EvalState {

    // Evaluation constants
    private static final int MOVE_LIM = 3;
    private static final double MOVE_WINDOW = 0.1;
    private static final double RAND_WINDOW = 0.05;
    private static final Random RNG;
    static final long SEED;

    // Game state information
    private Vector[][][] vecArray = new Vector[19][19][4];
    private LinkedList<Vector> vecList = new LinkedList<Vector>();
    private LinkedList<Point> pastMoves = new LinkedList<Point>();

    // Utility variables
    private boolean utd = false;    // Track whether evaluation is up to date or not
    private double evaluation;
    private LinkedList<Move> moves;
    private double[][] board = new double[19][19];

    static {
        SEED = new Random().nextLong();
//        SEED = -6895470983183486479l;
        RNG = new Random(SEED);
    }

    public void printVectors(int minLength) {
        Iterator<Vector> it = vecList.iterator();
        while (it.hasNext()) {
            Vector v = it.next();
            if (v.length() >= minLength)
                v.print();
        }
    }

    /* Produce a global evaluation of the game state */
    public double evaluate() {
        if (!utd)
            update();
        return evaluation * (1 + (RNG.nextGaussian() - 0.5) / 0.5 * RAND_WINDOW);
    }

    /* Produce list of moves to expand */
    public LinkedList<Move> listMoves() {
        if (!utd)
            update();
        return moves;
    }

    private void update() {

        evaluation = 0;
        moves = new LinkedList<Move>();
        for (int i = 0; i < 19; i++)
            for (int j = 0; j < 19; j++)
                board[i][j] = 0;

        // Loop through list of vectors
        Iterator<Vector> it = vecList.iterator();
        while (it.hasNext()) {

            Vector v = it.next();

            // Get open spaces past endpoints of vector
            Point a, b;
            if (v.a.equals(v.b)) {
                a = new Point(v.a.x + MainPanel.DIR[v.dir][0], v.a.y + MainPanel.DIR[v.dir][1]);
                b = new Point(v.a.x - MainPanel.DIR[v.dir][0], v.a.y - MainPanel.DIR[v.dir][1]);
            } else {
                a = dec(v.a, v.b);
                b = dec(v.b, v.a);
            }
            if (!MainPanel.inBoard(a.x, a.y) || !isEmpty(a.x, a.y))
                a = null;
            if (!MainPanel.inBoard(b.x, b.y) || !isEmpty(b.x, b.y))
                b = null;

            // Both open
            if (a != null && b != null) {
                double eval = evalMove(v.length() + 1, true);
                board[a.x][a.y] += eval;
                board[b.x][b.y] += eval;
                evaluation += v.player * evalMove(v.length(), true);
            // One open
            } else if (a != null) {
                board[a.x][a.y] += evalMove(v.length() + 1, false);
                evaluation += v.player * evalMove(v.length(), false);
            } else if (b != null) {
                board[b.x][b.y] += evalMove(v.length() + 1, false);
                evaluation += v.player * evalMove(v.length(), false);
            } else if (v.length() >= 5)
                evaluation += v.player * Double.POSITIVE_INFINITY;

        }

        // List moves
        double tval = 0;
        for (int i = 0; i < 19; i++)
            for (int j = 0; j < 19; j++) {
                if (board[i][j] != 0 && (moves.size() < MOVE_LIM || board[i][j] >= tval / moves.size() * (1 - MOVE_WINDOW))) {
                    Move m = new Move(new Point(i, j), board[i][j]);
                    if (moves.isEmpty() || board[i][j] >= moves.getFirst().value)
                        moves.addFirst(m);
                    else
                        moves.addLast(m);
                    tval += board[i][j];
                }
            }

        // Insert random move if first move
        if (RNG.nextGaussian() >= pastMoves.size()) {
            int rx, ry;
            do {
                rx = 9 + (int)(RNG.nextGaussian() * 9);
                ry = 9 + (int)(RNG.nextGaussian() * 9);
            } while (!isEmpty(rx, ry));
            moves.addFirst(new Move(new Point(rx, ry), 0));
        }

        utd = true;

    }

    /* Evaluate a move extending a vector of given length */
    private double evalMove(int length, boolean open) {
        if (length > 5)
            length = 5;
        double factor = open ? 1.0 : 0.25;
        return factor / Math.pow(5 - length, 3);
    }

    /* Modify state by adding a move */
    public void move(int x, int y, int player) {

        utd = false;

        if (isEmpty(x, y)) {

            Point p = new Point(x, y);
            pastMoves.addFirst(p);

            // Loop through directions
            for (int dir = 0; dir < 4; dir++) {

                // Look for vectors in adjacent squares along direction of same player
                Vector v1 = null, v2 = null;    // v1 forwards, v2 backwards
                int x1 = x + MainPanel.DIR[dir][0], y1 = y + MainPanel.DIR[dir][1],
                    x2 = x - MainPanel.DIR[dir][0], y2 = y - MainPanel.DIR[dir][1];
                if (MainPanel.inBoard(x1, y1))
                    v1 = vecArray[x1][y1][dir];
                if (MainPanel.inBoard(x2, y2))
                    v2 = vecArray[x2][y2][dir];
                if (v1 != null && v1.player != player)
                    v1 = null;
                if (v2 != null && v2.player != player)
                    v2 = null;

                // Both directions empty - create new vector
                if (v1 == null && v2 == null) {
                    Vector v = new Vector(p, dir, player);
                    vecArray[x][y][dir] = v;
                    vecList.add(v);

                // One direction empty - stretch the other
                } else if (v1 == null) {
                    v2.stretch(p, Vector.FORWARDS);
                    vecArray[x][y][dir] = v2;
                } else if (v2 == null) {
                    v1.stretch(p, Vector.BACKWARDS);
                    vecArray[x][y][dir] = v1;

                // Both directions have vector - join
                } else {
                    Vector v = Vector.join(v2, v1);
                    for (Point pi = v.a; !pi.equals(v.b); pi = inc(pi, v.b))
                        vecArray[pi.x][pi.y][dir] = v;
                    vecArray[v.b.x][v.b.y][dir] = v;
                    vecList.remove(v1);
                    vecList.remove(v2);
                    vecList.add(v);
                }

            }

        }

    }

    /* Modify state by undoing a move */
    public void undo() {

        utd = false;
        Point p = pastMoves.removeFirst();

        // Loop through directions
        for (int dir = 0; dir < 4; dir++) {

            Vector v = vecArray[p.x][p.y][dir];
            vecArray[p.x][p.y][dir] = null;

            // Look for vectors in adjacent squares along direction of same player
            Vector v1 = null, v2 = null;
            int x1 = p.x + MainPanel.DIR[dir][0], y1 = p.y + MainPanel.DIR[dir][1],
                x2 = p.x - MainPanel.DIR[dir][0], y2 = p.y - MainPanel.DIR[dir][1];
            if (MainPanel.inBoard(x1, y1))
                v1 = vecArray[x1][y1][dir];
            if (MainPanel.inBoard(x2, y2))
                v2 = vecArray[x2][y2][dir];
            if (v1 != null && v1.player != v.player)
                v1 = null;
            if (v2 != null && v2.player != v.player)
                v2 = null;

            // Both directions empty - remove point vector
            if (v1 == null && v2 == null) {
                vecList.remove(v);

            // One direction empty - shrink the other
            } else if (v1 == null) {
                v.shrink(p, Vector.FORWARDS);
            } else if (v2 == null) {
                v.shrink(p, Vector.BACKWARDS);

            // Both directions have vector - split the vector
            } else {
                v1 = new Vector(v.a, p, dir, v.player);
                v2 = new Vector(p, v.b, dir, v.player);
                v1.shrink(p, Vector.FORWARDS);
                v2.shrink(p, Vector.BACKWARDS);
                for (Point pi = v1.a; !pi.equals(v1.b); pi = inc(pi, v1.b))
                    vecArray[pi.x][pi.y][dir] = v1;
                vecArray[v1.b.x][v1.b.y][dir] = v1;
                for (Point pi = v2.a; !pi.equals(v2.b); pi = inc(pi, v2.b))
                    vecArray[pi.x][pi.y][dir] = v2;
                vecArray[v2.b.x][v2.b.y][dir] = v2;
                vecList.remove(v);
                vecList.add(v1);
                vecList.add(v2);
            }

        }

    }

    /* Checks if the given point is empty */
    public boolean isEmpty(int x, int y) {
        if (!MainPanel.inBoard(x, y))
            return false;
        else
            for (int i = 0; i < 4; i++)
                if (vecArray[x][y][i] != null)
                    return false;
        return true;
    }

    /* Increment a towards b by 1 */
    protected static Point inc(Point a, Point b) {
        int x = a.x,
            y = a.y;
        if (b.x - a.x != 0)
            x += (b.x - a.x) / Math.abs(b.x - a.x);
        if (b.y - a.y != 0)
            y += (b.y - a.y) / Math.abs(b.y - a.y);
        return new Point(x, y);
    }

    /* Increment a away from b by 1 */
    protected static Point dec(Point a, Point b) {
        int x = a.x,
            y = a.y;
        if (b.x - a.x != 0)
            x -= (b.x - a.x) / Math.abs(b.x - a.x);
        if (b.y - a.y != 0)
            y -= (b.y - a.y) / Math.abs(b.y - a.y);
        return new Point(x, y);
    }

    /* A representation for a connected line of stones */
    private static class Vector {

        /* Definition: forwards implies in the positive directions defined in
         * MainPanel; backwards implies in the negative directions
         */
        public static final int FORWARDS = 1;
        public static final int BACKWARDS = -1;

        public int player;
        public int dir;
        public Point a, b;  // a is backwards of b

        public Vector(Point p, int dir, int player) {
            a = b = p;
            this.dir = dir;
            this.player = player;
        }

        public Vector(Point a, Point b, int dir, int player) {
            this.a = a;
            this.b = b;
            this.dir = dir;
            this.player = player;
        }

        /* Set given endpoint to p */
        public void stretch(Point p, int dir) {
            if (dir == FORWARDS)
                b = p;
            else if (dir == BACKWARDS)
                a = p;
        }

        /* Modify endpoints to exclude given point p - assumes p is in vector */
        public void shrink(Point p, int dir) {
            if (dir == FORWARDS)
                b = inc(p, a);
            else if (dir == BACKWARDS)
                a = inc(p, b);
        }

        public int length() {
            return distance(a, b) + 1;
        }

        public void print() {
            System.out.println(player + ": (" + a.x + "," + a.y + ") to (" + b.x + "," + b.y + ")");
        }

        /* Create a new vector containing the given two vectors - assuming
         * collinear, order of endpoints, same player, etc.
         * Note: v1 must be backwards of v2
         */
        public static Vector join(Vector v1, Vector v2) {
            return new Vector(v1.a, v2.b, v1.dir, v1.player);
        }

        public static int distance(Point a, Point b) {
            return Math.max(Math.abs(a.x - b.x), Math.abs(a.y - b.y));
        }

    }

}
