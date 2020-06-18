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

package ai.konduit.serving.build;

import ai.konduit.serving.build.config.*;
import ai.konduit.serving.build.config.Target;
import ai.konduit.serving.build.dependencies.Dependency;
import ai.konduit.serving.build.deployments.ClassPathDeployment;
import ai.konduit.serving.build.deployments.UberJarDeployment;
import ai.konduit.serving.build.build.GradleBuild;
import ai.konduit.serving.models.deeplearning4j.step.DL4JModelPipelineStep;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.nd4j.common.util.ArchiveUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;

@Ignore //TO be run manually, not part of CI (as it requires all modules to be installed first)
public class TestGradleGeneration {

    @Rule
    public TemporaryFolder testDir = new TemporaryFolder();

    @Test
    public void testBasicGeneration() throws Exception {

        Pipeline p = SequencePipeline.builder()
                .add(new DL4JModelPipelineStep("file:///some/model/path.zip", null, null))
                .build();

        File dir = testDir.newFolder();
//        File dir = new File("C:/Temp/Gradle");
        File jsonF = new File(dir, "pipeline.json");
        FileUtils.writeStringToFile(jsonF, p.toJson(), StandardCharsets.UTF_8);


        File gradeDir = new File(dir, "gradle");
        File uberJarDir = new File(dir, "uberjar");


        Config c = new Config()
                .pipelinePath(jsonF.getAbsolutePath())
                .target(Target.LINUX_X86)
                .serving(Serving.HTTP)
                .deployments(new UberJarDeployment().outputDir(uberJarDir.getAbsolutePath()).jarName("my.jar"));

        GradleBuild.generateGradleBuildFiles(gradeDir, c);

        //Check for gradlew and gradlew.bat

        //Check for build.gradle.kts
        File buildGradle = new File(gradeDir, "build.gradle.kts");
        assertTrue(buildGradle.exists());
        String buildGradleStr = FileUtils.readFileToString(buildGradle, StandardCharsets.UTF_8);

        //Check that it includes the appropriate depndencies
        List<Dependency> deps = c.resolveDependencies();
        for(Dependency d : deps){
            String s = d.gavString();
            assertTrue(buildGradleStr.contains(s));
        }

        //Check that it includes the uber-jar component
        //TODO

        //Actually run the build
        //TODO this might not be doable in a unit test (unless all modules have been installed to local maven repo first)
        GradleBuild.runGradleBuild(gradeDir, c);


        //Check output JAR exists
        File expUberJar = new File(uberJarDir, "my.jar");
        assertTrue(expUberJar.exists());

        //Extract manifest and make sure it has correct main class
        File dest = new File(dir, "mf.txt");
        ArchiveUtils.zipExtractSingleFile(expUberJar, dest, "META-INF/MANIFEST.MF");
        String mfContent = FileUtils.readFileToString(dest, StandardCharsets.UTF_8);
        mfContent = mfContent.replace("\n ", "");
        boolean found = false;
        for(String line : mfContent.split("\n")){
            if (line.contains("Main-Class") && line.contains("ai.konduit.serving.cli.launcher.KonduitServingLauncher")) {
                found = true;
                break;
            }
        }
        assertTrue("No main class attribute found", found);
    }

    @Ignore
    @Test
    public void testManifestJarCreation() throws Exception {
        Pipeline p = SequencePipeline.builder()
                .add(new DL4JModelPipelineStep("file:///some/model/path.zip", null, null))
                .build();

        File dir = testDir.newFolder();
//        File dir = new File("C:/Temp/Gradle");
        File jsonF = new File(dir, "pipeline.json");
        FileUtils.writeStringToFile(jsonF, p.toJson(), StandardCharsets.UTF_8);

        File gradeDir = new File(dir, "gradle");
        File mfJar = new File(dir, "myManifestJar.jar");

        Config c = new Config()
                .pipelinePath(jsonF.getAbsolutePath())
                .target(Target.LINUX_X86)
                .serving(Serving.HTTP)
                .deployments(
                        new ClassPathDeployment().type(ClassPathDeployment.Type.JAR_MANIFEST).outputFile(mfJar.getAbsolutePath())
                );

        GradleBuild.generateGradleBuildFiles(gradeDir, c);

        //Check for gradlew and gradlew.bat

        //Check for build.gradle.kts
        File buildGradle = new File(gradeDir, "build.gradle.kts");
        assertTrue(buildGradle.exists());
        String buildGradleStr = FileUtils.readFileToString(buildGradle, StandardCharsets.UTF_8);

        //Check that it includes the appropriate section
        assertTrue(buildGradleStr, buildGradleStr.contains("manifest"));
        assertTrue(buildGradleStr, buildGradleStr.contains("Class-Path"));

        //Actually run the build
        //TODO this might not be doable in a unit test (unless all modules have been installed to local maven repo first)
        GradleBuild.runGradleBuild(gradeDir, c);

        assertTrue(mfJar.exists());
        File dest = new File(dir, "mf.txt");
        ArchiveUtils.zipExtractSingleFile(mfJar, dest, "META-INF/MANIFEST.MF");
        String mfContent = FileUtils.readFileToString(dest, StandardCharsets.UTF_8);
        mfContent = mfContent.replace("\n ", "");
        assertTrue(mfContent.contains("Class-Path: "));
        assertTrue(mfContent.contains(".jar"));
        assertTrue(mfContent.contains("konduit-serving-pipeline"));
        assertTrue(mfContent.contains("nd4j-native-1.0.0"));
        assertTrue(mfContent.contains("deeplearning4j-core"));
    }

    @Ignore
    @Test
    public void testManifestJarAdditionalDependency() throws Exception {
        Pipeline p = SequencePipeline.builder()
                .add(new DL4JModelPipelineStep("file:///some/model/path.zip", null, null))
                .build();

        File dir = testDir.newFolder();
//        File dir = new File("C:/Temp/Gradle");
        File jsonF = new File(dir, "pipeline.json");
        FileUtils.writeStringToFile(jsonF, p.toJson(), StandardCharsets.UTF_8);

        File gradeDir = new File(dir, "gradle");
        File mfJar = new File(dir, "myManifestJar.jar");

        Config c = new Config()
                .pipelinePath(jsonF.getAbsolutePath())
                .target(Target.LINUX_X86)
                .serving(Serving.HTTP)
                .additionalDependencies(Collections.singletonList("org.deeplearning4j:deeplearning4j-zoo:1.0.0-beta7"))
                .deployments(
                        new ClassPathDeployment().type(ClassPathDeployment.Type.JAR_MANIFEST).outputFile(mfJar.getAbsolutePath())
                );

        GradleBuild.generateGradleBuildFiles(gradeDir, c);

        //Check for gradlew and gradlew.bat

        //Check for build.gradle.kts
        File buildGradle = new File(gradeDir, "build.gradle.kts");
        assertTrue(buildGradle.exists());
        String buildGradleStr = FileUtils.readFileToString(buildGradle, StandardCharsets.UTF_8);

        //Check that it includes the appropriate section
        assertTrue(buildGradleStr, buildGradleStr.contains("manifest"));
        assertTrue(buildGradleStr, buildGradleStr.contains("Class-Path"));

        //Actually run the build
        //TODO this might not be doable in a unit test (unless all modules have been installed to local maven repo first)
        GradleBuild.runGradleBuild(gradeDir, c);

        assertTrue(mfJar.exists());
        File dest = new File(dir, "mf.txt");
        ArchiveUtils.zipExtractSingleFile(mfJar, dest, "META-INF/MANIFEST.MF");
        String mfContent = FileUtils.readFileToString(dest, StandardCharsets.UTF_8);
        mfContent = mfContent.replace("\n ", "");
        assertTrue(mfContent.contains("deeplearning4j-core"));

        assertTrue(mfContent.contains("deeplearning4j-zoo"));
    }
}