/*
 * Copyright (c) 2015, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */

package tonivade.db.command;

import java.util.List;

public interface IRequest {

    /**
     * @return the command
     */
    public String getCommand();

    /**
     * @return the params
     */
    public List<String> getParams();

    public String getParam(int i);

    public int getLength();

}