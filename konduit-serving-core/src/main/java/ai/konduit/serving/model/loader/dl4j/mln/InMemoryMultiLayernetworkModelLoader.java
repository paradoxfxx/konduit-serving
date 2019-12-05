/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2015-2019 Skymind Inc.
 *  *  * Copyright (c) 2019 Konduit AI.
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

package ai.konduit.serving.model.loader.dl4j.mln;

import ai.konduit.serving.model.loader.ModelLoader;
import io.vertx.core.buffer.Buffer;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@AllArgsConstructor
public class InMemoryMultiLayernetworkModelLoader implements ModelLoader<MultiLayerNetwork> {

    private MultiLayerNetwork multiLayerNetwork;


    @Override
    public Buffer saveModel(@NonNull MultiLayerNetwork model) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            ModelSerializer.writeModel(model, byteArrayOutputStream, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return Buffer.buffer(byteArrayOutputStream.toByteArray());
    }

    @Override
    public MultiLayerNetwork loadModel() {
        return multiLayerNetwork;
    }
}
