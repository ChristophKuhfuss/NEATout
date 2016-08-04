package Game.UserInterface;

import Game.Breakout.GameStats;
import Game.neat.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.text.NumberFormat;
import java.util.*;

/**
 * Created by qfi_2 on 27.07.2016.
 */
public class NEATDiagnostics extends JFrame implements Observer {

    private static final String TO_BE_DETERMINED = "TBD";
    private JTabbedPane networkPopulationTabbedPane;
    private JPanel populationPane;
    private JPanel networkDetailPane;
    private JLabel networkCountTextLabel;
    private JLabel generationCountTextLabel;
    private JLabel maxFitnessTextLabel;
    private JTable networkTable;
    private JPanel mainPane;
    private JLabel generationLabel;
    private JLabel scoreLabel;
    private JLabel livesLabel;
    private JLabel levelLabel;
    private JLabel maxFitnessLabel;
    private JLabel networkCountLabel;
    private JLabel shotsLabel;
    private JLabel currentNetLabelId;
    private JLabel networkDetailIDLabel;
    private JLabel networkDetailConnectionCountLabel;
    private JPanel networkDetailInputPanel;
    private JLabel networkDetailHiddenNodeCount;
    private JLabel networkDetailLayerCount;
    private JLabel networkDetailBallInput;
    private JLabel networkDetailPaddleInput;
    private JLabel networkDetailLeftOutput;
    private JLabel networkDetailRightOutput;
    private JComboBox networkDetailComboBox;
    private JButton networkDetaiLoadCurrent;
    private JButton networkDetailLoadID;
    private JToolBar toolBar;
    private JButton playButton;
    private JButton pauseButton;
    private JButton newPopulationButton;
    private JButton restartGenerationButton;
    private JPanel statisticsPane;
    private JTable statisticsTable;
    private JFileChooser fileChooser;
    private DefaultComboBoxModel comboModel;
    private DefaultTableModel currentPopulationTableModel;
    private DefaultTableModel statisticsTableModel;
    private Simulation simulation;
    private Population population;
    private GameStats gameStats;
    private Genome curDetailGenome;
    private boolean showCurrent;
    private String[] networkTableColumnNames = {"ID", "SpeciesID", "# Nodes", "#Connections (#Active)", "Fitness", "SharedFitness"};
    private String[] historyTableColumnNames = {"Gen. ID", "Average Fitness", "Top Fitness"};

    public NEATDiagnostics() {
        super("NEAT Diagnostics");

        simulation = new Simulation(GameStats.getInstance());
        population = simulation.getPopulation();
        gameStats = gameStats.getInstance();

        showCurrent = true;

        fileChooser = new JFileChooser();

        JMenuBar menubar = new JMenuBar();
        JMenu file = new JMenu("File");

        JMenuItem item = new JMenuItem("Save");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    serializeSim();
                } catch (IOException exception) {
                    System.out.println("Error during sim serialization");
                    exception.printStackTrace();
                }
            }
        });

        file.add(item);

        item = new JMenuItem("Save as...");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int returnVal = fileChooser.showSaveDialog(NEATDiagnostics.this);

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();

                    try {
                        if (!file.exists()) {
                            file.createNewFile();
                        }

                        serializeSim(file);
                    } catch (IOException exception) {
                        System.out.println("Error during sim serialization");
                        exception.printStackTrace();
                    }
                }
            }
        });

        file.add(item);

        item = new JMenuItem("Load population from file...");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fileChooser.showOpenDialog(NEATDiagnostics.this);
                try {
                    Simulation sim = loadSim(fileChooser.getSelectedFile());
                    setSimulation(sim);
                    updateUIComponents();
                } catch (Exception exception) {

                }
            }
        });

        file.add(item);

        item = new JMenuItem("Exit");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //TODO: Exit & cleanup
            }
        });

        file.add(item);
        menubar.add(file);
        setJMenuBar(menubar);
        setResizable(false);
        $$$setupUI$$$();

        networkDetailLoadID.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int id = Integer.parseInt((String) comboModel.getSelectedItem());
                Genome g = simulation.getPopulation().getGenomeById(id);

                if (g != null) {
                    curDetailGenome = g;
                    updateLabels();
                    showCurrent = false;
                }
            }
        });

        networkDetaiLoadCurrent.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                curDetailGenome = simulation.getCurrent().getGenome();
                updateLabels();
                showCurrent = true;
            }
        });

        addButtonActionListeners();

        gameStats.pauseGame();
        pauseButton.setEnabled(false);
        playButton.setEnabled(true);

        setContentPane(mainPane);
        mainPane.setPreferredSize(new Dimension(960, 480));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        pack();


        fillTables();
        updateLabels();

        gameStats.addObserver(this);
        simulation.addObserver(this);

        validate();
        setVisible(true);
    }

    private void addButtonActionListeners() {
        playButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (gameStats.isGamePaused()) {
                    gameStats.unPauseGame();
                    playButton.setEnabled(false);
                    pauseButton.setEnabled(true);
                }
            }
        });

        pauseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!gameStats.isGamePaused()) {
                    gameStats.pauseGame();
                    playButton.setEnabled(true);
                    pauseButton.setEnabled(false);
                }
            }
        });

        restartGenerationButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gameStats.resetGame();
                gameStats.pauseGame();
                playButton.setEnabled(true);
                pauseButton.setEnabled(false);

                simulation.resetGeneration();

                updateUIComponents();
            }
        });

        newPopulationButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gameStats.resetGame();
                gameStats.pauseGame();
                playButton.setEnabled(true);
                pauseButton.setEnabled(false);
                simulation.resetPopulation();
                population = simulation.getPopulation();
                updateUIComponents();
            }
        });
    }


    private void createUIComponents() {
        // TODO: place custom component creation code here
        currentPopulationTableModel = new DefaultTableModel(0, networkTableColumnNames.length);
        currentPopulationTableModel.setColumnIdentifiers(networkTableColumnNames);

        networkTable = new JTable(currentPopulationTableModel);
        networkTable.setAutoCreateRowSorter(true);
        //networkTable.setVisible(true);

        statisticsTableModel = new DefaultTableModel(0, historyTableColumnNames.length);
        statisticsTableModel.setColumnIdentifiers(historyTableColumnNames);

        statisticsTable = new JTable(statisticsTableModel);
        statisticsTable.setAutoCreateRowSorter(true);
        DefaultRowSorter sorter = (DefaultRowSorter) statisticsTable.getRowSorter();
        ArrayList<RowSorter.SortKey> keys = new ArrayList<RowSorter.SortKey>();
        keys.add(new RowSorter.SortKey(0, SortOrder.DESCENDING));
        sorter.setSortKeys(keys);

        Vector<String> vec = new Vector<String>();
        for (Genome g : simulation.getPopulation().getGenomes()) {
            vec.add("" + g.getId());
        }

        comboModel = new DefaultComboBoxModel(vec);

        networkDetailComboBox = new JComboBox(comboModel);

        mainPane = new JPanel();

        toolBar = new JToolBar();
        mainPane.add(toolBar);

    }

    @Override
    public void update(Observable o, Object arg) {
        if (arg != null && o instanceof Simulation) {
            Simulation.Update_Args argtype = (Simulation.Update_Args) arg;

            if (argtype == Simulation.Update_Args.NEW_GENERATION) {
                fillTables();
                updateComboBox();
                if (showCurrent) {
                    curDetailGenome = simulation.getCurrent().getGenome();
                }
            } else if (argtype == Simulation.Update_Args.PLAYER_DIED) {
                if (showCurrent) {
                    curDetailGenome = simulation.getCurrent().getGenome();
                }
            }
        }

        updateLabels();
    }

    private void updateUIComponents() {
        this.updateLabels();
        this.updateComboBox();
        this.fillTables();
    }

    private void updateComboBox() {
        comboModel.removeAllElements();

        for (Genome g : population.getGenomes()) {
            comboModel.addElement("" + g.getId());
        }
    }

    public void updateLabels() {
        if (population != null) {
            generationLabel.setText("" + population.getGenerationId());
            maxFitnessLabel.setText("" + population.getTopFitness());
            networkCountLabel.setText("" + population.getGenomes().size());
            currentNetLabelId.setText("" + simulation.getCurrent().getGenome().getId());
        } else {
            generationLabel.setText(TO_BE_DETERMINED);
            maxFitnessLabel.setText(TO_BE_DETERMINED);
            networkCountLabel.setText(TO_BE_DETERMINED);
            currentNetLabelId.setText(TO_BE_DETERMINED);
        }

        if (curDetailGenome == null) {
            curDetailGenome = simulation.getCurrent().getGenome();
        }

        if (curDetailGenome != null) {
            networkDetailIDLabel.setText("" + curDetailGenome.getId());
            networkDetailConnectionCountLabel.setText("" + curDetailGenome.getConnectionGenes().size());
            networkDetailHiddenNodeCount.setText("" + (curDetailGenome.getNodeGenes().size() - curDetailGenome.getMandatoryNodes().size()));
            networkDetailLayerCount.setText("" + curDetailGenome.getLayers());
            networkDetailBallInput.setText("" + curDetailGenome.getBallInputNeuron().getInput());
            networkDetailPaddleInput.setText("" + curDetailGenome.getPaddleInputNeuron().getInput());
            networkDetailLeftOutput.setText("" + curDetailGenome.getLeftOutputNeuron().getOutput());
            networkDetailRightOutput.setText("" + curDetailGenome.getRightOutputNeuron().getOutput());
        }

        if (gameStats.isGameStarted()) {
            scoreLabel.setText("" + gameStats.getScore());
            livesLabel.setText("" + gameStats.getLives());
            levelLabel.setText("" + gameStats.getLevel());
            shotsLabel.setText("" + gameStats.getShots());
        } else {
            scoreLabel.setText(TO_BE_DETERMINED);
            livesLabel.setText(TO_BE_DETERMINED);
            levelLabel.setText(TO_BE_DETERMINED);
            shotsLabel.setText(TO_BE_DETERMINED);
        }
    }

    public void fillTables() {
        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        nf.setMaximumFractionDigits(2);
        nf.setMinimumFractionDigits(2);

        for (int i = currentPopulationTableModel.getRowCount(); i > 0; i--) {
            currentPopulationTableModel.removeRow(0);
        }

        for (Species s : population.getSpecies()) {
            for (Genome g : s.getGenomes()) {

                currentPopulationTableModel.addRow(new String[]{"" + g.getId(), "" + s.getId(), "" + g.getNodeGenes().size(), "" + g.getConnectionGenes().size() + " (" + g.getActiveConnectionCount() + ")", "" + nf.format(g.getFitness()), "" + nf.format(g.getSharedFitness())});
            }
        }

        for (int i = statisticsTableModel.getRowCount(); i > 0; i--) {
            statisticsTableModel.removeRow(0);
        }

        TreeMap<Integer, HashMap<String, Double>> history = simulation.getHistoryMap();

        DefaultRowSorter sorter = (DefaultRowSorter) statisticsTable.getRowSorter();

        for (Integer i : history.keySet()) {
            HashMap<String, Double> values = history.get(i);

            statisticsTableModel.addRow(new String[]{"" + i, nf.format(values.get(Simulation.AVERAGE_FITNESS_KEY)), nf.format(values.get(Simulation.TOP_FITNESS_KEY))});
            sorter.sort();
        }
    }

    private void serializeSim() throws IOException {
        GregorianCalendar cal = new GregorianCalendar();
        String fileName = "simulation_gen" + simulation.getPopulation().getGenerationId() +
                "_" + cal.get(GregorianCalendar.DAY_OF_MONTH) +
                "-" + cal.get(GregorianCalendar.MONTH) +
                "-" + cal.get(GregorianCalendar.YEAR) +
                "_" + cal.get(GregorianCalendar.MINUTE) +
                "-" + cal.get(GregorianCalendar.HOUR_OF_DAY) +
                ".ser";

        File f = new File("./" + fileName);

        if (!f.exists()) {
            f.createNewFile();
        }

        serializeSim(f);
    }

    private Simulation loadSim(File f) throws FileNotFoundException, IOException, ClassNotFoundException {
        FileInputStream fis = new FileInputStream(f);
        ObjectInputStream ois = new ObjectInputStream(fis);

        Simulation res = (Simulation) ois.readObject();

        fis.close();
        ois.close();

        return res;
    }

    private void serializeSim(File f) throws IOException {
        FileOutputStream fos = new FileOutputStream(f);
        ObjectOutputStream oos = new ObjectOutputStream(fos);

        oos.writeObject(simulation);

        fos.close();
        oos.close();
    }

    private void setSimulation(Simulation s) {
        this.simulation = s;
        this.population = s.getPopulation();
        this.curDetailGenome = s.getCurrent().getGenome();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        mainPane = new JPanel();
        mainPane.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 2, new Insets(10, 10, 10, 10), -1, -1));
        networkPopulationTabbedPane = new JTabbedPane();
        mainPane.add(networkPopulationTabbedPane, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(200, 200), null, 0, false));
        populationPane = new JPanel();
        populationPane.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), 5, 5, true, false));
        networkPopulationTabbedPane.addTab("Population", populationPane);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(4, 2, new Insets(5, 5, 5, 5), -1, -1));
        populationPane.add(panel1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_NORTHWEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "General Info"));
        networkCountLabel = new JLabel();
        networkCountLabel.setText("Label");
        panel1.add(networkCountLabel, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        generationLabel = new JLabel();
        generationLabel.setText("Label");
        panel1.add(generationLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        maxFitnessLabel = new JLabel();
        maxFitnessLabel.setText("Label");
        panel1.add(maxFitnessLabel, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        maxFitnessTextLabel = new JLabel();
        maxFitnessTextLabel.setText("Maximum Fitness:");
        panel1.add(maxFitnessTextLabel, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(312, 16), null, 0, false));
        generationCountTextLabel = new JLabel();
        generationCountTextLabel.setText("Generation #: ");
        panel1.add(generationCountTextLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(312, 16), null, 0, false));
        networkCountTextLabel = new JLabel();
        networkCountTextLabel.setText("Network Count:");
        panel1.add(networkCountTextLabel, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(312, 16), null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Current Network:");
        panel1.add(label1, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        currentNetLabelId = new JLabel();
        currentNetLabelId.setText("Label");
        panel1.add(currentNetLabelId, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        scrollPane1.setVerticalScrollBarPolicy(22);
        populationPane.add(scrollPane1, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(376, 419), null, 0, false));
        scrollPane1.setViewportView(networkTable);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
        populationPane.add(panel2, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Current Game Stats"));
        final JLabel label2 = new JLabel();
        label2.setText("Score");
        panel2.add(label2, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Lives:");
        panel2.add(label3, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Level:");
        panel2.add(label4, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        scoreLabel = new JLabel();
        scoreLabel.setText("Label");
        panel2.add(scoreLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        livesLabel = new JLabel();
        livesLabel.setText("Label");
        panel2.add(livesLabel, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        levelLabel = new JLabel();
        levelLabel.setText("Label");
        panel2.add(levelLabel, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Shots:");
        panel2.add(label5, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        shotsLabel = new JLabel();
        shotsLabel.setText("Label");
        panel2.add(shotsLabel, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        networkDetailPane = new JPanel();
        networkDetailPane.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 4, new Insets(10, 10, 10, 10), -1, -1));
        networkPopulationTabbedPane.addTab("Network Detail", networkDetailPane);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(4, 2, new Insets(10, 10, 10, 10), -1, -1));
        networkDetailPane.add(panel3, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel3.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "General"));
        final JLabel label6 = new JLabel();
        label6.setText("ID:");
        panel3.add(label6, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("# Nodes (Hidden):");
        panel3.add(label7, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("# Connections (Active)");
        panel3.add(label8, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        networkDetailIDLabel = new JLabel();
        networkDetailIDLabel.setText("Label");
        panel3.add(networkDetailIDLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        networkDetailConnectionCountLabel = new JLabel();
        networkDetailConnectionCountLabel.setText("Label");
        panel3.add(networkDetailConnectionCountLabel, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("# Layers:");
        panel3.add(label9, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        networkDetailHiddenNodeCount = new JLabel();
        networkDetailHiddenNodeCount.setText("Label");
        panel3.add(networkDetailHiddenNodeCount, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        networkDetailLayerCount = new JLabel();
        networkDetailLayerCount.setText("Label");
        panel3.add(networkDetailLayerCount, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        networkDetailPane.add(panel4, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 2, new Insets(10, 10, 10, 10), -1, -1));
        panel4.add(panel5, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel5.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Inputs"));
        final JLabel label10 = new JLabel();
        label10.setText("BallX:");
        panel5.add(label10, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label11 = new JLabel();
        label11.setText("PaddleX:");
        panel5.add(label11, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        networkDetailBallInput = new JLabel();
        networkDetailBallInput.setText("Label");
        panel5.add(networkDetailBallInput, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        networkDetailPaddleInput = new JLabel();
        networkDetailPaddleInput.setText("Label");
        panel5.add(networkDetailPaddleInput, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 2, new Insets(10, 10, 10, 10), -1, -1));
        panel4.add(panel6, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel6.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Outputs"));
        final JLabel label12 = new JLabel();
        label12.setText("Left:");
        panel6.add(label12, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        networkDetailLeftOutput = new JLabel();
        networkDetailLeftOutput.setText("Label");
        panel6.add(networkDetailLeftOutput, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label13 = new JLabel();
        label13.setText("Right:");
        panel6.add(label13, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        networkDetailRightOutput = new JLabel();
        networkDetailRightOutput.setText("Label");
        panel6.add(networkDetailRightOutput, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label14 = new JLabel();
        label14.setText("Network");
        networkDetailPane.add(label14, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        networkDetailPane.add(networkDetailComboBox, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        networkDetaiLoadCurrent = new JButton();
        networkDetaiLoadCurrent.setText("Load Current");
        networkDetailPane.add(networkDetaiLoadCurrent, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        networkDetailLoadID = new JButton();
        networkDetailLoadID.setText("Load");
        networkDetailPane.add(networkDetailLoadID, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        statisticsPane = new JPanel();
        statisticsPane.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        networkPopulationTabbedPane.addTab("Statistics", statisticsPane);
        final JScrollPane scrollPane2 = new JScrollPane();
        statisticsPane.add(scrollPane2, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        scrollPane2.setViewportView(statisticsTable);
        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        mainPane.add(toolBar, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(-1, 20), null, 0, false));
        playButton = new JButton();
        playButton.setIcon(new ImageIcon(getClass().getResource("/Game/res/play24x24.png")));
        playButton.setText("");
        playButton.setToolTipText("Start/Resume simulation");
        toolBar.add(playButton);
        pauseButton = new JButton();
        pauseButton.setIcon(new ImageIcon(getClass().getResource("/Game/res/pause24x24.png")));
        pauseButton.setText("");
        pauseButton.setToolTipText("Pause simulation");
        toolBar.add(pauseButton);
        restartGenerationButton = new JButton();
        restartGenerationButton.setIcon(new ImageIcon(getClass().getResource("/Game/res/restart24x24.png")));
        restartGenerationButton.setText("");
        restartGenerationButton.setToolTipText("Restart simulation for current generation");
        toolBar.add(restartGenerationButton);
        newPopulationButton = new JButton();
        newPopulationButton.setIcon(new ImageIcon(getClass().getResource("/Game/res/new24x24.png")));
        newPopulationButton.setText("");
        newPopulationButton.setToolTipText("Start with new population");
        toolBar.add(newPopulationButton);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPane;
    }
}
