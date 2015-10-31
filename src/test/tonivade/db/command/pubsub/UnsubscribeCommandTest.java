/*
 * Copyright (c) 2015, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */

package tonivade.db.command.pubsub;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static tonivade.db.DatabaseValueMatchers.isSet;
import static tonivade.db.DatabaseValueMatchers.set;
import static tonivade.server.protocol.SafeString.safeString;

import java.util.Collection;
import java.util.Iterator;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import tonivade.db.command.CommandRule;
import tonivade.db.command.CommandUnderTest;
import tonivade.server.command.ISession;

@CommandUnderTest(UnsubscribeCommand.class)
public class UnsubscribeCommandTest {

    @Rule
    public final CommandRule rule = new CommandRule(this);

    @Captor
    private ArgumentCaptor<Collection<?>> captor;

    @Test
    public void testExecute() throws Exception {
        rule.withData("subscriptions:test", set("localhost:12345"))
            .withParams("test")
            .execute()
            .assertValue("subscriptions:test", isSet());

        rule.verify(ISession.class).removeSubscription(safeString("test"));

        rule.verify().addArray(captor.capture());

        Collection<?> response = captor.getValue();

        assertThat(response.size(), is(3));

        Iterator<?> iter = response.iterator();

        assertThat(iter.next(), is(safeString("unsubscribe")));
        assertThat(iter.next(), is(safeString("test")));
        assertThat(iter.next(), is(0));
    }

}
