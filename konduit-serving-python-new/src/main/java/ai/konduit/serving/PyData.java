package ai.konduit.serving;

import ai.konduit.serving.pipeline.api.data.*;
import ai.konduit.serving.pipeline.impl.data.JData;
import org.apache.commons.io.IOUtils;
import org.bytedeco.javacpp.BytePointer;
import org.eclipse.python4j.*;
import org.nd4j.common.primitives.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class PyData extends PythonType<Data> {

    public static final PyData INSTANCE = new PyData();


    public PyData() {
        super("Data", Data.class);
    }



    private PythonObject jListToPython(List val, ValueType type){
        List<PythonObject> list = new ArrayList<>();
        switch (type) {
            case STRING:
            case INT64:
            case DOUBLE:
            case BOOLEAN:
                for (Object item : val) {
                    list.add(PythonTypes.convert(item));
                }
                break;
            case BYTES:
                for (byte[] item : (List<byte[]>)val) {
                    list.add(PythonTypes.MEMORYVIEW.toPython(new BytePointer(item)));
                }
                break;
            case NDARRAY:
                for (NDArray item : (List<NDArray>)val) {
                    list.add(NumpyArray.INSTANCE.toPython(item.getAs(INDArray.class)));
                }
                break;
            case BOUNDING_BOX:
                for (BoundingBox item : (List<BoundingBox>)val) {
                    list.add(
                            Python.globals().get("BoundingBox").call(
                                    item.cx(),
                                    item.cy(),
                                    item.height(),
                                    item.width(),
                                    item.label(),
                                    item.probability()
                            )
                    );
                }
                break;
            case IMAGE:
                for(Image item: (List<Image>)val){
                    list.add(PILImage.INSTANCE.toPython(item));
                }
                break;
            case LIST:
                for(List item: (List<List>)val){
                    list.add(jListToPython(item, ValueType.LIST));
                }
            default:
                throw new PythonException("Unsupported type in list: " + type);
        }
        return PythonTypes.LIST.toPython(list);
    }

    private Pair<List, ValueType> pyListToJava(PythonObject val){
        long size = Python.len(val).toLong();
        if (size == 0) {
            throw new PythonException("Cannot infer type from empty list.");
        }
        PythonObject item0 = val.get(0);
        if (Python.isinstance(item0, Python.strType())) {
            List<String> jVal = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                jVal.add(PythonTypes.STR.toJava(val.get(i)));
            }
            return new Pair<>(jVal, ValueType.STRING);
        } else if (Python.isinstance(item0, Python.intType())) {
            List<Long> jVal = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                jVal.add(PythonTypes.INT.toJava(val.get(i)));
            }
            return new Pair<>(jVal, ValueType.INT64);
        } else if (Python.isinstance(item0, Python.floatType())) {
            List<Double> jVal = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                jVal.add(PythonTypes.FLOAT.toJava(val.get(i)));
            }
            return new Pair<>(jVal, ValueType.DOUBLE);
        } else if (Python.isinstance(item0, Python.boolType())) {
            List<Boolean> jVal = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                jVal.add(PythonTypes.BOOL.toJava(val.get(i)));
            }
            return new Pair<>(jVal, ValueType.BOOLEAN);
        } else if (Python.isinstance(item0, Python.memoryviewType())) {
            List<byte[]> jVal = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                jVal.add(PythonTypes.MEMORYVIEW.toJava(val.get(i)).getStringBytes());
            }
            return new Pair<>(jVal, ValueType.BYTES);
        } else if (Python.isinstance(item0, Python.importModule("numpy").attr("ndarray"))) {
            List<NDArray> jVal = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                jVal.add(NDArray.create(NumpyArray.INSTANCE.toJava(val.get(i))));
            }
            return new Pair<>(jVal, ValueType.NDARRAY);
        } else if (PythonTypes.getPythonTypeForPythonObject(item0).equals(PILImage.INSTANCE)) {
            List<Image> jVal = new ArrayList<>();
            for (int i = 0; i < size; i++){
                jVal.add(PILImage.INSTANCE.toJava(val.get(i)));
            }
            return new Pair<>(jVal, ValueType.IMAGE);
        } else if (Python.type(item0).attr("__name__").toString().equals("BoundingBox")) {
            List<BoundingBox> jVal = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                BoundingBox bbox = BoundingBox.create(val.attr("cx").toDouble(),
                        val.attr("cy").toDouble(),
                        val.attr("height").toDouble(),
                        val.attr("width").toDouble(),
                        val.attr("label").toString(),
                        val.attr("probability").toDouble());
                jVal.add(bbox);
            }
            return new Pair<>(jVal, ValueType.BOUNDING_BOX);
        } else if (Python.isinstance(item0, Python.listType())) {
            List<List> jVal = new ArrayList<>();
            for(int i = 0; i < size; i++){
                jVal.add(pyListToJava(val.get(i)).getFirst());
            }
            return new Pair<>(jVal, ValueType.LIST);
        } else{ // Data
            List<Data> jVal = new ArrayList<>();
            for(int i = 0; i < size; i++){
                jVal.add(toJava(val.get(i)));
            }
            return new Pair<>(jVal, ValueType.DATA);
        }
    }

    @Override
    public Data toJava(PythonObject pythonObject){
        JData data = new JData();
        try (PythonGC gc = PythonGC.watch()) {
            PythonObject pyKeysList = Python.list(pythonObject.attr("keys").call());
            List keysList = PythonTypes.LIST.toJava(pyKeysList);
            for (Object key : keysList) {
                if (!(key instanceof String)) {
                    throw new PythonException("String key expected."); // already checked in python
                }
                String strKey = (String) key;
                PythonObject val = pythonObject.get(strKey);
                if (Python.isinstance(val, Python.strType())) {
                    String jVal = PythonTypes.STR.toJava(val);
                    data.put(strKey, jVal);
                } else if (Python.isinstance(val, Python.intType())) {
                    long jVal = PythonTypes.INT.toJava(val);
                    data.put(strKey, jVal);
                } else if (Python.isinstance(val, Python.floatType())) {
                    double jVal = PythonTypes.FLOAT.toJava(val);
                    data.put(strKey, jVal);
                } else if (Python.isinstance(val, Python.boolType())) {
                    boolean jVal = PythonTypes.BOOL.toJava(val);
                    data.put(strKey, jVal);
                } else if (Python.isinstance(val, Python.memoryviewType())) {
                    BytePointer bp = PythonTypes.MEMORYVIEW.toJava(val);
                    byte[] jVal = bp.getStringBytes();
                    data.put(strKey, jVal);
                } else if ( Python.isinstance(val, Python.importModule("numpy").attr("ndarray"))) {
                    INDArray arr = NumpyArray.INSTANCE.toJava(val);
                    NDArray jVal = NDArray.create(arr);
                    data.put(strKey, jVal);
                } else if (PythonTypes.getPythonTypeForPythonObject(val).equals(PILImage.INSTANCE)) {
                    data.put(strKey, PILImage.INSTANCE.toJava(val));
                } else if (Python.type(val).attr("__name__").toString().equals("BoundingBox")) {
                    BoundingBox jVal = BoundingBox.create(val.attr("cx").toDouble(),
                            val.attr("cy").toDouble(),
                            val.attr("height").toDouble(),
                            val.attr("width").toDouble(),
                            val.attr("label").toString(),
                            val.attr("probability").toDouble());
                    data.put(strKey, jVal);
                } else if (Python.isinstance(val, Python.listType())) {
                    Pair<List, ValueType> listAndType = pyListToJava(val);
                   data.putList(strKey, listAndType.getFirst(), listAndType.getSecond());
                } else { // Data
                    Data jVal = toJava(val);
                    data.put(strKey, jVal);
                }
            }
            return data;
        }
    }


    @Override
    public PythonObject toPython(Data javaObject) throws PythonException {
        PythonObject dataCls = Python.globals().attr("get").call("Data");
        if (dataCls.isNone()) {
            String baseCode;
            try (InputStream is = PyData.class
                    .getResourceAsStream("pydata.py")) {
                baseCode = IOUtils.toString(is, StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new PythonException("Error reading pydata.py", e);
            }
            PythonExecutioner.exec(baseCode);
            dataCls = Python.globals().get("Data");
        }
        if (dataCls.isNone()) {
            throw new PythonException("Unable to get Data class.");
        }

        try (PythonGC gc = PythonGC.watch()) {
            PythonObject data = dataCls.call();
            for (String key : javaObject.keys()) {
                PythonObject pyKey = new PythonObject(key);
                switch (javaObject.type(key)) {
                    case STRING:
                    case INT64:
                    case DOUBLE:
                    case BOOLEAN:
                        data.set(pyKey, PythonTypes.convert(javaObject.get(key)));
                        break;
                    case BYTES:
                        data.set(pyKey, PythonTypes.MEMORYVIEW.toPython(new BytePointer(javaObject.getBytes(key))));
                        break;
                    case NDARRAY:
                        data.set(pyKey, NumpyArray.INSTANCE.toPython(javaObject.getNDArray(key).getAs(INDArray.class)));
                        break;
                    case BOUNDING_BOX:
                        PythonObject bboxCls = Python.globals().get("BoundingBox");
                        BoundingBox jBbox = javaObject.getBoundingBox(key);
                        PythonObject bbox = bboxCls.call(jBbox.cx(), jBbox.cy(), jBbox.height(), jBbox.width(), jBbox.label(), jBbox.probability());
                        data.set(pyKey, bbox);
                        break;
                    case IMAGE:
                        data.set(pyKey, PILImage.INSTANCE.toPython(javaObject.getImage(key)));
                        break;
                    case LIST:
                       data.set(pyKey, jListToPython(javaObject.getListData(key), javaObject.listType(key)));
                    case DATA:
                        data.set(pyKey, toPython(javaObject.getData(key)));
                        break;
                    default:
                        throw new PythonException("Unsupported type: " + javaObject.type(key));
                }
            }
            PythonGC.keep(data);
            return data;
        }
    }

    @Override
    public boolean accepts(Object javaObject) throws PythonException {
        return javaObject instanceof Data;
    }

    @Override
    public Data adapt(Object javaObject) throws PythonException {
        if (!(javaObject instanceof Data)) {
            throw new PythonException("Cannot cast " + javaObject.getClass() + " to Data.");
        }
        return (Data) javaObject;
    }


}
