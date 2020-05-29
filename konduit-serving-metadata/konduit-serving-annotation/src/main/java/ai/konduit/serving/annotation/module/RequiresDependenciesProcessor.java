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

package ai.konduit.serving.annotation.module;

import ai.konduit.serving.annotation.AnnotationUtils;
import ai.konduit.serving.annotation.runner.CanRun;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@SupportedAnnotationTypes({"ai.konduit.serving.annotation.module.ModuleInfo",
        "ai.konduit.serving.annotation.module.RequiresDependencies"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class RequiresDependenciesProcessor extends AbstractProcessor {

    private String moduleName;
    private List<String> toWrite = new ArrayList<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {

        if(env.processingOver()){
            if(moduleName == null){
                throw new IllegalStateException("No class in this module is annotated with @ModuleInfo - a class with " +
                        "@ModuleInfo(\"your-module-name\" should be added to the module that has the @CanRun(...) annotation");
            }
            writeFile();
        } else {
            if(moduleName == null){
                Collection<? extends Element> c = env.getElementsAnnotatedWith(ModuleInfo.class);
                List<TypeElement> types = ElementFilter.typesIn(c);
                for(TypeElement te : types){
                    moduleName = te.getAnnotation(ModuleInfo.class).value();
                    break;
                }
            }

            Collection<? extends Element> c = env.getElementsAnnotatedWith(RequiresDependencies.class);
            List<TypeElement> l = ElementFilter.typesIn(c);
            for(TypeElement annotation : l){

                List<? extends Element> enclosed = annotation.getEnclosedElements();
                Element elem = annotation.getEnclosingElement();

                Requires[] requires = annotation.getAnnotation(RequiresDependencies.class).value();

                for (Requires require : requires) {
                    Dependency[] deps = require.value();
                    Req req = require.requires();

                    List<String> depsStrList = new ArrayList<>();
                    for(Dependency d : deps){
                        //g:a:v:(any or all of classifiers)
                        String g = d.gId();
                        String a = d.aId();
                        String v = d.ver();
                        String[] cl = d.classifier();
                        Req r = d.cReq();
                        depsStrList.add(process(g,a,v,cl,r));
                    }

                    String s;
                    if(req == Req.ALL){
                        s = "[" + String.join(",", depsStrList) + "]";
                    } else {
                        //Any
                        s = "{" + String.join(",", depsStrList) + "}";
                    }

                    toWrite.add(s);
                }
            }


        }
        return true;
    }

    private static String process(String g, String a, String v, String[] cl, Req r){
        StringBuilder sb = new StringBuilder();
        sb.append("\"");
        sb.append(g).append(":").append(a).append(":").append(v);
        if(cl != null && cl.length == 1){
            sb.append(":").append(cl[0]);
        } else if(cl != null && cl.length > 1){
            sb.append(":");
            if(r == Req.ALL){
                sb.append("[").append(String.join(",", cl)).append("]");
            } else {
                //Any of
                sb.append("{").append(String.join(",", cl)).append("}");
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    protected void writeFile(){
        if(toWrite.isEmpty())           //Can be empty if @ModuleInfo exists but no required dependencies
            return;

        Filer filer = processingEnv.getFiler();
        List<String> toWrite2 = new ArrayList<>();
        for(String s : toWrite){
            toWrite2.add(moduleName + "," + s);
        }
        AnnotationUtils.writeFile(filer, RequiresDependencies.class, toWrite2);
    }
}
