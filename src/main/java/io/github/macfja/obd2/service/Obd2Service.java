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
import io.github.macfja.obd2.commander.CommanderInterface;
import io.github.macfja.obd2.commander.SupportedInterface;
import io.github.macfja.obd2.exception.ExceptionResponse;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * A wrapper around {@link CommanderInterface} mainly to ease the usage of OBD2 command scheduling.
 *
 * @author MacFJA
 * @see CommanderInterface
 */
public interface Obd2Service {
    /**
     * Add an observer on a command
     *
     * @param command  The command to listen
     * @param observer The associated observer
     */
    void addObserver(Class<? extends Command> command, ObdObserver observer);

    /**
     * Add an observer on a command
     *
     * @param command  The command to listen
     * @param observer The associated observer
     * @param once     If {@code true}, the observer will be removed after been called
     */
    void addObserver(Class<? extends Command> command, ObdObserver observer, boolean once);

    /**
     * Schedule a command to be execute at fixed interval.
     * <p>
     * If the command is a {@link MultiCommandInterface}, its the instance that will be executed,
     * otherwise, to avoid duplicate command, only one reference is keep (and the best frequency is select)
     *
     * @param command   The command to schedule
     * @param frequency The time between each execution (in seconds)
     */
    void schedule(Command command, double frequency);

    /**
     * Schedule and observe a command at a fixed interval.
     * <p>
     * If the command is a {@link MultiCommandInterface}, its the instance that will be executed,
     * otherwise, to avoid duplicate command, only one reference is keep (and the best frequency is select)
     *
     * @param command   The command to schedule
     * @param frequency The time between each execution (in seconds)
     * @param observer  The observe to associate
     */
    void schedule(Command command, double frequency, ObdObserver observer);

    /**
     * Schedule and observe a command at a fixed interval.
     * <p>
     * If the command is already scheduled, the best frequency will be set to the existing one.
     *
     * @param command   The command to schedule
     * @param frequency The time between each execution (in seconds)
     * @param observer  The observe to associate
     */
    void schedule(Class<? extends Command> command, double frequency, ObdObserver observer);

    /**
     * Remove an observer.
     *
     * @param observer The observer to remove
     */
    void removeObserver(ObdObserver observer);

    /**
     * Remove all schedulers that don't have at least one observer waiting
     */
    void cleanLeafSchedulers();

    /**
     * Indicate if the OBD is ready to receive commands
     *
     * @return {@code true} if the OBD (and the {@link CommanderInterface}) is ready.
     */
    boolean isReady();

    /**
     * Indicate if a command is available for the current OBD2.
     * <p>
     * If the {@link CommanderInterface} implement {@link SupportedInterface}, the method {@link SupportedInterface#isCommandSupported(Command)} is used,
     * otherwise the command is considered as supported if the {@link CommanderInterface} didn't throw an Exception.
     * <p>
     * If the command is a {@link MultiCommandInterface},
     * the method {@link #isAvailable(MultiCommandInterface, boolean)} is call with {@code allMustBeAvailable} set to {@code true}.
     *
     * @param command The command to test
     * @return {@code true} is the command is supported
     */
    boolean isAvailable(Command command);

    /**
     * Indicate if a {@link MultiCommandInterface} is available for the current OBD2.
     * <p>
     * Depending on the parameters {@code allMustBeAvailable}, all sub-command are tested with {@link #isAvailable(Command)}.
     *
     * @param multiCommand       The multi-command to test
     * @param allMustBeAvailable If set to {@code true}, all sub-command MUST be supported, otherwise at least one is needed to be supported
     * @return {@code true} if the multi-command is considered as supported
     */
    boolean isAvailable(MultiCommandInterface multiCommand, boolean allMustBeAvailable);

    /**
     * Execute a command.
     * <p>
     * This method also notify all concerned {@link ObdObserver}
     *
     * @param command The command to execute
     * @return The response of the command, can be an {@link ExceptionResponse}
     */
    Response run(Command command);

    /**
     * Set the communication stream from/to the OBD
     *
     * @param toObd   The stream to use to send command to the OBD
     * @param fromObd The stream to use to read response of the OBD
     */
    void setCommunication(OutputStream toObd, InputStream fromObd);

    /**
     * Action to execute when the service is ready to be used.
     * <p>
     * For example it can be used to register {@link ObdObserver}, or to notify the user that the service is ready.
     * <p>
     * Note: The {@link Runnable} is not run inside a separate Thread.
     *
     * @param runnable The action to execute.
     */
    void onReady(Runnable runnable);
}
