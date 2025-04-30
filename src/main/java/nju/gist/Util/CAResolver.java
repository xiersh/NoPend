package nju.gist.Util;

import nju.gist.Common.Comb;
import nju.gist.Common.MutableComb;
import nju.gist.Common.Testcase;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class CAResolver {
    static private String CAPath_STR = "src/main/resources/tables";
    /**
     * 处理TABLE文件夹下的文件，返回一个List<Testcase>
     * @param n: number of parameters
     * @param t: t-ways
     * @param v: number of values
     * @return
     * @throws IOException
     */

    public static List<Testcase> multiwayCA(int n, int t, int v) throws IOException {
        // 构造文件路径，例如 TABLE/2x2/ca2.2^2.txt
        String filePath = String.format("%dx%d/ca.%d.%d^%d.txt", t, v, t, v, n);
        Path path = Paths.get(String.join("/", CAPath_STR, filePath));

        // 检查文件是否存在
        if (!Files.exists(path)) {
            throw new FileNotFoundException("File not found: " + path);
        }

        List<Testcase> testCases = new ArrayList<>();

        // 读取文件内容
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line = reader.readLine();
            if (line == null) {
                throw new IOException("文件格式错误: 缺少测试用例数量");
            }

            int testCaseCount = Integer.parseInt(line.trim()); // 读取测试用例数量

            // 逐行读取并解析测试用例
            for (int i = 0; i < testCaseCount; i++) {
                line = reader.readLine();
                if (line == null) {
                    throw new IOException("文件格式错误: 测试用例数量不匹配");
                }

                // 将每行转换为 List<Integer>
                List<Integer> values = new ArrayList<>();
                for (String numStr : line.trim().split("\\s+")) {
                    if (numStr.equals("-")) {
                        values.add(0); // 后面看这块怎么处理？
                        continue;
                    }
                    values.add(Integer.parseInt(numStr));
                }

                // 创建 Testcase 实例
                testCases.add(new Testcase(new Comb(values)));
            }
        }

        return testCases;
    }

    public static List<Testcase> oneWayCA(int n, int v) {
        List<Testcase> testcases = new ArrayList<>();
        for (int i = 0; i < v; i++) {
            MutableComb comb = new MutableComb(n);
            for (int j = 0; j < n; j++) {
                comb.set(j, i);
            }
            testcases.add(new Testcase(comb));
        }
        return testcases;
    }

}
