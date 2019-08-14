/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2018 RedHat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.wildfly.camel.test.cdi.ear;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.cdi.ear.config.resourceloading.ResourceLoadingCamelContext;
import org.wildfly.camel.test.cdi.ear.config.resourceloading.ResourceLoadingRouteBuilderA;
import org.wildfly.camel.test.cdi.ear.config.resourceloading.ResourceLoadingRouteBuilderB;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

@CamelAware
@RunWith(Arquillian.class)
public class CDIEarResourceLoadingTest {

    @ArquillianResource
    private CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-cdi-ear-resource-loading-tests.jar")
            .addClass(TestUtils.class);
    }

    @Deployment(testable = false, name = "resource-loading.ear")
    public static EnterpriseArchive createEarDeployment() {
        return ShrinkWrap.create(EnterpriseArchive.class, "resource-loading.ear")
            .addAsModule(ShrinkWrap.create(WebArchive.class, "resource-loading-a.war")
                .addClasses(ResourceLoadingRouteBuilderA.class, ResourceLoadingCamelContext.class)
                .addAsResource("mustache/hello.mustache", "template.mustache")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            )
            .addAsModule(ShrinkWrap.create(WebArchive.class, "resource-loading-b.war")
                .addClass(ResourceLoadingRouteBuilderB.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            );
    }

    @Test
    public void testCamelCdiEarResourceLoading() throws Exception {

        CamelContext camelctx = contextRegistry.getCamelContext("sub-deployment-a");
        Assert.assertNotNull("Expected sub-deployment-a to not be null", camelctx);

        String moduleNameA = TestUtils.getClassLoaderModuleName(camelctx.getApplicationContextClassLoader());
        Assert.assertEquals("deployment.resource-loading.ear.resource-loading-a.war", moduleNameA);

        Assert.assertNotNull("Expected resource template.mustache to not be null", camelctx.getClassResolver().loadResourceAsURL("/template.mustache"));

        Map<String, Object> headers = new HashMap<>();
        headers.put("greeting", "Hello");
        headers.put("name", "camelctxA");

        ProducerTemplate template = camelctx.createProducerTemplate();
        String result = template.requestBodyAndHeaders("direct:startA", null, headers, String.class);
        Assert.assertEquals("Hello camelctxA!", result);

        headers.put("name", "camelctxB");

        template = camelctx.createProducerTemplate();
        result = template.requestBodyAndHeaders("direct:startB", null, headers, String.class);
        Assert.assertEquals("Hello camelctxB!", result);
    }
}
