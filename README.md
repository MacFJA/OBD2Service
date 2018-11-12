# OB2 Service

OBD2 Service is a small service around the library [io.github.macfja.obd2](https://github.com/MacFJA/OBD2).

## Usage

```java
import io.github.macfja.obd2.Command;
import io.github.macfja.obd2.Response;
import io.github.macfja.obd2.command.DTCsCommand;
import io.github.macfja.obd2.command.livedata.VehicleSpeed;
import io.github.macfja.obd2.elm327.Commander;
import io.github.macfja.obd2.response.MultipleDiagnosticTroubleCodeResponse;
import io.github.macfja.obd2.service.Obd2Service;
import io.github.macfja.obd2.service.ObdObserver;
import io.github.macfja.obd2.service.ObdObserverIgnoreError;
import io.github.macfja.obd2.service.Service;

public class Example {
    public static void main(String[] args) {
        new Example();
    }

    public Example()
    {
        Obd2Service service = new Service(new Commander());
        service.setCommunication(OBD.toComponent, OBD.fromComponent);

        service.schedule(VehicleSpeed.class, 5, new ObdObserver() {
            @Override
            public void update(Response response) {
                System.out.println(String.format("You are driving at: %s", response.getFormattedString()));
            }

            @Override
            public void error(Command request, Response response, Exception exception) {
                System.err.println(String.format("Unable to read vehicle speed: %s", exception.getLocalizedMessage()));
            }
        });

        service.addObserver(DTCsCommand.class, new ObdObserverIgnoreError() {
            @Override
            public void update(Response response) {
                if (response instanceof MultipleDiagnosticTroubleCodeResponse) {
                    System.out.println(String.format(
                            "DTCs have been requested. Here the result: %d DTC",
                            ((MultipleDiagnosticTroubleCodeResponse) response).getTroubleCodes().size()
                    ));
                }
            }
        });
    }
}
```

## Installation

To install this library you need to first have [io.github.macfja.obd2](https://github.com/MacFJA/OBD2) available.
The library is mandatory. See [OBD2 for Java README](https://github.com/MacFJA/OBD2) for more detail on how install it.
(Notice: JitPack will not work, because the groupId and artifactId are changed)

### From the sources

Clone the project:
```
git clone https://github.com/MacFJA/OBD2Service.git
```
Install the project into your local Maven repository:
```
cd OBD2Service/
mvn clean
mvn install
```
Remove the source:
```
cd ..
rm -r OBD2Service/
```
Add the dependency in your Maven project:
```xml
<project>
    <!-- ... -->
    <dependencies>
        <!-- ... -->
        <dependency>
            <groupId>io.github.macfja</groupId>
            <artifactId>obd2-service</artifactId>
            <version>1.0.0</version>
        </dependency>
        <!-- ... -->
    </dependencies>
    <!-- ... -->
</project>
```

### From a release

Go to the [releases page](https://github.com/MacFJA/OBD2Service/releases), and download the **jar**.

Next add the **jar** in your project classpath.