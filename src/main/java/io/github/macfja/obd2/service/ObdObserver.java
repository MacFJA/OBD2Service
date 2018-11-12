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

/**
 * Actions to do when a {@link Command} have been executed.
 *
 * @author MacFJA
 */
public interface ObdObserver {
    /**
     * The action to do when the response is received.
     *
     * @param response The response return by the OBD
     */
    void update(Response response);

    /**
     * The action to do when an error occurs.
     * <p>
     * The response can be {@code null}.
     *
     * @param request   The command that was send
     * @param response  The response return by the OBD
     * @param exception The associated exception
     */
    void error(Command request, Response response, Exception exception);
}
