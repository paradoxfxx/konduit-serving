/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2020 Konduit AI.
 *  *  *
 *  *  * This program and the accompanying materials are made available under the
 *  *  * terms of the Apache License, Version 2.0 which is available at
 *  *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  *  * License for the specific language governing permissions and limitations
 *  *  * under the License.
 *  *  *
 *  *  * SPDX-License-Identifier: Apache-2.0
 *  *  *****************************************************************************
 *
 *
 */
package ai.konduit.serving.data;

import org.datavec.image.data.Image;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;

import javax.json.JsonValue;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public interface Data {

    String toJson();

    List<String> keys();

    String key(int id);

    ValueType type(String key) throws ValueNotFoundException;

    ValueType listType(String key);

    // Getters
    INDArray getNDArray(String key) throws ValueNotFoundException;
    String getString(String key) throws ValueNotFoundException;
    boolean getBoolean(String key) throws ValueNotFoundException;
    byte[] getBytes(String key) throws ValueNotFoundException;
    double getDouble(String key) throws ValueNotFoundException;
    Image getImage(String key) throws ValueNotFoundException;
    long getLong(String key) throws ValueNotFoundException;
    List<JsonValue.ValueType> getList(String key, DataType type);
    Data getData(String key);

    void put(String key, String data);
    void put(String key, INDArray data);
    void put(String key, byte[] data);
    void put(String key, Image data);
    void put(String key, long data);
    void put(String key, double data);
    void put(String key, boolean data);
    //void put(String key, Data data);

    void put(String key, Data data);

    boolean hasMetaData();
    Data getMetaData();
    void setMetaData(Data data);

    // Serialization routines
    void save(File toFile);


    void write(OutputStream toStream);

    byte[] asBytes();

    // Get implementation data
    Map<String, Value> getDataMap();

    static Data fromJson(String key) {
        return null;
    }

    /*static Data fromFile(File file) {
        return null;
    }*/

    static Data fromStream(InputStream fromStream) {
        return null;
    }

    static Data fromBytes(InputStream fromStream) {
        return null;
    }

}