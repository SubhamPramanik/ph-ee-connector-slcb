package org.mifos.connector.slcb.transfer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.mifos.connector.slcb.config.SLCBConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.mifos.connector.slcb.camel.config.CamelProperties.SLCB_ACCESS_TOKEN;

abstract public class BaseSLCBRouteBuilder extends RouteBuilder {

    @Autowired
    public ObjectMapper objectMapper;

    @Autowired
    public SLCBConfig slcbConfig;

    public Logger logger = LoggerFactory.getLogger(this.getClass());

    public RouteDefinition getBaseAuthDefinitionBuilder(String camelDslString, HttpRequestMethod httpMethod) {
        return from(camelDslString)
                .removeHeader("*")
                .setHeader(Exchange.HTTP_METHOD, constant(httpMethod.text))
                .setHeader("X-Date", simple(ZonedDateTime.now( ZoneOffset.UTC ).format( DateTimeFormatter.ISO_INSTANT )))
                .setHeader("Authorization", simple("Bearer ${exchangeProperty."+SLCB_ACCESS_TOKEN+"}"))
                .setHeader("Content-Type", constant("application/json"));
    }

    protected enum HttpRequestMethod {
        GET("GET"),
        POST("POST"),
        PUT("PUT"),
        DELETE("DELETE")
        ;

        private final String text;

        HttpRequestMethod(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

}
