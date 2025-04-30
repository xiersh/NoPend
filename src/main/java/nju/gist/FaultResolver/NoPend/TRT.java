package nju.gist.FaultResolver.NoPend;

import lombok.Setter;
import nju.gist.Common.*;

import java.math.BigInteger;
import java.util.BitSet;

public class TRT {
    private int num_parameter, trt_size;
    private BitSet healthy, faulty, flag;
    @Setter
    private Testcase tfail;
    public TRT(int n) throws OutOfMemoryError{
        BigInteger trtSize = BigInteger.valueOf(2).pow(n);
        if (trtSize.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
            throw new UnsupportedOperationException("BitSet getLogicSize exceeds maximum supported getLogicSize");
        }

        num_parameter = n;
        trt_size = trtSize.intValue();
//        // // Output.print("n = " + n);
//        // // Output.print("trt_size = " + trt_size);
        if (trt_size <= 0 || Integer.bitCount(trt_size) != 1) {
            throw new IllegalArgumentException("Size should be the power of 2. Invalid getLogicSize: " + trt_size);
        }
        healthy = new BitSet(trt_size);
        faulty = new BitSet(trt_size);

        // set the failed test-case as faulty
        faulty.set(trt_size - 1);
        // set the empty schema as healthy
        healthy.set(0);

        flag = new BitSet(trt_size); // flag used in getTopSchemasRecursively and getBottomSchemasRecursively
    }

    public void setHealthy(Schema schema) {
        if (schema.getLogicSize() != num_parameter) {
            throw new IllegalArgumentException("Schema getLogicSize does not match TRT getLogicSize");
        }

        if (healthy.get(schema2int(schema))) { // already healthy
            return;
        }
        healthy.set(schema2int(schema));

        MutableSchema subschema = new MutableSchema(schema);
        for (int i = 0; i < subschema.getLogicSize(); i++) {
            if (subschema.get(i)) {
                subschema.set(i, false);
                setHealthy(subschema); // recursively set all subschema of schema as healthy
                subschema.set(i, true);
            }
        }
    }

    public void setFaulty(Schema schema) {
        if (schema.getLogicSize() != num_parameter) {
            throw new IllegalArgumentException("Schema getLogicSize does not match TRT getLogicSize");
        }
        if (faulty.get(schema2int(schema))) { // already faulty
            return;
        }
        faulty.set(schema2int(schema));
        MutableSchema superschema = new MutableSchema(schema);
        for (int i = 0; i < superschema.getLogicSize(); i++) {
            if (!superschema.get(i)) {
                superschema.set(i, true);
                setFaulty(superschema); // recursively set all superschema of schema as faulty
                superschema.set(i, false);
            }
        }
    }

    public MonotoneSchemaSet getTopSchemas() {
        MonotoneSchemaSet topSchemas = new MonotoneSchemaSet(false); // maximal schemas
        Schema top = new Schema(num_parameter, true);
        flag.set(0, flag.size(), false); // clear the flag
        getTopSchemasRecursively(top, topSchemas);
        return topSchemas;
    }

    private void getTopSchemasRecursively(Schema schema, MonotoneSchemaSet topSchemas) {
        if (flag.get(schema2int(schema))) { // has visited
//            // // Output.print(schema.getComb(tfail), System.out, "has visited");
            return;
        }
        flag.set(schema2int(schema));

        if (healthy.get(schema2int(schema))) { // healthy schema, no more pending
//            // // Output.print(schema.getComb(tfail), System.out, "healthy schema");
            return;
        } else if (!faulty.get(schema2int(schema))) { // pending schema
//            // // Output.print("pending schema: " + schema.getComb(tfail));
//            // // Output.print(schema.getComb(tfail), System.out, "pending schema");
            topSchemas.add(new Schema(schema));
            return;
        }
//        // // Output.print(schema.getComb(tfail), System.out, "faulty schema");
        // faulty schema, continue
        MutableSchema subschema = new MutableSchema(schema);
        for (int i = 0; i < subschema.getLogicSize(); i++) {
            if (subschema.get(i)) {
                subschema.set(i, false);
                getTopSchemasRecursively(subschema, topSchemas); // recursively check all subschema of schema
                subschema.set(i, true);
            }
        }
    }

    public MonotoneSchemaSet getBottomSchemas() {
        MonotoneSchemaSet bottomSchemas = new MonotoneSchemaSet(true); // minimal schemas
        Schema bottom = new Schema(num_parameter, false);
        flag.set(0, flag.size() - 1, false);
        getBottomSchemasRecursively(bottom, bottomSchemas);
        return bottomSchemas;
    }

    private void getBottomSchemasRecursively(Schema schema, MonotoneSchemaSet bottomSchemas) {
        if (flag.get(schema2int(schema))) { // has visited
            return;
        }
        flag.set(schema2int(schema));

        if (faulty.get(schema2int(schema))) { // faulty
            return;
        } else if (!healthy.get(schema2int(schema))) { // pending
            bottomSchemas.add(new Schema(schema));
            return;
        }
        // healthy
        MutableSchema superschema = new MutableSchema(schema);
        for (int i = 0; i < schema.getLogicSize(); i++) {
            if (!superschema.get(i)) {
                superschema.set(i, true);
                getBottomSchemasRecursively(superschema, bottomSchemas); // recursively check all superschema of schema
                superschema.set(i, false);
            }
        }
    }

    public MonotoneSchemaSet getMFS() {
        MonotoneSchemaSet MFS = new MonotoneSchemaSet(true);
        Schema bottom = new Schema(num_parameter, false);
        flag.set(0, flag.size() - 1, false);
        getMFSRecursively(bottom, MFS);
        return MFS;
    }

    private void getMFSRecursively(Schema schema, MonotoneSchemaSet MFS) {
        if (flag.get(schema2int(schema))) { // has visited
            return;
        }
        flag.set(schema2int(schema));
        if (faulty.get(schema2int(schema))) {
            MFS.add(new Schema(schema));
            return;
        }

        MutableSchema superschema = new MutableSchema(schema);
        for (int i = 0; i < schema.getLogicSize(); i++) {
            if (!superschema.get(i)) {
                superschema.set(i, true);
                getMFSRecursively(superschema, MFS);
                superschema.set(i, false);
            }
        }

    }

    private int schema2int(Schema schema) {
        int res = 0;
        for (int i = 0; i < schema.getLogicSize(); i++) {
            res += schema.get(i) ? 1 << i : 0;
        }
//        // // Output.print("schema2int: " + schema.getComb(tfail) + " -> " + res);
        return res;
    }
}
