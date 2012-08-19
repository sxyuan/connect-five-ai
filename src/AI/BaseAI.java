/*
 * BasAI: Interface for a simple AI.
 */

package AI;

import Main.MainPanel;
import java.awt.Point;

/**
 *
 * @author samuel
 */
public abstract class BaseAI implements Runnable {

    protected static final long SLEEP = 1000;

    protected final MainPanel parent;
    protected boolean end = false;

    public BaseAI(MainPanel parent) {
        this.parent = parent;
    }

    /* Signal end of game */
    public void end() {
        end = true;
    }

    /* AI thread logic: produce move and update if on turn, otherwise sleep */
    public void run() {
        // Loop while game is unfinished, i.e. until notified by the game
        while (!end) {
            if (!end) {
                // On turn - AI logic
                if (parent.turn != parent.human) {
                    update();
                    Point move = getMove();
                    update(move.x, move.y);
                    parent.update(move.x, move.y, -parent.human);

                // Off turn - sleep
                } else {
                    try {
                        Thread.sleep(SLEEP);
                    } catch (InterruptedException e) {}
                }
            }
        }

        die();
    }

    /* Update internal state */
    public abstract void update();

    /* Update internal state through own move */
    public abstract void update(int x, int y);

    /* Produce next move */
    public abstract Point getMove();

    /* The AI's last words */
    void die() {}

}
