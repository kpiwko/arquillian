/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.arquillian.impl;

import java.lang.reflect.Method;

import junit.framework.Assert;

import org.jboss.arquillian.impl.core.ManagerBuilder;
import org.jboss.arquillian.impl.core.spi.Manager;
import org.jboss.arquillian.impl.core.spi.context.ClassContext;
import org.jboss.arquillian.impl.core.spi.context.SuiteContext;
import org.jboss.arquillian.impl.core.spi.context.TestContext;
import org.jboss.arquillian.spi.LifecycleMethodExecutor;
import org.jboss.arquillian.spi.TestMethodExecutor;
import org.jboss.arquillian.spi.event.suite.After;
import org.jboss.arquillian.spi.event.suite.AfterClass;
import org.jboss.arquillian.spi.event.suite.AfterSuite;
import org.jboss.arquillian.spi.event.suite.Before;
import org.jboss.arquillian.spi.event.suite.BeforeClass;
import org.jboss.arquillian.spi.event.suite.BeforeSuite;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;


/**
 * Verifies that the {@link EventTestRunnerAdaptor} creates and fires the proper events.
 *
 * @author <a href="mailto:aknutsen@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
@RunWith(MockitoJUnitRunner.class)
public class EventTestRunnerAdaptorTestCase extends AbstractManagerTestBase
{
   @Override
   protected void addExtensions(ManagerBuilder builder) 
   {  
   }
   
   @Override
   protected void startContexts(Manager manager)
   {
      // this is a test of the Context activation, don't auto start
   }

   @Test
   public void shouldHandleLifeCycleEvents() throws Exception 
   {
      Manager manager = getManager();
      EventTestRunnerAdaptor adaptor = new EventTestRunnerAdaptor(manager);
      
      Class<?> testClass = getClass();
      Method testMethod = testClass.getMethod("shouldHandleLifeCycleEvents");
      Object testInstance = this;
      
      TestMethodExecutor testExecutor = Mockito.mock(TestMethodExecutor.class);
      Mockito.when(testExecutor.getInstance()).thenReturn(testInstance);
      Mockito.when(testExecutor.getMethod()).thenReturn(testMethod);
      
      // no active contexts 
      verify(false, false, false, manager);

      adaptor.beforeSuite();
      assertEventFired(BeforeSuite.class, 1);
      assertEventFiredInContext(BeforeSuite.class, SuiteContext.class);
      // suite context active
      verify(true, false, false, manager);

      adaptor.beforeClass(testClass, LifecycleMethodExecutor.NO_OP);
      assertEventFired(BeforeClass.class, 1);
      assertEventFiredInContext(BeforeClass.class, ClassContext.class);
      // suite and class context active
      verify(true, true, false, manager);

      adaptor.before(testInstance, testMethod, LifecycleMethodExecutor.NO_OP);
      assertEventFired(Before.class, 1);
      assertEventFiredInContext(Before.class, TestContext.class);
      // suite, class and test context active
      verify(true, true, true, manager);

      adaptor.test(testExecutor);
      assertEventFired(org.jboss.arquillian.spi.event.suite.Test.class, 1);
      assertEventFiredInContext(org.jboss.arquillian.spi.event.suite.Test.class, TestContext.class);
      // suite, class and test context active
      verify(true, true, true, manager);
      
      adaptor.after(testInstance, testMethod, LifecycleMethodExecutor.NO_OP);
      assertEventFired(After.class, 1);
      assertEventFiredInContext(After.class, TestContext.class);
      // only suite and class context active
      verify(true, true, false, manager);

      adaptor.afterClass(testClass, LifecycleMethodExecutor.NO_OP);
      assertEventFired(AfterClass.class, 1);
      assertEventFiredInContext(AfterClass.class, ClassContext.class);
      // only suite context active when done
      verify(true, false, false, manager);

      adaptor.afterSuite();
      assertEventFired(AfterSuite.class, 1);
      assertEventFiredInContext(AfterSuite.class, SuiteContext.class);
      // no active contexts when done
      verify(false, false, false, manager);
   }

   private void verify(boolean suite, boolean clazz, boolean test, Manager manager)
   {
      Assert.assertEquals(suite, manager.getContext(SuiteContext.class).isActive());
      Assert.assertEquals(clazz, manager.getContext(ClassContext.class).isActive());
      Assert.assertEquals(test, manager.getContext(TestContext.class).isActive());
   }
}
