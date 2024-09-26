package api.common;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public class StubServer
{
    public static void main(String[] args) {
        WireMockServer wireMockServer = new WireMockServer(options().port(8089)
                .notifier(new ConsoleNotifier(true))); //call get api and back information
        wireMockServer.start();


    }
}
