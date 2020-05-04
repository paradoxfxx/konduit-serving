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

package ai.konduit.serving.pipeline.api.data;

import ai.konduit.serving.pipeline.api.format.NDArrayFactory;
import ai.konduit.serving.pipeline.api.format.NDArrayFormat;
import ai.konduit.serving.pipeline.registry.NDArrayRegistry;
import lombok.NonNull;
import org.nd4j.common.base.Preconditions;

public interface NDArray {

    Object get();

    <T> T getAs(NDArrayFormat<T> format);

    static NDArray create(@NonNull Object from){
        NDArrayFactory f = NDArrayRegistry.getFactoryFor(from);
        Preconditions.checkState(f != null, "Unable to create NDArray from object of %s", from);

        return f.create(from);
    }

    static boolean canCreateFrom(@NonNull Object from){
        return NDArrayRegistry.getFactoryFor(from) != null;
    }

}
