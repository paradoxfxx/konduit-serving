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

import ai.konduit.serving.pipeline.api.data.BoundingBox;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.impl.data.JData;
import ai.konduit.serving.pipeline.impl.data.image.Png;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import ai.konduit.serving.python.PythonStep;
import org.junit.Assert;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public class PythonStepTest {

    @Test
    public void testPythonStepBasic() {
        String code = "def setup():pass\ndef run(data):\n\tdata['x'] += 1\n\treturn data";
        PythonStep step = new PythonStep().code(code).setupMethod("setup").runMethod("run");
        Pipeline pipeline = SequencePipeline.builder().add(step).build();
        Data input = new JData();
        input.put("x", 5);
        Data output = pipeline.executor().exec(input);
        Assert.assertEquals(output.getLong("x"), 6L);
    }

    @Test
    public void testPythonStepWithSetup() {
        String code = "five=0\n" +
                "def setup():\n" +
                "\tglobal five\n" +
                "\tfive+=5.\n" +
                "def run(data):\n" +
                "\tdata['x'] += five + 2.\n" +
                "\treturn data";

        PythonStep step = new PythonStep().code(code).setupMethod("setup").runMethod("run");
        Pipeline pipeline = SequencePipeline.builder().add(step).build();
        Data input = new JData();
        input.put("x", 5);
        Data output = pipeline.executor().exec(input);
        Assert.assertEquals(output.getDouble("x"), 12., 1e-5);
    }

    @Test
    public void testPythonStepAllPrimitiveTypes() {

        Data inputData = new JData();

        inputData.put("s1", "Hello");
        inputData.put("s2", "World");

        inputData.put("i1", 1);
        inputData.put("i2", 2);
        inputData.put("i3", 3);
//
        inputData.put("f1", 10.1);
        inputData.put("f2", 20.1);
        inputData.put("f3", 30.1);
//
        inputData.put("b1", true);
        inputData.put("b2", false);
//
        inputData.put("a1", new byte[]{97, 98, 99}); //abc

        Data expected = new JData();
        expected.put("s", "Hello World!");
        expected.put("i", 7);
        expected.put("f", 40.1);
        expected.put("b", true);
        expected.put("a", new byte[]{97, 98, 99, 100, 101, 102}); //abcdef


        String code = "def setup():pass\n" +
                "def run(input):\n" +
                "\tret=Data()\n" +
                "\tret['s']=input['s1']+' '+input['s2']+'!'\n" +
                "\tret['i']=input['i1']+input['i2']*input['i3']\n" +
                "\tret['f']=input['f2']-input['f1']+input['f3']\n" +
                "\tret['b']=input['b1']\n" +
                "\tret['a']=bytes(input['a1']) + b'def'\n" +
                "\treturn ret\n";
        PythonStep step = new PythonStep().code(code).setupMethod("setup").runMethod("run");
        Pipeline pipeline = SequencePipeline.builder().add(step).build();
        Data output = pipeline.executor().exec(inputData);
        Assert.assertEquals(expected, output);

    }


    @Test
    public void testPythonStepWithLists() {
        Data inputData = new JData();

        inputData.putListString("s", Arrays.asList("Hello", "World"));
        inputData.putListInt64("i", Arrays.asList(1L, 2L, 3L));
        inputData.putListDouble("f", Arrays.asList(10.1, 20.1, 30.1));
        inputData.putListBoolean("b", Arrays.asList(true, false, true));
        inputData.putListBytes("a", Arrays.asList(new byte[]{97, 98, 99}, new byte[]{100, 101, 102}));


        Data expected = new JData();

        expected.putListString("s", Arrays.asList("Hello", "World", "Hello World!"));
        expected.putListInt64("i", Arrays.asList(1L, 2L, 3L, 7L));
        expected.putListDouble("f", Arrays.asList(10.1, 20.1, 30.1, 40.1));
        expected.putListBoolean("b", Arrays.asList(true, false, true, false));
        expected.putListBytes("a", Arrays.asList(new byte[]{97, 98, 99}, new byte[]{100, 101, 102}, new byte[]{97, 98, 99, 100, 101, 102}));

        String code = "def setup():pass\n" +
                "def run(input):\n" +
                "\tret=Data()\n" +
                "\tret['s']=input['s']+[input['s'][0]+' '+input['s'][1]+'!']\n" +
                "\tret['i']=input['i']+[input['i'][0]+input['i'][1]*input['i'][2]]\n" +
                "\tret['f']=input['f']+[input['f'][1]-input['f'][0]+input['f'][2]]\n" +
                "\tret['b']=input['b']+[input['b'][1]]\n" +
                "\tret['a']=input['a'] + [bytes(input['a'][0]) + bytes(input['a'][1])]\n" +
                "\treturn ret\n";

        PythonStep step = new PythonStep().code(code).setupMethod("setup").runMethod("run");
        Pipeline pipeline = SequencePipeline.builder().add(step).build();
        Data output = pipeline.executor().exec(inputData);
        Assert.assertEquals(expected, output);

    }

    @Test
    public void testPythonStepWithNDArrays() {
        Data inputData = new JData();
        INDArray arr1 = Nd4j.rand(3, 2);
        INDArray arr2 = Nd4j.rand(3, 2);
        inputData.put("arr1", NDArray.create(arr1));
        inputData.put("arr2", NDArray.create(arr2));

        Data expected = new JData();
        expected.put("out", NDArray.create(arr1.mul(2).add(arr2)));

        String code = "def setup():pass\n" +
                "def run(input):\n" +
                "\tret=Data()\n" +
                "\tret['out']=input['arr1']*2+input['arr2']\n" +
                "\treturn ret\n";

        PythonStep step = new PythonStep().code(code).setupMethod("setup").runMethod("run");
        Pipeline pipeline = SequencePipeline.builder().add(step).build();
        Data output = pipeline.executor().exec(inputData);
        Assert.assertEquals(expected, output);
    }

    private static byte[] getTestPng() {
        try {
            byte[] aByteArray = {0xa, 0x2, 0xf, (byte) 0xff, (byte) 0xff, (byte) 0xff};
            int width = 1;
            int height = 2;
            DataBuffer buffer = new DataBufferByte(aByteArray, aByteArray.length);
            WritableRaster raster = Raster.createInterleavedRaster(buffer, width, height, 3 * width, 3, new int[]{0, 1, 2}, (Point) null);
            ColorModel cm = new ComponentColorModel(ColorModel.getRGBdefault().getColorSpace(), false, true, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
            BufferedImage image = new BufferedImage(cm, raster, true, null);
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(image, "png", baos);
                return baos.toByteArray();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    public void testPythonStepWithImage() {
        byte[] png = getTestPng();
        Data inputData = new JData();
        inputData.put("img1", Image.create(new Png(png)));

        Data expected = inputData.clone();
        expected.put("img2", Image.create(new Png(png)));

        String code = "def setup():pass\n" +
                "def run(input):\n" +
                "\tinput['img2']=input['img1']\n" +
                "\treturn input\n";

        PythonStep step = new PythonStep().code(code).setupMethod("setup").runMethod("run");
        Pipeline pipeline = SequencePipeline.builder().add(step).build();
        Data output = pipeline.executor().exec(inputData);
        Assert.assertEquals(expected, output);
    }

    @Test
    public void testPythonStepWithBoundingBox() {
        byte[] png = getTestPng();
        Data inputData = new JData();
        inputData.put("img1", Image.create(new Png(png)));


        Data expected = inputData.clone();
        expected.put("img2", Image.create(new Png(png)));
        expected.put("box1", BoundingBox.create(0.5, 0.5, 0.1, 0.1, "box1", 0.2));
        expected.put("box2", BoundingBox.create(0.5, 0.5, 0.1, 0.1, "box2", null));
        expected.put("box3", BoundingBox.create(0.5, 0.5, 0.1, 0.1));

        String code = "def setup():pass\n" +
                "def run(input):\n" +
                "\tinput['img2']=input['img1']\n" +
                "\tinput['box1']=BoundingBox(0.5, 0.5, 0.1, 0.1, 'box1', 0.2)\n"+
                "\tinput['box2']=BoundingBox(0.5, 0.5, 0.1, 0.1, 'box2')\n"+
                "\tinput['box3']=BoundingBox(0.5, 0.5, 0.1, 0.1)\n"+
                "\treturn input\n";

        PythonStep step = new PythonStep().code(code).setupMethod("setup").runMethod("run");
        Pipeline pipeline = SequencePipeline.builder().add(step).build();
        Data output = pipeline.executor().exec(inputData);
        Assert.assertEquals(expected, output);
    }
}
