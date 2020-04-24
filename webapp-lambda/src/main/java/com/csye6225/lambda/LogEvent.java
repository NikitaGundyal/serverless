package com.csye6225.lambda;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;

public class LogEvent implements RequestHandler<SNSEvent, Object> {
	
	private DynamoDB dynamoDB;
	private final String TABLE_NAME = "csye6225";
	private Regions REGION = Regions.US_EAST_1;
	static final String DOMAIN = System.getenv("Domain");
	static final String SUBJECT = "Bills Due";
	private String textPart;
	private String htmlPart;
	private String fromAddress="";
	private String username;
	private JSONArray billIds;
	private static String token;


	@Override
	public Object handleRequest(SNSEvent input, Context context) {
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
        context.getLogger().log("domain"+DOMAIN);
        fromAddress = "noreply@test." + DOMAIN;

        //Creating ttl
        context.getLogger().log("TTL Invocation Started: " + timeStamp);
        long secondsSinceInvocationStarted = Calendar.getInstance().getTimeInMillis()/1000; 
        long ttl_60_minutes = 60 * 60; // 
        long totalttl = ttl_60_minutes + secondsSinceInvocationStarted ;

        //Function Excecution for sending the email
        
        try {
            JSONObject body = new JSONObject(input.getRecords().get(0).getSNS().getMessage());
            username=body.getString("username");
            billIds=body.getJSONArray("billIds");
            context.getLogger().log("Bills Due request for username: "+username);
            context.getLogger().log("Bills Due Id's are: "+billIds);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    
        token = UUID.randomUUID().toString();
        context.getLogger().log("TTL Invocation Completed: " + timeStamp);
        
        try {
            initDynamoDbClient();
            long ttlDynamoDb = 0;
            Item item = this.dynamoDB.getTable(TABLE_NAME).getItem("id", username);
            if (item != null) {
                context.getLogger().log("Checking for timestamp");
                ttlDynamoDb = item.getLong("ttl");
            }

            if (item == null || (ttlDynamoDb < ttl_60_minutes && ttlDynamoDb != 0)) {
                context.getLogger().log("Checking for if TTL is valid");
                context.getLogger().log("TTL has been expired. So creating a new one and sending the email");
                this.dynamoDB.getTable(TABLE_NAME)
                        .putItem(
                                new PutItemSpec().withItem(new Item()
                                        .withString("id", username)
                                        .withString("token", token)
                                        .withLong("ttl", totalttl)));
                StringBuilder billIdsforEmail = new StringBuilder();
                for (int i=0; i < billIds.length(); i++){
                	billIdsforEmail.append(DOMAIN +  "/v1/bill/"+billIds.get(i) + System.lineSeparator());
                }
                
                textPart = "Hello "+username+ "\n You have created the following bills due . The urls are as below \n Links : "+billIdsforEmail;
                context.getLogger().log("Text " + textPart);
                htmlPart = "<h2>Email sent from Amazon SES</h2>"+"<p>The url to view your bills are :"+"Link: "+billIdsforEmail+"</p>";
                context.getLogger().log("This is HTML body: " + htmlPart);


                //Sending email using Amazon SES client
                AmazonSimpleEmailService clients = AmazonSimpleEmailServiceClientBuilder.standard()
                        .withRegion(REGION).build();
                SendEmailRequest emailRequest = new SendEmailRequest()
                        .withDestination(
                                new Destination().withToAddresses(username))
                        .withMessage(new Message()
                                .withBody(new Body()
                                        .withHtml(new Content()
                                                .withCharset("UTF-8").withData(htmlPart))
                                        .withText(new Content()
                                                .withCharset("UTF-8").withData(textPart)))
                                .withSubject(new Content()
                                        .withCharset("UTF-8").withData(SUBJECT)))
                        .withSource(fromAddress);
                clients.sendEmail(emailRequest);
                context.getLogger().log("Email sent successfully to email id: " +username);

            } else {
                context.getLogger().log("TTL is not expired. New request is not processed for the user: " +username+" Please try after sometime");
            }
        } catch (Exception ex) {
            context.getLogger().log("Email was not sent. Error message: " + ex.getMessage());
        }
        return null;
		
	}
	
	private void initDynamoDbClient() {
		AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(REGION)
                .build();
		dynamoDB = new DynamoDB(client);
	}

}
