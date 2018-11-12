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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * Implementation of {@link Obd2Service}.
 *
 * @author MacFJA
 */
public class Service implements Obd2Service {
    /**
     * The lowest frequency (in seconds) possible for the scheduling of commands.
     */
    protected static final Double MINIMUM_FREQUENCY = 1.5;

    private List<Schedule> schedules = new ArrayList<>();
    private CommanderInterface commander;
    private Timer timer;
    private Logger logger = LoggerFactory.getLogger(getClass());
    private boolean ready = false;
    private List<Runnable> onReadys = new ArrayList<>();

    private List<ResponseListener> responseListeners = new ArrayList<>();

    /**
     * Create a new service for the provided Commander
     * @param commander The Commander to use
     */
    public Service(CommanderInterface commander) {
        this.commander = commander;
    }

    @Override
    public void addObserver(Class<? extends Command> command, ObdObserver observer) {
        addObserver(command, observer, false);
    }

    @Override
    public void addObserver(Class<? extends Command> command, ObdObserver observer, boolean once) {
        responseListeners.add(new ResponseListener(command, observer, once));
    }

    /**
     * Schedule a command to be execute at fixed interval.
     * <p>
     * If the command is a {@link MultiCommandInterface}, its the instance that will be executed,
     * otherwise, to avoid duplicate command, only one reference is keep (and the best frequency is select).
     * <p>
     * It can throws an {@link IllegalArgumentException} if the frequency is not a positive number
     *
     * @param command   The command to schedule
     * @param frequency The time between each execution (in seconds)
     */
    @Override
    public void schedule(Command command, double frequency) {
        if (frequency <= 0) {
            throw new IllegalArgumentException("The frequency must be a positive number");
        }
        if (command instanceof MultiCommandInterface) {
            schedules.add(new Schedule(command, frequency));
            prepareTimer();
            return;
        }

        for (Schedule schedule : schedules) {
            if (schedule.getCommand().getClass() == command.getClass()) {
                double newFrequency = GCD((int) (frequency * 10), (int) (schedule.getFrequency() * 10));
//                schedules.add(new Schedule(command, Math.max(MINIMUM_FREQUENCY, newFrequency / 10)));
                schedule.frequency = Math.max(MINIMUM_FREQUENCY, newFrequency / 10);
                prepareTimer();
                return;
            }
        }

        schedules.add(new Schedule(command, frequency));
        prepareTimer();
    }

    /**
     * Schedule and observe a command at a fixed interval.
     * <p>
     * If the command is a {@link MultiCommandInterface}, its the instance that will be executed,
     * otherwise, to avoid duplicate command, only one reference is keep (and the best frequency is select).
     * <p>
     * It can throws an {@link IllegalArgumentException} if the frequency is not a positive number
     *
     * @param command   The command to schedule
     * @param frequency The time between each execution (in seconds)
     * @param observer  The observe to associate
     */
    @Override
    public void schedule(Command command, double frequency, ObdObserver observer) {
        schedule(command, frequency);
        responseListeners.add(new ResponseListener(command, observer, false));
    }

    /**
     * Schedule and observe a command at a fixed interval
     * <p>
     * If the command is already scheduled, the best frequency will be set to the existing one.
     * <p>
     * It can throws an {@link IllegalArgumentException} if the frequency is not a positive number
     *
     * @param command   The command to schedule
     * @param frequency The time between each execution (in seconds)
     * @param observer  The observe to associate
     */
    @Override
    public void schedule(Class<? extends Command> command, double frequency, ObdObserver observer) {
        try {
            schedule(command.newInstance(), frequency);
            addObserver(command, observer);
        } catch (InstantiationException | IllegalAccessException e) {
            logger.error(String.format("Unable to schedule %s", command.getName()), e);
        }
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public boolean isAvailable(Command command) {
        if (!isReady()) {
            return false;
        }

        if (command instanceof MultiCommandInterface) {
            return isAvailable((MultiCommandInterface) command, true);
        }

        if (commander instanceof SupportedInterface) {
            return ((SupportedInterface) commander).isCommandSupported(command);
        }

        try {
            commander.sendCommand(command);
            return true;
        } catch (IOException | ScriptException | ExceptionResponse e) {
            return false;
        }
    }

    @Override
    public boolean isAvailable(MultiCommandInterface multiCommand, boolean allMustBeAvailable) {
        if (!isReady()) {
            return false;
        }

        for (Command command : multiCommand.getCommands()) {
            boolean result = isAvailable(command);

            if (allMustBeAvailable && !result) {
                return false;
            }
            if (!allMustBeAvailable && result) {
                return true;
            }
        }

        return allMustBeAvailable;
    }

    @Override
    public Response run(Command command) {
        Response response = doRun(command);
        notifyObservers(command, response);
        return response;
    }

    /**
     * The method is the one who actually end request to the OBD.
     * <p>
     * It handle {@link MultiCommandInterface} command as well as normal {@link Command}.
     * It also always return a response (which can be an {@link ExceptionResponse}).
     *
     * @param command The command to execute
     * @return The result of the command
     */
    private Response doRun(Command command) {
        if (command instanceof MultiCommandInterface) {
            for (Command subCommand : ((MultiCommandInterface) command).getCommands()) {
                Response response = doRun(subCommand);
                ((MultiCommandInterface) command).setResponse(subCommand.getRequest(), response);
            }
            try {
                return command.getResponse("".getBytes());
            } catch (ScriptException e) {
                logger.warn("An error occurs while transforming the result of the multi-command '%s': %s", command.getRequest(), e.getLocalizedMessage());
                return new ExceptionResponse(e.getLocalizedMessage().getBytes());
            }
        }

        try {
            return commander.sendCommand(command);
        } catch (IOException | ScriptException | ExceptionResponse e) {
            logger.warn("An error occurs while running command '%s': %s", command.getRequest(), e.getLocalizedMessage());
            return new ExceptionResponse(e.getLocalizedMessage().getBytes());
        }
    }

    @Override
    public void setCommunication(OutputStream toObd, InputStream fromObd) {
        commander.setCommunicationInterface(toObd, fromObd);
        ready = true;
        for (Runnable runnable : onReadys) {
            runnable.run();
        }
    }

    @Override
    public void onReady(Runnable runnable) {
        onReadys.add(runnable);
    }

    /**
     * Handle the scheduling of commands.
     * <p>
     * Stop any running timer.
     * Create a new timer that run as the best rate possible.
     */
    private void prepareTimer() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
        }
        if (schedules.isEmpty()) {
            return;
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!isReady()) {
                    return;
                }
                for (Schedule schedule : schedules) {
                    if (!schedule.canRun()) {
                        continue;
                    }
                    Service.this.run(schedule.getCommand());
                    markSimilarSchedulerAsRan(schedule);
                }
            }
        }, 500, new Double(minFrequency() * 1000).longValue());
    }

    /**
     * Mark all scheduler that are schedule for same command as ran to avoid unnecessary execution
     *
     * @param schedule The scheduler that just be executed
     */
    private void markSimilarSchedulerAsRan(Schedule schedule) {
        if (schedule.getCommand() instanceof MultiCommandInterface) {
            schedule.hasRun();
            return;
        }
        for (Schedule otherSchedule : schedules) {
            if (otherSchedule.getCommand().getRequest().equals(schedule.getCommand().getRequest())) {
                otherSchedule.hasRun();
            }
        }
    }

    @Override
    public void removeObserver(ObdObserver observer) {
        for (ResponseListener responseListener : responseListeners) {
            if (responseListener.getObserver().equals(observer)) {
                responseListeners.remove(responseListener);
            }
        }
    }

    @Override
    public void cleanLeafSchedulers() {
        boolean shouldPrepareTimer = false;
        Set<String> requests = new HashSet<>();
        for (ResponseListener listener : responseListeners) {
            requests.add(listener.getRequest());
        }
        for (Schedule schedule : schedules) {
            if (!requests.contains(schedule.getCommand().getRequest())) {
                schedules.remove(schedule);
                shouldPrepareTimer = true;
            }
        }
        if (shouldPrepareTimer) {
            prepareTimer();
        }
    }

    /**
     * Find every observer that are waiting response for the command and update them
     *
     * @param command  The command that have been executed
     * @param response The response of the command
     */
    private void notifyObservers(Command command, Response response) {
        for (ResponseListener responseListener : responseListeners) {
            if (responseListener.getRequest().equals(command.getRequest())) {
                if (response instanceof ExceptionResponse) {
                    responseListener.getObserver().error(command, response, (ExceptionResponse) response);
                } else {
                    responseListener.getObserver().update(response);
                }

                if (responseListener.isOnce()) {
                    responseListeners.remove(responseListener);
                }
            }
        }
    }

    /**
     * Find the greatest common divisor
     *
     * @param a The first number
     * @param b The second number
     * @return The greatest number that can divide both of the number
     */
    private int GCD(int a, int b) {
        if (b == 0) return a;
        return GCD(b, a % b);
    }

    /**
     * Get the best frequency for every {@link Schedule}
     *
     * @return the best frequency between {@link #MINIMUM_FREQUENCY} and Inf.
     */
    private double minFrequency() {
        if (schedules.isEmpty()) {
            return 30;
        }
        int gcd = (int) (schedules.get(0).getFrequency() * 10);
        for (Schedule schedule : schedules) {
            gcd = GCD(gcd, Double.valueOf(schedule.getFrequency() * 10).intValue());
        }
        return Math.max(MINIMUM_FREQUENCY, gcd / 10);
    }

    /**
     * A class that hold the command to listen to and the action to do
     *
     * @author MacFJA
     */
    private class ResponseListener {
        private String request;
        private ObdObserver observer;
        private boolean once;

        ResponseListener(Command command, ObdObserver observer, boolean once) {
            request = command.getRequest();
            this.observer = observer;
            this.once = once;
        }

        ResponseListener(Class<? extends Command> commandClass, ObdObserver observer, boolean once) {
            Command command;
            request = "";
            try {
                command = commandClass.newInstance();
                request = command.getRequest();
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
            this.observer = observer;
            this.once = once;
        }

        String getRequest() {
            return request;
        }

        ObdObserver getObserver() {
            return observer;
        }

        boolean isOnce() {
            return once;
        }
    }

    /**
     * A class to keep track of which must be scheduled, how ofter and when it have been run for the last time
     */
    private class Schedule {
        private Command command;
        private double frequency;
        private Date lastRun;

        Schedule(Command command, double frequency) {
            this.command = command;
            this.frequency = frequency;
        }

        double getFrequency() {
            return frequency;
        }

        public Command getCommand() {
            return command;
        }

        /**
         * Mark the schedule as just ran.
         */
        void hasRun() {
            lastRun = GregorianCalendar.getInstance().getTime();
        }

        /**
         * Check if the schedule can be execute based on the last execution time and the frequency
         *
         * @return {@code true} if the execution time + the frequency is less than right now
         */
        public boolean canRun() {
            if (lastRun == null) {
                return true;
            }
            Date now = GregorianCalendar.getInstance().getTime();
            long elapse = now.getTime() - lastRun.getTime();
            return elapse > (frequency * 1000);
        }
    }
}
