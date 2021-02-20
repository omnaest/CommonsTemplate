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
    public TemplateProcessorBuilder builder()
    {
        return new TemplateProcessorBuilder()
        {
            private Map<String, Supplier<String>> keyToValueProvider = new HashMap<>();
            private List<String>                  templates          = new ArrayList<>();

            @Override
            public TemplateProcessorBuilder add(String key, String value)
            {
                return this.add(key, () -> value);
            }

            @Override
            public TemplateProcessorBuilder add(String key, Supplier<String> valueProvider)
            {
                this.keyToValueProvider.put(key, valueProvider);
                return this;
            }

            @Override
            public TemplateProcessorBuilder useTemplateClassResource(Class<?> type, String templateResource)
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
            public TemplateProcessorBuilder useTemplate(String template)
            {
                this.templates.add(template);
                return this;
            }

            @Override
            public TemplateProcessor build()
            {
                Map<String, Supplier<String>> keyToValueProvider = this.keyToValueProvider;
                List<String> templates = this.templates;
                return new TemplateProcessorImpl(templates, keyToValueProvider);
            }
        };
    }

    private static class TemplateProcessorImpl implements TemplateProcessor
    {
        private final List<String>                  templates;
        private final Map<String, Supplier<String>> keyToValueProvider;
        private Configuration                       configuration;

        private TemplateProcessorImpl(List<String> templates, Map<String, Supplier<String>> keyToValueProvider)
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
                                                                         .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()
                                                                                                                                          .get())),
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
            Configuration configuration = new Configuration(new Version(2, 3, 31));
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
        public TemplateProcessorBuilder add(String key, String value);

        public TemplateProcessorBuilder add(String key, Supplier<String> value);

        public TemplateProcessorBuilder useTemplate(String template);

        public TemplateProcessorBuilder useTemplateClassResource(Class<?> type, String templateResource);

        public TemplateProcessor build();

    }

    public static interface TemplateProcessor extends Supplier<String>
    {
    }

}
