/*
 * ABEvalAI: Improvement of AlphaBetaAI. Uses a better heuristic and couples
 * this with the node expansion logic to reduce the exponential explosion
 * (somewhat).
 */

package AI;

import Main.MainPanel;
import java.awt.Point;
import java.util.*;

/**
 *
 * @author samuel
 */
public class ABEvalAI extends AlphaBetaAI {

    protected static final boolean DIAG = true;
    protected static final boolean T_DIAG = true;
    protected static final boolean PV_DIAG = false;

    protected static final int N_DEPTH = 4;
    private VectorEval eval = new VectorEval();

    static {
        long seed = new Random().nextLong();
        RNG.setSeed(seed);
        if (DIAG) System.out.println("AI seed: " + seed);
        if (DIAG) System.out.println("Evaluation seed: " + VectorEval.SEED);
    }

    public ABEvalAI(MainPanel parent) {
        super(parent);
    }

    /* Update internal state through game state */
    @Override
    public void update() {
        super.update();
        long st = System.currentTimeMillis();
        eval.move(lastX, lastY, MainPanel.BLACK);
        evalTime += System.currentTimeMillis() - st;
        if (DIAG) {
            System.out.println();
            System.out.println("Black: " + lastX + "," + lastY + " (" + eval.evaluate() + ")");
        }
    }

    /* Time diagnostic */
    public static void printDiagnostic() {
        System.out.println("Eval / kn: " + 1000.0 * AlphaBetaAI.evalTime / AlphaBetaAI.nodes + " ms");
        if (T_DIAG) AlphaBetaAI.printDiagnostic();
    }

    /* Minimax search with alpha-beta pruning and negamax simplification */
    private void alphabeta(EvalState eval, Node node, int player, int depth) {

        long st = System.currentTimeMillis();                                   // Diagnostic
        double heuristic = player * eval.evaluate();
        evalTime += System.currentTimeMillis() - st;                            // Diagnostic

        // Leaf is reached
        if (depth == 0 || Double.isInfinite(heuristic)) {
            node.alpha = node.beta = heuristic;

        // Full depth not reached - keep expanding
        } else {

            boolean cutoff = false;

            st = System.currentTimeMillis();                                    // Diagnostic
            LinkedList<EvalState.Move> ml = eval.listMoves();
            evalTime += System.currentTimeMillis() - st;                        // Diagnostic

            // Loop through all given moves
            Iterator<EvalState.Move> it = ml.iterator();
            EvalState.Move m;
            while(it.hasNext()) {
                
                m = it.next();

                st = System.currentTimeMillis();                                // Diagnostic

                // Get the next node or create a new one
                Node nn = node.next.get(m.p);
                if (nn == null) {
                    nn = new Node(node, m.p.x, m.p.y);
                    node.next.put(m.p, nn);
                }
                nn.alpha = -node.beta;
                nn.beta = -node.alpha;
                nodes++;                                                        // Diagnostic

                nodeTime += System.currentTimeMillis() - st;                    // Diagnostic

                // Modify board position and recurse
                st = System.currentTimeMillis();                                // Diagnostic
                eval.move(m.p.x, m.p.y, player);
                evalTime += System.currentTimeMillis() - st;                    // Diagnostic

                alphabeta(eval, nn, -player, depth - 1);

                st = System.currentTimeMillis();                                // Diagnostic
                eval.undo();
                evalTime += System.currentTimeMillis() - st;                    // Diagnostic

                // Update and check for cutoff
                node.alpha = Math.max(node.alpha, -nn.alpha);
                if (node.alpha >= node.beta) {
                    cutoff = true;
                    node.alpha = Double.POSITIVE_INFINITY;
                }

            }

        }

    }

    /* Produce next move */
    @Override
    public Point getMove() {

        long st = System.currentTimeMillis();                                   // Diagnostic

        alphabeta(eval, root, MainPanel.WHITE, N_DEPTH);
        root = follow(root);
        long st2 = System.currentTimeMillis();                                  // Diagnostic
        eval.move(root.x, root.y, MainPanel.WHITE);
        evalTime += System.currentTimeMillis() - st2;                           // Diagnostic
        if (DIAG) {
            System.out.println("White: " + root.x + "," + root.y + " (" + eval.evaluate() + ")");
            System.out.println();
        }
        if (PV_DIAG) printPrincip(root, MainPanel.WHITE);

        totalTime += System.currentTimeMillis() - st;                           // Diagnostic

        return new Point(root.x, root.y);

    }

}
