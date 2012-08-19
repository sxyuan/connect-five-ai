/*
 * Overview: Successor of Five AI project. The aim here is to overhaul the move
 * system with a threaded AI, and create this in an applet environment. Better
 * AI, linking evaluation with node expansion, will follow afterwards.
 * Note: much of the game logic is copy/pasted from previous version.
 * 
 * MainPanel: The interface, handling user interaction and drawing to screen.
 */

package Main;

import AI.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import javax.swing.*;

/**
 *
 * @author samuel
 */
public class MainPanel extends JPanel {

    private static final Random RNG = new Random();

    // GUI constants
    private static final int GRID_WIDTH = 30;
    private static final int DIM = 20 * GRID_WIDTH;
    private static final String BLACK_MSG = "Your move";
    private static final String WHITE_MSG = "Dumb AI thinking...";
    private static final Color background = new Color(239, 228, 176);
    private static final Color highlight = new Color(255, 235, 0);
    private final MainFrame reference;

    // Board position constants
    public static final int EMPTY = 0;
    public static final int BLACK = 1;
    public static final int WHITE = -BLACK;
    public static final int[][] DIR = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};

    // Game state variables
    public int[][] board = new int[19][19];
    public int human = BLACK;
    public int turn = BLACK;
    public int lastX = -1, lastY = -1;

    // AI
    private ABEvalAI ai;
    private Thread aiThread;

    /* Initialize the AI */
    public void initAI() {
        ai = new ABEvalAI(this);
        aiThread = new Thread(ai);
        aiThread.start();
    }

    /* Create and initialize the panel */
    public MainPanel(final MainFrame reference) {

        this.reference = reference;
        initAI();

        // Mouse click - user move
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent evt) {
                int x = (evt.getX() - GRID_WIDTH / 2) / GRID_WIDTH,
                    y = (evt.getY() - GRID_WIDTH / 2) / GRID_WIDTH;
                update(x, y, human);
                requestFocusInWindow();
            }
        });

    }

    /* Update game state with move - would be best to have in separate thread,
     * along with game state data; this should do for now, though
     */
    public void update(int x, int y, int player) {

        // Check validity
        if (turn == player && inBoard(x, y) && board[x][y] == EMPTY) {

            // Lock and update
            turn = EMPTY;
            board[x][y] = player;
            lastX = x;
            lastY = y;

            // Check for end game conditions
            int status = checkBoard();
            if (status != EMPTY) {
                repaint();
                ai.end();
                aiThread.interrupt();
                reference.declareWinner(status);
                restart();

            // Roll over turn
            } else {
                turn = -player;
                if (turn == human)
                    ai.printDiagnostic();                                       // Diagnostic
                else
                    aiThread.interrupt();
            }
            repaint();
            
        }

    }

    /* Check if the given point is inside the board */
    public static boolean inBoard(int x, int y) {
        return x >= 0 && x < 19 && y >= 0 && y < 19;
    }

    /* Check board for winner */
    private int checkBoard() {
        for (int i = 0; i < 19; i++) {
            for (int j = 0; j < 19; j++) {
                int p = checkPoint(i, j);
                if (p != EMPTY)
                    return p;
            }
        }
        return EMPTY;
    }

    /* Check point for winner */
    private int checkPoint(int x, int y) {
        if (board[x][y] != EMPTY) {
            int[][] dir = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
            // Loop through directions
            for (int i = 0; i < 4; i++) {
                int num = 1;
                int k = -1;
                while ((x+k*dir[i][0] >= 0) && (x+k*dir[i][0] < 19) &&
                       (y+k*dir[i][1] >= 0) && (y+k*dir[i][1] < 19) &&
                       (board[x+k*dir[i][0]][y+k*dir[i][1]] == board[x][y])) {
                    num++;
                    k--;
                }
                k = 1;
                while ((x+k*dir[i][0] >= 0) && (x+k*dir[i][0] < 19) &&
                       (y+k*dir[i][1] >= 0) && (y+k*dir[i][1] < 19) &&
                       (board[x+k*dir[i][0]][y+k*dir[i][1]] == board[x][y])) {
                    num++;
                    k++;
                }
                if (num >= 5)
                    return board[x][y];
            }
        }
        return EMPTY;
    }

    /* Restart - assumes lock on entry */
    public void restart() {
        initAI();
        board = new int[19][19];
        lastX = lastY = -1;
        human = BLACK;
        turn = BLACK;
        repaint();
    }

     /* Draw and return buffer */
    public BufferedImage buffer() {

        // Create buffer
        BufferedImage buffer = new BufferedImage(DIM, DIM, BufferedImage.TYPE_INT_RGB);
        Graphics2D bGraph = buffer.createGraphics();

        // Prepare to draw to buffer
        bGraph.setBackground(background);
        bGraph.setColor(Color.BLACK);
        bGraph.clearRect(0, 0, DIM, DIM);

        // Draw board - repeated lines down and across, GRID_WIDTH apart
        for (int i = 1; i < 20; i++) {
            bGraph.drawLine(GRID_WIDTH, i * GRID_WIDTH, 19 * GRID_WIDTH, i * GRID_WIDTH);
            bGraph.drawLine(i * GRID_WIDTH, GRID_WIDTH, i * GRID_WIDTH, 19 * GRID_WIDTH);
        }

        // Draw stones - slightly smaller than grid
        for (int i = 0; i < 19; i++) {
            for (int j = 0; j < 19; j++) {
                // Not the prettiest or most efficient, but whatever
                double f = 0.4, hf = 0.5;
                int x = (int)(GRID_WIDTH * (i + 1 - f)),
                    y = (int)(GRID_WIDTH * (j + 1 - f)),
                    w = (int)(GRID_WIDTH * 2 * f),
                    hx = (int)(GRID_WIDTH * (i + 1 - hf)),
                    hy = (int)(GRID_WIDTH * (j + 1 - hf)),
                    hw = (int)(GRID_WIDTH * 2 * hf);
                // Highlight
                if (i == lastX && j == lastY) {
                    bGraph.setColor(highlight);
                    bGraph.fillOval(hx, hy, hw, hw);
                }
                // Stone
                if (board[i][j] != EMPTY) {
                    if (board[i][j] == BLACK) {
                        bGraph.setColor(Color.BLACK);
                        bGraph.fillOval(x, y, w, w);
                    } else if (board[i][j] == WHITE) {
                        bGraph.setColor(Color.WHITE);
                        bGraph.fillOval(x, y, w, w);
                    }
                    bGraph.setColor(Color.BLACK);
                    bGraph.drawOval(x, y, w, w);
                }
            }
        }

        // Draw coords
        for (int i = 0; i < 19; i++) {
            // Copied (with modification) from above
            int x1 = (int)(GRID_WIDTH * (i + 0.9)),
                y1 = (int)(GRID_WIDTH * (0 + 0.6)),
                x2 = (int)(GRID_WIDTH * (0 + 0.4)),
                y2 = (int)(GRID_WIDTH * (i + 1.1));
            bGraph.drawString(new Integer(i).toString(), x1, y1);
            bGraph.drawString(new Integer(i).toString(), x2, y2);
        }

        // Draw prompt
        if (turn == human)
            bGraph.drawString(BLACK_MSG, GRID_WIDTH * 17, GRID_WIDTH * 39 / 2);
        else
            bGraph.drawString(WHITE_MSG, GRID_WIDTH * 15, GRID_WIDTH * 39 / 2);

        // Return buffer
        return buffer;

    }

    /* Custom paint */
    @Override
    public void paint(Graphics sGraph) {
        super.paint(sGraph);
        sGraph.drawImage(buffer(), 0, 0, this);
    }

    /* Size */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(DIM, DIM);
    }

}
