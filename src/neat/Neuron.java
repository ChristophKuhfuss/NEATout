package neat;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by qfi_2 on 25.07.2016.
 */
class Neuron {

    public enum Neuron_Type { SENSOR, HIDDEN, OUTPUT }
    private final int id;
    private static int num_neurons = 0;
    private double output;
    private double input;
    private static final double SIGMOID_STEEPNESS = 2.0;
    private ArrayList<Connection> successors;
    private Neuron_Type type;

    private int depth;

    public Neuron (Neuron_Type type) {
        this.id = num_neurons++;
        this.type = type;
        successors = new ArrayList<Connection>();
    }

    public Neuron(Neuron n) {
        this.id = n.getId();
        this.type = n.getType();
        this.successors = new ArrayList<Connection>();

        for (Connection c : n.successors) {
            this.successors.add(new Connection(c));
        }
    }

    public void calculateOutput() {
        if (this.type == Neuron_Type.SENSOR)
            output = input;
        else
            output = calculateSigmoid(input);
    }

    public double calculateSigmoid(double x) {
        return 1.0 / (1 - Math.exp(-1 * SIGMOID_STEEPNESS * x));
    }

    public double getOutput() {
        return output;
    }

    private void propagateOutputToSuccessors() {
        for (Connection c : successors) {
            if (c.isEnabled())
                c.getOut().addToInput(this.output * c.getWeight());
        }
    }

    private void addToInput(double d) {
        input += d;
    }

    public void setDepth(int i) {
        depth = i;
    }

    public int getDepth() {
        return depth;
    }

    public Neuron_Type getType() {
        return type;
    }

    public int getId() {
        return id;
    }

    public boolean hasSuccessor(Neuron n) {
        for (Connection c : successors) {
            if (c.getOut().equals(n))
                return true;
        }

        return false;
    }

    public void addSuccessor(Connection c) {
        successors.add(c);
    }
}
