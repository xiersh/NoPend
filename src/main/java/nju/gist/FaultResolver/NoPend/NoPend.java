package nju.gist.FaultResolver.NoPend;

import com.sun.istack.Nullable;
import nju.gist.Common.MonotoneSchemaSet;
import nju.gist.Common.Schema;

import java.util.Map;

public class NoPend {

    @Nullable
    static public Map.Entry<Schema, Schema> getLongestChain(MonotoneSchemaSet topSchemas, MonotoneSchemaSet bottomSchemas) {
        if (topSchemas == null || bottomSchemas == null) {
            throw new RuntimeException("Max non-faulty or min non-healthy is null");
        } else if (topSchemas.isEmpty() || bottomSchemas.isEmpty()) {
            throw new RuntimeException("Max non-faulty or min non-healthy is empty");
        }

        Schema max = null, min = null;
        int longest_size = -1;
        for (Schema top : topSchemas.getSchemas()) {
            for (Schema bottom : bottomSchemas.getSchemas()) {
                if (top.isSupersetOf(bottom) && (top.size() - bottom.size() + 1) > longest_size) {
                    max = top;
                    min = bottom;
                    longest_size = top.size() - bottom.size() + 1;
                }
            }
        }

        if (max == null || min == null) {
            return null;
        }
        return Map.entry(max, min);
    }
}
