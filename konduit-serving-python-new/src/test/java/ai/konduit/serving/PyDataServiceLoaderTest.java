/* ******************************************************************************
 * Copyright (c) 2020 Konduit K.K.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/


package ai.konduit.serving;

import ai.konduit.serving.pipeline.impl.data.JData;
import ai.konduit.serving.python.PyData;
import org.eclipse.python4j.PythonTypes;
import org.junit.Assert;
import org.junit.Test;

public class PyDataServiceLoaderTest{
    @Test
    public void testPyDataServiceLoader(){
        Assert.assertEquals(PyData.INSTANCE, PythonTypes.getPythonTypeForJavaObject(new JData()));
    }
}
