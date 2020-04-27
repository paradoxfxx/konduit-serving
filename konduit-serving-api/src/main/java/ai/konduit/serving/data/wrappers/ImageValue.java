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
package ai.konduit.serving.data.wrappers;

import ai.konduit.serving.data.Value;
import ai.konduit.serving.data.ValueType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.datavec.image.data.Image;

@AllArgsConstructor
public class ImageValue implements Value<Image> {
    private Image image;

    @Override
    public ValueType type() {
        return ValueType.IMAGE;
    }

    @Override
    public Image get() {
        return image;
    }

    @Override
    public void set(Image value) {
        this.image = value;
    }
}
