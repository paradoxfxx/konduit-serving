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

package ai.konduit.serving.build.config;

import ai.konduit.serving.build.config.devices.CUDADevice;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;
import org.nd4j.common.base.Preconditions;
import org.nd4j.shade.jackson.annotation.JsonProperty;

/**
 * The deployment target - OS, architecture, CUDA vs. CPU, etc
 */
@Data
@Accessors(fluent = true)
public class Target {
    public enum OS {LINUX, WINDOWS, MACOSX, ANDROID;
        public static OS forName(String s){
            if("MAC".equalsIgnoreCase(s) || "OSX".equalsIgnoreCase(s)){
                return MACOSX;
            }

            return valueOf(s.toUpperCase());
        }
    }
    public enum Arch {x86, x86_avx2, x86_avx512, armhf, arm64, ppc64le;
        public static Arch forName(String s){
            switch (s.toLowerCase()) {
                case "x86":
                case "x86_64":
                    return Arch.x86;
                case "x86_avx2":
                case "x86-avx2":
                case "x86_64-avx2":
                    return Arch.x86_avx2;
                case "x86_avx512":
                case "x86-avx512":
                case "x86_64-avx512":
                    return Arch.x86_avx512;
                case "arm64":
                    return Arch.arm64;
                case "armhf":
                    return Arch.armhf;
                case "ppc64le":
                    return Arch.ppc64le;
                default:
                    return null;
            }
        }

        /**
         * What other architectures is this compatible with?
         * Mainly used for x86: i.e., can run x86 on x86-avx2 and x86-avx512 systems.
         * Note that this method also includes the original.
         * For example, x86 -> {x86, x86_avx2, x86_avx512}
         */
        public Arch[] compatibleWith(){
            switch (this){
                case x86:
                    return new Arch[]{Arch.x86, Arch.x86_avx2, Arch.x86_avx512};
                case x86_avx2:
                    return new Arch[]{Arch.x86_avx2, Arch.x86_avx512};
                default:
                    return new Arch[]{this};
            }
        }

        /**
         * Returns true if the code for this arch can generally be run on the specified arch.
         * Mainly: x86 can be run on x86-avx2 and x86-avx512; x86-avx2 can be run on x86-avx512,
         * but x86-avx2 can NOT be run on x86, and so on.
         */
        public boolean isCompatibleWith(Arch other){
            return this == other || (this == Arch.x86 && (other == Arch.x86_avx2 || other == Arch.x86_avx512)) ||
                    (this == Arch.x86_avx2 && other == Arch.x86_avx512);
        }

        public boolean lowerThan(Arch other){
            Preconditions.checkState(isCompatibleWith(other), "Unable to compare non-compatible archs: %s and %s", this, other);
            if(this == other)
                return false;
            if(this == Arch.x86 && other != Arch.x86)
                return true;
            if(this == Arch.x86_avx2 && other == Arch.x86_avx512)
                return true;
            return false;
        }
    }


    public static final Target LINUX_X86 = new Target(OS.LINUX, Arch.x86, null);
    public static final Target LINUX_X86_AVX2 = new Target(OS.LINUX, Arch.x86_avx2, null);
    public static final Target LINUX_X86_AVX512 = new Target(OS.LINUX, Arch.x86_avx512, null);

    public static final Target WINDOWS_X86 = new Target(OS.WINDOWS, Arch.x86, null);
    public static final Target WINDOWS_X86_AVX2 = new Target(OS.WINDOWS, Arch.x86_avx2, null);

    public static final Target MACOSX_X86 = new Target(OS.MACOSX, Arch.x86, null);
    public static final Target MACOSX_X86_AVX2 = new Target(OS.MACOSX, Arch.x86_avx2, null);

    public static final Target LINUX_CUDA_10_0 = new Target(OS.LINUX, Arch.x86, new CUDADevice("10.0"));
    public static final Target LINUX_CUDA_10_1 = new Target(OS.LINUX, Arch.x86, new CUDADevice("10.1"));
    public static final Target LINUX_CUDA_10_2 = new Target(OS.LINUX, Arch.x86, new CUDADevice("10.2"));
    public static final Target WINDOWS_CUDA_10_0 = new Target(OS.WINDOWS, Arch.x86, new CUDADevice("10.0"));
    public static final Target WINDOWS_CUDA_10_1 = new Target(OS.WINDOWS, Arch.x86, new CUDADevice("10.1"));
    public static final Target WINDOWS_CUDA_10_2 = new Target(OS.WINDOWS, Arch.x86, new CUDADevice("10.2"));

    /** Linux, Windows and Mac x86, x86 avx2 and avx512 */
    public static final Target[] LWM_X86 = new Target[]{LINUX_X86, LINUX_X86_AVX2, LINUX_X86_AVX512, WINDOWS_X86, WINDOWS_X86_AVX2,
            MACOSX_X86, MACOSX_X86_AVX2};

    private OS os;
    private Arch arch;
    private ComputeDevice device;       //If null: CPU

    public Target(@JsonProperty("os") OS os, @JsonProperty("arch") Arch arch, @JsonProperty("device") ComputeDevice device){
        this.os = os;
        this.arch = arch;
        this.device = device;
    }

    @Override
    public String toString(){
        return "Target(" + os + "," + arch + (device == null ? "" : "," + device.toString()) + ")";
    }

}