package com.github.tonivade.tinydb.command.string;

import static com.github.tonivade.tinydb.data.DatabaseKey.safeKey;
import static com.github.tonivade.tinydb.data.DatabaseValue.string;

import com.github.tonivade.resp.annotation.Command;
import com.github.tonivade.resp.annotation.ParamLength;
import com.github.tonivade.resp.command.Request;
import com.github.tonivade.resp.protocol.RedisToken;
import com.github.tonivade.resp.protocol.SafeString;
import com.github.tonivade.tinydb.command.TinyDBCommand;
import com.github.tonivade.tinydb.data.Database;

@Command("setex")
@ParamLength(3)
public class SetExpiredCommand implements TinyDBCommand {

  @Override
  public RedisToken execute(Database db, Request request) {
    try {
      db.put(safeKey(request.getParam(0)), string(request.getParam(2)).expiredAt(parseTtl(request.getParam(1))));
      return RedisToken.responseOk();
    } catch (NumberFormatException e) {
      return RedisToken.error("ERR value is not an integer or out of range");
    }
  }

  private int parseTtl(SafeString safeString) throws NumberFormatException {
    return Integer.parseInt(safeString.toString());
  }

}