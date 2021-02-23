package org.omnaest.utils.template;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.omnaest.utils.template.TemplateUtils.TemplateProcessor;

/**
 * @see TemplateUtils
 * @author omnaest
 */
public class TemplateUtilsTest
{
    @Test
    public void testProcessor()
    {
        TemplateProcessor processor = TemplateUtils.builder()
                                                   .useTemplateClassResource(this.getClass(), "/example1.ftl")
                                                   .add("text", "test")
                                                   .build();
        String result = processor.get();
        assertEquals("This is a test", result);
    }
}
