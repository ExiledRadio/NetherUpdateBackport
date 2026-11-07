package com.exiledradio.netherupdatebackport.piglin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 1.16's {@code FirstShuffledTask}: a few weighted behaviors, tried in a freshly shuffled order (heavier ones
 * likelier first) until one starts. A behavior that starts runs for a tick, so the group shuffles again two ticks
 * later; the "do nothing" entry ({@code DummyTask}) holds the group for its own random time.
 *
 * @param <P> the mob it runs for
 */
final class Shuffled<P extends AbstractPiglin> {

    /** One entry: tries to start; returns false when it cannot. */
    interface Behavior<P> {
        boolean start(P mob);
    }

    private final List<Behavior<P>> behaviors = new ArrayList<Behavior<P>>();
    private final List<Integer> weights = new ArrayList<Integer>();
    private final List<int[]> waits = new ArrayList<int[]>();
    private int busy;

    Shuffled<P> add(int weight, Behavior<P> behavior) {
        this.behaviors.add(behavior);
        this.weights.add(weight);
        this.waits.add(null);
        return this;
    }

    /** Adds {@code DummyTask(min, max)}: stand idle for min to max ticks. */
    Shuffled<P> idle(int weight, int min, int max) {
        this.behaviors.add(null);
        this.weights.add(weight);
        this.waits.add(new int[]{min, max});
        return this;
    }

    void run(P mob) {
        if (this.busy > 0) {
            this.busy--;
            return;
        }
        Random random = mob.getRNG();
        // 1.16's WeightedList.shuffle: each entry keyed by -random^(1/weight), sorted.
        List<double[]> order = new ArrayList<double[]>();
        for (int i = 0; i < this.behaviors.size(); i++) {
            order.add(new double[]{-Math.pow(random.nextFloat(), 1.0F / this.weights.get(i)), i});
        }
        Collections.sort(order, (a, b) -> Double.compare(a[0], b[0]));
        for (double[] entry : order) {
            int i = (int) entry[1];
            int[] wait = this.waits.get(i);
            if (wait != null) {
                this.busy = wait[0] + random.nextInt(wait[1] - wait[0] + 1);
                return;
            }
            if (this.behaviors.get(i).start(mob)) {
                this.busy = 1;
                return;
            }
        }
    }
}
