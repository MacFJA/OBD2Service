package io.github.macfja.obd2.service;

import io.github.macfja.obd2.Command;
import io.github.macfja.obd2.Response;
import io.github.macfja.obd2.Unit;
import io.github.macfja.obd2.command.livedata.AirFlowRate;
import io.github.macfja.obd2.command.livedata.EngineRPM;
import io.github.macfja.obd2.command.livedata.IntakeAirTemperature;
import io.github.macfja.obd2.command.livedata.IntakeManifoldAbsolutePressure;
import io.github.macfja.obd2.response.CalculatedResponse;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A multi-command implementation for OBD2 that doesn't support the command {@code 0110}.
 * <p>
 * It use result of 3 sub command.
 * <p>
 * Based on Bruce D. Lightner article.
 *
 * @see <a href="http://www.lightner.net/obd2guru/IMAP_AFcalc.html">MAP- and MAF-Based Air/Fuel Flow Calculator by Bruce D. Lightner</a>
 */
abstract public class CalculatedMassAirFlowCommand extends AirFlowRate implements MultiCommandInterface {
    /**
     * The mass of air.
     * <p>
     * Condition:<ul>
     * <li>Temperature: 273 K</li>
     * <li>Air behavior: Same as an ideal gas</li>
     * <li>Pressure: 101.325 kPa</li></ul>
     * <p>
     * Unit: grams per moles (<code>g&middot;mol<sup>-1</sup></code>)
     */
    public static final Double MASS_OF_AIR = 28.9644;
    /**
     * The ideal gas constant.
     * <p>
     * Unit: litres, Kilo pascals product per moles per kelvins (<code>L&middot;kPa&middot;mol<sup>-1</sup>&middot;K<sup>-1</sup></code>)
     */
    public static final Double IDEAL_GAS_CONSTANT = 8.314472;

    private float displacement;

    private EngineRPM rpm = new EngineRPM();
    private IntakeManifoldAbsolutePressure imap = new IntakeManifoldAbsolutePressure();
    private IntakeAirTemperature iat = new IntakeAirTemperature();
    private Map<String, Response> responses = new HashMap<>();

    /**
     * Class Constructor.
     *
     * @param displacement The size of the engine (in litre)
     */
    public CalculatedMassAirFlowCommand(float displacement) {
        super();
        this.displacement = displacement;
    }

    /**
     * Get the response of a single sub command
     *
     * @param command The sub command to lookup
     * @return The response associated to the command
     */
    private Response getR(Command command) {
        return responses.get(command.getRequest());
    }

    @Override
    public Response getResponse(byte[] rawResult) throws ScriptException {
        CalculatedResponse rpm = (CalculatedResponse) getR(this.rpm);
        return new CalculatedResponse(new byte[0], calculateMaf(calculateImap(), rpm.getCalculated().intValue(), displacement)) {
            @Override
            public Unit getUnit() {
                return Unit.GramPerSecond;
            }
        };
    }

    /**
     * Get the IMAP.
     * <p>
     * Calculated with the engine speed, the temperature of the air entering the engine, the pressure of air entering the engine.
     * <p>
     * The result is expressed in:
     * Kilo Pascal per minutes per kelvins
     * (Pressure variation in time and temperature)
     *
     * @return The IMAP value
     */
    private float calculateImap() {
        /*
        imap            = RPM    * MAP / IAT / 2

        min^-1 kPa K^-1 = min^-1 * kPa / K   / {no unit}
        */
        CalculatedResponse rpm = (CalculatedResponse) getR(this.rpm);
        CalculatedResponse map = (CalculatedResponse) getR(this.imap);
        CalculatedResponse iat = (CalculatedResponse) getR(this.iat);

        return rpm.getCalculated().intValue() * map.getCalculated().floatValue() / iat.getCalculated().floatValue() / 2;
    }

    /**
     * The calculated Mass Air Flow.
     * <p>
     * The result is expressed in:
     * Grams per seconds
     * (Mass variation in time)
     *
     * @param imap         The value of the IMAP (see {@link #calculateImap()})
     * @param rpm          The engine RPM (used to get the {@link #getVolumetricEfficiency(int)})
     * @param displacement The size of the engine (in litre)
     * @return The mass of air per second
     */
    private double calculateMaf(float imap, int rpm, float displacement) {
        /*
                      imap       VolumetricEfficiency(rmp)                     Mass of air
        map    =      ----     * ------------------------- * Displacement * ------------------
                       60                   100                             Ideal Gas constant



                                                                                g mol^-1
        g s^-1 = s^-1 kPa K^-1 *         {no unit}         *        L     * ------------------
                                                                            L kPa mol^-1 K^-1
         */

        return (imap / 60) * (getVolumetricEfficiency(rpm) / 100) * (displacement) * (MASS_OF_AIR / IDEAL_GAS_CONSTANT);
    }

    @Override
    public List<Command> getCommands() {
        List<Command> commands = new ArrayList<>();
        commands.add(rpm);
        commands.add(imap);
        commands.add(iat);
        return commands;
    }

    @Override
    public void setResponse(String request, Response response) {
        responses.put(request, response);
    }

    /**
     * Get the efficiency of the engine at a specific RPM
     *
     * @param rpm The RPM of the engine
     * @return The efficiency (0% = 0, 50% = 50, 100% = 100)
     */
    abstract public double getVolumetricEfficiency(int rpm);
}
