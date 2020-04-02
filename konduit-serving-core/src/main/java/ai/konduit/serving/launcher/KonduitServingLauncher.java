/*
 * *****************************************************************************
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
 * ****************************************************************************
 */

package ai.konduit.serving.launcher;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.launcher.command.*;
import ai.konduit.serving.verticles.inference.InferenceVerticle;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.Launcher;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import lombok.extern.slf4j.Slf4j;
import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4J;

import java.io.File;

import static io.vertx.core.file.FileSystemOptions.DEFAULT_FILE_CACHING_DIR;
import static io.vertx.core.file.impl.FileResolver.CACHE_DIR_BASE_PROP_NAME;
import static io.vertx.core.file.impl.FileResolver.DISABLE_CP_RESOLVING_PROP_NAME;
import static io.vertx.core.logging.LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME;
import static java.lang.System.setProperty;

@Slf4j
public class KonduitServingLauncher extends Launcher {

    InferenceConfiguration inferenceConfiguration;

    static {
        SysOutOverSLF4J.sendSystemOutAndErrToSLF4J();

        setProperty(LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
        LoggerFactory.getLogger(LoggerFactory.class); // Required for Logback to work in Vertx

        setProperty("vertx.cwd", new File(".").getAbsolutePath());
        setProperty(CACHE_DIR_BASE_PROP_NAME, DEFAULT_FILE_CACHING_DIR);
        setProperty(DISABLE_CP_RESOLVING_PROP_NAME, Boolean.TRUE.toString());
    }

    @Override
    protected String getMainVerticle() {
        return InferenceVerticle.class.getCanonicalName();
    }

    @Override
    protected String getDefaultCommand() {
        return ServeCommand.class.getAnnotation(Name.class).value();
    }

    /**
     * Initializes the {@link VertxOptions} for deployment and use in a
     * {@link Vertx} instance.
     * The following other initialization also happens:
     * {@code Vertx Working Directory} gets set (vertx.cwd) and {vertx.caseDirBase)
     * (vertx.disableFileCPResolving) gets set to true
     * (vertx.logger-delegate-factory-class-name) gets set to io.vertx.core.logging.SLF4JLogDelegateFactory
     * The {@link MeterRegistry} field and associated prometheus configuration gets setup
     * The {@link VertxOptions} event but options also get set
     */
    @Override
    public void beforeStartingVertx(VertxOptions options) {
        MicrometerMetricsOptions micrometerMetricsOptions = new MicrometerMetricsOptions()
                .setMicrometerRegistry(new PrometheusMeterRegistry(PrometheusConfig.DEFAULT))
                .setPrometheusOptions(new VertxPrometheusOptions()
                        .setEnabled(true));

        log.info("Setup micro meter options.");
        BackendRegistries.setupBackend(micrometerMetricsOptions);

        options.setMetricsOptions(micrometerMetricsOptions);
    }

    @Override
    public void afterStartingVertx(Vertx vertx) {
        super.afterStartingVertx(vertx);
    }

    @Override
    public void afterConfigParsed(JsonObject config) {
        this.inferenceConfiguration = InferenceConfiguration.fromJson(config.encode());
    }

    public static void main(String[] args) {
        new KonduitServingLauncher()
                .unregister("start")
                .unregister("test")
                .register(ServeCommand.class, ServeCommand::new)
                .register(ListCommand.class, ListCommand::new)
                .register(StopCommand.class, StopCommand::new)
                .register(PredictCommand.class, PredictCommand::new)
                .register(VersionCommand.class, VersionCommand::new)
                .dispatch(args);
    }
}
