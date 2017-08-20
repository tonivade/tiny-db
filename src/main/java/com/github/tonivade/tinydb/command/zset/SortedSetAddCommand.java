/*
 * Copyright (c) 2015-2017, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */

package com.github.tonivade.tinydb.command.zset;

import static com.github.tonivade.tinydb.data.DatabaseKey.safeKey;
import static com.github.tonivade.tinydb.data.DatabaseValue.score;
import static com.github.tonivade.tinydb.data.DatabaseValue.zset;
import static java.lang.Float.parseFloat;
import static java.util.stream.Collectors.toList;

import java.util.Map.Entry;
import java.util.Set;

import com.github.tonivade.resp.annotation.Command;
import com.github.tonivade.resp.annotation.ParamLength;
import com.github.tonivade.resp.command.Request;
import com.github.tonivade.resp.protocol.RedisToken;
import com.github.tonivade.resp.protocol.SafeString;
import com.github.tonivade.tinydb.command.TinyDBCommand;
import com.github.tonivade.tinydb.command.annotation.ParamType;
import com.github.tonivade.tinydb.data.DataType;
import com.github.tonivade.tinydb.data.DatabaseValue;
import com.github.tonivade.tinydb.data.Database;
import com.github.tonivade.tinydb.data.SortedSet;

@Command("zadd")
@ParamLength(3)
@ParamType(DataType.ZSET)
public class SortedSetAddCommand implements TinyDBCommand {

  @Override
  public RedisToken execute(Database db, Request request) {
    try {
      DatabaseValue initial = db.getOrDefault(safeKey(request.getParam(0)), DatabaseValue.EMPTY_ZSET);
      DatabaseValue result = db.merge(safeKey(request.getParam(0)), parseInput(request),
          (oldValue, newValue) -> {
            Set<Entry<Double, SafeString>> merge = new SortedSet();
            merge.addAll(oldValue.getValue());
            merge.addAll(newValue.getValue());
            return zset(merge);
          });
      return RedisToken.integer(changed(initial.getValue(), result.getValue()));
    } catch (NumberFormatException e) {
      return RedisToken.error("ERR value is not a valid float");
    }
  }

  private int changed(Set<Entry<Float, String>> input, Set<Entry<Float, String>> result) {
    return result.size() - input.size();
  }

  private DatabaseValue parseInput(Request request) throws NumberFormatException {
    Set<Entry<Double, SafeString>> set = new SortedSet();
    SafeString score = null;
    for (SafeString string : request.getParams().stream().skip(1).collect(toList())) {
      if (score != null) {
        set.add(score(parseFloat(score.toString()), string));
        score =  null;
      } else {
        score = string;
      }
    }
    return zset(set);
  }

}