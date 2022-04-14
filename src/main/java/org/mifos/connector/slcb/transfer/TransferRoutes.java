package org.mifos.connector.slcb.transfer;

import org.apache.camel.LoggingLevel;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.mifos.connector.slcb.dto.PaymentRequestDTO;
import org.mifos.connector.slcb.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.mifos.connector.slcb.camel.config.CamelProperties.*;

@Component
public class TransferRoutes extends BaseSLCBRouteBuilder {

    @Autowired
    private TransferResponseProcessor transferResponseProcessor;

    @Override
    public void configure() {

        /*
         * Base route for transactions
         */
        from("direct:transfer-route")
                .id("transfer-route")
                .log(LoggingLevel.INFO, "Transfer route started")
                .to("direct:get-access-token")
                .log(LoggingLevel.INFO, "Got access token, moving on")
                .log(LoggingLevel.INFO, "Moving on to API call")
                .to("direct:commit-transaction")
                .log(LoggingLevel.INFO, "Status: ${header.CamelHttpResponseCode}")
                .log(LoggingLevel.INFO, "Transaction API response: ${body}")
                .to("direct:transaction-response-handler");

        /*
         * Route to handle async API responses
         */
        from("direct:transaction-response-handler")
                .id("transaction-response-handler")
                .choice()
                .when(header("CamelHttpResponseCode").isEqualTo("200"))
                .log(LoggingLevel.INFO, "Transaction request successful")
                .unmarshal().json(JsonLibrary.Jackson, PaymentRequestDTO.class)
                .process(exchange ->
                        logger.info("Status: " + exchange.getIn().getBody(PaymentRequestDTO.class).getStatus()))
                .otherwise()
                .log(LoggingLevel.ERROR, "Transaction request unsuccessful")
                .process(exchange -> {
                    exchange.setProperty(TRANSACTION_ID, exchange.getProperty(CORRELATION_ID)); // TODO: Improve this
                })
                .setProperty(TRANSACTION_FAILED, constant(true))
                .process(transferResponseProcessor);

        /*
         * Calls SLCB API to commit transaction
         */
        getBaseAuthDefinitionBuilder("direct:commit-transaction", HttpRequestMethod.POST)
                .process(exchange -> {
                    // TODO: Verify the exact content to be signed from SLCB team.
                    String signedBody = SecurityUtils.signContent(
                            exchange.getProperty(SLCB_CHANNEL_REQUEST, String.class), slcbConfig.signatureKey);
                    PaymentRequestDTO slcbChannelRequestBody = objectMapper.readValue(
                            exchange.getProperty(SLCB_CHANNEL_REQUEST, String.class), PaymentRequestDTO.class);
                    slcbChannelRequestBody.setAuthorizationCode(signedBody);
                    exchange.setProperty(SLCB_CHANNEL_REQUEST, slcbChannelRequestBody);
                })
                .setBody(exchange -> exchange.getProperty(SLCB_CHANNEL_REQUEST))
                .log(LoggingLevel.INFO, "Transaction Request Body: ${body}")
                .toD(slcbConfig.transactionRequestUrl + "?bridgeEndpoint=true&throwExceptionOnFailure=false");

    }
}
