/*
 *  ******************************************************************************
 *  * Copyright (c) 2020 Konduit K.K.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */

package ai.konduit.serving.models.tensorflow.python;

import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.data.NDArrayType;
import ai.konduit.serving.pipeline.api.format.NDArrayConverter;
import ai.konduit.serving.pipeline.api.format.NDArrayFormat;
import ai.konduit.serving.pipeline.impl.data.ndarray.SerializedNDArray;
import lombok.AllArgsConstructor;
import org.nd4j.python4j.Python;
import org.nd4j.python4j.PythonGC;
import org.nd4j.python4j.PythonObject;
import org.nd4j.python4j.PythonTypes;

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

public class NumpyArrayConverters {

    @AllArgsConstructor
    public static class SerializedToNumpyArrayConverter implements NDArrayConverter {

        @Override
        public boolean canConvert(NDArray from, NDArrayFormat to) {
            return canConvert(from, to.formatType());
        }

        @Override
        public boolean canConvert(NDArray from, Class<?> to) {
            return SerializedNDArray.class.isAssignableFrom(from.get().getClass()) && NumpyArray.class.isAssignableFrom(to);
        }

        @Override
        public <U> U convert(NDArray from, Class<U> to) {
            if (!canConvert(from, to)) {
                throw new IllegalArgumentException("Unable to convert NDArray to " + to);
            }
            SerializedNDArray t = (SerializedNDArray) from.get();
            NumpyArray out = convert(t);
            return (U) out;
        }

        @Override
        public <U> U convert(NDArray from, NDArrayFormat<U> to) {
            if (!canConvert(from, to)) {
                throw new IllegalArgumentException("Unable to convert NDArray to " + to);
            }
            SerializedNDArray f = (SerializedNDArray) from.get();
            NumpyArray arr = convert(f);
            return (U) arr;
        }

        private NumpyArray convert(SerializedNDArray from) {
            NDArrayType type = from.getType();
            if (!type.isFixedWidth()) {
                throw new UnsupportedOperationException("Variable width data types are not supported yet.");
            }
            String npDType;
            switch (type) {
                case BFLOAT16:
                    // TODO
                    throw new UnsupportedOperationException("BFloat16 is not supported yet.");
                case FLOAT:
                    npDType = "float32";
                    break;
                default:
                    npDType = type.name().toLowerCase();

            }

            ByteBuffer bb = from.getBuffer();

            if (bb.isDirect()) {
                long address;
                try {
                    //DirectBuffer dBuff = (DirectBuffer)bb; // (java: package sun.nio.ch does not exist)
                    Field addressField = Buffer.class.getDeclaredField("address");
                    addressField.setAccessible(true);
                    address = addressField.getLong(bb);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                try (PythonGC gc = PythonGC.watch()) {
                    PythonObject ctypes = Python.importModule("ctypes");
                    PythonObject cArr = ctypes.attr("c_byte")
                            .mul(new PythonObject(bb.capacity()))
                            .attr("from_address").call(address);
                    PythonObject npArr = Python.importModule("numpy").attr("frombuffer").call(cArr, npDType);
                    npArr = npArr.attr("reshape").call(from.getShape());
                    PythonGC.keep(npArr);
                    return new NumpyArray(npArr);
                }
            } else {
                byte[] bytes = bb.array();
                try (PythonGC gc = PythonGC.watch()) {
                    PythonObject fromBuffer = Python.importModule("numpy").attr("frombuffer");
                    PythonObject npArr = fromBuffer.call(bytes, npDType);
                    npArr = npArr.attr("reshape").call(from.getShape());
                    PythonGC.keep(npArr);
                    return new NumpyArray(npArr);
                }
            }


        }
    }

    public static class NumpyArrayToSerializedConverter implements NDArrayConverter {
        @Override
        public boolean canConvert(NDArray from, NDArrayFormat to) {
            return canConvert(from, to.formatType());
        }

        @Override
        public boolean canConvert(NDArray from, Class<?> to) {
            return NumpyArray.class.isAssignableFrom(from.get().getClass()) && SerializedNDArray.class.isAssignableFrom(to);
        }

        @Override
        public <U> U convert(NDArray from, Class<U> to) {
            if (!canConvert(from, to)) {
                throw new IllegalArgumentException("Unable to convert NDArray to " + to);
            }
            NumpyArray f = (NumpyArray) from.get();
            SerializedNDArray t = convert(f);
            return (U) t;
        }

        @Override
        public <U> U convert(NDArray from, NDArrayFormat<U> to) {
            if (!canConvert(from, to)) {
                throw new IllegalArgumentException("Unable to convert NDArray to " + to);
            }
            NumpyArray f = (NumpyArray) from.get();
            SerializedNDArray t = convert(f);
            return (U) t;
        }

        public SerializedNDArray convert(NumpyArray from) {
            try (PythonGC gc = PythonGC.watch()) {
                NDArrayType type;
                String npDtype = from.getPythonObject().attr("dtype").attr("name").toString();
                switch (npDtype) {
                    case "float64":
                        type = NDArrayType.DOUBLE;
                        break;
                    case "float32":
                        type = NDArrayType.FLOAT;
                        break;
                    default:
                        try {
                            type = NDArrayType.valueOf(npDtype);
                        } catch (IllegalArgumentException iae) {
                            throw new UnsupportedOperationException("Unsupported numpy data type: " + npDtype);
                        }
                }
                List shapeList = PythonTypes.LIST.toJava(from.getPythonObject().attr("shape"));
                long[] shape = new long[shapeList.size()];
                for (int i = 0; i < shape.length; i++) {
                    shape[i] = (Long) shapeList.get(i);
                }
                try {
                    long address = Long.parseLong(from.getPythonObject().attr("__array_interface__").get("data").get(0).toString());
                    Field addressField = Buffer.class.getDeclaredField("address");
                    addressField.setAccessible(true);
                    Field limitField = Buffer.class.getDeclaredField("limit");
                    limitField.setAccessible(true);
                    ByteBuffer buff = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder());
                    addressField.setLong(buff, address);
                    limitField.setInt(buff, from.getPythonObject().attr("nbytes").toInt());
                    return new SerializedNDArray(type, shape, buff);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
//                byte[] bytes = PythonTypes.BYTES.toJava(Python.bytes(from.getPythonObject()));
//                return new SerializedNDArray(type, shape, ByteBuffer.wrap(bytes));
            }

        }
    }
}
