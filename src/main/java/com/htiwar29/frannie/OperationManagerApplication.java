package com.htiwar29.frannie;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.bedrock.converse.BedrockProxyChatModel;
import org.springframework.ai.bedrock.titan.BedrockTitanEmbeddingModel;
import org.springframework.ai.bedrock.titan.api.TitanEmbeddingBedrockApi;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.mongodb.atlas.MongoDBAtlasVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
public class OperationManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(OperationManagerApplication.class, args);
	}

	AwsBasicCredentials awsCredentials = AwsBasicCredentials.create("abc",
			"def");

	@Bean
	@Primary
	public EmbeddingModel embeddingModel() {
		var abc = new TitanEmbeddingBedrockApi("amazon.titan-embed-text-v1", StaticCredentialsProvider.create(awsCredentials), "us-east-1", new ObjectMapper(), Duration.ofSeconds(1000));
		return new BedrockTitanEmbeddingModel(abc, ObservationRegistry.create());
	}

	@Bean
	@Primary
	@Qualifier("customVectorStore")
	public VectorStore customVectorStore(MongoTemplate mongoTemplate, EmbeddingModel embeddingModel) {
		return MongoDBAtlasVectorStore.builder(mongoTemplate, embeddingModel).build();
	}

	@Bean
	ChatClient chatClient(VisitRepository visitRepository, @Qualifier("customVectorStore") VectorStore vectorStore) {


//		visitRepository.findAll().forEach(visit -> vectorStore.add(List.of(new Document(visit.toString()))));

		var systemPrompt = """
				You are a highly intelligent AI assistant supporting Franchise Business Consultants (FBCs).
				Your role is to analyze store visit records and provide structured, actionable insights.

				Responsibilities:
					Visit Analysis: Carefully review the structured and unstructured data associated with a store visit.
					Summary Generation: Create a concise and well-structured summary of the visit, including:
						 Key observations
						 Performance highlights and issues
						 Action items discussed during the visit
					Recommendations: Suggest improvements or follow-up actions aligned with industry best practices in franchise operations.
					Task Suggestions: Recommend specific tasks the FBC or franchisee can create based on visit insights (e.g., compliance fixes, training needs).
					Question Answering: Accurately answer any questions related to the visit using the provided context.

				Tone & Format Guidelines:
					Be professional, concise, and helpful.
					Use bullet points or structured sections for clarity.
					Where appropriate, include “Recommendations” and “Next Steps” sections.
					Always provide a clear and concise response, avoiding overly complex language or jargon.
				    I currently do not have access to the requested visit information. Please provide the relevant visit details so I can assist you further. Avoid speculating or explaining general processes unless specifically asked.
				""";

		AwsBasicCredentials awsCredentials = AwsBasicCredentials.create("abc",
				"def");

		BedrockProxyChatModel chatModel = BedrockProxyChatModel.builder()
				.credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
				.region(Region.US_EAST_1)
				.build();

		return ChatClient.builder(chatModel).defaultSystem(systemPrompt).build();
	}

	@Bean
	ChatOptions chatOptions() {
		return ChatOptions.builder()
				.model("anthropic.claude-3-5-sonnet-20240620-v1:0")
				.build();
	}

}

@RestController
class ChatController {

	private final ChatClient chatClient;

	private final ChatOptions chatOptions;

	private final Map<String, PromptChatMemoryAdvisor> advisorMap = new ConcurrentHashMap<>();

	private final QuestionAnswerAdvisor questionAnswerAdvisor;


	public ChatController(ChatClient chatClient, ChatOptions chatOptions, @Qualifier("customVectorStore") VectorStore vectorStore) {
		this.chatClient = chatClient;
		this.chatOptions = chatOptions;
		this.questionAnswerAdvisor = QuestionAnswerAdvisor.builder(vectorStore).searchRequest(SearchRequest.builder().topK(11).build()).build();
	}

	@GetMapping("{userId}/chat")
	public String chat(@PathVariable String userId, @RequestParam String question) {
		var advisor = advisorMap.computeIfAbsent(userId, map -> PromptChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().build()).build());

		var abc = chatClient.
				prompt()
				.options(chatOptions)
				.user(question)
				.advisors(advisor, questionAnswerAdvisor)
				.call()
				.content();

		return abc;
	}

}


@Repository
interface VisitRepository extends MongoRepository<VisitDocument, String> {  }

@org.springframework.data.mongodb.core.mapping.Document(collection = "visits")
record VisitDocument(
		@Id String visitId,
		String visitNo,
		String franchisee,
		String consultant,
		String status,
		String completionDate,
		String startDate,
		String scheduledDate,
		String formName,
		String totalScore,
		List<Form> form
) {}

record Form(
		String name,
		List<Question> questions
) {}

record Question(
		String question,
		String answer,
		String score,
		String comments
) {}
