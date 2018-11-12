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
import io.github.macfja.obd2.Unit;

import java.util.HashMap;
import java.util.Map;

/**
 * Base implementation of a response that contains several sub-response.
 * <p>
 * This class can be useful if you use a {@link MultiCommandInterface}
 *
 * @author MacFJA
 */
public class MultiResponse implements Response {
    private Map<String, Response> responses = new HashMap<>();

    /**
     * Initialize the MultiResponse with a list of sub-response
     *
     * @param responses The map that contains responses
     */
    public MultiResponse(Map<String, Response> responses) {
        this.responses = responses;
    }

    /**
     * Create an empty MultiResponse
     */
    public MultiResponse() {}

    /**
     * Set/Save the response of a command.
     *
     * @param request  The Command code ({@link Command#getRequest()}) associated to the response
     * @param response The response to store
     */
    public void setResponse(String request, Response response) {
        responses.put(request, response);
    }

    /**
     * Get a sub-response base on a {@link Command}
     *
     * @param command The Command to use of the response lookup ({@link Command#getRequest()} is used for the lookup)
     * @return The response of a command
     */
    public Response getResponse(Command command) {
        return getResponse(command.getRequest());
    }

    /**
     * Get a sub-response base on a {@link Command#getRequest()}
     *
     * @param command The command code to search
     * @return The response of a command
     */
    public Response getResponse(String command) {
        return responses.get(command);
    }

    @Override
    public byte[] getRawResult() {
        return new byte[0];
    }

    @Override
    public String getFormattedString() {
        return null;
    }

    @Override
    public Unit getUnit() {
        return Unit.Multiple;
    }
}
