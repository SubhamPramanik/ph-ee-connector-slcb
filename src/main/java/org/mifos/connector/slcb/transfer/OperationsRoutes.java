package org.mifos.connector.slcb.transfer;

import org.apache.camel.LoggingLevel;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.mifos.connector.slcb.dto.BalanceRequestDTO;
import org.mifos.connector.slcb.utils.SecurityUtils;
import org.springframework.stereotype.Component;

import static org.mifos.connector.slcb.camel.config.CamelProperties.*;

@Component
public class OperationsRoutes extends BaseSLCBRouteBuilder {

    @Override
    public void configure() {

        /*
         * Base route for fetching account balance
         */
        from("direct:account-balance-route")
                .id("direct:account-balance-route")
                .log(LoggingLevel.INFO, "Transfer route started")
                .to("direct:get-access-token")
                .log(LoggingLevel.INFO, "Got access token, moving on")
                .log(LoggingLevel.INFO, "Moving on to API call")
                .to("direct:get-account-balance")
                .log(LoggingLevel.INFO, "Status: ${header.CamelHttpResponseCode}")
                .log(LoggingLevel.INFO, "Fetch Balance API response: ${body}")
                .to("direct:balance-response-handler");


        /*
         * Calls SLCB API to fetch balance
         */
        getBaseAuthDefinitionBuilder("direct:get-account-balance", HttpRequestMethod.GET)
                .process(exchange -> {
                    // TODO: Verify the exact content to be signed from SLCB team.
                    String signedBody = SecurityUtils.signContent(
                            exchange.getProperty(SLCB_BALANCE_REQUEST, String.class), slcbConfig.signatureKey);
                    BalanceRequestDTO balanceRequestDTO = objectMapper.readValue(
                            exchange.getProperty(SLCB_CHANNEL_REQUEST, String.class),
                            BalanceRequestDTO.class);
                    balanceRequestDTO.setAuthorizationCode(signedBody);
                    exchange.setProperty(SLCB_BALANCE_REQUEST, balanceRequestDTO);
                })
                .setBody(exchange -> exchange.getProperty(SLCB_BALANCE_REQUEST))
                .log(LoggingLevel.INFO, "Transaction Request Body: ${body}")
                .toD(slcbConfig.accountBalanceUrl + "?bridgeEndpoint=true&throwExceptionOnFailure=false");

        /*
         * Route to handle account balance async API responses
         */
        from("direct:balance-response-handler")
                .id("balance-response-handler")
                .choice()
                .when(header("CamelHttpResponseCode").isEqualTo("200"))
                .log(LoggingLevel.INFO, "Balance request successful")
                .unmarshal().json(JsonLibrary.Jackson, BalanceRequestDTO.class)
                .process(exchange -> {
                    BalanceRequestDTO response = exchange.getIn().getBody(BalanceRequestDTO.class);
                    logger.info("Status: " + response.getStatus());
                    exchange.setProperty("BODY", response.getAmount());
                })
                .otherwise()
                .log(LoggingLevel.ERROR, "Balance request unsuccessful")
                .process(exchange -> exchange.setProperty("BODY", "Fetch balance request unsuccessful"))
                .setBody(exchange -> exchange.getProperty("BODY"));
    }
}
