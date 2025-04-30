package nju.gist.Util;

import nju.gist.Common.Comb;
import nju.gist.Common.Testcase;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class CSVResolver {
    // benchmark.csv
    private static final Integer COLUMN_SUT_PATH = 0;
    private static final Integer COLUMN_NUMBER_OF_PARAMETERS = 1;
    private static final Integer COLUMN_NUMBER_OF_MFS = 2;
    private static final Integer COLUMN_SIZE_OF_MFS = 3;

    /* benchmark.csv */

    public static List<String> readSUTPath(String filePath) throws IOException {
        List<String> paths = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(Paths.get(filePath))) {
            String line;
            boolean isFirstLine = true;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (isFirstLine) { // 跳过CSV标题行
                    isFirstLine = false;
                    continue;
                }
                if (parts.length >= 3) {
                    paths.add(parts[COLUMN_SUT_PATH].trim());
                }
            }
        }
        return paths;
    }

    public static List<Integer> readNumberOfParameters(String filePath) throws NumberFormatException, IOException {
        List<Integer> paths = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(Paths.get(filePath))) {
            String line;
            boolean isFirstLine = true;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (isFirstLine) { // 跳过CSV标题行
                    isFirstLine = false;
                    continue;
                }
                if (parts.length > 0) {
                    paths.add(Integer.parseInt(parts[COLUMN_NUMBER_OF_PARAMETERS].trim()));
                }
            }
        }
        return paths;
    }

    /**
     * 读取所有项目中最大的MFS维度
     * @param filePath
     * @return
     * @throws IOException
     */
    public static List<Integer> readMaxMFS(String filePath) throws IOException {
        List<Integer> maxValues = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(Paths.get(filePath))) {
            String line;
            boolean isFirstLine = true;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (isFirstLine) { // 跳过CSV标题行
                    isFirstLine = false;
                    continue;
                }
                if (parts.length >= 3) {
                    String[] values = parts[COLUMN_SIZE_OF_MFS].split(";");
                    int max = Arrays.stream(values)
                            .mapToInt(Integer::parseInt)
                            .max()
                            .orElse(0);
                    maxValues.add(max);
                }
            }
        }
        return maxValues;
    }

    public static List<Map.Entry<String, Integer>> readSUTPATH_MaxMFS(String filePath) throws IOException {
        List<Integer> maxValues = readMaxMFS(filePath);
        List<String> paths = readSUTPath(filePath);
        List<Map.Entry<String, Integer>> result = new ArrayList<>();
        for (int i = 0; i < paths.size(); i++) {
            result.add(new AbstractMap.SimpleEntry<>(paths.get(i), maxValues.get(i)));
        }
        return result;
    }

    /* *-model.csv */
    public static List<Map.Entry<String, Integer>> readParameterValues(String filePath) throws IOException {
        List<Map.Entry<String, Integer>> parameters = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(Paths.get(filePath))) {
            String line;
            boolean isFirstLine = true;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (isFirstLine) { // 跳过CSV标题行
                    isFirstLine = false;
                    continue;
                }
                if (parts.length >= 2) {
                    String name = parts[1].trim();
//                    int numberOfValues = Integer.parseInt(parts[2].trim());
                    int numberOfValues = 2; // 2 values system
                    parameters.add(new AbstractMap.SimpleEntry<>(name, numberOfValues));
                }
            }
        }
        return parameters;
    }

    /* *-mfs.csv */
    public static List<Comb> readCombinations(String filePath) throws IOException {
        List<Comb> combinations = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(Paths.get(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                List<Integer> values = new ArrayList<>();
                for (String part : parts) {
                    if (part.trim().equals("-")) {
                        values.add(Comb.UNKNOWN);
                    } else {
                        values.add(Integer.parseInt(part.trim()));
                    }
                }
                combinations.add(new Comb(values));
            }
        }
        return combinations;
    }

    /* *-safe.csv */
    public static Comb readSafeValue(String filePath) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(Paths.get(filePath))) {
            String line = br.readLine();
            String[] parts = line.split(",");
            List<Integer> values = new ArrayList<>();
            for (String part : parts) {
                if (part.trim().equals("-")) {
                    values.add(Comb.UNKNOWN);
                } else {
                    values.add(Integer.parseInt(part.trim()));
                }
            }
            return new Comb(values);
        }
    }
}
