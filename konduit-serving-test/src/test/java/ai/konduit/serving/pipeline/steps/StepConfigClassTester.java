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

package ai.konduit.serving.pipeline.steps;

import ai.konduit.serving.pipeline.step.*;
import ai.konduit.serving.pipeline.step.model.*;
import org.junit.Test;

public class StepConfigClassTester {

    @Test
    public void testPipelineClasses() throws Exception {
        Class.forName(new ImageLoadingStep().pipelineStepClazz());
        Class.forName(new Dl4jStep().pipelineStepClazz());
        Class.forName(new TensorFlowStep().pipelineStepClazz());
        Class.forName(new KerasStep().pipelineStepClazz());
        Class.forName(new PmmlStep().pipelineStepClazz());
        Class.forName(new SameDiffStep().pipelineStepClazz());
        Class.forName(new OnnxStep().pipelineStepClazz());
        Class.forName(new PythonStep().pipelineStepClazz());
        Class.forName(new TransformProcessStep().pipelineStepClazz());
        Class.forName(new WordPieceTokenizerStep().pipelineStepClazz());
    }
}
