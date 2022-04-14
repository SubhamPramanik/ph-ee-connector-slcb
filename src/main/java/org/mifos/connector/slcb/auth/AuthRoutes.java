package org.mifos.connector.slcb.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.mifos.connector.slcb.config.SLCBConfig;
import org.mifos.connector.slcb.dto.AccessTokenDTO;
import org.mifos.connector.slcb.dto.AuthErrorDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static org.mifos.connector.slcb.camel.config.CamelProperties.SLCB_ACCESS_TOKEN;

@Component
public class AuthRoutes extends RouteBuilder {

    @Autowired
    public SLCBConfig slcbConfig;

    @Autowired
    private ObjectMapper objectMapper;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void configure() throws Exception {

        /**
         * Error handling route
         */
        from("direct:access-token-error")
                .id("access-token-error")
                .unmarshal().json(JsonLibrary.Jackson, AuthErrorDTO.class)
                .process(exchange -> {
                    logger.error("Error while fetching Access Token from server: " + exchange.getIn().getBody(AuthErrorDTO.class).getDescription());
                    // TODO: Improve Error Handling
                });

        /**
         * Save Access Token to AccessTokenStore
         */
        from("direct:access-token-save")
                .id("access-token-save")
                .unmarshal().json(JsonLibrary.Jackson, AccessTokenDTO.class)
                .process(exchange -> {
                    // TODO: Figure out access token storage if required
                    exchange.setProperty(SLCB_ACCESS_TOKEN, exchange.getIn().getBody(AccessTokenDTO.class).getToken());
                    logger.debug("Saved Access Token: " + exchange.getProperty(SLCB_ACCESS_TOKEN, String.class));
                });

        /**
         * Fetch Access Token from SLCB
         */
        from("direct:access-token-fetch")
                .id("access-token-fetch")
                .log(LoggingLevel.INFO, "Fetching access token")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader("Content-Type", constant("application/json"))
                .removeHeader(Exchange.HTTP_PATH)
                .setBody(exchange -> {
                    AccessTokenDTO slcbAuthRequest = new AccessTokenDTO(slcbConfig.username, slcbConfig.password);
                    try {
                        return objectMapper.writeValueAsString(slcbAuthRequest); // TODO: Might break things here
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                    return null;
                })
                .toD(slcbConfig.authUrl + "?bridgeEndpoint=true");

        /**
         * Access Token check validity and return value
         */
        from("direct:get-access-token")
                .id("get-access-token")
                .to("direct:access-token-fetch")
                .choice()
                .when(header("CamelHttpResponseCode").isEqualTo("200"))
                .log("Access Token Fetch Successful")
                .to("direct:access-token-save")
                .otherwise()
                .log("Access Token Fetch Unsuccessful")
                .to("direct:access-token-error");
    }
}
