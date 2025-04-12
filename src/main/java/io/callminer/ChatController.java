package io.callminer;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.awssdk.services.bedrock.model.*;
import software.amazon.awssdk.services.opensearch.OpenSearchClient;
import software.amazon.awssdk.services.opensearch.model.*;

import java.util.Map;

@Controller("/chat")
public class ChatController {

    private final OpenSearchClient openSearchClient;
    private final BedrockClient bedrockClient;

    public ChatController(OpenSearchClient openSearchClient, BedrockClient bedrockClient) {
        this.openSearchClient = openSearchClient;
        this.bedrockClient = bedrockClient;
    }

    @Get
    public String getChatResponse(@QueryValue String query) {
        // Step 1: Query OpenSearch for context
        SearchRequest searchRequest = SearchRequest.builder()
                .index("your-index-name")
                .query(q -> q.match(m -> m.field("content").query(query)))
                .build();

        SearchResponse searchResponse = openSearchClient.search(searchRequest);
        String context = searchResponse.hits().hits().stream()
                .map(hit -> hit.source().toString())
                .reduce("", (acc, item) -> acc + "\n" + item);

        // Step 2: Send request to Bedrock LLM
        InvokeModelRequest modelRequest = InvokeModelRequest.builder()
                .modelId("your-model-id")
                .body(context + "\nQuery: " + query)
                .build();

        InvokeModelResponse modelResponse = bedrockClient.invokeModel(modelRequest);
        String llmResponse = modelResponse.body().asUtf8String();

        // Step 3: Return the response
        return llmResponse;
    }
}
