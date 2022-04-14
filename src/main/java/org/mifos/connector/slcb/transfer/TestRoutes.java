package org.mifos.connector.slcb.transfer;

import org.apache.camel.builder.RouteBuilder;
import org.mifos.connector.slcb.zeebe.ZeebeProcessStarter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class TestRoutes extends RouteBuilder {

    @Autowired
    private ZeebeProcessStarter zeebeProcessStarter;

    @Override
    public void configure() throws Exception {
        from("rest:get:/start")
                .id("slcb-flow-start")
                .process(exchange -> {
                    Map<String, Object> variables = new HashMap<>();
                    variables.put("transactionId", UUID.randomUUID());
                    zeebeProcessStarter.startZeebeWorkflow("SLCB", variables);
                })
                .setBody(constant("Started"));
    }
}
