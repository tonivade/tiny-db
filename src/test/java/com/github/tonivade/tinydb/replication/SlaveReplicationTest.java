/*
 * Copyright (c) 2015-2017, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.tinydb.replication;

import static com.github.tonivade.resp.protocol.RedisToken.array;
import static com.github.tonivade.resp.protocol.RedisToken.string;
import static com.github.tonivade.resp.protocol.SafeString.safeString;
import static com.github.tonivade.tinydb.data.DatabaseKey.safeKey;
import static com.github.tonivade.tinydb.data.DatabaseValue.entry;
import static com.github.tonivade.tinydb.data.DatabaseValue.hash;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.github.tonivade.resp.command.Request;
import com.github.tonivade.resp.command.RespCommand;
import com.github.tonivade.resp.command.Session;
import com.github.tonivade.resp.protocol.SafeString;
import com.github.tonivade.tinydb.TinyDBRule;
import com.github.tonivade.tinydb.TinyDBServerContext;
import com.github.tonivade.tinydb.data.OnHeapDatabaseFactory;

@RunWith(MockitoJUnitRunner.class)
public class SlaveReplicationTest {

  @Rule
  public final TinyDBRule rule = new TinyDBRule();

  @Mock
  private TinyDBServerContext context;
  @Mock
  private Session session;
  @Mock
  private RespCommand command;
  @Captor
  private ArgumentCaptor<Request> requestCaptor;
  @Captor
  private ArgumentCaptor<InputStream> captor;

  @Test
  public void testReplication() throws IOException  {
    when(context.getAdminDatabase()).thenReturn(new OnHeapDatabaseFactory().create("test"));

    SlaveReplication slave = new SlaveReplication(context, session, "localhost", 7081);

    slave.start();

    verifyConectionAndRDBDumpImported();
    verifyStateUpdated();
  }

  @Test
  public void testProcessCommand()  {
    when(context.getCommand("PING")).thenReturn(command);

    SlaveReplication slave = new SlaveReplication(context, session, "localhost", 7081);

    slave.onMessage(array(string("PING")));

    verifyCommandExecuted();
  }

  private void verifyCommandExecuted() {
    verify(command).execute(requestCaptor.capture());

    Request request = requestCaptor.getValue();
    assertThat(request.getCommand(), is("PING"));
  }

  private void verifyConectionAndRDBDumpImported() throws IOException {
    verify(context, timeout(3000)).importRDB(captor.capture());

    InputStream stream = captor.getValue();

    byte[] buffer = new byte[stream.available()];

    int readed = stream.read(buffer);

    assertThat(readed, is(buffer.length));
    assertThat(new SafeString(buffer).toHexString(), equalTo("524544495330303036FF224AF218835A1E69"));
  }

  private void verifyStateUpdated() {
    assertThat(context.getAdminDatabase().get(safeKey("master")),
               equalTo(hash(entry(safeString("host"), safeString("localhost")),
                            entry(safeString("port"), safeString("7081")),
                            entry(safeString("state"), safeString("connected")))));
  }
}
