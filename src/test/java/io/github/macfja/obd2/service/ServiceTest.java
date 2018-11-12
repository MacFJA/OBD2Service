package io.github.macfja.obd2.service;

import io.github.macfja.obd2.Command;
import io.github.macfja.obd2.Response;
import io.github.macfja.obd2.SimpleCommands;
import io.github.macfja.obd2.Unit;
import io.github.macfja.obd2.command.livedata.EngineCoolantTemperature;
import io.github.macfja.obd2.command.livedata.EngineRPM;
import io.github.macfja.obd2.commander.CommanderInterface;
import io.github.macfja.obd2.elm327.response.ResponseOK;
import io.github.macfja.obd2.exception.ExceptionResponse;
import org.junit.Test;

import javax.script.ScriptException;
import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ServiceTest {

    protected CommanderInterface mockCommanderResponding(Response expectedResponse)
    {
        CommanderInterface commander = mock(CommanderInterface.class);
        try {
            when(commander.sendCommand(any(Command.class))).thenReturn(expectedResponse);
        } catch (IOException | ScriptException | ExceptionResponse e) {
            fail();
        }

        return commander;
    }

    @Test
    public void addObserver() {
        Response expectedResponse = new ResponseOK("OK".getBytes());

        CommanderInterface commander = mockCommanderResponding(expectedResponse);

        Service service = new Service(commander);
        service.setCommunication(null, null);

        ObdObserver observerRpm = mock(ObdObserver.class);
        ObdObserver observerCoolant = mock(ObdObserver.class);

        service.addObserver(EngineRPM.class, observerRpm);
        service.addObserver(EngineCoolantTemperature.class, observerCoolant);

        service.run(new EngineRPM());

        verify(observerRpm).update(expectedResponse);
        verify(observerCoolant, times(0)).update(expectedResponse);
    }


    @Test
    public void schedule() {
        Response expectedResponse = new ResponseOK("OK".getBytes());

        CommanderInterface commander = mockCommanderResponding(expectedResponse);

        Service service = new Service(commander);
        service.setCommunication(null, null);

        ObdObserver observer = mock(ObdObserver.class);

        service.schedule(SimpleCommands.create("FAKE"), 2, observer);

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            fail();
        }

        verify(observer, times(2)).update(expectedResponse);
    }

    @Test
    public void scheduleSame() {
        Response expectedResponse = new ResponseOK("OK".getBytes());
        Command toRun = SimpleCommands.create("FAKE");

        CommanderInterface commander = mockCommanderResponding(expectedResponse);

        Service service = spy(new Service(commander));
        service.setCommunication(null, null);

        ObdObserver observer = mock(ObdObserver.class);

        service.schedule(toRun, 21, observer);
        service.schedule(toRun, 24);

        try {
            Thread.sleep(8000);
        } catch (InterruptedException e) {
            fail();
        }

        verify(observer, times(2)).update(expectedResponse);
    }

    @Test
    public void isReady() {
        CommanderInterface commander = mock(CommanderInterface.class);
        Service service = new Service(commander);
        service.setCommunication(null, null);

        assertTrue(service.isReady());
        verify(commander).setCommunicationInterface(null, null);
    }

    @Test
    public void run() {
        CommanderInterface commander = mock(CommanderInterface.class);
        Command command = SimpleCommands.create("TEST");
        try {
            when(commander.sendCommand(command)).thenReturn(new ResponseOK("OK".getBytes()));
        } catch (IOException | ScriptException | ExceptionResponse e) {
            fail();
        }
        Service service = new Service(commander);
        Response response = service.run(command);

        assertEquals("OK", response.getFormattedString());
        assertEquals(Unit.Unknown.getClass(), response.getUnit().getClass());
    }

    @Test
    public void onReady() {
        CommanderInterface commander = mock(CommanderInterface.class);
        Service service = new Service(commander);
        Runnable action = mock(Runnable.class);
        service.onReady(action);

        verify(action, times(0)).run();

        service.setCommunication(null, null);

        verify(action).run();
    }
}