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
