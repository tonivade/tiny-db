package tonivade.db.command.key;

import java.util.concurrent.TimeUnit;

import tonivade.db.command.IRedisCommand;
import tonivade.db.data.DatabaseKey;
import tonivade.db.data.IDatabase;
import tonivade.server.annotation.Command;
import tonivade.server.annotation.ParamLength;
import tonivade.server.command.IRequest;
import tonivade.server.command.IResponse;

@Command("ttl")
@ParamLength(1)
public class TimeToLiveCommand implements IRedisCommand {

    @Override
    public void execute(IDatabase db, IRequest request, IResponse response) {
        DatabaseKey key = db.getKey(DatabaseKey.safeKey(request.getParam(0)));
        if (key != null) {
            response.addInt(TimeUnit.MILLISECONDS.toSeconds(key.timeToLive()));
        } else {
            response.addInt(-2);
        }
    }
}