/*
 * AlphaBetaAI: (Refinement of previous version.)
 * This AI uses a basic heuristic to evaluate unknown positions,
 * along with a simple alpha-beta pruning tree search (really just beta
 * pruning along with the negamax simplification).
 */

package AI;

import Main.MainPanel;
import java.awt.Point;
import java.util.*;

/**
 *
 * @author samuel
 */
public class AlphaBetaAI extends StupidAI {

    protected static final int DEPTH = 4;                                       // Needs to be at least 4

    // Diagnostics
    protected static long totalTime = 0;
    protected static long evalTime = 0;
    protected static long nodeTime = 0;
    protected static long nodes = 1;

    // Transpositions
    Hashtable<Integer,Node> transpositions = new Hashtable<Integer,Node>();
    protected static long[][] hashBase = new long[19][19];
    protected static long totTrans = 0;

    Node root;

    /* Initialize base array for hash */
    static {
        for (int i = 0; i < 19; i++)
            for (int j = 0; j < 19; j++)
                hashBase[i][j] = RNG.nextInt();
    }

    public AlphaBetaAI(MainPanel parent) {
        super(parent);
    }

    /* Update internal state through game state */
    @Override
    public void update() {
        super.update();
        if (root == null) {
            root = new Node(lastX, lastY, upperLeft, lowerRight);
        } else {
            root = root.next.get(new Point(lastX, lastY));
            if (root == null)
                root = new Node(lastX, lastY, upperLeft, lowerRight);
            else {
                root.alpha = Double.NEGATIVE_INFINITY;
                root.beta = Double.POSITIVE_INFINITY;
            }
        }
    }

    /* Time diagnostic */
    public static void printDiagnostic() {
        System.out.println("Total thinking time: " + totalTime + " ms");
        System.out.println("Evaluation time: " + evalTime + " ms");
        System.out.println("Node expansion time: " + nodeTime + " ms");
        System.out.println("Total number of nodes: " + nodes);
        System.out.println("Total number of transpositions: " + totTrans);
        totalTime = 0;
        evalTime = 0;
        nodeTime = 0;
        nodes = 1;
        totTrans = 0;
    }

    /* Node diagnostic */
    protected static void printNode(Node node, int player, int depth) {
        System.out.print(node.x + "," + node.y + " (");
        if (player == MainPanel.WHITE)
            System.out.print("Black to move / ");
        else
            System.out.print("White to move / ");
        System.out.println("depth " + depth + ")");
        System.out.println("  Alpha: " + node.alpha);
        System.out.println("  Beta: " + node.beta);
    }

    /* Line diagnostic */
    protected static void printPrincip(Node node, int player) {
        if (node != null) {
            printNode(node, player, -1);
            printPrincip(follow(node), -player);
        }
    }

    /* Board diagnostic */
    protected static void printBoard(int[][] board) {
        for (int i = 0; i < 18; i++)
            for (int j = 0; j < 18; j++)
                if (board[i][j] != 0)
                    System.out.print(i + "," + j + ": " + board[i][j] + " ");
        System.out.println();
    }

    /* Follow the tree down a random move among the "best" evaluated */
    protected static Node follow(Node node) {
        double min = Double.POSITIVE_INFINITY;
        ArrayList<Node> best = new ArrayList<Node>();
        Enumeration<Node> elems = node.next.elements();
        while (elems.hasMoreElements()) {
            Node cur = elems.nextElement();
            if (cur.alpha < min) {
                min = cur.alpha;
                best.clear();
                best.add(cur);
            } else if (cur.alpha == min)
                best.add(cur);
        }
        if (best.size() > 0) {
            int n = RNG.nextInt(best.size());
            return best.get(n);
        } else
            return null;
    }

    /* Hash for transpositions */
    protected static int hash2D(int[][] board) {
        int hash = 0;
        for (int i = 0; i < 19; i++)
            for (int j = 0; j < 19; j++)
                hash += board[i][j] * hashBase[i][j];
        return hash;
    }

    /* Evaluate the given point in the given direction */
    protected static double evalDir(int[][] board, int x, int y, int dx, int dy) {
        if (board[x][y] == 0)
            return 0;
        else {
            int i;
            for (i = 1; i < 5 && MainPanel.inBoard(x+i*dx, y+i*dy) && board[x+i*dx][y+i*dy] == board[x][y]; i++);
            return board[x][y] / (double)(5 - i);
        }
    }

    /* Evaluate the given point */
    protected static double evalPoint(int[][] board, int x, int y) {
        double status = 0;
        for (int i = 0; i < 4; i++)
            status += evalDir(board, x, y, MainPanel.DIR[i][0], MainPanel.DIR[i][1]);
        return status;
    }

    /* Evaluate the board */
    protected double evalBoard(int[][] board, Point ul, Point lr) {
        long st = System.currentTimeMillis();   // Diagnostic
        double status = 0;
        for (int i = ul.x; i <= lr.x; i++)
            for (int j = ul.y; j <= lr.y; j++)
                status += evalPoint(board, i, j);
        evalTime += System.currentTimeMillis() - st;
        return status;
    }

    /* Minimax search with alpha-beta pruning and negamax simplification */
    protected void alphabeta(int[][] board, Node node, int player, int depth) {

        double heuristic = player * evalBoard(board, node.upperLeft, node.lowerRight);

        // Check in transposition table
        if (transpositions.containsKey(hash2D(board))) {
            Node tn = transpositions.get(hash2D(board));
            node.alpha = tn.alpha;
            node.beta = tn.beta;
            totTrans++;
        } else {

            // Leaf is reached
            if (depth == 0 || Math.abs(heuristic) == Double.POSITIVE_INFINITY) {
                node.alpha = node.beta = heuristic;

            // Full depth not reached - keep expanding
            } else {

                boolean cutoff = false;

                // Loop through all nodes within window
                for (int i = node.upperLeft.x - BUFFER; i <= node.lowerRight.x + BUFFER && !cutoff; i++)
                    for (int j = node.upperLeft.y - BUFFER; j <= node.lowerRight.y + BUFFER && !cutoff; j++)
                        if (MainPanel.inBoard(i, j) && board[i][j] == 0) {

                            long st = System.currentTimeMillis();               // Diagnostic

                            // Get the next node or create a new one
                            Node nn = node.next.get(new Point(i, j));
                            if (nn == null) {
                                nn = new Node(node, i, j);
                                node.next.put(new Point(i, j), nn);
                            }
                            nn.alpha = -node.beta;
                            nn.beta = -node.alpha;
                            nodes++;                                            // Diagnostic

                            nodeTime += System.currentTimeMillis() - st;        // Diagnostic

                            // Modify board position and recurse
                            board[i][j] = player;
                            alphabeta(board, nn, -player, depth - 1);
                            board[i][j] = 0;

                            // Update and check for cutoff
                            node.alpha = Math.max(node.alpha, -nn.alpha);
                            if (node.alpha >= node.beta) {
                                cutoff = true;
                                node.alpha = Double.POSITIVE_INFINITY;
                            }

                        }

            }

            transpositions.put(hash2D(board), node);

        }

    }

    /* Produce next move */
    @Override
    public Point getMove() {

        long st = System.currentTimeMillis();                                   // Diagnostic

        alphabeta(board, root, MainPanel.WHITE, DEPTH);
        root = follow(root);
        transpositions.clear();

        totalTime += System.currentTimeMillis() - st;                           // Diagnostic

        return new Point(root.x, root.y);

    }

    /* A node in the game tree, representing a single move (independent of the
     * game state, to save space)
     */
    protected static class Node {

        int x, y;
        double alpha = Double.NEGATIVE_INFINITY, beta = Double.POSITIVE_INFINITY;
        Point upperLeft, lowerRight;
        Hashtable<Point,Node> next = new Hashtable<Point,Node>();

        private void init(int x, int y, Point ul, Point lr) {
            this.x = x;
            this.y = y;
            upperLeft = (Point)ul.clone();
            lowerRight = (Point)lr.clone();
        }

        protected Node(int x, int y, Point ul, Point lr) {
            init(x, y, ul, lr);
        }

        protected Node(Node parent, int x, int y) {
            init(x, y, parent.upperLeft, parent.lowerRight);
            stretch(upperLeft, lowerRight, x, y);
        }

    }

}
