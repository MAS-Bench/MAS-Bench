package masbench.generator;

import masbench.ModelProperty;
import masbench.generator.value.Time;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

public class SimpleGenerationFileGenerator extends AbstractGenerationFileGenerator {
    ArrayList<ArrayList<Integer>> generationArray = new ArrayList<>();
    public SimpleGenerationFileGenerator(Path parameterCsvPath, Path generationJsonPath, Path simulationStartFlowCsvPath, ModelProperty modelProperty) {
        super(parameterCsvPath, generationJsonPath, simulationStartFlowCsvPath, new Double(modelProperty.getNumberOfPeople()), modelProperty.getGpsSeparating());
        readCsv();
    }

    @Override
    void readCsv() {
        try {
            for (String line : Files.readAllLines(inputCsvFilePath)) {
                if (line.toUpperCase().startsWith("#SIMPLE") || line.trim().isEmpty()) { continue; }
                ArrayList aTypeGen = new ArrayList<>();
                for (String v : line.split(",")) {
                    aTypeGen.add(Integer.parseInt(v.trim()));
                }
                generationArray.add(aTypeGen);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    void writeJson() {
        try {
            FileWriter fw = new FileWriter(outputJsonFilePath.toFile(), false);
            PrintWriter pw = new PrintWriter(new BufferedWriter(fw));

            pw.println("#{ \"version\" : 2}");
            pw.println("[");

            for (Time t = new Time(19, 0, 0, 0); t.h < 24; t.step()) {
                String startTime = t.toString();

                int type = 0;
                for (ArrayList<Integer> aTypeGen : generationArray) {
                    if (t.c < aTypeGen.size() && aTypeGen.get(t.c) > 0) {
                        switch (type) {
                            case 0:
                                pw.println(naiveAgent(startTime, aTypeGen.get(t.c)));
                                break;
                            case 1:
                                pw.println(rationalAgent(startTime, aTypeGen.get(t.c)));
                                break;
                            case 2:
                                pw.println(rubyAgent(startTime, aTypeGen.get(t.c)));
                        }
                    }
                    type += 1;
                }


                if (t.c % gpsSeparating == 0) {
                    for (int i = 1; i <= 3; i++) {
                        pw.println(naiveAgentOneMinutes(startTime, i, t.c));
                    }
                }
            }
            pw.println(terminalAgent());
            pw.println("]");
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    void writeCsv() {
        try {
            FileWriter fw = new FileWriter(outputCsvFilePath.toFile(), false);
            PrintWriter pw = new PrintWriter(new BufferedWriter(fw));

            pw.println("currentTime,naiveAgent,rationalAgent,rubyAgent,allAgent");

            for (Time t = new Time(19, 0, 0, 0); t.h < 24; t.step()) {
                int[] nums = {0, 0, 0};

                int type = 0;
                for (ArrayList<Integer> aTypeGen : generationArray) {
                    if (t.c < aTypeGen.size() && aTypeGen.get(t.c) > 0) {
                        nums[type] = aTypeGen.get(t.c);
                    }
                    type += 1;
                }

                Integer sumDist = Arrays.stream(nums).sum();
                pw.println(t + "," + nums[0] + "," + nums[1] + "," + nums[2] + "," + sumDist);
            }
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
