package org.arquillian.algeron.pact.provider.core;

import au.com.dius.pact.model.Consumer;
import au.com.dius.pact.model.Pact;
import au.com.dius.pact.model.RequestResponseInteraction;
import org.arquillian.algeron.pact.provider.core.httptarget.Target;
import org.arquillian.algeron.pact.provider.core.loader.PactFolder;
import org.arquillian.algeron.pact.provider.api.Pacts;
import org.arquillian.algeron.pact.provider.spi.CurrentConsumer;
import org.arquillian.algeron.pact.provider.spi.CurrentInteraction;
import org.arquillian.algeron.pact.provider.spi.Provider;
import org.arquillian.algeron.pact.provider.spi.State;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.spi.EventContext;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;
import org.jboss.arquillian.test.spi.event.suite.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class InteractionRunnerTest {

    @Mock
    private EventContext<Test> eventContext;

    @Mock
    private Test test;

    @Mock
    private Target target;

    private Instance<Pacts> pactsInstance;

    @Before
    public void setup() {
        final PactsReader pactsReader = new PactsReader();
        final List<Pact> pacts = pactsReader.getPacts(new BeforeClass(PactProvider.class));
        pactsInstance = () -> new Pacts(pacts);

        when(eventContext.getEvent()).thenReturn(test);

    }

    @org.junit.Test
    public void should_execute_test_for_each_interaction() {
        when(test.getTestClass()).thenReturn(new TestClass(PactProvider.class));
        PactProvider pactDefinition = new PactProvider();
        when(test.getTestInstance()).thenReturn(pactDefinition);

        InteractionRunner interactionRunner = new InteractionRunner();
        interactionRunner.pactsInstance = pactsInstance;
        interactionRunner.targetInstance = () -> target;
        interactionRunner.executePacts(eventContext);

        assertThat(pactDefinition.consumer).isEqualTo(new Consumer("planets_consumer"));
        assertThat(pactDefinition.interaction).isNotNull();

        verify(eventContext, times(2)).proceed();

    }

    @org.junit.Test
    public void should_throw_exception_when_no_target() {
        when(test.getTestClass()).thenReturn(new TestClass(PactProviderWithNoTarget.class));
        PactProviderWithNoTarget pactDefinition = new PactProviderWithNoTarget();
        when(test.getTestInstance()).thenReturn(pactDefinition);

        InteractionRunner interactionRunner = new InteractionRunner();
        interactionRunner.pactsInstance = pactsInstance;
        interactionRunner.targetInstance = () -> target;

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> interactionRunner.executePacts(eventContext))
                .withMessage("Field annotated with org.jboss.arquillian.test.api.ArquillianResource should implement org.arquillian.algeron.pact.provider.core.httptarget.Target and didn't found any");

    }

    @org.junit.Test
    public void should_throw_exception_when_state_param_is_not_empy_nor_map() {
        when(test.getTestClass()).thenReturn(new TestClass(PactProviderWithWrongStateMethod.class));
        PactProviderWithWrongStateMethod pactDefinition = new PactProviderWithWrongStateMethod();
        when(test.getTestInstance()).thenReturn(pactDefinition);

        final InteractionRunner interactionRunner = new InteractionRunner();
        interactionRunner.pactsInstance = pactsInstance;
        interactionRunner.targetInstance = () -> target;

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> interactionRunner.executePacts(eventContext))
                .withMessage("Method stateMethod should take only a single Map parameter");
    }

    @Provider("planets_provider")
    @PactFolder("pacts")
    public static class PactProviderWithNoTarget {

        @CurrentConsumer
        Consumer consumer;

        @CurrentInteraction
        RequestResponseInteraction interaction;

    }

    @Provider("planets_provider")
    @PactFolder("pacts")
    public static class PactProvider {

        @CurrentConsumer
        Consumer consumer;

        @CurrentInteraction
        RequestResponseInteraction interaction;

        @ArquillianResource
        Target target;

    }

    @Provider("planets_provider")
    @PactFolder("pacts")
    public static class PactProviderWithWrongStateMethod {

        @State("default")
        public void stateMethod(String param) {

        }

        @CurrentConsumer
        Consumer consumer;

        @CurrentInteraction
        RequestResponseInteraction interaction;

        @ArquillianResource
        Target target;

    }

}
