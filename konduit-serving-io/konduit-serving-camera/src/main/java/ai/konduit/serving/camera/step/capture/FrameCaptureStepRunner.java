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
package ai.konduit.serving.camera.step.capture;

import ai.konduit.serving.annotation.runner.CanRun;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;

@Slf4j
@CanRun(CameraFrameCaptureStep.class)
public class FrameCaptureStepRunner implements PipelineStepRunner {

    protected final PipelineStep step;
    protected final String outputKey;
    protected boolean initialized;
    protected FrameGrabber grabber;
    protected OpenCVFrameConverter.ToIplImage converter;
    private Runnable init;

    public FrameCaptureStepRunner(CameraFrameCaptureStep step){
        this.outputKey = step.getOutputKey();
        this.step = step;
        init = () -> {
            this.initOpenCVFrameGrabber(step);
        };
    }

    public FrameCaptureStepRunner(VideoFrameCaptureStep step){
        this.outputKey = step.getOutputKey();
        this.step = step;
        init = () -> {
            this.initFFmpegFrameGrabber(step);
        };
    }

    @Override
    public synchronized void close() {
        if(initialized){
            initialized = false;
            try {
                grabber.stop();
                grabber.close();
            } catch (Throwable t){
                log.warn("Error stopping/closing FrameGrabber", t);
            }
        }
    }

    @Override
    public PipelineStep getPipelineStep() {
        return step;
    }

    @Override
    public synchronized Data exec(Context ctx, Data data) {
        if(!initialized)
            init.run();

        try {
            Frame frame = grabber.grab();
            Image i = Image.create(frame);
            return Data.singleton(outputKey, i);
        } catch (Throwable t){
            throw new RuntimeException("Error getting frame", t);
        }
    }

    protected void initOpenCVFrameGrabber(CameraFrameCaptureStep step){
        grabber = new OpenCVFrameGrabber(step.getCamera());
        converter = new OpenCVFrameConverter.ToIplImage();

        //TODO NEED TO CONFIGURE - RESOLUTION ETC
        int w = step.getWidth();
        int h = step.getHeight();

        try {
            grabber.start();
        } catch (Throwable t){
            log.error("Failed to start video frame grabber with step {}", step);
            throw new RuntimeException("Failed to start video frame grabber", t);
        }

        initialized = true;
    }

    protected void initFFmpegFrameGrabber(VideoFrameCaptureStep step){
        grabber = new FFmpegFrameGrabber(step.getFilePath());
        converter = new OpenCVFrameConverter.ToIplImage();

        try {
            grabber.start();
        } catch (Throwable t){
            log.error("Failed to start video frame grabber with step {}", step);
            throw new RuntimeException("Failed to start video frame grabber", t);
        }

        initialized = true;
    }
}
