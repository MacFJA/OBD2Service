/*
  Copyright (c) 2018 MacFJA

  Permission is hereby granted, free of charge,
  to any person obtaining a copy of this software and associated documentation files (the "Software"),
  to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
  publish, distribute, sublicense, and/or sell copies of the Software,
  and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
  Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
  INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
  WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.macfja.obd2.service;

import io.github.macfja.obd2.Command;
import io.github.macfja.obd2.Response;
import io.github.macfja.obd2.SimpleCommands;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An abstract implementation of the {@link MultiCommand}.
 * <p>
 * This implementation give all the need tool to run several command and gets its result.
 *
 * @author MacFJA
 */
abstract public class MultiCommand implements Command, MultiCommandInterface {
    private List<Command> commands;
    private Map<String, Response> responses = new HashMap<>();

    /**
     * Create a new MultiCommand from a list of command code
     *
     * @param requests The list of code to send to the OBD
     */
    public MultiCommand(String[] requests) {
        List<Command> commands = new ArrayList<>();
        for (String request : requests) {
            commands.add(SimpleCommands.create(request));
        }
        this.commands = commands;
    }

    /**
     * Create a new MultiCommand from a list of command
     *
     * @param commands The list of command to send to the OBD
     */
    public MultiCommand(List<Command> commands) {
        this.commands = commands;
    }

    /**
     * Get the response of a single sub command
     *
     * @param command The sub command to lookup
     * @return The response associated to the command
     */
    protected Response getResponseOfCommand(Command command) {
        return getResponseOfCommand(command.getRequest());
    }

    /**
     * Get the response of a single sub command
     *
     * @param code The sub command code to lookup
     * @return The response associated to the command
     */
    protected Response getResponseOfCommand(String code) {
        return responses.get(code);
    }

    @Override
    public String getRequest() {
        StringBuilder request = new StringBuilder();
        for (Command command : commands) {
            request.append("[");
            request.append(command.getRequest());
            request.append("]");
        }
        return request.toString();
    }

    @Override
    public List<Command> getCommands() {
        return commands;
    }

    @Override
    public void setResponse(String request, Response response) {
        responses.put(request, response);
    }

    /**
     * Get the {@link MultiResponse} of the command base on every sub command
     *
     * @param rawResult The response of all sub OBD command
     * @return The response object
     * @throws ScriptException If the conversion equation is wrong
     */
    @Override
    public Response getResponse(byte[] rawResult) throws ScriptException {
        return new MultiResponse(responses);
    }
}
