/*
 * Copyright (c) 2015-2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.claudb.data;

import static com.github.tonivade.purefun.Matcher1.instanceOf;
import static com.github.tonivade.purefun.Matcher1.is;
import static com.github.tonivade.resp.protocol.SafeString.safeString;
import static java.time.Instant.now;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collector;
import java.util.stream.Stream;

import com.github.tonivade.purefun.Pattern1;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.ImmutableMap;
import com.github.tonivade.purefun.data.ImmutableSet;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.typeclasses.Equal;
import com.github.tonivade.resp.protocol.SafeString;

public class DatabaseValue implements Serializable {

  private static final long serialVersionUID = -1001729166107392343L;

  public static final DatabaseValue EMPTY_STRING = string("");
  public static final DatabaseValue EMPTY_LIST = list();
  public static final DatabaseValue EMPTY_SET = set();
  public static final DatabaseValue EMPTY_ZSET = zset();
  public static final DatabaseValue EMPTY_HASH = hash();

  public static final DatabaseValue NULL = null;

  private transient DataType type;
  private transient Object value;
  private transient Instant expiredAt;

  private DatabaseValue(DataType type, Object value) {
    this(type, value, null);
  }

  private DatabaseValue(DataType type, Object value, Instant expiredAt) {
    this.type = type;
    this.value = value;
    this.expiredAt = expiredAt;
  }

  public DataType getType() {
    return type;
  }

  public SafeString getString() {
    requiredType(DataType.STRING);
    return getValue();
  }

  public ImmutableList<SafeString> getList() {
    requiredType(DataType.LIST);
    return getValue();
  }

  public ImmutableSet<SafeString> getSet() {
    requiredType(DataType.SET);
    return getValue();
  }

  public NavigableSet<Entry<Double, SafeString>> getSortedSet() {
    requiredType(DataType.ZSET);
    return getValue();
  }

  public ImmutableMap<SafeString, SafeString> getHash() {
    requiredType(DataType.HASH);
    return getValue();
  }

  public int size() {
    return Pattern1.<Object, Integer>build()
        .when(instanceOf(Collection.class))
          .then(collection -> ((Collection<?>) collection).size())
        .when(instanceOf(Sequence.class))
          .then(sequence -> ((Sequence<?>) sequence).size())
        .when(instanceOf(ImmutableMap.class))
          .then(map -> ((ImmutableMap<?, ?>) map).size())
        .when(instanceOf(SafeString.class))
          .returns(1)
        .otherwise()
          .returns(0)
        .apply(this.value);
  }

  public Instant getExpiredAt() {
    return expiredAt;
  }

  public boolean isExpired(Instant now) {
    if (expiredAt != null) {
      return now.isAfter(expiredAt);
    }
    return false;
  }

  public long timeToLiveMillis(Instant now) {
    if (expiredAt != null) {
      return timeToLive(now);
    }
    return -1;
  }

  public int timeToLiveSeconds(Instant now) {
    if (expiredAt != null) {
      return (int) Math.floorDiv(timeToLive(now), 1000L);
    }
    return -1;
  }

  public DatabaseValue expiredAt(Instant instant) {
    return new DatabaseValue(this.type, this.value, instant);
  }

  public DatabaseValue expiredAt(int ttlSeconds) {
    return new DatabaseValue(this.type, this.value, toInstant(toMillis(ttlSeconds)));
  }

  public DatabaseValue noExpire() {
    return new DatabaseValue(this.type, this.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, value);
  }

  @Override
  public boolean equals(Object obj) {
    return Equal.of(this)
        .append((one, other) -> Objects.equals(one.type, other.type))
        .append((one, other) -> Objects.equals(one.value, other.value))
        .applyTo(obj);
  }

  @Override
  public String toString() {
    return "DatabaseValue [type=" + type + ", value=" + value + "]";
  }

  public static DatabaseValue string(String value) {
    return string(safeString(value));
  }

  public static DatabaseValue string(SafeString value) {
    return new DatabaseValue(DataType.STRING, value);
  }

  public static DatabaseValue list(Sequence<SafeString> values) {
    return new DatabaseValue(DataType.LIST, values.asList());
  }

  public static DatabaseValue list(Collection<SafeString> values) {
    return new DatabaseValue(DataType.LIST, ImmutableList.from(requireNonNull(values).stream()));
  }

  public static DatabaseValue list(SafeString... values) {
    return new DatabaseValue(DataType.LIST, ImmutableList.from(Stream.of(values)));
  }

  public static DatabaseValue set(Sequence<SafeString> values) {
    return new DatabaseValue(DataType.SET, values.asSet());
  }

  public static DatabaseValue set(Collection<SafeString> values) {
    return new DatabaseValue(DataType.SET, ImmutableSet.from(requireNonNull(values).stream()));
  }

  public static DatabaseValue set(SafeString... values) {
    return new DatabaseValue(DataType.SET, ImmutableSet.from(Stream.of(values)));
  }

  public static DatabaseValue zset(Collection<Entry<Double, SafeString>> values) {
    return new DatabaseValue(DataType.ZSET,
        requireNonNull(values).stream().collect(collectingAndThen(toSortedSet(),
                                                                  Collections::unmodifiableNavigableSet)));
  }

  @SafeVarargs
  public static DatabaseValue zset(Entry<Double, SafeString>... values) {
    return new DatabaseValue(DataType.ZSET,
        Stream.of(values).collect(collectingAndThen(toSortedSet(),
                                                    Collections::unmodifiableNavigableSet)));
  }

  public static DatabaseValue hash(ImmutableMap<SafeString, SafeString> values) {
    return new DatabaseValue(DataType.HASH, values);
  }

  public static DatabaseValue hash(Collection<Tuple2<SafeString, SafeString>> values) {
    return new DatabaseValue(DataType.HASH, ImmutableMap.from(requireNonNull(values).stream()));
  }

  public static DatabaseValue hash(Sequence<Tuple2<SafeString, SafeString>> values) {
    return new DatabaseValue(DataType.HASH, ImmutableMap.from(requireNonNull(values).stream()));
  }

  @SafeVarargs
  public static DatabaseValue hash(Tuple2<SafeString, SafeString>... values) {
    return new DatabaseValue(DataType.HASH, ImmutableMap.from(Stream.of(values)));
  }

  public static DatabaseValue bitset(int... ones) {
    BitSet bitSet = new BitSet();
    for (int position : ones) {
      bitSet.set(position);
    }
    return new DatabaseValue(DataType.STRING, new SafeString(bitSet.toByteArray()));
  }

  public static Tuple2<SafeString, SafeString> entry(SafeString key, SafeString value) {
    return Tuple.of(key, value);
  }

  public static Entry<Double, SafeString> score(double score, SafeString value) {
    return new SimpleEntry<>(score, value);
  }

  private static Collector<Entry<Double, SafeString>, ?, NavigableSet<Entry<Double, SafeString>>> toSortedSet() {
    return toCollection(SortedSet::new);
  }

  private long timeToLive(Instant now) {
    return Duration.between(now, expiredAt).toMillis();
  }

  private Instant toInstant(long ttlMillis) {
    return now().plusMillis(ttlMillis);
  }

  private long toMillis(int ttlSeconds) {
    return TimeUnit.SECONDS.toMillis(ttlSeconds);
  }

  @SuppressWarnings("unchecked")
  private <T> T getValue() {
    return (T) value;
  }

  private void requiredType(DataType type) {
    if (this.type != type) {
      throw new IllegalStateException("invalid type: " + type);
    }
  }

  private void writeObject(ObjectOutputStream out) throws IOException {
    out.writeObject(this.type);
    out.writeObject(serializableValue());
    out.writeObject(this.expiredAt);
  }

  private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
    this.type = (DataType) input.readObject();
    Object object = input.readObject();
    this.value = Pattern1.build()
        .when(is(DataType.STRING))
          .returns(object)
        .when(is(DataType.LIST))
          .then(value -> ImmutableList.from((Collection<?>) object))
        .when(is(DataType.SET))
          .then(value -> ImmutableSet.from((Collection<?>) object))
        .when(is(DataType.HASH))
          .then(value -> ImmutableMap.from((Map<?, ?>) object))
        .when(is(DataType.ZSET))
          .returns(object)
        .apply(this.type);
    this.expiredAt = (Instant) input.readObject();
  }

  private Object serializableValue() {
    return Pattern1.build()
        .when(is(DataType.STRING))
          .then(value -> this.<SafeString>getValue())
        .when(is(DataType.LIST))
          .then(value -> this.<ImmutableList<SafeString>>getValue().toList())
        .when(is(DataType.SET))
          .then(value -> this.<ImmutableSet<SafeString>>getValue().toSet())
        .when(is(DataType.HASH))
          .then(value -> this.<ImmutableMap<SafeString, SafeString>>getValue().toMap())
        .when(is(DataType.ZSET))
          .then(value -> this.<NavigableSet<Entry<Double, SafeString>>>getValue())
        .apply(this.type);
  }
}
