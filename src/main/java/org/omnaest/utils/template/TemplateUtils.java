/*******************************************************************************
 * Copyright 2021 Danny Kunz
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.omnaest.utils.template;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.IOUtils;
import org.omnaest.utils.ObjectUtils;
import org.omnaest.utils.PredicateUtils;

import freemarker.cache.StringTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;

/**
 * Utility to process freemarker templates
 * 
 * @author omnaest
 */
public class TemplateUtils
{
    public static TemplateProcessorBuilder builder()
    {
        return new TemplateProcessorBuilder()
        {
            private List<String> templates = new ArrayList<>();

            @Override
            public PreparableTemplateProcessor useTemplateClassResource(Class<?> type, String templateResource)
            {
                try
                {
                    return this.useTemplate(IOUtils.toString(type.getResourceAsStream(templateResource), StandardCharsets.UTF_8));
                }
                catch (IOException e)
                {
                    throw new IllegalArgumentException("Unable to load class resource " + type + " -> " + templateResource, e);
                }
            }

            @Override
            public PreparableTemplateProcessor useTemplate(String template)
            {
                this.templates.add(template);
                return new PreparableTemplateProcessor()
                {
                    private Map<String, Supplier<Object>> keyToValueProvider = new HashMap<>();

                    @Override
                    public PreparableTemplateProcessor add(String key, Object value)
                    {
                        return this.add(key, () -> value);
                    }

                    @SuppressWarnings("unchecked")
                    @Override
                    public PreparableTemplateProcessor add(String key, Supplier<?> valueProvider)
                    {
                        this.keyToValueProvider.put(key, (Supplier<Object>) valueProvider);
                        return this;
                    }

                    @Override
                    public TemplateProcessor build()
                    {
                        return new TemplateProcessorImpl(templates, this.keyToValueProvider);
                    }
                };
            }

        };
    }

    private static class TemplateProcessorImpl implements TemplateProcessor
    {
        private final List<String>                  templates;
        private final Map<String, Supplier<Object>> keyToValueProvider;
        private Configuration                       configuration;

        private TemplateProcessorImpl(List<String> templates, Map<String, Supplier<Object>> keyToValueProvider)
        {
            this.templates = templates;
            this.keyToValueProvider = keyToValueProvider;
            this.configuration = this.createConfiguration(this.createTemplateLoader(templates));
        }

        @Override
        public String get()
        {
            try (Writer writer = new StringWriter())
            {
                IntStream.range(0, this.templates.size())
                         .forEach(templateId ->
                         {
                             try
                             {
                                 Template template = this.configuration.getTemplate("" + templateId);
                                 template.process(this.keyToValueProvider.entrySet()
                                                                         .stream()
                                                                         .filter(PredicateUtils.notNull())
                                                                         .filter(entry -> entry.getKey() != null)
                                                                         .filter(entry -> entry.getValue() != null)
                                                                         .collect(Collectors.toMap(entry -> entry.getKey(),
                                                                                                   entry -> Optional.ofNullable(entry.getValue())
                                                                                                                    .map(Supplier::get)
                                                                                                                    .orElse(""),
                                                                                                   (o1, o2) -> ObjectUtils.defaultIfNull(o1, o2))),
                                                  writer);
                             }
                             catch (Exception e)
                             {
                                 throw new IllegalStateException(e);
                             }
                         });
                return writer.toString();
            }
            catch (Exception e)
            {
                throw new IllegalStateException("Failed to process template", e);
            }
        }

        private Configuration createConfiguration(TemplateLoader templateLoader)
        {
            Configuration configuration = new Configuration(new Version(2, 3, 29));
            configuration.setClassForTemplateLoading(TemplateUtils.class, "templates");
            configuration.setTemplateLoader(templateLoader);
            configuration.setDefaultEncoding("UTF-8");
            configuration.setLocale(Locale.US);
            configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
            return configuration;
        }

        private TemplateLoader createTemplateLoader(List<String> templates)
        {
            StringTemplateLoader stringTemplateLoader = new StringTemplateLoader();
            AtomicInteger templateId = new AtomicInteger();
            Optional.ofNullable(templates)
                    .orElse(Collections.emptyList())
                    .forEach(template -> stringTemplateLoader.putTemplate("" + templateId.getAndIncrement(), template));
            return stringTemplateLoader;
        }
    }

    public static interface TemplateProcessorBuilder
    {
        public PreparableTemplateProcessor useTemplate(String template);

        public PreparableTemplateProcessor useTemplateClassResource(Class<?> type, String templateResource);
    }

    public static interface PreparableTemplateProcessor
    {

        public PreparableTemplateProcessor add(String key, Object value);

        public PreparableTemplateProcessor add(String key, Supplier<?> value);

        public TemplateProcessor build();

    }

    public static interface TemplateProcessor extends Supplier<String>
    {
    }

}
