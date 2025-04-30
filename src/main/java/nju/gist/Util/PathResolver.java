package nju.gist.Util;

public class PathResolver {
    public static String getBase(String fileName) {
        // 获取最后一个点的位置
        int lastDotIndex = fileName.lastIndexOf('.');

        if (lastDotIndex != -1) {
            // 分离基础名称和扩展名
            String baseName = fileName.substring(0, lastDotIndex);
            String extension = fileName.substring(lastDotIndex + 1);
            return baseName;
        }
        // 如果没有扩展名
        return null;
    }
}
